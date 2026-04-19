package bhupendra.ai.launcher.ai.providers;

import bhupendra.ai.launcher.ai.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MockProvider implements AIProvider {

    private final String fixedText;
    private final List<ToolCall> fixedToolCalls;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public MockProvider(String fixedText) {
        this.fixedText = fixedText;
        this.fixedToolCalls = null;
    }

    public MockProvider(List<ToolCall> toolCalls) {
        this.fixedText = null;
        this.fixedToolCalls = toolCalls;
    }

    @Override
    public void complete(AIRequest request, String requestId, AICallback callback) {
        callback.onStateChange(requestId, AIRequestState.THINKING);
        executor.execute(() -> {
            AIResponse response = fixedToolCalls != null
                ? AIResponse.toolCalls(requestId, fixedToolCalls)
                : AIResponse.text(requestId, fixedText != null ? fixedText : "");
            callback.onResponse(response);
        });
    }

    @Override public boolean supportsToolUse() { return false; }
    @Override public boolean supportsStreaming() { return false; }
    @Override public String providerId() { return "mock"; }
}