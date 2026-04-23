package bhupendra.ai.launcher.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompactionEngine {

    private static final String[] PREF_KW = {"prefer", "always", "never", "don't", "please"};
    private static final String[] GOAL_KW = {"trying to", "want to", "need to", "working on"};

    public String buildCompactionPrompt(List<ConversationTurn> history) {
        boolean hasPrefs = false;
        boolean hasGoals = false;
        Map<String, Integer> trigramCounts = new HashMap<>();

        for (ConversationTurn t : history) {
            if (t.content == null) continue;
            
            String lower = t.content.toLowerCase();
            for (String kw : PREF_KW) if (lower.contains(kw)) { hasPrefs = true; break; }
            for (String kw : GOAL_KW) if (lower.contains(kw)) { hasGoals = true; break; }

            String[] words = t.content.split("\\s+");
            for (int i = 0; i + 2 < words.length; i++) {
                String tri = words[i] + " " + words[i+1] + " " + words[i+2];
                trigramCounts.put(tri, trigramCounts.getOrDefault(tri, 0) + 1);
            }
        }

        List<String> singletons = new ArrayList<>();
        for (Map.Entry<String, Integer> e : trigramCounts.entrySet()) {
            if (e.getValue() == 1 && e.getKey().length() > 10) singletons.add(e.getKey());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Compress the following conversation into a structured summary.\n");
        sb.append("Preserve with high fidelity:\n");
        if (hasPrefs) sb.append("- User preferences (merge into canonical list)\n");
        if (hasGoals) sb.append("- Ongoing goals still in progress\n");
        if (!singletons.isEmpty()) {
            sb.append("- Facts mentioned only once — do not lose them:\n");
            for (String s : singletons.subList(0, Math.min(singletons.size(), 5))) {
                sb.append("  * ").append(s, 0, Math.min(s.length(), 60)).append("\n");
            }
        }
        sb.append("\nConversation:\n");
        for (ConversationTurn t : history) {
            String content = t.content != null ? t.content : "[tool calls]";
            sb.append(t.role.name()).append(": ").append(content).append("\n");
        }
        sb.append("\nOutput a structured summary only.");
        return sb.toString();
    }

    public void applyCompaction(ConversationManager cm, ConversationTurn summary) {
        cm.replaceWithCompacted(summary);
    }
}