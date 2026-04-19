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
    public void submit_promptsForLaunchToolAndExecutesAfterConfirm() throws InterruptedException {
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

        CountDownLatch promptLatch = new CountDownLatch(1);
        CountDownLatch executionLatch = new CountDownLatch(1);
        AtomicReference<String> firstResponse = new AtomicReference<>();
        AtomicReference<String> secondResponse = new AtomicReference<>();

        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {
                if (firstResponse.get() == null) {
                    firstResponse.set(response.text);
                    promptLatch.countDown();
                } else {
                    secondResponse.set(response.text);
                    executionLatch.countDown();
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {}
        });

        assertTrue(promptLatch.await(1, TimeUnit.SECONDS));
        assertTrue(firstResponse.get().contains("Apple Music"));
        assertTrue(subsystem.isAwaitingConfirmation());

        subsystem.confirmCurrentTool();

        assertTrue(executionLatch.await(1, TimeUnit.SECONDS));
        assertEquals(1, executeCount.get());
        assertEquals("launch:com.apple.android.music", lastToolName.get());
        assertEquals("[launched Apple Music]", secondResponse.get());
    }

    @Test
    public void submit_withoutContextReturnsClearExecutionError() throws InterruptedException {
        ToolCall call = new ToolCall("call-1", "launch:com.apple.android.music", "{}");
        MockProvider provider = new MockProvider(Collections.singletonList(call));
        AISubsystem subsystem = new AISubsystem(provider);

        subsystem.getToolRegistry().register(
            ToolRegistry.Tier.APP_INTENT,
            new Tool("launch:com.apple.android.music", "Apple Music", Collections.<String, String>emptyMap(), ToolRiskClass.LAUNCH_ONLY));

        CountDownLatch promptLatch = new CountDownLatch(1);
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();

        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT) {
                    promptLatch.countDown();
                } else if (response.type == AIResponse.Type.ERROR) {
                    errorMessage.set(response.errorMessage);
                    errorLatch.countDown();
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {}
        });

        assertTrue(promptLatch.await(1, TimeUnit.SECONDS));
        subsystem.confirmCurrentTool();
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

        CountDownLatch promptLatch = new CountDownLatch(1);
        subsystem.submit("open Apple Music", new AICallback() {
            @Override public void onToken(String rid, String token) {}

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT) {
                    promptLatch.countDown();
                }
            }

            @Override public void onStateChange(String rid, AIRequestState state) {}
        });

        assertTrue(promptLatch.await(1, TimeUnit.SECONDS));
        assertTrue(subsystem.isAwaitingConfirmation());

        subsystem.cancel();
        subsystem.confirmCurrentTool();

        assertFalse(subsystem.isAwaitingConfirmation());
        assertEquals(0, executeCount.get());
    }
}
