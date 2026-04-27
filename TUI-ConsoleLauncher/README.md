# T-UI Linux CLI Launcher

Updated for compatibility with modern Android versions (API 34+) and enhanced with security hardening and an integrated AI subsystem.

---

## 🚀 Recent Changes & Modernization

These updates ensure the launcher remains functional, secure, and performant on modern Android devices (Android 11 through Android 14+).

> **Pro Tip:** On the very first install, if background transparency does not take effect immediately, simply type \`restart\` in the terminal and press enter.

### 🤖 AI Subsystem (Modularisation)
The launcher now features a modular AI subsystem that transforms your terminal into a smart assistant.
*   **Smart Onboarding:** A new guided setup flow for AI providers (Gemini, Claude, OpenAI, Ollama).
*   **Auto-Restore:** Automatically detects T-UI backups in your Downloads folder on fresh installs and offers a one-tap restore.
*   **Context-Aware Tools:** AI can now interact with your device (launch apps, check system info) using natural language.
*   **Security Hardening:** AI interactions are isolated and use secure credential management.

### ⌨️ New Commands
*   **`ai [prompt]`**: Interact directly with your selected AI provider.
*   **`username [user] [device]`**: Instantly customize your terminal prompt. Changes both the username and device name and reloads the UI to apply.
*   **`theme -preset [name]`**: Rapidly switch between high-quality pre-configured themes.
    *   **Available Presets:** `blue`, `red`, `green`, `pink`, `bw`, `cyberpunk`.
    *   **Smart Suggestions:** Applying a preset automatically colors the suggestion bar and shortcut buttons to match the aesthetic.
*   **`bbman`**: The new BusyBox manager for installing and verifying Linux binaries.

### ✨ Enhanced Features
*   **Automated Permissions:** Smart detection and request for "All Files Access" (MANAGE_EXTERNAL_STORAGE) required for backup scanning and file management on Android 11+.
*   **Built-in BusyBox Manager:** Gain access to 300+ Linux commands (ls, grep, awk, top, etc.) via the new `bbman -install` command.
*   **Theme Preset Shortcut Buttons:** Enhanced the `theme -preset` command to show interactive shortcut buttons for presets.
*   **Synchronized Theme UI:** Applying a preset now automatically colors the shortcut buttons (suggestions) to match the overall theme.
*   **One-Tap Application:** Shortcut buttons for theme presets execute immediately upon clicking.

---

## 🛡 Security Hardening (Android 14+ & OWASP MASVS)

This project has been audited and hardened following the **OWASP Mobile Application Security Verification Standard (MASVS)** and latest Android 14 security requirements.

### 📦 Platform & OS Security
*   **Android 14 Explicit Intents:** Hardened `PendingIntent` logic to use explicit Class-based targets for notification actions, preventing `CannotPostForegroundServiceNotificationException`.
*   **Mutable Intent Safety:** Strategic use of `FLAG_MUTABLE` where required (e.g., RemoteInput) with strict `IMMUTABLE` defaults for all other system interactions (Android 12+).
*   **Signature-Level Protection:** Custom permission `bhupendra.ai.launcher.permission.RECEIVE_CMD` with `protectionLevel="signature"` ensures only authorized apps can trigger launcher commands.

### 💾 Data Storage and Privacy
*   **Scoped Storage Implementation:** All application data has been moved from public external storage (`/sdcard/t-ui/`) to secure, app-private **Scoped Storage** (`Context.getExternalFilesDir()`).
*   **Backup Protection:** `android:allowBackup` is set to `false` to prevent sensitive data extraction via ADB backups (MASVS-STORAGE-1).
*   **Secure File Sharing:** Uses `FileProvider` for secure, permission-based file sharing.

---

## 🛠 Modern Build System
*   **Target SDK:** Updated to **API 34 (Android 14)**.
*   **Min SDK:** API 21 (Android 5.0).
*   **AndroidX Migration:** Fully migrated from legacy Support Libraries to **AndroidX**.
*   **Gradle & AGP:** Updated to Gradle 8.2 and Android Gradle Plugin 8.2.0.
*   **Java Compatibility:** Built with **Java 17** support.
*   **Dependency Injection:** Integrated **Hilt/Dagger** for modern modular architecture.


---

## 🔗 Useful links

**Official community**&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[Reddit](https://www.reddit.com/r/tui_launcher/)**<br>
**Official Group**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[Telegram](https://t.me/tuilauncher)**<br>
**Wiki**&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;-->&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;**[GitHub.com](https://github.com/Andre1299/TUI-ConsoleLauncher/wiki)**<br>

## 📚 Open Source Libraries
* [**CompareString2**](https://github.com/fAndreuzzi/CompareString2)
* [**OkHttp**](https://github.com/square/okhttp)
* [**HTML cleaner**](http://htmlcleaner.sourceforge.net/)
* [**JsonPath**](https://github.com/json-path/JsonPath)
* [**jsoup**](https://github.com/jhy/jsoup/)
