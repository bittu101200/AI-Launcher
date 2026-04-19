package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CompactionEngineTest {

    @Test
    public void buildPrompt_notEmpty() {
        List<ConversationTurn> history = new ArrayList<>();
        history.add(new ConversationTurn(ConversationTurn.Role.USER, "I prefer dark mode."));
        history.add(new ConversationTurn(ConversationTurn.Role.ASSISTANT, "Noted."));

        CompactionEngine engine = new CompactionEngine();
        String prompt = engine.buildCompactionPrompt(history);

        assertNotNull(prompt);
        assertTrue(prompt.length() > 30);
    }

    @Test
    public void buildPrompt_mentionsPreserve() {
        List<ConversationTurn> history = new ArrayList<>();
        history.add(new ConversationTurn(ConversationTurn.Role.USER,
            "My Spotify username is testuser123. Always ask before launching apps."));
        CompactionEngine engine = new CompactionEngine();
        String prompt = engine.buildCompactionPrompt(history);
        assertTrue(prompt.toLowerCase().contains("preserve") || prompt.toLowerCase().contains("compress"));
    }

    @Test
    public void applyCompaction_replacesHistory() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 100);
        for (int i = 0; i < 20; i++) {
            cm.append(new ConversationTurn(ConversationTurn.Role.USER, "filler content here"));
        }
        CompactionEngine engine = new CompactionEngine();
        ConversationTurn summary = new ConversationTurn(ConversationTurn.Role.ASSISTANT, "[Summary]");
        engine.applyCompaction(cm, summary);
        assertTrue(cm.getHistory().size() < 20);
    }
}