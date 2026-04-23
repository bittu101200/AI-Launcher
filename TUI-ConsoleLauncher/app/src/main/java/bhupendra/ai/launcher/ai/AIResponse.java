package bhupendra.ai.launcher.ai;

import java.util.List;

public class AIResponse {
    public enum Type { TEXT, TOOL_CALLS, ERROR }
    public final String requestId;
    public final Type type;
    public final String text;
    public final List<ToolCall> toolCalls;
    public final String errorMessage;
    public final boolean isToolOutput;

    public final ToolCall toolCall;

    private AIResponse(String requestId, Type type, String text,
                       List<ToolCall> toolCalls, ToolCall toolCall, String errorMessage, boolean isToolOutput) {
        this.requestId = requestId;
        this.type = type;
        this.text = text;
        this.toolCalls = toolCalls;
        this.toolCall = toolCall;
        this.errorMessage = errorMessage;
        this.isToolOutput = isToolOutput;
    }

    public static AIResponse text(String requestId, String text) {
        return new AIResponse(requestId, Type.TEXT, text, null, null, null, false);
    }

    public static AIResponse toolOutput(String requestId, String text, ToolCall toolCall) {
        return new AIResponse(requestId, Type.TEXT, text, null, toolCall, null, true);
    }

    public static AIResponse toolCalls(String requestId, List<ToolCall> calls) {
        return new AIResponse(requestId, Type.TOOL_CALLS, null, calls, null, null, false);
    }

    public static AIResponse error(String requestId, String message) {
        return new AIResponse(requestId, Type.ERROR, null, null, null, message, false);
    }
}