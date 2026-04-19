package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import bhupendra.ai.launcher.ai.providers.MockProvider;

public class RequestManagerTest {

    @Test
    public void submit_completesSuccessfully() throws InterruptedException {
        MockProvider provider = new MockProvider("response text");
        RequestManager rm = new RequestManager(provider, 10000, 30000);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIRequestState> finalState = new AtomicReference<>();

        rm.submit(new AIRequest.Builder().requestId("r1").userMessage("hello").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    finalState.set(s);
                    if (s == AIRequestState.COMPLETED || s == AIRequestState.FAILED) latch.countDown();
                }
            });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(AIRequestState.COMPLETED, finalState.get());
    }

    @Test
    public void cancel_stopsRequest() throws InterruptedException {
        MockProvider provider = new MockProvider("text");
        RequestManager rm = new RequestManager(provider, 10000, 30000);
        CountDownLatch latch = new CountDownLatch(1);

        rm.submit(new AIRequest.Builder().requestId("r2").userMessage("q").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    if (s == AIRequestState.CANCELLED || s == AIRequestState.COMPLETED) latch.countDown();
                }
            });

        rm.cancel("r2");
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void isInFlight_trueWhileThinking() throws InterruptedException {
        MockProvider slowProvider = new MockProvider("text");

        RequestManager rm = new RequestManager(slowProvider, 10000, 30000);
        CountDownLatch thinking = new CountDownLatch(1);
        rm.submit(new AIRequest.Builder().requestId("r3").userMessage("q").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    if (s == AIRequestState.THINKING) thinking.countDown();
                }
            });

        thinking.await(2, TimeUnit.SECONDS);
        assertTrue(rm.isInFlight());
    }

    @Test
    public void submit_toolCallsEnterExecutingToolsState() throws InterruptedException {
        MockProvider provider = new MockProvider(java.util.Collections.singletonList(
            new ToolCall("c1", "launch:com.apple.android.music", "{}")));
        RequestManager rm = new RequestManager(provider, 10000, 30000);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIRequestState> finalState = new AtomicReference<>();

        rm.submit(new AIRequest.Builder().requestId("r4").userMessage("open music").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    finalState.set(s);
                    if (s == AIRequestState.EXECUTING_TOOLS) latch.countDown();
                }
            });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(AIRequestState.EXECUTING_TOOLS, finalState.get());
        assertTrue(rm.isInFlight());
    }
}
