package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ConversationManagerTest {

    @Test
    public void stateless_historyAlwaysEmpty() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.STATELESS, 4000);
        cm.append(new ConversationTurn(ConversationTurn.Role.USER, "hello"));
        assertTrue(cm.getHistory().isEmpty());
    }

    @Test
    public void session_appendsHistory() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        cm.append(new ConversationTurn(ConversationTurn.Role.USER, "hello"));
        cm.append(new ConversationTurn(ConversationTurn.Role.ASSISTANT, "hi"));
        assertEquals(2, cm.getHistory().size());
    }

    @Test
    public void clear_emptiesHistory() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        cm.append(new ConversationTurn(ConversationTurn.Role.USER, "hello"));
        cm.clear();
        assertTrue(cm.getHistory().isEmpty());
    }

    @Test
    public void compactionNeeded_aboveThreshold() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 100);
        for (int i = 0; i < 50; i++) {
            cm.append(new ConversationTurn(ConversationTurn.Role.USER, "word word word"));
        }
        assertTrue(cm.isCompactionNeeded());
    }

    @Test
    public void replaceWithCompacted_keepsOnlySummaryAndPinned() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        ConversationTurn pinned = new ConversationTurn(ConversationTurn.Role.USER, "I prefer dark mode");
        pinned.pinned = true;
        cm.append(pinned);
        cm.append(new ConversationTurn(ConversationTurn.Role.ASSISTANT, "old content"));

        ConversationTurn summary = new ConversationTurn(ConversationTurn.Role.ASSISTANT, "[Summary]");
        cm.replaceWithCompacted(summary);

        List<ConversationTurn> h = cm.getHistory();
        assertEquals(2, h.size());
        assertTrue(h.get(0).pinned);
        assertEquals("[Summary]", h.get(h.size() - 1).content);
    }
}