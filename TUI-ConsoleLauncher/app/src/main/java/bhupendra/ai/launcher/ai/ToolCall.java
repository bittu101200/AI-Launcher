package bhupendra.ai.launcher.ai;

public class ToolCall {
    public final String callId;
    public final String toolName;
    public final String argumentsJson;
    public final String thoughtSignature;

    public ToolCall(String callId, String toolName, String argumentsJson) {
        this(callId, toolName, argumentsJson, null);
    }

    public ToolCall(String callId, String toolName, String argumentsJson, String thoughtSignature) {
        this.callId = callId;
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
        this.thoughtSignature = thoughtSignature;
    }
}