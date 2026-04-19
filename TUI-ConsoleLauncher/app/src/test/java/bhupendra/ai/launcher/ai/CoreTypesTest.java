package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CoreTypesTest {

    @Test
    public void aiRequest_storesFields() {
        List<ConversationTurn> history = new ArrayList<>();
        history.add(new ConversationTurn(ConversationTurn.Role.USER, "hello"));

        AIRequest req = new AIRequest.Builder()
            .requestId("req-1")
            .userMessage("what time is it?")
            .history(history)
            .systemPrompt("You are a terminal assistant.")
            .maxTokens(1024)
            .build();

        assertEquals("req-1", req.requestId);
        assertEquals("what time is it?", req.userMessage);
        assertEquals(1, req.history.size());
        assertEquals("You are a terminal assistant.", req.systemPrompt);
        assertEquals(1024, req.maxTokens);
    }

    @Test
    public void aiResponse_textType() {
        AIResponse r = AIResponse.text("req-1", "It is 3pm.");
        assertEquals(AIResponse.Type.TEXT, r.type);
        assertEquals("req-1", r.requestId);
        assertEquals("It is 3pm.", r.text);
    }

    @Test
    public void aiResponse_errorType() {
        AIResponse r = AIResponse.error("req-1", "timeout");
        assertEquals(AIResponse.Type.ERROR, r.type);
        assertEquals("timeout", r.errorMessage);
    }

    @Test
    public void toolCall_storesFields() {
        ToolCall tc = new ToolCall("call-1", "brightness", "{\"level\":50}");
        assertEquals("call-1", tc.callId);
        assertEquals("brightness", tc.toolName);
    }

    @Test
    public void toolResult_success() {
        ToolResult tr = ToolResult.success("call-1", "done");
        assertTrue(tr.success);
        assertEquals("done", tr.output);
    }

    @Test
    public void toolResult_failure() {
        ToolResult tr = ToolResult.failure("call-1", "denied");
        assertFalse(tr.success);
        assertEquals("denied", tr.errorMessage);
    }
}