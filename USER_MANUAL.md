# AI Launcher: The Definitive User Manual

Welcome to the **AI Launcher**, a rock-solid, Linux-style terminal launcher for Android, powered by a modular AI brain. This guide will walk you through every command and show you how to leverage your new AI assistant to automate your life.

---

## 1. Terminal Commands
These are the core building blocks of the launcher. Type these directly into the terminal.

### **Apps & System**
*   `apps`: List all installed applications. Use `apps -ls` for a clean list.
*   `open [appName]`: Launch an application.
*   `uninstall [appName]`: Remove an app from your device.
*   `status`: View system health, battery, RAM, and storage info.
*   `volume [0-100]`: Adjust system volume levels.
*   `brightness [0-100]`: Change screen brightness.
*   `wifi`: Toggle WiFi state or connect to a network.
*   `bluetooth`: Toggle Bluetooth on/off.
*   `airplane`: Toggle Airplane Mode.
*   `flash`: Turn your flashlight on or off.
*   `vibrate [ms]`: Vibrate the device for a specific duration.
*   `restart`: Reboots the launcher UI.
*   `exit`: Close the launcher (or return to your previous home app).

### **Utilities**
*   `calc [expression]`: A powerful command-line calculator (e.g., `calc 5 + 5 * 2`).
*   `time`: Display current time and date in a customizable format.
*   `location`: Get your current GPS coordinates.
*   `notes`: A simple, persistent notepad. Use `notes -add "text"` to save a note.
*   `rss`: Manage and read your favorite RSS feeds.
*   `weather`: Fetch live weather updates based on your location.
*   `search [query]`: Search the web using your default engine.

### **Customization & Files**
*   `theme`: Change colors, fonts, and the overall look of your terminal.
*   `alias`: Create shortcuts for long commands (e.g., `alias -add g "search -g"`).
*   `regex`: Advanced text replacement rules for the terminal.
*   `backup`: **(New)** Copies all your TUI settings, notes, and AI config to `Downloads/TUI_Backup`.
*   `restore`: **(New)** Interactively restores your settings from a previous backup.
*   `tuixt`: A built-in text editor for modifying files on the fly.

---

## 2. The AI Assistant
Type `ai` followed by your request to talk to your personal assistant. The AI isn't just a chatbot; it has deep access to your device.

### **Great Things You Can Do With AI:**

#### **📱 Communication & Contacts**
*   *"ai call Mom"* (Searches your contacts and initiates a call instantly).
*   *"ai send an sms to Poorab saying I'll be late"* (Handles contact resolution and sends the message).
*   *"ai add a new contact named John with number 9876543210"*

#### **🤖 Intelligent Automation**
*   **Auto-Replies:** *"ai always reply to माँ saying 'I am in a meeting' between 2pm and 5pm"*
*   **Urgency Detection:** When the AI auto-replies in the background, it will **BEEP** and alert you in **ORANGE** if it detects the sender needs your immediate attention.
*   **Update Logs:** You can ask *"ai what's the update?"* to see a summary of all notifications the AI caught while you were busy.

#### **🐧 Advanced Linux Integration**
*   *"ai run 'ls -la' in termux"*
*   *"ai git commit my changes in the launcher folder"*
*   The AI can execute any Linux command via the **Termux Engine**, making it a powerhouse for developers.

#### **🔍 Web Intelligence**
*   *"ai who won the match last night?"* (Fetches and renders a clean, ad-free summary in your terminal).
*   *"ai summarize this website: [URL]"*

#### **⚙️ System Mastery**
*   *"ai turn on my flashlight and set brightness to 10%"*
*   *"ai backup my settings and beep me when done"*
*   *"ai change my terminal input color to green"* (The AI knows every config key and can re-theme your device on command).

#### **🧠 Persistent Memory**
*   *"ai remember that my favorite color is obsidian"*
*   Later, you can ask *"ai what's my favorite color?"* and it will retrieve the info from its long-term disk memory.

---

## 3. Advanced Features

### **Proactive Signaling**
Your AI will **BEEP** you using the system speaker when:
1.  It finishes a long task (like a backup).
2.  It needs you to make a choice.
3.  It detects an **URGENT** incoming message.

### **The Onboarding Flow**
If you ever start fresh, the AI will guide you through:
-   Detecting old backups to save your time.
-   Configuring your API keys for Gemini, Claude, or OpenAI.
-   Setting up your basic preferences.

---

## 4. Tips for Success
-   **Be Specific:** Instead of "do it," say "ai backup my files."
-   **Use the AI Prefix:** Always start with the word `ai` to trigger the assistant.
-   **Terminal Power:** You can mix and match. Run a TUI command like `clear` and then follow up with an `ai` request.
-   **Check the Orange Logs:** If you hear a loud beep, look at your terminal—the AI has found something urgent for you.

**Your terminal is now more than just a launcher. It's a partner.**
