package bhupendra.ai.launcher.ai;

import org.junit.After;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import bhupendra.ai.launcher.ai.providers.MockProvider;

public class AISubsystemTest {

    @After
    public void teardown() {
        AISubsystem s = AISubsystem.getInstance();
        if (s != null) s.dispose();
    }

    @Test
    public void singleton_setAndGet() {
        AISubsystem sub = new AISubsystem(new MockProvider("hi"));
        sub.setInstance();
        assertSame(sub, AISubsystem.getInstance());
    }

    @Test
    public void dispose_clearsInstance() {
        AISubsystem sub = new AISubsystem(new MockProvider("hi"));
        sub.setInstance();
        sub.dispose();
        assertNull(AISubsystem.getInstance());
    }

    @Test
    public void submit_deliversTextResponse() throws InterruptedException {
        AISubsystem sub = new AISubsystem(new MockProvider("hello from AI"));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean gotText = new AtomicBoolean(false);

        sub.submit("ping", new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) {
                gotText.set(r.type == AIResponse.Type.TEXT);
                latch.countDown();
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(gotText.get());
    }

    @Test
    public void isAvailable_trueWithProvider() {
        assertTrue(new AISubsystem(new MockProvider("hi")).isAvailable());
    }
}