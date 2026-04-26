# AI Launcher

AI Launcher is an Android terminal-style home screen launcher with an embedded tool-using AI assistant. It keeps the text-first feel of T-UI, but extends it with modern Android compatibility, AI-powered command execution, notification automation, live UI customization, and developer-oriented device tooling.

All Android application source code lives in [`TUI-ConsoleLauncher/`](./TUI-ConsoleLauncher/). The repository root contains project-level docs, plans, and helper scripts.

## What This Repository Contains

- `TUI-ConsoleLauncher/`: the Android app
- `adb-run.sh`: send commands to a running device/emulator and stream output from logcat
- `CLAUDE.md`: concise engineering context for the codebase
- `ARCHITECTURAL_GOLDEN_CONTEXT.md`: architectural intent and refactor history
- `USER_MANUAL.md`: user-facing command and AI usage guide
- `docs/superpowers/`: design notes, plans, and specs

## Core Capabilities

- Terminal-style Android launcher UI with configurable prompt, theme, notes, status labels, and suggestion bar
- Traditional launcher commands for apps, search, notes, status, RSS, weather, themeing, config, backups, and more
- AI assistant with structured tool execution for system actions, config changes, notifications, memory, web access, task scheduling, and Termux integration
- Multiple AI provider options including `opencode_zen`, `gemini`, `claude`, `openai`, `ollama`, and `mock`
- Notification ingestion, filtering, display, reply actions, and automation hooks
- On-device command routing that can operate in explicit `ai ...` mode or more agentic fallback modes
- F-Droid and Play Store product flavors

## Architecture Overview

The app is a single-module Android project with a service-oriented split across UI, command routing, and AI execution.

### Main runtime entrypoints

- [`LauncherActivity.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/LauncherActivity.java): activity lifecycle, permissions, AI bootstrap, reload behavior
- [`UIManager.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/UIManager.java): terminal rendering, input view, suggestions, live config updates
- [`MainManager.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/MainManager.java): command dispatch, shell fallback, AI fallback, output routing
- [`TUIApplication.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/TUIApplication.java): Hilt application entrypoint

### Command system

Built-in launcher commands live in [`commands/main/raw/`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/). Commands are collected by `MainPack` and operate through `ExecutePack` / `CommandContext`.

Examples include:

- app and system actions: `apps`, `open`, `call`, `wifi`, `bluetooth`, `brightness`, `volume`, `flash`
- launcher controls: `config`, `theme`, `alias`, `notes`, `username`, `restart`, `refresh`
- utilities: `search`, `weather`, `rss`, `backup`, `restore`, `tuixt`
- AI entrypoints: `ai`, `compact`

### AI subsystem

The AI stack lives under [`app/src/main/java/bhupendra/ai/launcher/ai/`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/).

Key parts:

- `AISubsystem`: request lifecycle, prompt loading, provider selection, tool registration, AI state notifications
- `AITrigger`: explicit `ai ` handling and agentic entry logic
- `RequestManager`: request timeout/execution wrapper
- `ConversationManager` and `CompactionEngine`: conversation history and summarization
- `LongTermMemory`: persistent memory storage
- `BaseAITool` and `ai/tools/*`: individual system tools

Current built-in tool families include:

- launcher config read/write and config search
- notifications, notification replies, and notification hooks
- contacts and SMS
- app capability discovery and execution
- brightness and volume
- web search and fetch
- long-term memory store/retrieve
- task scheduling and cancellation
- Termux command execution
- user choice / parameter request interactions

### Notification subsystem

Notification-related code lives under [`managers/notifications/`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/managers/notifications/).

Important components:

- `NotificationService`: notification listener entrypoint
- `NotificationDisplayManager`: shared final-stage display path
- `NotificationAttentionDecider`: late-stage filtering/priority decisions
- `NotificationHookManager`: persisted automation rules
- `NotificationUpdateManager`: update logging
- `ReplyManager`: reply-capable notification integration

### Dependency injection and managers

Hilt is present, but the codebase is mixed: some services are injected, while `AISubsystem` is still created manually.

Useful managers include:

- `FileSystemManager`
- `DeviceStateManager`
- `NotesManager`
- `TuiLocationManager`
- `MusicService` / `MusicManager2`
- `SuggestionsManager`
- `XMLPrefsManager`

## Build And Run

Run all Gradle commands from `TUI-ConsoleLauncher/`.

### Prerequisites

- Android SDK / platform tools
- Java 8-compatible toolchain for the Gradle build
- an attached Android device or emulator for installation/testing

### Debug builds

```bash
cd TUI-ConsoleLauncher
chmod +x gradlew
./gradlew assembleFdroidDebug
./gradlew assemblePlaystoreDebug
```

APK outputs:

- `app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk`
- `app/build/outputs/apk/playstore/debug/app-playstore-debug.apk`

### Release build

```bash
cd TUI-ConsoleLauncher
./gradlew assembleRelease
```

Release signing values are read from `TUI-ConsoleLauncher/local.properties`:

- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`

### Install over ADB

```bash
cd TUI-ConsoleLauncher
adb install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk
```

## Testing And Debugging

### Unit tests

Flavor-specific test tasks are required:

```bash
cd TUI-ConsoleLauncher
./gradlew testFdroidDebugUnitTest
./gradlew testPlaystoreDebugUnitTest
```

Focused example:

```bash
cd TUI-ConsoleLauncher
./gradlew testFdroidDebugUnitTest --tests 'bhupendra.ai.launcher.ai.AISubsystemToolExecutionTest'
```

### Lint

```bash
cd TUI-ConsoleLauncher
./gradlew lint
```

### Command execution from host

Use the helper script from the repository root:

```bash
./adb-run.sh 'status'
./adb-run.sh 'ai summarize my notifications'
```

`adb-run.sh` sends a signed broadcast command and waits on logcat tag `AI_OUTPUT` until:

- `CMD_FINISHED` for normal commands
- `AI_TURN_FINISHED` for AI turns

### Useful debugging notes

- `Notification access`, AI onboarding, and config behavior are easiest to verify on a physical device.
- The launcher stores runtime config and prompt files in app-private external storage via `FileSystemManager`.
- The live AI system prompt is built from the bundled asset prompt plus any on-device override file in the launcher storage directory.

## AI Providers And Modes

Provider selection is stored in `ai.xml` and can be managed through onboarding or the `config` command.

Current provider paths:

- `opencode_zen`
- `gemini`
- `claude`
- `openai`
- `ollama`
- `mock`

Notable runtime behaviors:

- In normal mode, AI is invoked explicitly with the `ai ` prefix.
- In agentic mode, unmatched input can route to AI first.
- `always_on_fallback` allows non-agentic fallback to AI after traditional command and shell routing fail.
- AI requests can request user choices and typed parameters through the suggestions UI.

## Product Flavors

The app has two product flavors:

- `fdroid`: includes the F-Droid-only SMS command source under [`app/src/fdroid/`](./TUI-ConsoleLauncher/app/src/fdroid/)
- `playstore`: excludes the SMS-specific flavor code

## Security And Platform Constraints

Some constraints are intentional and should be preserved:

- `android:usesCleartextTraffic="false"`
- signature-protected command broadcast permission: `bhupendra.ai.launcher.permission.RECEIVE_CMD`
- `FLAG_IMMUTABLE` for `PendingIntent` usage
- `FileProvider`-based file sharing instead of raw `file://` URIs

The app targets modern Android while remaining compatible with API 21+:

- `compileSdk 34`
- `targetSdk 34`
- `minSdk 21`

## Repository Layout

```text
AI Launcher/
├── TUI-ConsoleLauncher/
│   ├── app/
│   │   ├── src/main/java/bhupendra/ai/launcher/
│   │   ├── src/main/assets/
│   │   ├── src/main/res/
│   │   └── src/fdroid/
│   ├── COMMANDS.md
│   └── README.md
├── docs/
│   └── superpowers/
├── adb-run.sh
├── AGENTS.md
├── CLAUDE.md
├── ARCHITECTURAL_GOLDEN_CONTEXT.md
└── USER_MANUAL.md
```

## Contributor Workflow

### Add a launcher command

1. Create a class in `commands/main/raw/`.
2. Implement `exec(ExecutePack pack)`.
3. Register it in `MainPack`.
4. Use `pack.get...()` accessors instead of reaching into `MainPack` internals.

### Add an AI tool

1. Create a class in `app/src/main/java/.../ai/tools/` extending `BaseAITool`.
2. Define the JSON argument schema in the constructor.
3. Implement `execute(Context, JSONObject args)`.
4. Register it in `AISubsystem.registerSystemTools()`.

### Add a shared service

1. Create the manager/service class.
2. Provide it from `di/AppModule` if it belongs in the Hilt graph.
3. Avoid adding feature-specific logic to generic core executors.

## Current Source-Of-Truth Notes

- Prefer current code and Gradle config over older prose docs when they disagree.
- The historical app README under `TUI-ConsoleLauncher/README.md` is useful context but contains stale details such as BusyBox-centric workflow and Java 17 claims.
- The build currently compiles with Java 8 compatibility in `app/build.gradle`.

## Related Docs

- [Architecture context](./ARCHITECTURAL_GOLDEN_CONTEXT.md)
- [Engineering context](./CLAUDE.md)
- [User manual](./USER_MANUAL.md)
- [App command/build notes](./TUI-ConsoleLauncher/COMMANDS.md)

## License

See [`TUI-ConsoleLauncher/LICENSE`](./TUI-ConsoleLauncher/LICENSE).
