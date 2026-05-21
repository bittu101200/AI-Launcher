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

    @Test
    public void testContentSplit() {
        // Standard text without tags
        AITrigger.ContentSplit split1 = AITrigger.ContentSplit.split("Hello world");
        org.junit.Assert.assertEquals("", split1.thinking);
        org.junit.Assert.assertEquals("Hello world", split1.content);

        // Text with complete <think> block
        AITrigger.ContentSplit split2 = AITrigger.ContentSplit.split("<think>I am thinking</think>Here is the response");
        org.junit.Assert.assertEquals("I am thinking", split2.thinking);
        org.junit.Assert.assertEquals("Here is the response", split2.content);

        // Text with incomplete <think> block
        AITrigger.ContentSplit split3 = AITrigger.ContentSplit.split("<think>I am still thinking");
        org.junit.Assert.assertEquals("I am still thinking", split3.thinking);
        org.junit.Assert.assertEquals("", split3.content);

        // Partial think tags at content end should be stripped
        AITrigger.ContentSplit split4 = AITrigger.ContentSplit.split("Hello<thin");
        org.junit.Assert.assertEquals("", split4.thinking);
        org.junit.Assert.assertEquals("Hello", split4.content);

        AITrigger.ContentSplit split5 = AITrigger.ContentSplit.split("<think>Thinking</think>Hello<t");
        org.junit.Assert.assertEquals("Thinking", split5.thinking);
        org.junit.Assert.assertEquals("Hello", split5.content);

        // Truncating end-tag prefixes inside thinking
        AITrigger.ContentSplit split6 = AITrigger.ContentSplit.split("<think>Thinking</thi");
        org.junit.Assert.assertEquals("Thinking", split6.thinking);
        org.junit.Assert.assertEquals("", split6.content);
    }
}
