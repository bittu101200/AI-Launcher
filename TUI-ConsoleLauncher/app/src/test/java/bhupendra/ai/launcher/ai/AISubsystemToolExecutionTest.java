package bhupendra.ai.launcher.ai;

import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

import bhupendra.ai.launcher.ai.providers.MockProvider;

public class AISubsystemToolExecutionTest {

    @Test
    public void submit_executesLaunchToolWithoutConfirmation() throws InterruptedException {
        ToolCall call = new ToolCall("call-1", "launch:com.apple.android.music", "{}");
        MockProvider provider = new MockProvider(Collections.singletonList(call));
        AtomicInteger executeCount = new AtomicInteger();
        AtomicReference<String> lastToolName = new AtomicReference<>();
        AISubsystem subsystem = new AISubsystem(provider, null, (context, tool, toolCall) -> {
            executeCount.incrementAndGet();
            lastToolName.set(tool.name);
            return "[launched " + tool.description + "]";
        });

        subsystem.getToolRegistry().register(
            ToolRegistry.Tier.APP_INTENT,
            new Tool("launch:com.apple.android.music", "Apple Music", Collections.<String, String>emptyMap(), ToolRiskClass.LAUNCH_ONLY));

        CountDownLatch executionLatch = new CountDownLatch(1);
        AtomicReference<String> toolOutput = new AtomicReference<>();

        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {
                if (response.isToolOutput) {
                    toolOutput.set(response.text);
                    executionLatch.countDown();
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {}
        });

        assertTrue(executionLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, executeCount.get());
        assertEquals("launch:com.apple.android.music", lastToolName.get());
        assertEquals("[launched Apple Music]", toolOutput.get());
    }

    @Test
    public void submit_toolExecutionCompletesRequestWithFollowUpResponse() throws InterruptedException {
        ToolCall call = new ToolCall("call-1", "launch:com.apple.android.music", "{}");
        
        AIProvider customProvider = new AIProvider() {
            private int callCount = 0;
            @Override
            public void complete(AIRequest request, String requestId, AICallback callback) {
                callback.onStateChange(requestId, AIRequestState.THINKING);
                new Thread(() -> {
                    try { Thread.sleep(25); } catch (InterruptedException ignored) {}
                    callCount++;
                    AIResponse response;
                    if (callCount == 1) {
                        response = AIResponse.toolCalls(requestId, Collections.singletonList(call));
                    } else {
                        response = AIResponse.text(requestId, "Music has been launched successfully.");
                    }
                    callback.onResponse(response);
                }).start();
            }
            @Override public boolean supportsToolUse() { return true; }
            @Override public boolean supportsStreaming() { return false; }
            @Override public String providerId() { return "custom"; }
        };

        AtomicInteger executeCount = new AtomicInteger();
        AISubsystem subsystem = new AISubsystem(customProvider, null, (context, tool, toolCall) -> {
            executeCount.incrementAndGet();
            return "[launched " + tool.description + "]";
        });

        subsystem.getToolRegistry().register(
            ToolRegistry.Tier.APP_INTENT,
            new Tool("launch:com.apple.android.music", "Apple Music", Collections.emptyMap(), ToolRiskClass.LAUNCH_ONLY));

        CountDownLatch finalLatch = new CountDownLatch(1);
        AtomicReference<String> finalTextResponse = new AtomicReference<>();
        AtomicReference<AIRequestState> finalState = new AtomicReference<>();

        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT && !response.isToolOutput) {
                    finalTextResponse.set(response.text);
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {
                finalState.set(state);
                if (state == AIRequestState.COMPLETED) {
                    finalLatch.countDown();
                }
            }
        });

        assertTrue(finalLatch.await(3, TimeUnit.SECONDS));
        assertEquals(1, executeCount.get());
        assertEquals("Music has been launched successfully.", finalTextResponse.get());
        assertEquals(AIRequestState.COMPLETED, finalState.get());
        assertFalse(subsystem.isInFlight());
    }

    @Test
    public void submit_withoutContextReturnsClearExecutionError() throws InterruptedException {
        ToolCall call = new ToolCall("call-1", "launch:com.apple.android.music", "{}");
        MockProvider provider = new MockProvider(Collections.singletonList(call));
        AISubsystem subsystem = new AISubsystem(provider);

        subsystem.getToolRegistry().register(
            ToolRegistry.Tier.APP_INTENT,
            new Tool("launch:com.apple.android.music", "Apple Music", Collections.<String, String>emptyMap(), ToolRiskClass.LAUNCH_ONLY));

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.ERROR) {
                    errorMessage.set(response.errorMessage);
                    errorLatch.countDown();
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {}
        });

        assertTrue(errorLatch.await(1, TimeUnit.SECONDS));
        assertTrue(errorMessage.get().contains("requires app context"));
    }

    @Test
    public void cancel_clearsPendingConfirmationAction() throws InterruptedException {
        ToolCall call = new ToolCall("call-1", "launch:com.apple.android.music", "{}");
        MockProvider provider = new MockProvider(Collections.singletonList(call));
        AtomicInteger executeCount = new AtomicInteger();
        AISubsystem subsystem = new AISubsystem(provider, null, (context, tool, toolCall) -> {
            executeCount.incrementAndGet();
            return "[launched " + tool.description + "]";
        });

        subsystem.getToolRegistry().register(
            ToolRegistry.Tier.APP_INTENT,
            new Tool("launch:com.apple.android.music", "Apple Music", Collections.<String, String>emptyMap(), ToolRiskClass.LAUNCH_ONLY));

        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {}

            @Override public void onStateChange(String rid, AIRequestState state) {}
        });

        subsystem.cancel();
        Thread.sleep(200);
        assertEquals(0, executeCount.get());
    }
}
