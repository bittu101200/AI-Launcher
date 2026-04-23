# AI Launcher: Golden Context & Architectural Blueprint

**Date:** 2026-04-23
**Status:** Post-Refactor (Modular/Service-Oriented)
**Branch:** `refactor/ai-subsystem-modularization`

This document serves as the single source of truth for the modernized AI Launcher architecture. It is designed to provide future agents with the "Golden Context" required to maintain and extend the system without technical debt or architectural regression.

---

## 1. Architectural Philosophy: "Additive, Not Modificative"
The core goal of the 2026 refactor was to transition from a monolithic "God Object" architecture (`Tuils`, `MainPack`) to a modular, service-oriented system. 
- **Rule:** Never add tool-specific logic to core executors. 
- **Rule:** Use Dependency Injection (Hilt) for cross-component communication.
- **Rule:** Interfaces (`CommandContext`, `AITool`) are the only way subsystems should interact.

---

## 2. Key Alterations & Findings

### A. AI Subsystem (The Plug-and-Play Brain)
- **Alteration:** Dismantled the 400+ line `if-else` block in `AndroidToolExecutor`. Created `BaseAITool` and extracted 19 capabilities into discrete classes in `bhupendra.ai.launcher.ai.tools`.
- **Finding:** Hardcoded system prompts made iteration slow. **Moved the system prompt to `app/src/main/assets/ai_system_prompt.md`**. This file is copied to the internal TUI folder on first run and is used as the AI's base identity.
- **Effect:** You can now add AI capabilities (e.g., "Control Spotify") simply by creating a new class and registering it in `AISubsystem`.

### B. Utility Decomposition (The Death of Tuils)
- **Alteration:** Reduced `Tuils.java` (previously 1700 lines) by extracting logic into three specialized managers:
    1. **`FileSystemManager`**: Centralizes file I/O, downloads, and `Uri` building.
    2. **`DeviceStateManager`**: Manages Android system sensors, battery, WiFi, and memory checks.
    3. **`TextProcessor`**: Handles regex matching, Markdown parsing, and string evaluation.
- **Finding:** `Tuils` was a global bottleneck causing tight coupling. 
- **Effect:** Reduced class-loading time and memory footprint. Code is now searchable and domain-organized.

### C. Dependency Injection (The Glue)
- **Alteration:** Integrated **Dagger/Hilt**. Created `TUIApplication` and `di.AppModule`.
- **Finding:** Passing `MainPack` or `Context` through 5 layers of constructors was causing "context garbage."
- **Effect:** Use `@Inject` to access managers. Singletons are managed by the framework, ensuring consistent state across the UI and background threads.

### D. Command Pipeline (Decoupled Execution)
- **Alteration:** Refactored all 52 raw commands (e.g., `wifi`, `apps`, `status`) to use the **`CommandContext`** interface. 
- **Finding:** Commands were casting `ExecutePack` to `MainPack` to access internal fields, breaking encapsulation.
- **Effect:** Commands now interact with a clean API (e.g., `pack.getMusicManager()`). They no longer "know" about the Launcher's internal state.

### E. Execution & Shell (Termux Transition)
- **Alteration:** Deprecated `BusyBoxInstaller` and removed bundled BusyBox binaries. Updated `MainManager` to route standard Linux commands (ls, cat, git) to the **Termux Bridge**.
- **Finding:** Bundled binaries were brittle and posed security/compatibility risks. Termux is faster and more powerful.
- **Effect:** The app is smaller, and the shell environment is a full, updated Linux environment.

---

## 3. Stability & Rendering (The "Golden" Fixes)

### Markdown Rendering
- **Critical Fix:** Re-implemented `applyRegex` in `TextProcessor` to process matches **back-to-front**.
- **The Reason:** Deleting or replacing text (like stripping `**` for bold) shifts the indices for all subsequent matches. Processing backwards ensures the remaining indices stay valid.
- **Span Safety:** Switched all `Spannable` flags to `SPAN_EXCLUSIVE_EXCLUSIVE`. Used `SpannableStringBuilder` exclusively for concatenation to prevent span-stripping.

### Graceful Failure
- **Alteration:** Every critical rendering path in `TerminalManager` and `TextProcessor` is now wrapped in a `try-catch`.
- **Effect:** If the AI sends malformed Markdown or a preference is missing, the app **will not crash**. It will log the error and display raw text.

---

## 4. How to Extend (Developer Workflow)

### To Add an AI Tool:
1. Create `YourToolName.java` in `ai.tools` extending `BaseAITool`.
2. Define the JSON parameters in the constructor.
3. Implement `execute(Context, JSONObject args)`.
4. Register it in `AISubsystem.registerSystemTools()`.

### To Add a Launcher Command:
1. Create a class in `commands.main.raw`.
2. Implement `exec(ExecutePack pack)`.
3. Use `pack.get...()` methods to get the services you need.

### To Add a Global Service:
1. Create the Manager class.
2. Add a `@Provides @Singleton` method in `di.AppModule`.
3. Inject it into your target using `@Inject`.

---

## 6. Execution Control & Stability Improvements

### Command Signaling & adb-run.sh
- **Mechanism:** `adb-run.sh` streams Logcat with the tag `AI_OUTPUT` and waits for two specific signals: `CMD_FINISHED` or `AI_TURN_FINISHED`.
- **Logic:**
    - **Normal Commands:** (TuiCommands, ShellCommands, Apps) MUST log `CMD_FINISHED` to `AI_OUTPUT` when their synchronous execution completes.
    - **AI Turns:** The `ai` command suppresses `CMD_FINISHED`. The `AISubsystem` logs `AI_TURN_FINISHED` only after the full turn (including tool calls and final response) is complete.
- **Refinement:** In `MainManager.java`, the `isAI` check must be specific (`command.getClass().getSimpleName().equals("ai")`) to avoid matching other commands in the `ai` package (like `compact`).
- **Termux Bridge:** Commands routed to Termux log `CMD_FINISHED` immediately after the intent is sent, as Termux execution is asynchronous to the launcher.

### AI Compaction Robustness
- **Fix:** `CompactionEngine` includes null checks for `t.content` to handle `assistantCalls` turns which contain tool calls but no text content. This prevents `NullPointerException` during history processing.

---

## 7. Known "Dangling Edges" to Watch
- **Reflection in Preferences:** `XMLPrefsManager` still uses reflection for some type transformations. This should eventually be moved to a type-safe adapter system.
- **MusicManager:** The `MusicManager2` is still quite complex and could be further modularized into specific player adapters (Local vs. Spotify).

**End of Context.**
