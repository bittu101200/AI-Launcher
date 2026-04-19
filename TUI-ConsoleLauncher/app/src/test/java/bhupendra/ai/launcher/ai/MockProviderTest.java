package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import bhupendra.ai.launcher.ai.providers.MockProvider;

public class MockProviderTest {

    @Test
    public void returnsScriptedTextResponse() throws InterruptedException {
        MockProvider provider = new MockProvider("Hello from mock!");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIResponse> result = new AtomicReference<>();

        AIRequest req = new AIRequest.Builder().requestId("t1").userMessage("ping").build();
        provider.complete(req, "t1", new AICallback() {
            @Override public void onToken(String rid, String token) {}
            @Override public void onResponse(AIResponse r) { result.set(r); latch.countDown(); }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(AIResponse.Type.TEXT, result.get().type);
        assertEquals("Hello from mock!", result.get().text);
        assertEquals("t1", result.get().requestId);
    }

    @Test
    public void reportsProviderMetadata() {
        MockProvider p = new MockProvider("ok");
        assertFalse(p.supportsToolUse());
        assertFalse(p.supportsStreaming());
        assertEquals("mock", p.providerId());
    }

    @Test
    public void returnsToolCallResponse() throws InterruptedException {
        List<ToolCall> calls = new ArrayList<>();
        calls.add(new ToolCall("c-1", "brightness", "{\"level\":75}"));
        MockProvider provider = new MockProvider(calls);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIResponse> result = new AtomicReference<>();

        AIRequest req = new AIRequest.Builder().requestId("t2").userMessage("dim").build();
        provider.complete(req, "t2", new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) { result.set(r); latch.countDown(); }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(AIResponse.Type.TOOL_CALLS, result.get().type);
        assertEquals("brightness", result.get().toolCalls.get(0).toolName);
    }
}