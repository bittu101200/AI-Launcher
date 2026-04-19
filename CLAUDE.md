# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **T-UI Linux CLI Launcher** — an Android terminal-style home screen launcher that emulates a Linux CLI environment. It is a modernized fork targeting API 34 (Android 14), with min SDK API 21 (Android 5.0).

All source code lives in `TUI-ConsoleLauncher/`.

## Build Commands

Run all commands from `TUI-ConsoleLauncher/`:

```bash
# Make gradlew executable (first time only)
chmod +x gradlew

# Build F-Droid debug APK (includes SMS permissions)
./gradlew assembleFdroidDebug
# Output: app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk

# Build Play Store variant
./gradlew assemblePlaystoreDebug

# Release build (requires signing config in local.properties)
./gradlew assembleRelease

# Clean
./gradlew clean
```

## Deployment (ADB)

```bash
# Start emulator
emulator -avd Pixel_9_Pro -gpu host -accel on &

# Install (overwriting existing)
adb wait-for-device
adb install -r app/build/outputs/apk/fdroid/debug/app-fdroid-debug.apk

# Filter logs
adb logcat | grep ohi.andre

# Uninstall
adb uninstall ohi.andre.consolelauncher
```

## Architecture

The app follows an MVC-inspired pattern using Android `LocalBroadcastManager` for internal communication:

- **`LauncherActivity.java`** — Main Activity. Handles lifecycle, permission requests, input routing, and theme initialization.
- **`UIManager.java`** — Terminal-style UI renderer. Manages text display, cursor, color theming, and input/output layout (~1744 lines).
- **`MainManager.java`** — Command execution orchestrator. Manages broadcast receivers that route command input/output and handle I/O redirection.

### Command System

Commands live in `commands/main/raw/` (one class per command) and all implement the `Command` interface. `MainPack.java` aggregates all built-in commands. There is a secondary pack in `commands/tuixt/`.

Special command types in `commands/main/specific/`:
- `APICommand.java` — HTTP-based commands
- `ParamCommand.java` — Commands with parsed parameters
- `RedirectCommand.java` — I/O redirection support
- `PermanentSuggestionCommand.java` — Commands that always appear in suggestions

### Managers

`managers/` contains subsystem managers:
- `XMLPrefsManager.java` + `managers/xml/options/` — All app preferences stored as XML in scoped storage (`Context.getExternalFilesDir()`).
- `AppsManager.java` — Enumerates installed apps for the `apps` command.
- `SuggestionsManager.java` — Drives the autocomplete bar.
- `music/` — Background music playback via `MusicService`.
- `notifications/` — Notification listener service and reply system.
- `TuiLocationManager.java` — GPS/location services.

### Product Flavors

- **`fdroid`** — Includes `commands/main/raw/sms.java` (SMS permissions). Source in `app/src/fdroid/`.
- **`playstore`** — No SMS command.

## Key Configuration

- Signing: configured via `local.properties` (keys: `storeFile`, `storePassword`, `keyAlias`, `keyPassword`).
- Lint is disabled for release builds (`checkReleaseBuilds false`).
- R8/ProGuard minification is enabled for release. `proguard-rules.pro` keeps all classes in `commands/`, `managers/`, and `tuils/` from obfuscation by name.
- Custom permission `ohi.andre.consolelauncher.permission.RECEIVE_CMD` (`protectionLevel="signature"`) gates external programmatic command access.

## Security Constraints

- All network calls must use HTTPS — `android:usesCleartextTraffic="false"` is enforced globally.
- BusyBox binaries in `BusyBoxInstaller.java` are verified against hardcoded SHA-256 hashes; do not bypass this check.
- File sharing must go through `GenericFileProvider` (FileProvider), never raw `file://` URIs.
- `PendingIntent` flags must use `FLAG_IMMUTABLE`.
