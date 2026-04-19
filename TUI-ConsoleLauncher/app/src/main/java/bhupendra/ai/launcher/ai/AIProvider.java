package bhupendra.ai.launcher.ai;

public interface AIProvider {
    void complete(AIRequest request, String requestId, AICallback callback);
    boolean supportsToolUse();
    boolean supportsStreaming();
    String providerId();
}