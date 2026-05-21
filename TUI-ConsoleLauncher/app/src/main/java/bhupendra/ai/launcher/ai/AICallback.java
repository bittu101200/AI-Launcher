package bhupendra.ai.launcher.ai;

public interface AICallback {
    void onToken(String requestId, String token);
    default void onThinkingToken(String requestId, String token) {}
    void onResponse(AIResponse response);
    void onStateChange(String requestId, AIRequestState state);
}