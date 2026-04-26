package bhupendra.ai.launcher.ai;

import android.content.Context;
import android.graphics.Color;
import org.json.JSONObject;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.Tuils;

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

    public boolean triggerDirect(final String query) {
        return submitQuery(query, null);
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

        return submitQuery(finalInput, input);
    }

    private boolean submitQuery(final String query, final String fallbackInput) {
        if (aiSubsystem == null || !aiSubsystem.isAvailable()) return false;

        Tuils.sendOutput(Color.GRAY, context, "[thinking...]", TerminalManager.CATEGORY_OUTPUT);

        aiSubsystem.submit(query, new AICallback() {
            private StringBuilder tokenBuffer = new StringBuilder();
            private final java.util.regex.Pattern TOOL_CALL_PATTERN = java.util.regex.Pattern.compile("^tool_[A-Za-z0-9_]+\\(.*\\)$", java.util.regex.Pattern.DOTALL);

            @Override public void onToken(String rid, String token) {
                // Hold tokens until the final response so markdown is rendered consistently.
                tokenBuffer.append(token);
            }

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT && response.text != null) {
                    if (response.isToolOutput) {
                        if (response.toolCall != null) {
                            Tuils.sendOutput(Color.GRAY, context, buildToolStatus(response), TerminalManager.CATEGORY_OUTPUT);
                        }
                        return;
                    }
                    String currentText = response.text.trim();
                    if (currentText.startsWith("[AI wants to]")) {
                        Tuils.sendOutput(Color.GRAY, context, response.text, TerminalManager.CATEGORY_OUTPUT);
                        return;
                    }
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
                        if (shellFallback != null && fallbackInput != null) shellFallback.triggerShell(fallbackInput);
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

    private String buildToolStatus(AIResponse response) {
        try {
            JSONObject args = new JSONObject(response.toolCall.argumentsJson);
            if ("system.web_search_query".equals(response.toolCall.toolName)) {
                return "[searching: " + args.optString("query", "...") + "]";
            }
            if ("system.web_fetch".equals(response.toolCall.toolName)) {
                String url = args.optString("url", "...");
                if (url.length() > 40) url = url.substring(0, 37) + "...";
                return "[fetching: " + url + "]";
            }
        } catch (Exception ignored) {}
        return "[executed: " + response.toolCall.toolName + "]";
    }
}
