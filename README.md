# AI Launcher

> My attempt at building a truly agentic operating system — starting with the home screen.

The idea is simple but kind of wild: what if your phone's home screen *understood* you? Not just responded to taps, but actually reasoned about what you want, used tools to get things done, and felt more like a thinking layer on top of Android than a grid of icons.

This is that experiment. It's a terminal-style Android launcher where an AI agent lives at the command line — and can actually *do* things: launch apps, toggle settings, read your notifications, send messages, search the web, run shell commands. It's not a chatbot wrapper. It's trying to be something closer to an OS shell with a brain.

It's rough in places. Some things are held together with duct tape. But the core idea works, and I'm genuinely excited about where it's going.

---

## What it actually does

You get a terminal on your home screen. You type (or just talk) and the AI figures out what you mean and executes it — using real Android APIs, not simulated responses.

- **It's your launcher.** This replaces your home screen. Everything goes through the terminal.
- **The AI uses tools.** It can open apps, toggle Wi-Fi/Bluetooth/flashlight, send SMS, look up contacts, adjust volume, search the web, and run Termux shell commands. These are real actions, not just text replies.
- **It supports multiple AI backends.** Gemini, Claude, OpenAI, and Ollama (for local/offline use) — switchable from config.
- **It reads your notifications.** Notifications are ingested, filtered, and can be summarized or replied to by the AI.
- **It has themes.** `cyberpunk`, `bw`, and more — because a terminal should look cool.
- **It onboards itself.** First launch walks you through picking an AI provider, entering your API key, and optionally restoring a T-UI backup — all from inside the terminal.

---

## The bigger vision

I've been thinking about what a truly agentic OS would look like. Not an AI assistant bolted onto an existing OS, but something where the AI *is* the interface layer — where instead of you navigating menus and tapping through apps, you just say what you want and the system figures out how to do it.

This launcher is my first attempt at that. The terminal is the UI because terminals are honest — every action is explicit, visible, and composable. The AI sits between you and the system and handles the "how." You handle the "what."

Long-term I want this to be something where:
- The AI proactively manages your phone (not just reactively)
- Tools are composable — the AI chains multiple actions to complete complex tasks
- The system learns your patterns and preferences over time
- Eventually, maybe, it runs on a custom ROM where the AI has deeper OS access

For now, it's a launcher. But I think it's a real step toward something bigger.

---

## How it's built

All app source code is in [`TUI-ConsoleLauncher/`](./TUI-ConsoleLauncher/). The repo root has scripts and docs.

The architecture has three main layers that talk to each other:

**UI Layer** — `LauncherActivity.java` + `UIManager`
The home screen activity. Handles the terminal display, keyboard, theme rendering, and permission flows. It wires up the input/output pipeline and delegates everything else downward.

**Command Layer** — `MainManager.java`
The central dispatcher. Takes your input and decides: is this a built-in command? A shell command? Something for the AI? It routes accordingly and keeps the AI from accidentally swallowing commands it shouldn't.

**AI Layer** — `ai/` package
The interesting part. `AISubsystem` manages the full request lifecycle — sending your message to the provider, receiving tool calls back, executing them via `AndroidToolExecutor`, and looping until the task is done. `BaseAITool` is the abstract base that all Android-interacting tools extend (SMS, contacts, volume, app launcher, etc.). `AITrigger` decides when to hand off to the AI vs. handle things locally.

**Onboarding** — `AIOnboardingManager.java`
A state machine that runs on first launch (or when AI isn't configured). Guides you through provider selection, API key entry, and backup restoration — all rendered in the terminal itself.

---

## Getting it running

You need Android SDK API 34+, Java 17, and ADB for device testing.

```bash
cd TUI-ConsoleLauncher

# Debug build (for development)
./gradlew :app:installDebug

# Release build (for daily use)
./gradlew :app:installRelease
```

After installing, set it as your default launcher. On first launch, the onboarding flow will walk you through everything.

For AI to work you'll need an API key from one of the supported providers (Gemini, Claude, OpenAI) — or have Ollama running locally if you want fully offline operation.

---

## Recent changes (System-Modularisation branch)

The current branch has been a big cleanup focused on making the Android integration actually solid:

- Fixed crashes caused by implicit `PendingIntent` targeting — Android 14 requires explicit class references for notification actions, and the old code didn't do that
- Rewrote the onboarding flow as a proper state machine so it doesn't break mid-setup
- Added auto-detection and one-tap restore for T-UI backup files in Downloads
- Stopped natural language and numeric input from leaking into the shell when the AI setup flow was active
- Fixed a bug where restored configs and theme changes didn't apply until restart — now they apply instantly via a cache invalidation in `XMLPrefsManager`

---

## Status

This is an active personal project. I'm learning Android and AI systems simultaneously while building this, so the code reflects that — some parts are clean and intentional, others are "it works, I'll fix it later."

If you're a fellow student or just someone interested in agentic systems, feel free to dig around. Issues and PRs are welcome, though I can't promise fast responses.

---

*Built on top of [T-UI Launcher](https://github.com/tui-dev/t-ui) — a terminal launcher for Android that I've been heavily modifying and extending.*
