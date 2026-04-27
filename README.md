# AI Launcher

AI Launcher is a high-performance, modern Android terminal-style home screen launcher with a modular, tool-using AI assistant. It bridges the text-first philosophy of T-UI with Android 14+ security, agentic AI command execution, and a state-driven onboarding experience.

All Android application source code lives in [`TUI-ConsoleLauncher/`](./TUI-ConsoleLauncher/). The repository root contains project-level docs, plans, and automation scripts.

## 🚀 Recent Modernization (System-Modularisation)

The project has undergone a significant architectural overhaul to ensure stability on modern Android versions (API 34+) while hardening the AI integration:

- **Android 14 Security Hardening:** Fixed critical `PendingIntent` crashes by implementing explicit Class-based targeting for notification actions and remote inputs.
- **Smart Onboarding & Auto-Restore:** A new state-driven flow that automatically scans for T-UI backups in the Downloads folder and offers a seamless one-tap restoration.
- **Automated Permission Management:** Integrated intelligent requests for "All Files Access" (MANAGE_EXTERNAL_STORAGE) on Android 11+ to facilitate backup scanning and system management.
- **Refined Input Routing:** Hardened the command dispatcher to prevent natural language or numeric inputs from "falling through" to the shell during active AI setup or restore phases.
- **Preference Integrity:** Implemented a robust cache disposal system for `XMLPrefsManager` to ensure instant application of restored configurations and theme changes.

## Core Capabilities

- **Modern Terminal UI:** Configurable prompt, high-quality theme presets (`cyberpunk`, `bw`, etc.), and interactive shortcut buttons.
- **Agentic AI Subsystem:** Modular AI core supporting Gemini, Claude, OpenAI, and Ollama with structured tool execution (launching apps, system toggles, web search).
- **Hardened Notifications:** Secure ingestion, filtering, and AI-powered summarization/reply actions using Android's latest security primitives.
- **Integrated Tooling:** Built-in BusyBox manager (`bbman`) for verified Linux binary installation.

## Architecture Overview

The app follows a service-oriented architecture with a clear separation between the UI, the command controller, and the AI agentic layer.

### Main Runtime Entrypoints

- [`LauncherActivity.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/LauncherActivity.java): Lifecycle management, permission arbitration, and UI reload orchestration.
- [`AIOnboardingManager.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/platform/AIOnboardingManager.java): State-driven setup flow for AI providers and backup restoration.
- [`MainManager.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/MainManager.java): Central command dispatcher with synchronized routing for traditional commands, AI, and shell.
- [`TerminalManager.java`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/managers/TerminalManager.java): Low-level input handling and redirection priority management.

### AI Subsystem

The AI stack lives under [`app/src/main/java/bhupendra/ai/launcher/ai/`](./TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/).

- `AISubsystem`: Manages the request lifecycle, tool registration, and provider-specific execution.
- `AITrigger`: Logic for explicit `ai ` invocation and agentic fallbacks.
- `BaseAITool`: Abstract base for AI tools allowing the assistant to interact with Android system services (Sms, Contacts, Volume, Apps, etc.).

## Build and Run

Run all Gradle commands from the `TUI-ConsoleLauncher/` directory.

### Prerequisites

- Android SDK (API 34+)
- Java 17 toolchain
- ADB access for device testing

### Installation

```bash
cd TUI-ConsoleLauncher
./gradlew :app:installDebug  # For development
./gradlew :app:installRelease # For production testing (hardened)
```

## Security & Compliance

The launcher is built with **Security-by-Design** principles:
- **MASVS Alignment:** Scoped Storage, secure `FileProvider` URIs, and enforced TLS.
- **Signature Permissions:** Programmable command entry is protected by signature-level custom permissions.
- **PendingIntent Security:** Strict enforcement of `FLAG_IMMUTABLE` and `FLAG_MUTABLE` based on Android 14 requirements.

