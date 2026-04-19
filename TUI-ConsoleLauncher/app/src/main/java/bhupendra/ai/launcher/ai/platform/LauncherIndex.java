package bhupendra.ai.launcher.ai.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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
        String q = query.toLowerCase();
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.label.toLowerCase().contains(q)) matches.add(entry);
        }
        Collections.sort(matches, Comparator.comparing(e -> e.label.toLowerCase()));
        return matches.subList(0, Math.min(limit, matches.size()));
    }
}