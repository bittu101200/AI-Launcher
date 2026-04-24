# Repository Guide

- Primary app repo: `TUI-ConsoleLauncher/`. Run Gradle, build, and test commands from that directory, not from the workspace root.
- Highest-signal context files: `CLAUDE.md` and `ARCHITECTURAL_GOLDEN_CONTEXT.md`. They are useful, but prefer current code/config when they disagree with prose.

# Build And Test

- First-time setup: `chmod +x gradlew`
- Debug APKs: `./gradlew assembleFdroidDebug` or `./gradlew assemblePlaystoreDebug`
- Release APK: `./gradlew assembleRelease` using signing values from `TUI-ConsoleLauncher/local.properties` (`storeFile`, `storePassword`, `keyAlias`, `keyPassword`)
- Clean: `./gradlew clean`
- Lint: `./gradlew lint`
- Unit tests are flavor-specific. `testDebugUnitTest` is ambiguous; use `./gradlew testFdroidDebugUnitTest` or `./gradlew testPlaystoreDebugUnitTest`.
- Focused unit test: `./gradlew testFdroidDebugUnitTest --tests 'bhupendra.ai.launcher.ai.AISubsystemToolExecutionTest'`

# Repo Shape

- Single-module Android app: `TUI-ConsoleLauncher/settings.gradle` only includes `:app`.
- Main entrypoints: `app/src/main/java/bhupendra/ai/launcher/LauncherActivity.java`, `MainManager.java`, `UIManager.java`, and `TUIApplication.java`.
- Hilt is present, but not everything uses it. `di/AppModule.java` provides a few singletons; `AISubsystem` is still a manual singleton created outside Hilt.
- Built-in launcher commands live in `app/src/main/java/.../commands/main/raw/` and are discovered through `CommandGroup`/`MainPack`.
- Flavor-only code exists under `app/src/fdroid/`; `sms.java` is F-Droid only.

# AI And Command Wiring

- AI runtime wiring is split between `LauncherActivity` (creation), `MainManager` (routing/fallback), `AITrigger` (prefix/agentic behavior), and `AISubsystem` (request/tool lifecycle).
- In normal mode, `AITrigger` requires the `ai ` prefix. In agentic mode, all input may route to AI first unless a TUI command is detected.
- `adb-run.sh` at the workspace root sends commands by broadcast and waits on logcat tag `AI_OUTPUT` for `CMD_FINISHED` or `AI_TURN_FINISHED`.
- Current non-AI completion logic in `MainManager` treats a command as AI when the parsed command class is `ai` or the raw input starts with `ai `; do not rely on older docs that describe a stricter exact-match check.

# Extension Points

- Add a launcher command by creating a class in `commands.main.raw`, implementing `exec(ExecutePack pack)`, and registering it in `MainPack`.
- Use `pack.get...()` accessors from `ExecutePack`/`CommandContext`; do not add new casts back to `MainPack` unless there is no existing interface path.
- Add an AI tool by creating a class in `app/src/main/java/.../ai/tools/` extending `BaseAITool` and registering it in `AISubsystem.registerSystemTools()`.

# Verified Gotchas

- Prefer executable config over README prose: `app/build.gradle` compiles with Java 8 compatibility (`sourceCompatibility`/`targetCompatibility` 1.8), even though `README.md` says Java 17.
- The runtime AI system prompt is read from `FileSystemManager.getFolder()/ai_system_prompt.md` in `AISubsystem.getSystemPrompt()`. Editing only `app/src/main/assets/ai_system_prompt.md` does not change the live prompt path used by current code.
- App-private storage is the working assumption in current code: `FileSystemManager.init()` uses `context.getExternalFilesDir(null)` and falls back to `getFilesDir()`.
- Security-sensitive constraints worth preserving: `android:usesCleartextTraffic="false"`, signature permission `bhupendra.ai.launcher.permission.RECEIVE_CMD`, and `FLAG_IMMUTABLE` for `PendingIntent` usage.
