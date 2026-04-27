package bhupendra.ai.launcher.managers.notifications;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NotificationDisplayManagerTest {

    @Test
    public void stableFingerprintIgnoresRenderedTimestampWhenResolvedContentExists() {
        NotificationContentResolver.ResolvedContent resolved =
            new NotificationContentResolver.ResolvedContent("Alice", "Hello", "Alice", "Hello", false);

        String first = NotificationDisplayManager.buildStableSystemFingerprint(
            "com.example.chat",
            "[10:00] Chat: Alice --- Hello",
            resolved
        );
        String second = NotificationDisplayManager.buildStableSystemFingerprint(
            "com.example.chat",
            "[10:01] Chat: Alice --- Hello",
            resolved
        );

        assertEquals(first, second);
    }

    @Test
    public void stableFingerprintDoesNotLeakLiteralNullContent() {
        NotificationContentResolver.ResolvedContent resolved =
            new NotificationContentResolver.ResolvedContent("null", "null", "null", "null", false);

        String fingerprint = NotificationDisplayManager.buildStableSystemFingerprint(
            "com.example.chat",
            "null",
            resolved
        );

        assertFalse(fingerprint.contains("|null"));
    }
}
