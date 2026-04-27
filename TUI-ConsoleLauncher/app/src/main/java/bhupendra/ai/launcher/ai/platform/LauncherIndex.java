package bhupendra.ai.launcher.ai.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bhupendra.ai.launcher.managers.suggestions.SuggestionScorer;

public class LauncherIndex {

    public static class Entry {
        public final String id;
        public final String label;
        public final String kind;

        public Entry(String id, String label, String kind) {
            this.id = id;
            this.label = label;
            this.kind = kind;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public synchronized void put(String id, String label, String kind) {
        entries.add(new Entry(id, label, kind));
    }

    public synchronized List<Entry> search(String query, int limit) {
        List<ScoredEntry> matches = new ArrayList<>();
        int minScore = SuggestionScorer.minimumScore(query);
        for (Entry entry : entries) {
            int score = Math.max(
                    SuggestionScorer.score(query, entry.label),
                    SuggestionScorer.score(query, entry.id));
            if (score >= minScore) matches.add(new ScoredEntry(entry, score));
        }
        Collections.sort(matches, new Comparator<ScoredEntry>() {
            @Override
            public int compare(ScoredEntry a, ScoredEntry b) {
                int byScore = b.score - a.score;
                if (byScore != 0) return byScore;
                return a.entry.label.compareToIgnoreCase(b.entry.label);
            }
        });

        List<Entry> result = new ArrayList<>();
        for (int i = 0; i < matches.size() && i < limit; i++) result.add(matches.get(i).entry);
        return result;
    }

    private static final class ScoredEntry {
        final Entry entry;
        final int score;

        ScoredEntry(Entry entry, int score) {
            this.entry = entry;
            this.score = score;
        }
    }
}
