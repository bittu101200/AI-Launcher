package bhupendra.ai.launcher.managers.xml.options;

import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsElement;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;

public enum Ai implements XMLPrefsSave {

    enabled {
        @Override public String defaultValue() { return "true"; }
        @Override public String info() { return "Enable AI features"; }
        @Override public String type() { return XMLPrefsSave.BOOLEAN; }
    },
    provider {
        @Override public String defaultValue() { return "mock"; }
        @Override public String info() { return "AI provider: claude, openai, ollama, gemini, mock"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    api_key {
        @Override public String defaultValue() { return ""; }
        @Override public String info() { return "API key for the selected provider"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    base_url {
        @Override public String defaultValue() { return ""; }
        @Override public String info() { return "Base URL for openai/ollama (e.g. http://localhost:11434)"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    model {
        @Override public String defaultValue() { return ""; }
        @Override public String info() { return "Model name override. Leave empty for provider default."; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    always_on_fallback {
        @Override public String defaultValue() { return "true"; }
        @Override public String info() { return "Send unrecognized input to AI before shell fallback"; }
        @Override public String type() { return XMLPrefsSave.BOOLEAN; }
    },
    memory_mode {
        @Override public String defaultValue() { return "session"; }
        @Override public String info() { return "Conversation memory: stateless, session, persistent"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    history_token_cap {
        @Override public String defaultValue() { return "4000"; }
        @Override public String info() { return "Max token budget for conversation history"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    compact_threshold {
        @Override public String defaultValue() { return "80"; }
        @Override public String info() { return "History fill % that triggers auto-compaction (0-100)"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    connect_timeout_ms {
        @Override public String defaultValue() { return "10000"; }
        @Override public String info() { return "Connect timeout ms before TIMED_OUT_CONNECT"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    inactivity_timeout_ms {
        @Override public String defaultValue() { return "30000"; }
        @Override public String info() { return "Inactivity timeout ms before TIMED_OUT_INACTIVITY"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    max_tokens {
        @Override public String defaultValue() { return "2048"; }
        @Override public String info() { return "Maximum tokens in AI response"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    confirm_state_changing {
        @Override public String defaultValue() { return "false"; }
        @Override public String info() { return "Require confirmation for STATE_CHANGING tools"; }
        @Override public String type() { return XMLPrefsSave.BOOLEAN; }
    };

    @Override public XMLPrefsElement parent() { return XMLPrefsManager.XMLPrefsRoot.AI; }
    @Override public String label() { return name(); }
    @Override public String[] invalidValues() { return null; }
    public String getLowercaseString() { return label(); }
    public String getString() { return label(); }
}