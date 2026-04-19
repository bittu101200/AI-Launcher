# AI Smart Terminal Launcher — Design Spec
**Date:** 2026-04-18  
**Revision:** 2 (post code-review)  
**Status:** Approved  
**Scope:** Phase 1 implementation + architectural scaffolding for Phases 2 & 3

---

## Context

T-UI Linux CLI Launcher is a well-architected Android terminal-style home screen launcher with 47 built-in commands, a reflection-based command registry, broadcast-based IPC, and OkHttp already wired in. It has zero existing AI integrations — a clean foundation.

The goal is to transform it into an **agentic OS precursor**: a launcher where AI is the cognitive layer woven through every subsystem, not bolted on. The AI understands installed apps, executes commands on the user's behalf, interacts with third-party apps via their exposed APIs, and scales from Phase 1 (natural language → commands) through Phase 3 (ambient, proactive intelligence) — each phase clicking in like a lego piece without reworking what came before.

**Mobile efficiency and user safety are first-class constraints throughout.**

**Platform target:** Android 16-17 is the primary optimization target. The design should prefer new supported system APIs from these releases when they improve launcher cohesion, but it must not assume system-privileged agent powers that third-party launchers do not have.

### Android 16-17 Platform Alignment

The launcher should lean into the stable system surfaces that already make a home app feel more OS-native:
- `LauncherApps` + `ShortcutManager` for published app shortcuts and cross-profile launcher discovery
- `AppWidgetHost` for home-surface widgets rather than AI-only custom panels
- `AppSearch` for a fast on-device index of apps, commands, shortcuts, integrations, and recent AI artifacts
- `Notification.ProgressStyle` / Live Update semantics for ongoing "journey" tasks rather than bespoke long-running overlays
- `OnBackInvokedDispatcher` / predictive back for all launcher activities and overlays on Android 16+
- `Intent.ACTION_PICK_CONTACTS` on Android 17+ for person disambiguation without broad contacts access
- `Handoff` on Android 17+ as the preferred future cross-device continuity surface

The design also keeps a watchlist for newer or constrained platform directions:
- Android 17 `App Functions` is architecturally aligned with the long-term goal, but remains experimental and appears oriented toward trusted/system-privileged agents; the launcher must not depend on it for Phase 1 or 2
- Android 17 assistant audio routing (`USAGE_ASSISTANT`, `MODE_ASSISTANT_CONVERSATION`) is relevant only if the launcher later grows into a true voice assistant surface
- Android 16/17 local-network protections are treated as opt-in gates for future nearby/LAN discovery, not assumed baseline capability

---

## Architecture Philosophy

The `AISubsystem` is initialized at startup alongside all other managers and injected into `MainPack` — making it available to every command, manager, and service, exactly like `OkHttpClient` is already shared. No existing manager is rewritten. Each gets a small, optional AI-aware enhancement that degrades gracefully when AI is disabled, offline, or the battery is low.

```
LauncherActivity
  └── initializes AISubsystem (lazy-loaded components)
        └── injected into MainPack
              └── available to all commands, managers, services

AISubsystem
  ├── AIProvider (interface — Claude, OpenAI, Ollama, Mock)
  ├── ToolRegistry (3 tiers — allowlisted TUI commands, curated app intents, system tools)
  ├── ConversationManager (stateless / session / persistent + CompactionEngine)
  ├── RequestManager (lifecycle, requestId, state machine)
  ├── LauncherIndex (AppSearch-backed on-device index)
  ├── ShortcutBridge (LauncherApps/ShortcutManager adapter)
  ├── JourneyManager (progress-centric notifications / live updates)
  └── AISettings (via XMLPrefsManager → ai.xml)

Existing system (minimally modified):
  MainManager         ← AITrigger added to trigger chain (exclusive ownership)
  SuggestionsManager  ← AI-ranked completions hook
  NotificationService ← AI triage hook
  RssManager          ← AI summarization hook
  UIManager           ← streaming output + thinking indicator
  AppsManager         ← context-aware app ranking hook
  AliasManager        ← AI alias suggestion hook
  HTMLExtractManager  ← AI content extraction hook
  AppCapabilityScanner← dynamic app tool discovery (Phase 1: shortcuts + launch intents)
```

---

## Section 1: AISubsystem & Request Lifecycle

**File:** `ai/AISubsystem.java`, `ai/RequestManager.java`, `ai/AIRequestState.java`, `ai/platform/LauncherIndex.java`, `ai/platform/ShortcutBridge.java`, `ai/platform/JourneyManager.java`

### AISubsystem
Single entry point. Initialized once in `LauncherActivity.finishOnCreate()`, stored in `MainPack`. All components are **lazily initialized** — only constructed on first use to minimize startup overhead.

**Access patterns — two paths, defined explicitly:**
- **Commands and MainManager** — receive `AISubsystem` via `MainPack` (the existing context-object pattern)
- **Services and managers created independently** (`NotificationService`, `KeeperService`, `RssManager`, etc.) — access via `AISubsystem.getInstance()`. The singleton reference is set during `LauncherActivity.finishOnCreate()` and cleared in `dispose()`. Services check `getInstance() != null` before use; null means the launcher is not yet initialized and the AI hook is skipped silently.

This avoids threading Application subclass changes into the existing architecture while giving all consumers a well-defined access path.

```java
public class AISubsystem {
    // Singleton access for services/managers without MainPack
    public static AISubsystem getInstance();

    public String submit(AIRequest request, AICallback callback); // returns requestId
    public void cancel(String requestId);
    public boolean isInFlight();
    public ToolRegistry getToolRegistry();
    public ConversationManager getConversationManager();
    public LauncherIndex getLauncherIndex();
    public ShortcutBridge getShortcutBridge();
    public JourneyManager getJourneyManager();
    public boolean isAvailable();
    public void dispose(); // clears singleton reference
}
```

All AI calls are async and return a `requestId` immediately. Results are delivered via callback. `isInFlight()` is checked by `MainManager` to intercept `stop`/bare-Enter cancellation.

### Request Lifecycle State Machine

Every AI interaction follows an explicit state machine tracked by `RequestManager`. Every UI update, stream chunk, tool result, and callback carries the `requestId` — stale callbacks from a previous request are silently dropped if `requestId` does not match the current active request.

```
IDLE
  │ submit(request)
  ▼
THINKING         — [thinking...] shown in terminal
  │ first byte received within connect timeout
  ▼
STREAMING        — tokens rendered progressively
  │ response complete / provider non-streaming
  ▼
EXECUTING_TOOLS  — tool calls dispatched one by one with confirmation gate
  │ all tools resolved, results collected
  ▼
FOLLOWUP         — results sent back to AI for final response
  │ final response received
  ▼
COMPLETED        — terminal updated, history appended

  (from any state)
  ├── user types "stop"/bare Enter     → CANCELLED  (input consumed, terminal shows [cancelled])
  ├── connect timeout exceeded         → TIMED_OUT_CONNECT
  ├── inactivity timeout exceeded      → TIMED_OUT_INACTIVITY  (partial output preserved)
  └── provider error / bad response    → FAILED (error shown, history not appended)

TIMED_OUT_CONNECT:
  AITrigger already claimed the input — it cannot un-claim it.
  AITrigger explicitly re-dispatches the original raw input string directly to
  ShellCommandTrigger.trigger() (bypassing the full chain, not re-entering it).
  Terminal shows: [AI unavailable — retrying as shell command]
  This is a defined re-dispatch, not a fallthrough race. The full trigger chain
  never sees the input a second time.
  If always_on_fallback=false, re-dispatch is skipped and terminal shows [AI timeout] only.
```

**State transitions are single-threaded** via a dedicated handler thread in `RequestManager`. UI updates post back to the main thread via `Handler(Looper.getMainLooper())`, matching the existing pattern in `UIManager`.

---

## Section 2: AIProvider Interface

**Files:** `ai/AIProvider.java`, `ai/providers/`

```java
public interface AIProvider {
    void complete(AIRequest request, String requestId, AICallback callback);
    boolean supportsToolUse();
    boolean supportsStreaming();
    String providerId(); // "claude", "openai", "ollama", "mock"
}
```

**Implementations:**
- `ClaudeProvider.java` — Anthropic API. Uses prompt caching for system prompt + tool schemas on repeated calls. Supports streaming.
- `OpenAIProvider.java` — OpenAI-compatible. `base_url` is configurable. Three supported Ollama networking scenarios on Android:
  - **On-device Ollama** (e.g., via Termux): `base_url = http://localhost:11434` — `localhost` refers to the Android device itself
  - **Developer machine over USB**: run `adb reverse tcp:11434 tcp:11434` on the host, then `base_url = http://localhost:11434` on device
  - **LAN server**: `base_url = http://192.168.x.x:11434` (host machine's LAN IP)
  
  `localhost:11434` is never assumed to be the developer machine — users must configure the appropriate address. The settings screen prompts for `base_url` when `provider=ollama` is selected.
- `MockProvider.java` — Returns scripted responses. No network, no API key. Used for offline testing and CI.

Provider is selected and configured via `ai.xml`. Switching providers requires no code changes.

**Request/Response models:**

```java
public class AIRequest {
    String requestId;
    List<ConversationTurn> history;
    String userMessage;
    List<Tool> tools;           // from ToolRegistry (allowlisted, compact form)
    String systemPrompt;
    int maxTokens;              // capped for mobile efficiency
}

public class AIResponse {
    String requestId;           // must match active request or is dropped
    enum Type { TEXT, TOOL_CALLS, ERROR }
    Type type;
    String text;
    List<ToolCall> toolCalls;
}
```

---

## Section 3: ToolRegistry & Tool Safety

**Files:** `ai/ToolRegistry.java`, `ai/Tool.java`, `ai/ToolRiskClass.java`

### Tool Metadata
Every tool carries a risk class. This drives the confirmation gate in `EXECUTING_TOOLS` state.

```java
public enum ToolRiskClass {
    READ_ONLY,           // safe, silent execution (e.g., status, help, list apps)
    LAUNCH_ONLY,         // opens an app or activity, no data written (e.g., open Spotify)
    STATE_CHANGING,      // modifies device state (e.g., brightness, volume, wifi toggle)
    SENSITIVE,           // touches contacts, storage, notifications
    REQUIRES_CONFIRMATION // always prompt regardless of other rules (e.g., call, send)
}
```

Confirmation policy per risk class:

| Risk Class | Silent execution | Requires preview | Requires confirmation |
|---|---|---|---|
| READ_ONLY | yes | — | — |
| LAUNCH_ONLY | — | yes (shows app name) | — |
| STATE_CHANGING | — | yes (shows what changes) | user can configure |
| SENSITIVE | — | — | always |
| REQUIRES_CONFIRMATION | — | — | always |

Preview format rendered in terminal before execution:
```
[AI wants to] open Spotify → search "focus music"
Run? (Enter / stop)
```

### Tier 1 — T-UI Command Allowlist (Phase 1)
**Not all 47 commands are exposed.** Phase 1 exposes only deterministic, low-ambiguity commands. Each is hand-annotated with a `ToolRiskClass`. Commands are added to the allowlist deliberately, not by default.

Phase 1 allowlist (representative, not exhaustive):

| Command | Risk Class |
|---|---|
| `status` | READ_ONLY |
| `help` | READ_ONLY |
| `apps` | READ_ONLY |
| `brightness` | STATE_CHANGING |
| `volume` | STATE_CHANGING |
| `wifi` | STATE_CHANGING |
| `calc` | READ_ONLY |
| `theme` | STATE_CHANGING |
| `alias` (read) | READ_ONLY |
| `music` play/pause/next | STATE_CHANGING |
| `notification` (read) | READ_ONLY |
| `call` | REQUIRES_CONFIRMATION |
| `rss` | READ_ONLY |

Commands that are interactive, ambiguous, destructive, or require shell prompting are excluded from Phase 1 and added deliberately in later phases.

Schemas are **compact**: parameter names and types only. Descriptions are generated once via AI on first run and cached — not regenerated on every launch.

### Tier 2 — App Intent Tools (Phase 1: launch intents + curated deep links only)
Populated by the `integrate` command. **Phase 1 scope is strictly limited to:**
- Published app shortcuts discovered through `LauncherApps` / `ShortcutManager`
- Explicit launch intents (`ACTION_VIEW`, `ACTION_MAIN`, `ACTION_SEARCH`)
- Curated deep link patterns from a known-good list

**Preference order:** If an app exposes a stable published shortcut for the requested action, the launcher prefers that over a generic launch intent because shortcuts are user-visible, app-authored, and more semantically precise.

**Deferred to Phase 2+:** Content providers, generic service discovery, `startService()` calls, accessibility-driven control. These have unpredictable behaviour across Android versions and OEM skins and are not safe to "probe" generically.

### Tier 3 — System Tools (Phase 2+)
Reserved. Phase 2 registers tools here without touching Tiers 1 or 2.

**Token budget management:** When context is tight, `ToolRegistry` returns a compressed set — Tier 2 and 3 dropped first, then descriptions stripped from Tier 1, keeping only names and parameter types.

---

## Section 4: ConversationManager

**File:** `ai/ConversationManager.java`

Three memory modes, configurable in `ai.xml`:

| Mode | Behavior | Storage |
|---|---|---|
| `stateless` | Each request is independent | None |
| `session` | History held in memory, cleared on restart | RAM only |
| `persistent` | History survives restarts, managed by CompactionEngine | `ai_history.json` in scoped storage |

**Memory management in Phase 1 is handled entirely by `CompactionEngine`** (Section 5). There is no naive rolling-window drop. When history exceeds `compact_threshold` (default 80% of `history_token_cap`), the launcher triggers compaction. Raw dropping of turns only occurs as a last-resort emergency fallback if compaction explicitly fails.

**Thread safety:** All read/write operations on history are synchronized. Serialization to disk is done on a background thread.

**Privacy:** Conversation history stored on-device is not encrypted at rest in Phase 1 (Android scoped storage provides OS-level isolation). Users can clear all history via `/clearhistory` command. What is sent to remote providers: the active history window + current message + tool schemas. Pinned turns are included. Nothing else leaves the device.

---

## Section 5: Context Compaction

**Files:** `ai/CompactionEngine.java`, `commands/main/raw/compact.java`, `commands/main/raw/pin.java`

### Overview
When conversation history grows long, a naive drop loses nuance — preferences stated once early on, app schemas the user spent time configuring, decisions made and why. `CompactionEngine` replaces dropping with intelligent, structure-preserving compression.

### Invocation & Decision Authority
**The launcher decides when compaction happens.** The AI does not decide — it helps produce the summary. The only two triggers are:

- **Manual:** User types `/compact` → `compact.java` calls `ConversationManager.compact()` immediately
- **Automatic:** `ConversationManager` detects history exceeding `compact_threshold` → calls `ConversationManager.compact()` automatically, notifies user via terminal: `"[auto-compact triggered — history at 80% of token budget]"`

The `compact_context` tool is **not** exposed to the AI as a callable tool. This removes the ambiguity of the AI deciding when to compact and prevents unexpected compaction mid-conversation.

### Compaction Flow

**Step 1 — Pre-compaction analysis**
`CompactionEngine` scans all turns and categorises what is present:
```
[preferences]   — user preferences stated ("always ask before launching apps")
[integrations]  — apps integrated, schemas, capabilities registered
[commands]      — commands executed and outcomes
[errors]        — errors, causes, resolutions
[decisions]     — decisions made and reasoning
[goals]         — ongoing or multi-step tasks still in progress
[facts]         — one-off facts mentioned only once (highest loss risk)
```

**Step 2 — Dynamic prompt construction**
The compaction prompt is built from the analysis output, not a static template. Each category found gets explicit preservation instructions proportional to importance and rarity. Items found only once receive `"do not lose this"` emphasis.

Example prompt fragment (generated dynamically):
```
Compress the following conversation into a structured summary.
Preserve ALL of the following with high fidelity:
- User preferences (found 3 times — merge into canonical list)
- Spotify integration schema (found once — reproduce exactly)
- Ongoing goal: "set up morning briefing routine" (in progress — keep full detail)
Compress with lower fidelity:
- Command execution history (summarise patterns, drop individual runs)
```

**Step 3 — Structured output**
Compaction produces a structured summary, not prose:
```
[preferences]   terse output; always confirm before app launches; WiFi-only summaries
[integrations]  Spotify: search(query), play(query); Google Maps: navigate(destination)
[decisions]     Claude as primary provider; persistent memory mode selected
[goals]         morning briefing routine — in progress, user approved schedule draft
[facts]         user timezone UTC+5:30; preferred alias prefix "t-"
[recent]        last N turns verbatim (N = compact_recent_keep, default 5)
```

**Step 4 — Pinning**
Turns marked pinned survive compaction verbatim and are never summarised:
- User types `/pin` → pins the most recent AI response turn
- Pinned turns stored separately in `ConversationManager`, prepended to every `AIRequest` ahead of rolling history
- The AI does **not** auto-pin. Pinning is a user gesture only — this prevents silent accumulation of pinned content that could grow the context uncontrollably.

**Step 5 — Transparency & archiving**
After compaction:
- Terminal displays: `"[compact] 48 turns → structured summary + 5 recent + 3 pinned. Archive saved."`
- Pre-compaction history written to `ai_history_archive_<timestamp>.json` in scoped storage
- `ConversationManager` replaces active history with structured summary as a single synthetic turn

**Archive retention:** Archives are kept for 30 days by default (`compact_archive_days` setting), then deleted automatically on next app launch. Users can delete all archives immediately via `/cleararchive`.

---

## Section 6: `integrate` Command & App Interface Layer

**Files:** `commands/main/raw/integrate.java`, `ai/appinterface/`

### `integrate` command
The integration gateway. Callable manually only in Phase 1. The AI does not self-invoke `integrate` — user consent is required before any new app tool is registered.

**Phase 1 scope: published app shortcuts, launch intents, and curated deep links only.**

**Flow:**
1. `AppCapabilityScanner` queries `LauncherApps` / `ShortcutManager` for published shortcuts, then `PackageManager` for installed apps that have exported Activities with standard launch or search intent filters
2. Results presented in terminal grouped by category:
   ```
   [SHORTCUT]  App-published action (preferred)
   [LAUNCH]    Open app to home screen
   [SEARCH]    Trigger in-app search
   [DEEPLINK]  Curated deep link patterns (known-good list)
   ```
   Content providers, services, and accessibility-level control are not shown in Phase 1.
3. User selects which apps/capabilities to onboard
4. **Description generation** — two paths depending on AI availability:
   - **AI available:** Single batched API call generates human-readable descriptions for all selected capabilities (e.g., `"Search Spotify for a song or artist"`)
   - **AI unavailable** (offline, no key, battery gate, or `provider=mock`): Deterministic fallback generates descriptions from structured intent data — app label + action type, e.g., `"[App Name] — Launch"`, `"[App Name] — Search"`, `"[App Name] — Open URL"`. Integration proceeds without AI. Descriptions can be improved later by re-running `integrate` when AI is available.
5. `IntentExecutor` validates by resolving the intent via `PackageManager.resolveActivity()` — **no probe execution**, just resolution check. Avoids false capabilities and fragile assumptions.
6. Valid schemas registered in `ToolRegistry` Tier 2, persisted to `ai_app_tools.xml`

### `AppCapabilityScanner`
**File:** `ai/appinterface/AppCapabilityScanner.java`

- Runs on a background thread; result cached to disk
- Listens for `ACTION_PACKAGE_ADDED` / `ACTION_PACKAGE_REMOVED` — incremental re-scan on change
- Uses `LauncherApps` / `ShortcutManager` first so Phase 1 integrations prefer app-published, user-visible shortcuts over inferred deep links
- Uses `PackageManager` with `QUERY_ALL_PACKAGES` for Android 11+ visibility. This permission is **already declared in the existing manifest** for the `apps` command — no new permission is added in Phase 1. Both F-Droid and Play Store variants already carry this permission with justification covered by the existing `apps` command use case. No manifest change required.
- Never blocks main thread; deferred when battery < 15%

### `LauncherIndex`
**File:** `ai/platform/LauncherIndex.java`

- Backed by `AppSearch` and stored entirely on-device
- Indexes launcher-owned entities: commands, aliases, apps, shortcuts, integrated tools, recent AI sessions, compacted summaries, notification journey metadata, and optionally cached RSS/article summaries
- Used before remote AI calls for fast local retrieval, local suggestion ranking, and explicit `ai` command grounding
- Never indexes third-party private data unless the launcher already legitimately holds it (for example, notification text from `NotificationListenerService` or user-approved integrations)

This is a core Android 16-17 alignment choice: local intelligence should come from an on-device index first, not from always asking the model.

### `IntentExecutor`
**File:** `ai/appinterface/IntentExecutor.java`

- Fires Android Intents on behalf of the AI after confirmation gate passes
- Wraps in try/catch with `ActivityNotFoundException` — graceful message if app uninstalled after schema registered
- Every AI-initiated app interaction logged to `ai_action_log.txt` in scoped storage for user transparency and debuggability

### Self-healing
On re-scan, if `AppCapabilityScanner` detects a registered schema no longer resolves (app updated or uninstalled), it marks the tool `stale` in `ai_app_tools.xml` and shows: `"[app] integration is stale. Re-run integrate to refresh."`

---

## Section 7: AITrigger & Input Ownership

**File:** `ai/AITrigger.java`

Implements `MainManager.CmdTrigger`. Inserted after `AppTrigger`, before `ShellCommandTrigger`:

```
[ GroupTrigger → AliasTrigger → TuiCommandTrigger → AppTrigger → AITrigger → ShellCommandTrigger ]
```

### Ownership Policy
**Once `AITrigger` claims an input, it owns it exclusively.** There is no "fall through to shell after async start." This eliminates the race condition identified in the review.

Claim decision is synchronous and made before any async work begins:
1. If `AISubsystem.isAvailable()` is false → return `false` immediately (ShellCommandTrigger handles it)
2. If a request is already in-flight and input is `"stop"` or bare Enter → call `AISubsystem.cancel(currentRequestId)`, return `true` (input consumed, no shell fallback)
3. Otherwise → claim input (`return true`), transition to `THINKING` state, dispatch async

The shell never sees input that the AI has claimed. The AI never sees input that a prior trigger already handled.

### Execution Flow (within claimed input)
1. Transition to `THINKING` → show `[thinking...]` in terminal immediately
2. Build `AIRequest` with requestId, user message, allowlisted tools, conversation history
3. Call `AISubsystem.submit()` async
4. On `AIResponse`:
   - **TEXT** → deliver via `PrivateIOReceiver.ACTION_OUTPUT` with AI color tag → `COMPLETED`
   - **TOOL_CALLS** → enter `EXECUTING_TOOLS`:
     - For each tool call: check `ToolRiskClass`, run confirmation gate if required
     - If user declines a tool: skip it, note the skip in results, continue
     - Tier 1: dispatch via `MainManager.ACTION_EXEC` (reuses existing command execution)
     - Tier 2: dispatch via `IntentExecutor`
     - Collect all results → `FOLLOWUP`
   - **FOLLOWUP** → single follow-up AI call with tool results → render final response → `COMPLETED`

**Always-on fallback is configurable.** When disabled, `AITrigger` skips all non-cancel inputs and `ShellCommandTrigger` handles unrecognized commands as before. Users invoke AI explicitly via the `ai` command:

```
ai <natural language query>
```

`ai.java` is a standard `CommandAbstraction` in `commands/main/raw/`. It accepts `PLAIN_TEXT` args, routes directly to `AISubsystem.submit()`, and renders the response in the terminal. It is present regardless of `always_on_fallback` setting — it is the explicit invocation path and always available.

---

## Section 8: AI Enhancements in Existing Managers

All enhancements are guarded by `AISubsystem.isAvailable()` and add zero overhead when AI is off.

### SuggestionsManager
After local fuzzy matching completes, query `LauncherIndex` for indexed commands, apps, shortcuts, integrations, and recent relevant entities. Then pass the merged local candidates + current input to `AISubsystem` for re-ranking. Debounced: AI request fires after 500ms of no typing. Result cached per input prefix (LRU, max 50 entries). Falls back to local fuzzy + AppSearch ranking when AI is unavailable.

### NotificationService
When a notification arrives, AI triages it in background: summarizes grouped notifications, flags urgent, suppresses low-priority based on user-defined natural language rules stored in `ai.xml`. Triage result cached per notification key. Suppression never silently discards — suppressed notifications are logged and user can review via `/notifications suppressed`.

On Android 16+, notifications using `Notification.ProgressStyle` are also parsed into structured "journey" metadata by `JourneyManager` and surfaced as richer launcher-native ongoing tasks (for example: navigation, delivery, ride progress, uploads). On Android 17+, promoted Live Update notifications are treated as high-confidence ongoing tasks and shown ahead of generic summaries. The launcher consumes these system surfaces; it does not invent parallel long-running overlays for the same task.

### RssManager
After fetching a feed, AI optionally summarizes each article to 1-2 sentences. Summaries cached alongside raw feed XML. Runs on WiFi only by default (`wifi_only_summary` setting).

### UIManager
- Renders `[thinking...]` while AI processes (new text category: `CATEGORY_AI_THINKING`)
- Streams token-by-token output when provider supports streaming
- AI output rendered in distinct color (`Theme.ai_output_color` in `theme.xml` — visual property lives with other color settings, not in `ai.xml`)
- All launcher activities and overlays use `OnBackInvokedDispatcher` on Android 16+ so predictive back and back-to-home transitions remain system-native in both gesture and 3-button navigation

### AppsManager
When listing apps, AI scores relevance by time-of-day and recent usage. Computed in background, cached 5 minutes. Falls back to alphabetical sort.

### AliasManager
After a command runs for the 3rd time in a session, suggests an alias: `"You've run this 3 times. Create alias '[suggested]'? (y/n)"`. User must confirm — AI never creates aliases silently.

### HTMLExtractManager
AI post-processes extracted HTML: cleans markup, extracts main content, optionally answers a question about the page passed as context.

---

## Section 9: Action Safety & Confirmation Policy

All AI-initiated actions (tool calls) pass through a confirmation gate in the `EXECUTING_TOOLS` state before execution. This applies to **every** state-changing or side-effecting action — there is no silent execution of anything beyond `READ_ONLY` tools.

**Confirmation gate:**
```
[AI wants to] <action description>
Run? (Enter to confirm / stop to cancel)
```

For `REQUIRES_CONFIRMATION` tools (e.g., `call`), the confirmation message is more explicit:
```
[AI wants to] call +91-XXXXXXXXXX
This will immediately dial the number. Run? (Enter / stop)
```

**Batch tool calls:** If AI requests multiple tools in one response, each non-READ_ONLY tool is confirmed individually in sequence. The user can stop the sequence at any point.

**Declined tools:** If the user declines a tool, the AI is informed ("user declined [tool]") and generates a final response acknowledging the limitation. The conversation continues.

**Settings override:** Users can configure `auto_confirm_state_changing = true` in `ai.xml` to skip confirmation for `STATE_CHANGING` tools only (e.g., brightness, volume). `SENSITIVE` and `REQUIRES_CONFIRMATION` tools always require explicit confirmation regardless of settings.

**Android 17 contact flows:** For `call`, `message`, `email`, or share-like tools that need person disambiguation, Android 17+ should prefer `Intent.ACTION_PICK_CONTACTS` and work from the returned temporary grant instead of broad `READ_CONTACTS`. On older Android versions, the launcher falls back to its existing contact-selection path.

---

## Section 10: Feature-Gating Truth Table

For each condition, every AI-backed feature has a defined behaviour — no implementation guesswork.

| Feature | `enabled=false` | No network | No API key | `provider=mock` | Battery < 15% | WiFi-only mode |
|---|---|---|---|---|---|---|
| AITrigger (fallback) | Skip, ShellTrigger handles | Skip, ShellTrigger handles | Skip, ShellTrigger handles | Active, mock responses | Active (AI text only; no heavy scans) | Active |
| Suggestions re-ranking | Off, fuzzy only | Off, fuzzy only | Off, fuzzy only | Active, mock ranking | Off, fuzzy only | Active |
| Notification triage | Off, raw display | Off, raw display | Off, raw display | Active, mock triage | Off | Active |
| RSS summarization | Off, raw feed | Off, raw feed | Off, raw feed | Active, mock summary | Off | Off |
| App ranking | Off, alphabetical | Off, alphabetical | Off, alphabetical | Active, mock ranking | Off, alphabetical | Active |
| Alias suggestion | Off | Off | Off | Active | Off | Active |
| AppCapabilityScanner | Off | N/A (local) | N/A (local) | N/A (local) | Deferred to charge | N/A (local) |
| Compaction | Manual only | Manual only (no AI summary) | Manual only | Active | Deferred (auto); manual still works | Active |

---

## Section 11: Settings

**New file:** `managers/xml/options/Ai.java` (mirrors pattern of `Behavior.java`)  
**New XML:** `ai.xml` in scoped storage

```
provider               — "claude" | "openai" | "ollama" | "mock"
api_key                — keystore alias (actual key in Android Keystore, not in XML)
model                  — model identifier string
base_url               — for Ollama or custom OpenAI-compatible endpoints
memory_mode            — "stateless" | "session" | "persistent"
history_token_cap      — integer tokens (default 4000)
enabled                — boolean
always_on_fallback     — boolean (AITrigger active or explicit ai <query> only)
wifi_only_summary      — boolean (RSS/HTML summarization on WiFi only)
timeout_connect        — integer seconds, time to first byte (default 10)
timeout_inactivity     — integer seconds, time since last token (default 30)
timeout_response       — integer seconds, non-streaming fallback (default 60)
compact_threshold      — float 0.0–1.0, fraction of cap triggering auto-compaction (default 0.8)
compact_recent_keep    — integer, verbatim turns preserved after compaction (default 5)
compact_archive        — boolean, write archive on compaction (default true)
compact_archive_days   — integer, archive retention in days (default 30)
auto_confirm_state_changing — boolean, skip confirmation for STATE_CHANGING tools (default false)
notification_rules     — string, natural language suppression rules
```

**Color settings** (`Theme.ai_output_color`) live in `theme.xml` alongside all other color options — visual properties stay with visual settings.

**API key security:** Stored in Android Keystore. `ai.xml` holds only the keystore alias. Consistent with existing security posture: HTTPS-only, scoped storage, `FLAG_IMMUTABLE`.

**Privacy policy (explicit):**
- Conversation history, pinned turns, and compaction archives are stored in Android scoped storage (app-private, OS-isolated). Not encrypted at rest in Phase 1.
- What leaves the device: active history window + current message + tool schemas. Nothing else.
- User can delete all history: `/clearhistory` (active + session history)
- User can delete all archives: `/cleararchive`
- History and archives are never sent to any analytics or logging service.

---

## Section 12: Mobile Efficiency Constraints

| Constraint | Implementation |
|---|---|
| Never block main thread | All AI calls async with callbacks; UI updates via `Handler(Looper.getMainLooper())` |
| Lazy initialization | `AISubsystem` components constructed on first use |
| Three-layer timeout | Connection + inactivity + user-cancel (see below) |
| Battery gate | `BatteryManager` check before heavy ops; deferred when < 15% |
| Token budget | CompactionEngine manages history; compact tool schemas; descriptions stripped when tight |
| AppCapabilityScanner cache | Persisted to disk; incremental re-scan on package change only |
| Suggestion debounce | 500ms before AI re-ranking; LRU cache of 50 prefix results |
| RSS/HTML summarization | WiFi-only by default; cached alongside source |
| Prompt caching | System prompt + tool schemas sent with cache headers (Claude) |
| Graceful degradation | `isAvailable()` at every entry point; zero regression when offline |

**Three-layer timeout strategy:**

| Layer | Trigger | Value | Action |
|---|---|---|---|
| **Connection** | Time to first byte | 10s (fixed) | TIMED_OUT_CONNECT → AITrigger explicitly re-dispatches original input to `ShellCommandTrigger.trigger()` directly (if `always_on_fallback=true`), or shows `[AI timeout]` only (if false). Full trigger chain is not re-entered. |
| **Inactivity** | Time since last token during streaming | 30s (configurable) | Cancel → TIMED_OUT, partial output preserved + `[response interrupted]` |
| **User-cancel** | User types `stop` or bare Enter while in-flight | Immediate | `OkHttpClient.call.cancel()` → CANCELLED, partial output preserved + `[cancelled]` |

For non-streaming providers, `timeout_response` is a single configurable wall-clock timeout the user sets based on their hardware. Streaming is the primary heartbeat — an actively producing stream is never considered hung.

---

## Section 13: Phase Roadmap

### Phase 1 — Core AI Layer *(this spec)*
- `AISubsystem`, `RequestManager`, `AIProvider` interface + 3 implementations
- `ToolRegistry` with Phase 1 allowlist (Tier 1) + published shortcuts / curated app intents (Tier 2)
- `ConversationManager` + `CompactionEngine` (all memory modes)
- `AITrigger` with exclusive ownership policy
- `integrate` command (published shortcuts + launch intents + curated deep links only)
- `AppCapabilityScanner` + `IntentExecutor`
- `LauncherIndex` backed by `AppSearch` for local retrieval and suggestions
- `JourneyManager` consuming `Notification.ProgressStyle` / Live Update semantics for ongoing tasks
- `/compact`, `/pin`, `/clearhistory`, `/cleararchive` commands
- `AISettings` + Android Keystore API key storage
- `UIManager` streaming output + thinking indicator
- Action confirmation gate for all non-READ_ONLY tools
- AI hooks in `SuggestionsManager`, `NotificationService`, `UIManager`
- Light hooks in `RssManager`, `AliasManager`, `HTMLExtractManager`, `AppsManager`

### Phase 2 — Scheduled Agent Tasks
- `SchedulerManager` + `ai_schedules.xml`
- `TaskRunner` background service
- Workflow definition format
- Tier 3 tools: `schedule_task`, `list_tasks`, `cancel_task`, `run_workflow`
- Tier 2 expansion: content providers (opted-in, per-app consent)
- Android 17 `Handoff` bridge for cross-device continuity surfaced in launcher/taskbar entry points
- Android 17 local-network consent path for nearby/LAN model and device discovery when such features are added
- No changes to Phase 1 code

### Phase 3 — Ambient Intelligence
- `ContextEngine` — battery, location, time, active app, notification stream
- `AmbientTriggerService` — AI-defined trigger conditions evaluated in background
- `AccessibilityBridge` — deep in-app UI control (explicit opt-in, separate permission)
- Companion-device presence integration (Android 16 CDM presence APIs) for earbuds/wearables/car context
- Assistant-audio mode on Android 17+ (`USAGE_ASSISTANT`, `MODE_ASSISTANT_CONVERSATION`) if launcher becomes a true voice assistant surface
- Experimental adapter for Android 17 `App Functions` when/if third-party agent access becomes viable
- `AIAgent` — ReAct planning loop (plan → tool call → observe → replan)
- No changes to Phase 1 or 2 code

---

## New Files Summary

```
ai/
  AISubsystem.java
  AIProvider.java
  AIRequest.java
  AIResponse.java
  AICallback.java
  AIException.java
  AIRequestState.java
  RequestManager.java
  ConversationManager.java
  ConversationTurn.java
  CompactionEngine.java
  ToolRegistry.java
  Tool.java
  ToolCall.java
  ToolRiskClass.java
  AITrigger.java
  platform/
    LauncherIndex.java
    ShortcutBridge.java
    JourneyManager.java
  providers/
    ClaudeProvider.java
    OpenAIProvider.java
    MockProvider.java
  appinterface/
    AppCapabilityScanner.java
    AppToolSchema.java
    IntentExecutor.java
commands/main/raw/
  ai.java              — explicit AI invocation: ai <query>
  integrate.java
  compact.java
  pin.java
  clearhistory.java
  cleararchive.java
managers/xml/options/
  Ai.java
```

## Modified Files Summary

```
LauncherActivity.java       — init AISubsystem, load ai.xml
MainManager.java            — add AITrigger, stop/bare-Enter interception
MainPack.java               — add AISubsystem field
XMLPrefsManager.java        — load ai.xml
UIManager.java              — streaming output, thinking indicator, CATEGORY_AI_THINKING
theme.xml / Theme.java      — add ai_output_color option
SuggestionsManager.java     — AI re-ranking hook + debounce + LRU cache
NotificationService.java    — AI triage hook
LauncherApps / ShortcutManager integration — published shortcut discovery for app tools
AppSearch schema/config      — local launcher index for apps/commands/tools/history
RssManager.java             — AI summarization hook (WiFi-only)
AliasManager.java           — AI alias suggestion hook (user-confirmed)
HTMLExtractManager.java     — AI post-processing hook
AppsManager.java            — AI relevance ranking hook
app/build.gradle            — no new dependencies (OkHttp already present)
AndroidManifest.xml         — no new permissions for Phase 1 (QUERY_ALL_PACKAGES already present for apps command)
```

---

## Verification Plan

### Happy Path
1. **Build passes:** `./gradlew assembleFdroidDebug` with zero errors
2. **AI disabled:** `enabled=false` → all existing commands work identically, no regressions
3. **MockProvider:** `provider=mock` → AI responses appear without network
4. **Always-on fallback:** Unrecognized input → AI responds via MockProvider
5. **READ_ONLY tool:** Ask "what's my battery?" → `status` fires silently, result shown
6. **STATE_CHANGING tool:** Ask "set brightness to 50" → preview shown, user confirms, brightness changes
7. **REQUIRES_CONFIRMATION tool:** Ask "call [contact]" → explicit warning shown, user confirms
8. **Tool declined:** User types `stop` at confirmation → AI acknowledges, conversation continues
9. **`integrate` command:** Run `integrate`, select app, confirm schema in ToolRegistry Tier 2
10. **Published shortcut integration:** App exposing `ShortcutManager` shortcut appears under `[SHORTCUT]` in `integrate` and is preferred over generic launch intent
11. **AppSearch local retrieval:** Search/query for a recent command, integrated tool, or app returns from `LauncherIndex` without network
12. **Journey tasks:** On Android 16+, a `Notification.ProgressStyle` notification appears as structured ongoing task metadata in launcher UI
13. **App launch:** Ask "open Spotify" (integrated) → confirmation shown → Spotify opens
14. **Memory modes:** Stateless (fresh each call), session (context within run), persistent (survives restart)
15. **Battery gate:** Mock battery < 15% → AppCapabilityScanner defers, direct AI queries still work
16. **Streaming output:** ClaudeProvider → tokens render progressively in terminal
17. **Manual compaction:** Fill history → `/compact` → summary + recent turns + pinned shown
18. **Auto-compaction:** History reaches 80% cap → launcher triggers compaction, user notified
19. **Pinning:** `/pin` → `/compact` → pinned turn preserved verbatim
20. **Archive:** After `/compact` → `ai_history_archive_<timestamp>.json` exists; `/cleararchive` removes it
21. **Information preservation:** State preference early → `/compact` → AI recalls preference after compaction

### Failure Cases
22. **Cancel mid-stream:** AI streaming → user types `stop` → stream stops, partial output preserved, state → CANCELLED
23. **Connect timeout (`always_on_fallback=true`):** Kill network → submit query → after 10s → TIMED_OUT_CONNECT → terminal shows `[AI unavailable — retrying as shell command]` → original input dispatched directly to `ShellCommandTrigger.trigger()`
24. **Connect timeout (`always_on_fallback=false`):** Kill network → submit query → after 10s → TIMED_OUT_CONNECT → terminal shows `[AI timeout]` only → no shell re-dispatch → input is consumed
25. **Inactivity timeout:** Mock provider stalls mid-stream → after 30s inactivity → TIMED_OUT_INACTIVITY, partial output + `[response interrupted]`
26. **Stale callback suppression:** Start request, cancel it, new request arrives → callback from cancelled request is dropped (requestId mismatch)
27. **Provider switch mid-session:** Change provider in settings while session active → next request uses new provider, history carried over
28. **App uninstall after integration:** Integrate app, uninstall it, ask AI to use it → `ActivityNotFoundException` caught, graceful error, tool marked stale
29. **App update after integration:** App updates, intent changes → re-scan marks tool stale, user prompted to re-integrate
30. **Malformed tool response:** Provider returns malformed tool call JSON → caught in `RequestManager`, state → FAILED, error displayed, history not appended
31. **Compaction during restart:** Compaction in progress, app killed → on next launch, detect incomplete compaction via flag, restore from pre-compaction history
32. **Package visibility (Android 11+):** `PackageManager` returns partial results due to visibility restrictions → scanner logs which apps were invisible, user informed
33. **AI requests non-allowlisted tool:** Provider hallucinates a tool name not in ToolRegistry → `RequestManager` rejects the call, informs AI "tool not available", AI responds gracefully
34. **History persistence corruption:** `ai_history.json` unreadable on load → `ConversationManager` starts fresh session, logs error, user notified
35. **integrate without AI:** Run `integrate` with `provider=mock` or no network → deterministic descriptions generated from intent metadata, integration completes without AI call
36. **Predictive back:** On Android 16+, launcher overlays and activities animate correctly back-to-home under both gesture and 3-button navigation
37. **Android 17 contact disambiguation:** Ask AI to call or message a person with ambiguous identity → system contact picker resolves the target without broad contacts permission
