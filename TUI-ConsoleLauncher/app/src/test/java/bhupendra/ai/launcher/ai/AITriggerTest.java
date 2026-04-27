package bhupendra.ai.launcher.ai;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AITriggerTest {

    @Test
    public void shouldFallbackToShell_rejectsNaturalLanguageQuestions() {
        assertFalse(AITrigger.shouldFallbackToShell("how do I evaluate math expressions"));
        assertFalse(AITrigger.shouldFallbackToShell("What is the calc syntax?"));
        assertFalse(AITrigger.shouldFallbackToShell("explain notifications"));
    }

    @Test
    public void shouldFallbackToShell_allowsCommandLikeInput() {
        assertTrue(AITrigger.shouldFallbackToShell("date"));
        assertTrue(AITrigger.shouldFallbackToShell("pm list packages"));
        assertTrue(AITrigger.shouldFallbackToShell("logcat -d"));
    }
}
