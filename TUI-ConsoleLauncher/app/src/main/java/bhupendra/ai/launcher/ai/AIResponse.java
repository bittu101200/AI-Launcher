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

    private AIResponse(String requestId, Type type, String text,
                       List<ToolCall> toolCalls, String errorMessage, boolean isToolOutput) {
        this.requestId = requestId;
        this.type = type;
        this.text = text;
        this.toolCalls = toolCalls;
        this.errorMessage = errorMessage;
        this.isToolOutput = isToolOutput;
    }

    public static AIResponse text(String requestId, String text) {
        return new AIResponse(requestId, Type.TEXT, text, null, null, false);
    }

    public static AIResponse toolOutput(String requestId, String text) {
        return new AIResponse(requestId, Type.TEXT, text, null, null, true);
    }

    public static AIResponse toolCalls(String requestId, List<ToolCall> calls) {
        return new AIResponse(requestId, Type.TOOL_CALLS, null, calls, null, false);
    }

    public static AIResponse error(String requestId, String message) {
        return new AIResponse(requestId, Type.ERROR, null, null, message, false);
    }
}