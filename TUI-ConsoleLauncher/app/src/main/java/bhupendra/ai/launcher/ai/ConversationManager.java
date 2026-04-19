package bhupendra.ai.launcher.ai;

import java.util.ArrayList;
import java.util.List;

public class ConversationManager {

    public enum Mode { STATELESS, SESSION }

    private final Mode mode;
    private final int maxTokens;
    private final List<ConversationTurn> history = new ArrayList<>();

    public ConversationManager(Mode mode, int maxTokens) {
        this.mode = mode;
        this.maxTokens = maxTokens;
    }

    public void append(ConversationTurn turn) {
        if (mode == Mode.STATELESS) return;
        history.add(turn);
    }

    public List<ConversationTurn> getHistory() {
        return new ArrayList<>(history);
    }

    public void clear() {
        history.clear();
    }

    public boolean isCompactionNeeded() {
        int total = 0;
        for (ConversationTurn t : history) {
            total += t.content.length();
        }
        return total > maxTokens;
    }

    public void replaceWithCompacted(ConversationTurn summary) {
        List<ConversationTurn> pinned = new ArrayList<>();
        for (ConversationTurn t : history) {
            if (t.pinned) pinned.add(t);
        }
        history.clear();
        history.addAll(pinned);
        history.add(summary);
    }

    public boolean pinLatestAssistantTurn() {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).role == ConversationTurn.Role.ASSISTANT) {
                history.get(i).pinned = true;
                return true;
            }
        }
        return false;
    }

    public int clearArchives() {
        return 0;
    }
}