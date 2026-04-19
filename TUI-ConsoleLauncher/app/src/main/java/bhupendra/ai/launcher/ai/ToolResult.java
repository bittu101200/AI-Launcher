package bhupendra.ai.launcher.ai;

public class ToolResult {
    public final String callId;
    public final boolean success;
    public final String output;
    public final String errorMessage;

    private ToolResult(String callId, boolean success, String output, String errorMessage) {
        this.callId = callId;
        this.success = success;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public static ToolResult success(String callId, String output) {
        return new ToolResult(callId, true, output, null);
    }

    public static ToolResult failure(String callId, String errorMessage) {
        return new ToolResult(callId, false, null, errorMessage);
    }
}