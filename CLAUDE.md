# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**T-UI Linux CLI Launcher** — an Android terminal-style home screen launcher with an embedded AI subsystem. Modernized fork of the original T-UI, now targeting API 34 (Android 14), min SDK API 21. Application ID: `bhupendra.ai.launcher`.

All source code lives in `TUI-ConsoleLauncher/`. Run all build and Gradle commands from that directory.

## Build Commands

Run all commands from `TUI-ConsoleLauncher/`:

```bash
chmod +x gradlew

# F-Droid debug APK (includes SMS permissions)
./gradlew assembleFdroidDebug
# Output: app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk

./gradlew assemblePlaystoreDebug
./gradlew assembleRelease   # requires signing config in local.properties
./gradlew clean
```

Signing keys go in `local.properties`: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`.

## Deployment (ADB)

```bash
emulator -avd Pixel_9_Pro -gpu host -accel on &
adb wait-for-device
adb install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
adb logcat | grep bhupendra.ai
adb uninstall bhupendra.ai.launcher
```

### Scripted Command Execution

`adb-run.sh` (repo root) sends a command to the running app and streams its output via Logcat tag `AI_OUTPUT`. It exits on `CMD_FINISHED` or `AI_TURN_FINISHED` signals.

```bash
./adb-run.sh 'ai what is the weather?'
```

Normal commands log `CMD_FINISHED`; the `ai` command suppresses it — `AISubsystem` logs `AI_TURN_FINISHED` after the full turn (tool calls + final response). Termux-routed commands log `CMD_FINISHED` immediately after dispatch (async). The `isAI` check in `MainManager` uses `getSimpleName().equals("ai")` — exact match only, to avoid matching other classes in the `ai` package like `compact`.

## Architecture

The app uses an MVC-inspired pattern with `LocalBroadcastManager` for internal communication, and **Dagger/Hilt** for dependency injection.

### Core Layer

- **`LauncherActivity.java`** — Main Activity. Lifecycle, permission requests, input routing, theme init.
- **`UIManager.java`** — Terminal-style UI renderer (~1744 lines). Text display, cursor, color theming.
- **`MainManager.java`** — Command execution orchestrator. Manages broadcast receivers that route input/output and I/O redirection.
- **`TUIApplication.java`** — Hilt application class. Entry point for the DI graph.
- **`di/AppModule.java`** — Provides singletons: `FileSystemManager`, `DeviceStateManager`, `TextProcessor`, `ToolRegistry`.

### Command System

Commands live in `commands/main/raw/` (one class per command) and implement the `Command` interface. `MainPack.java` aggregates all built-in commands. Commands interact via `CommandContext` (`pack.get...()`); never cast to `MainPack` to access internals.

Special base types in `commands/main/specific/`: `APICommand`, `ParamCommand`, `RedirectCommand`, `PermanentSuggestionCommand`.

**To add a launcher command:** Create a class in `commands.main.raw`, implement `exec(ExecutePack pack)`, register in `MainPack`.

### AI Subsystem (`ai/`)

`AISubsystem` is a manual singleton (not Hilt-managed). Initialized by `LauncherActivity` with an `AIProvider` and `ToolExecutor`.

Key components:
- **`AIProvider`** — Interface; implementations: `ClaudeProvider` (claude-haiku-4-5-20251001), `OpenAIProvider`, `MockProvider`.
- **`ToolRegistry`** / **`AndroidToolExecutor`** — Tool registration and dispatch.
- **`ConversationManager`** — Maintains conversation history as `ConversationTurn` list.
- **`CompactionEngine`** — Summarizes history when it grows too large. Handles null `content` on `assistantCalls` turns (tool-call turns have no text).
- **`LongTermMemory`** — Persistent key-value memory across sessions.
- **`tools/BaseAITool`** — Abstract base. Implement `execute(Context, JSONObject args)`.

**System prompt:** `app/src/main/assets/ai_system_prompt.md`. Copied to internal TUI folder on first run — edit this file to change AI identity without code changes.

**To add an AI tool:**
1. Create `YourTool.java` in `ai/tools/` extending `BaseAITool`.
2. Define JSON parameter schema in the constructor.
3. Implement `execute(Context, JSONObject args)`.
4. Register in `AISubsystem.registerSystemTools()`.

Onboarding/journey state: `ai/platform/` (`AIOnboardingManager`, `JourneyManager`, `LauncherIndex`, `ShortcutBridge`).

### Notification Hook System

`NotificationService` (static `instance`) calls `NotificationHookManager` on every incoming notification. Rules persist in `notification_hooks.json` and support:
- **Temporal windows** (`HH:mm-HH:mm`)
- **AI-generated replies** via `AISubsystem`
- **Update logging** via `NotificationUpdateManager` (`notification_updates.json`)
- **Debouncing**: 10-second cooldown per hook/sender (`COOLDOWN_MS`)

### Managers (`managers/`)

**Hilt-provided singletons** (via `di/AppModule`):
- **`FileSystemManager`** — All file I/O, downloads, Uri building.
- **`DeviceStateManager`** — Battery, WiFi, memory sensors.
- **`TextProcessor`** — Regex matching, Markdown rendering. Processes span matches **back-to-front** to keep indices valid after deletion. Uses `SpannableStringBuilder` with `SPAN_EXCLUSIVE_EXCLUSIVE` throughout.

All rendering paths in `TerminalManager` and `TextProcessor` are wrapped in try-catch — malformed Markdown logs an error and displays raw text rather than crashing.

**Other managers** (accessed via `MainPack`):
- `XMLPrefsManager` + `managers/xml/options/` — All preferences as XML in `Context.getExternalFilesDir()`. `options/Ai.java` holds AI config (API key, provider).
- `AppsManager` — Enumerates installed apps.
- `SuggestionsManager` — Drives autocomplete bar.
- `music/MusicManager2` + `MusicService` — Background music playback.
- `TuiLocationManager` — GPS/location.
- `TermuxManager` — Bridges commands to Termux (BusyBox is deprecated).

### Product Flavors

- **`fdroid`** — Includes `commands/main/raw/sms.java`. Source in `app/src/fdroid/`.
- **`playstore`** — No SMS command.

## Key Constraints

- All network calls must use HTTPS (`android:usesCleartextTraffic="false"` is enforced).
- File sharing through `GenericFileProvider`, never raw `file://` URIs.
- `PendingIntent` flags must use `FLAG_IMMUTABLE`.
- R8/ProGuard minification enabled for release. `proguard-rules.pro` keeps `commands/`, `managers/`, and `tuils/` from name obfuscation — new packages under those paths are automatically protected.
- Custom permission `bhupendra.ai.launcher.permission.RECEIVE_CMD` (`protectionLevel="signature"`) gates external command access via broadcast.
- Lint disabled for release builds (`checkReleaseBuilds false`).

## Worktrees

Worktrees live in `TUI-ConsoleLauncher/.worktrees/` (already in `.gitignore`).
