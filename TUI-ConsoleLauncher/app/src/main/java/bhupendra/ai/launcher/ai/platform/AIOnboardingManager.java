package bhupendra.ai.launcher.ai.platform;

import android.content.Context;
import android.graphics.Color;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Ai;
import bhupendra.ai.launcher.tuils.Tuils;

public class AIOnboardingManager {

    private static final String ONBOARDING_STEP_KEY = "ai_onboarding_step";
    private static final String SELECTED_PROVIDER_KEY = "ai_onboarding_provider";

    public static void checkAndStart(Context context) {
        String provider = XMLPrefsManager.get(Ai.provider);
        String key = XMLPrefsManager.get(Ai.api_key);

        if (provider.equals("mock") || key.isEmpty()) {
            startOnboarding(context);
        }
    }

    private static void startOnboarding(Context context) {
        Tuils.sendOutput(Color.CYAN, context, "\n--- AI LAUNCHER SETUP ---", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(context, "Welcome! Let's configure your AI assistant.", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(context, "Please choose an AI provider by typing its name:", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, context, "- gemini (Recommended, fast & free tier available)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, context, "- openai (requires GPT-4o key)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, context, "- ollama (local, requires server URL)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(context, "\nType your choice below:", TerminalManager.CATEGORY_OUTPUT);
        
        // Use Redirection to capture the next input
        // We'll need a specialized Redirection class
    }

    public static class ProviderRedirection implements bhupendra.ai.launcher.tuils.interfaces.Redirectator.Redirect {
        @Override
        public String onRedirect(bhupendra.ai.launcher.commands.main.MainPack pack) {
            String input = afterObjects.get(0).toLowerCase().trim();
            if (input.equals("gemini") || input.equals("openai") || input.equals("ollama")) {
                XMLPrefsManager.set(Ai.provider, input);
                pack.redirectator.cleanup();
                
                Tuils.sendOutput(pack.context, "Selected: " + input, TerminalManager.CATEGORY_OUTPUT);
                Tuils.sendOutput(pack.context, "Now, please enter your API Key:", TerminalManager.CATEGORY_OUTPUT);
                
                pack.redirectator.setRedirect(new KeyRedirection());
                return null;
            } else {
                return "Invalid choice. Please choose: gemini, openai, or ollama.";
            }
        }

        @Override public boolean isWaitingPermission() { return false; }
        public java.util.List<String> afterObjects = new java.util.ArrayList<>();
    }

    public static class KeyRedirection implements bhupendra.ai.launcher.tuils.interfaces.Redirectator.Redirect {
        @Override
        public String onRedirect(bhupendra.ai.launcher.commands.main.MainPack pack) {
            String key = afterObjects.get(0).trim();
            if (key.length() < 5) return "That key seems too short. Please try again:";
            
            XMLPrefsManager.set(Ai.api_key, key);
            pack.redirectator.cleanup();
            
            Tuils.sendOutput(Color.CYAN, pack.context, "AI Configuration Complete!", TerminalManager.CATEGORY_OUTPUT);
            Tuils.sendOutput(pack.context, "Restarting to apply changes...", TerminalManager.CATEGORY_OUTPUT);
            
            // Reload the activity
            if (pack.context instanceof bhupendra.ai.launcher.tuils.interfaces.Reloadable) {
                ((bhupendra.ai.launcher.tuils.interfaces.Reloadable) pack.context).reload();
            }
            return null;
        }

        @Override public boolean isWaitingPermission() { return false; }
        public java.util.List<String> afterObjects = new java.util.ArrayList<>();
    }
}
