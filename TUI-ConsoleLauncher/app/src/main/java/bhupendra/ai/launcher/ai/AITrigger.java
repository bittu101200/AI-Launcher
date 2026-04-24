package bhupendra.ai.launcher.ai;

import android.content.Context;
import android.graphics.Color;
import bhupendra.ai.launcher.tuils.Tuils;

import bhupendra.ai.launcher.managers.TerminalManager;

public class AITrigger {

    public interface ShellFallback {
        void triggerShell(String input);
    }

    private final AISubsystem aiSubsystem;
    private final Context context;
    private final ShellFallback shellFallback;

    public AITrigger(AISubsystem aiSubsystem, Context context, ShellFallback shellFallback) {
        this.aiSubsystem = aiSubsystem;
        this.context = context;
        this.shellFallback = shellFallback;
    }

    public boolean trigger(final String input) {
        if (aiSubsystem == null || !aiSubsystem.isAvailable()) return false;

        boolean agenticMode = bhupendra.ai.launcher.managers.xml.XMLPrefsManager.getBoolean(bhupendra.ai.launcher.managers.xml.options.Ai.agentic_mode);
        String finalInput = input;
        
        if (!agenticMode) {
            // Traditional mode: require "ai " prefix
            if (!input.toLowerCase().startsWith("ai ")) {
                return false;
            }
            finalInput = input.substring(3).trim();
        } else {
            // Agentic mode: if it has prefix, strip it; otherwise take as is
            if (input.toLowerCase().startsWith("ai ")) {
                finalInput = input.substring(3).trim();
            }
        }

        Tuils.sendOutput(Color.GRAY, context, "[thinking...]", TerminalManager.CATEGORY_OUTPUT);

        aiSubsystem.submit(finalInput, new AICallback() {
            private StringBuilder tokenBuffer = new StringBuilder();
            private final java.util.regex.Pattern TOOL_CALL_PATTERN = java.util.regex.Pattern.compile("^tool_[A-Za-z0-9_]+\\(.*\\)$", java.util.regex.Pattern.DOTALL);

            @Override public void onToken(String rid, String token) {
                tokenBuffer.append(token);
                // Buffer tool calls to avoid leaking them to UI
                if (tokenBuffer.toString().trim().startsWith("tool_")) {
                    return;
                }
                Tuils.sendOutput(Color.WHITE, context, token, TerminalManager.CATEGORY_AI);
            }

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT && response.text != null) {
                    if (response.isToolOutput) {
                        // Suppress tool results from UI, they are for AI consumption
                        Tuils.log("Suppressed tool output: " + response.text);
                        return;
                    }
                    String currentText = response.text.trim();
                    if (TOOL_CALL_PATTERN.matcher(currentText).matches()) {
                        return;
                    }
                    Tuils.sendOutput(Color.WHITE, context, response.text, TerminalManager.CATEGORY_AI);
                } else if (response.type == AIResponse.Type.ERROR) {
                    Tuils.sendOutput(Color.RED, context, "[AI error: " + response.errorMessage + "]", TerminalManager.CATEGORY_ERROR);
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {
                switch (state) {
                    case EXECUTING_TOOLS:
                        Tuils.sendOutput(Color.GRAY, context, "[executing tools...]", TerminalManager.CATEGORY_OUTPUT);
                        break;
                    case CANCELLED:
                        Tuils.sendOutput(Color.GRAY, context, "[cancelled]", TerminalManager.CATEGORY_OUTPUT);
                        break;
                    case TIMED_OUT_CONNECT:
                        Tuils.sendOutput(Color.YELLOW, context, "[AI unavailable \u2014 retrying as shell command]", TerminalManager.CATEGORY_OUTPUT);
                        if (shellFallback != null) shellFallback.triggerShell(input);
                        break;
                    case TIMED_OUT_INACTIVITY:
                        Tuils.sendOutput(Color.YELLOW, context, "[AI response timed out]", TerminalManager.CATEGORY_OUTPUT);
                        break;
                    case FAILED:
                        Tuils.sendOutput(Color.RED, context, "[AI request failed]", TerminalManager.CATEGORY_ERROR);
                        break;
                    default:
                        break;
                }
            }
        });

        return true;
    }
}