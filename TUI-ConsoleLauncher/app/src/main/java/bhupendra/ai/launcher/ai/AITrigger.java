package bhupendra.ai.launcher.ai;

import android.content.Context;
import android.graphics.Color;
import org.json.JSONObject;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.terminal.TerminalEventBus;

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

        final boolean supportsStreaming = aiSubsystem.supportsStreaming();

        if (!supportsStreaming) {
            Tuils.sendOutput(Color.GRAY, context, "[thinking...]", TerminalManager.CATEGORY_OUTPUT);
        }

        final boolean[] streamStartedHolder = new boolean[]{false};

        final AICallback callback = new AICallback() {
            private final StringBuilder apiThinkingBuffer = new StringBuilder();
            private final StringBuilder apiContentBuffer = new StringBuilder();
            private final java.util.regex.Pattern TOOL_CALL_PATTERN = java.util.regex.Pattern.compile("^tool_[A-Za-z0-9_]+\\(.*\\)$", java.util.regex.Pattern.DOTALL);

            @Override public void onThinkingToken(String rid, String token) {
                apiThinkingBuffer.append(token);
                if (supportsStreaming) {
                    postUpdate(rid);
                }
            }

            @Override public void onToken(String rid, String token) {
                apiContentBuffer.append(token);
                if (supportsStreaming) {
                    postUpdate(rid);
                }
            }

            private void postUpdate(String rid) {
                ContentSplit split = ContentSplit.split(apiContentBuffer.toString());
                String combinedThinking = apiThinkingBuffer.toString() + split.thinking;
                TerminalEventBus.get().post(new TerminalEventBus.UpdateStreamEvent(combinedThinking, split.content, TerminalManager.CATEGORY_AI, rid));
            }

            @Override public void onResponse(AIResponse response) {
                String rid = response.requestId;
                if (response.isToolOutput) {
                    if (response.toolCall != null) {
                        Tuils.sendOutput(Color.GRAY, context, buildToolStatus(response), TerminalManager.CATEGORY_OUTPUT);
                    }
                    return;
                }
                ContentSplit split = ContentSplit.split(apiContentBuffer.toString());
                String combinedThinking = apiThinkingBuffer.toString() + split.thinking;
                if (supportsStreaming && streamStartedHolder[0]) {
                    ContentSplit finalSplit = ContentSplit.split(response.text != null ? response.text : split.content);
                    String finalCombinedThinking = apiThinkingBuffer.length() > 0 ? apiThinkingBuffer.toString() : finalSplit.thinking;
                    TerminalEventBus.get().post(new TerminalEventBus.FinishStreamEvent(finalCombinedThinking, finalSplit.content, rid));
                    streamStartedHolder[0] = false;
                }
                if (response.type == AIResponse.Type.TEXT && response.text != null) {
                    String currentText = response.text.trim();
                    if (currentText.startsWith("[AI wants to]")) {
                        Tuils.sendOutput(Color.GRAY, context, response.text, TerminalManager.CATEGORY_OUTPUT);
                        return;
                    }
                    if (TOOL_CALL_PATTERN.matcher(currentText).matches()) {
                        return;
                    }
                    if (!supportsStreaming) {
                        ContentSplit finalSplit = ContentSplit.split(response.text);
                        if (finalSplit.thinking != null && finalSplit.thinking.length() > 0) {
                            Tuils.sendOutput(Color.GRAY, context, finalSplit.thinking, TerminalManager.CATEGORY_OUTPUT);
                        }
                        Tuils.sendOutput(Color.WHITE, context, finalSplit.content, TerminalManager.CATEGORY_AI);
                    }
                } else if (response.type == AIResponse.Type.ERROR) {
                    Tuils.sendOutput(Color.RED, context, "[AI error: " + response.errorMessage + "]", TerminalManager.CATEGORY_ERROR);
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {
                if (supportsStreaming && !streamStartedHolder[0] && (state == AIRequestState.FOLLOWUP || state == AIRequestState.THINKING)) {
                    streamStartedHolder[0] = true;
                    apiThinkingBuffer.setLength(0);
                    apiContentBuffer.setLength(0);
                    TerminalEventBus.get().post(new TerminalEventBus.StartStreamEvent(rid));
                }
                ContentSplit split = ContentSplit.split(apiContentBuffer.toString());
                String combinedThinking = apiThinkingBuffer.toString() + split.thinking;
                if (supportsStreaming && streamStartedHolder[0]) {
                    if (state != AIRequestState.THINKING && state != AIRequestState.FOLLOWUP && state != AIRequestState.STREAMING) {
                        TerminalEventBus.get().post(new TerminalEventBus.FinishStreamEvent(combinedThinking, split.content, rid));
                        streamStartedHolder[0] = false;
                    }
                }
                switch (state) {
                    case EXECUTING_TOOLS:
                        Tuils.sendOutput(Color.GRAY, context, "[executing tools...]", TerminalManager.CATEGORY_OUTPUT);
                        break;
                    case CANCELLED:
                        Tuils.sendOutput(Color.GRAY, context, "[cancelled]", TerminalManager.CATEGORY_OUTPUT);
                        break;
                    case TIMED_OUT_CONNECT:
                        if (shellFallback != null && shouldFallbackToShell(fallbackInput)) {
                            Tuils.sendOutput(Color.YELLOW, context, "[AI unavailable — retrying as shell command]", TerminalManager.CATEGORY_OUTPUT);
                            shellFallback.triggerShell(fallbackInput);
                        } else {
                            Tuils.sendOutput(Color.YELLOW, context, "[AI unavailable]", TerminalManager.CATEGORY_OUTPUT);
                        }
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
        };

        String rid = aiSubsystem.submit(query, callback);
        if (supportsStreaming) {
            streamStartedHolder[0] = true;
            TerminalEventBus.get().post(new TerminalEventBus.StartStreamEvent(rid));
        }

        return true;
    }

    static boolean shouldFallbackToShell(String input) {
        if (input == null) return false;
        String trimmed = input.trim();
        if (trimmed.length() == 0) return false;

        String lower = trimmed.toLowerCase();
        if (lower.endsWith("?")) return false;

        String[] naturalLanguagePrefixes = {
                "how ", "what ", "why ", "when ", "where ", "who ", "which ",
                "can you ", "could you ", "would you ", "should i ", "do i ",
                "does ", "is ", "are ", "tell me ", "explain ", "help me "
        };
        for (String prefix : naturalLanguagePrefixes) {
            if (lower.startsWith(prefix)) return false;
        }

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

    public static class ContentSplit {
        public final String thinking;
        public final String content;

        public ContentSplit(String thinking, String content) {
            this.thinking = thinking;
            this.content = content;
        }

        public static ContentSplit split(String rawContent) {
            if (rawContent == null) {
                return new ContentSplit("", "");
            }
            int thinkStart = rawContent.indexOf("<think>");
            if (thinkStart != -1) {
                int thinkEnd = rawContent.indexOf("</think>", thinkStart + 7);
                if (thinkEnd != -1) {
                    String prefix = stripPartialThink(rawContent.substring(0, thinkStart));
                    String thinking = rawContent.substring(thinkStart + 7, thinkEnd);
                    String suffix = stripPartialThink(rawContent.substring(thinkEnd + 8));
                    return new ContentSplit(thinking, prefix + suffix);
                } else {
                    String prefix = stripPartialThink(rawContent.substring(0, thinkStart));
                    String thinking = rawContent.substring(thinkStart + 7);
                    String lowercaseThinking = thinking.toLowerCase();
                    if (lowercaseThinking.endsWith("</think")) {
                        thinking = thinking.substring(0, thinking.length() - 7);
                    } else if (lowercaseThinking.endsWith("</thin")) {
                        thinking = thinking.substring(0, thinking.length() - 6);
                    } else if (lowercaseThinking.endsWith("</thi")) {
                        thinking = thinking.substring(0, thinking.length() - 5);
                    } else if (lowercaseThinking.endsWith("</th")) {
                        thinking = thinking.substring(0, thinking.length() - 4);
                    } else if (lowercaseThinking.endsWith("</t")) {
                        thinking = thinking.substring(0, thinking.length() - 3);
                    } else if (lowercaseThinking.endsWith("</")) {
                        thinking = thinking.substring(0, thinking.length() - 2);
                    } else if (lowercaseThinking.endsWith("<")) {
                        thinking = thinking.substring(0, thinking.length() - 1);
                    }
                    return new ContentSplit(thinking, prefix);
                }
            } else {
                return new ContentSplit("", stripPartialThink(rawContent));
            }
        }

        private static String stripPartialThink(String s) {
            if (s == null || s.isEmpty()) return s;
            String lower = s.toLowerCase();
            String[] tags = {"<think>", "<think", "<thin", "<thi", "<th", "<t", "<"};
            for (String tag : tags) {
                if (lower.endsWith(tag)) {
                    return s.substring(0, s.length() - tag.length());
                }
            }
            return s;
        }
    }
}
