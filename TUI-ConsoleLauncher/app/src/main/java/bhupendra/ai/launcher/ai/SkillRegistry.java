package bhupendra.ai.launcher.ai;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class SkillRegistry {
    private static final String FILE_NAME = "ai_skills.json";
    private final File skillFile;
    private final List<SkillEntry> entries = new ArrayList<>();

    public static class SkillEntry {
        public final String name;
        public final String trigger;
        public final String instructions;
        public final long timestamp;

        public SkillEntry(String name, String trigger, String instructions, long timestamp) {
            this.name = name;
            this.trigger = trigger;
            this.instructions = instructions;
            this.timestamp = timestamp;
        }
    }

    public SkillRegistry(Context context) {
        this(new File(context.getFilesDir(), FILE_NAME));
    }

    SkillRegistry(File skillFile) {
        this.skillFile = skillFile;
        load();
    }

    public synchronized void store(String name, String trigger, String instructions) {
        String safeName = normalizeName(name);
        String safeTrigger = trigger != null ? trigger.trim() : "";
        String safeInstructions = instructions != null ? instructions.trim() : "";
        if (safeName.length() == 0 || safeTrigger.length() == 0 || safeInstructions.length() == 0) {
            throw new IllegalArgumentException("name, trigger, and instructions are required");
        }

        Iterator<SkillEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().name.equalsIgnoreCase(safeName)) {
                iterator.remove();
            }
        }

        entries.add(new SkillEntry(safeName, safeTrigger, safeInstructions, System.currentTimeMillis()));
        save();
    }

    public synchronized String loadSkill(String query) {
        SkillEntry match = findBest(query);
        if (match == null) {
            return "[no matching skill found]";
        }
        return "SKILL: " + match.name + "\nWHEN TO LOAD: " + match.trigger + "\nINSTRUCTIONS:\n" + match.instructions;
    }

    public synchronized String promptReminder() {
        if (entries.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\nPERSISTENT SKILL INDEX:\n");
        sb.append("If a user asks for a task matching one of these concise triggers, call system.load_skill before acting. Do not load skills for unrelated tasks.\n");
        for (SkillEntry entry : entries) {
            sb.append("- ").append(entry.name).append(": ").append(entry.trigger).append("\n");
        }
        return sb.toString();
    }

    public synchronized int size() {
        return entries.size();
    }

    private SkillEntry findBest(String query) {
        String normalizedQuery = normalizeSearch(query);
        if (normalizedQuery.length() == 0) {
            return null;
        }

        SkillEntry best = null;
        int bestScore = 0;
        for (SkillEntry entry : entries) {
            int score = score(entry, normalizedQuery);
            if (score > bestScore) {
                bestScore = score;
                best = entry;
            }
        }
        return bestScore > 0 ? best : null;
    }

    private int score(SkillEntry entry, String query) {
        String name = normalizeSearch(entry.name);
        String trigger = normalizeSearch(entry.trigger);
        String instructions = normalizeSearch(entry.instructions);

        if (name.equals(query) || trigger.equals(query)) return 1000;
        if (trigger.contains(query) || query.contains(trigger)) return 800;
        if (name.contains(query) || query.contains(name)) return 650;

        int score = 0;
        for (String token : query.split("\\s+")) {
            if (token.length() < 3) continue;
            if (trigger.contains(token)) score += 20;
            else if (name.contains(token)) score += 12;
            else if (instructions.contains(token)) score += 4;
        }
        return score;
    }

    private String normalizeName(String value) {
        if (value == null) return "";
        return value.trim().replaceAll("\\s+", "_").replaceAll("[^A-Za-z0-9_\\-]", "").toLowerCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (SkillEntry entry : entries) {
                JSONObject obj = new JSONObject();
                obj.put("name", entry.name);
                obj.put("trigger", entry.trigger);
                obj.put("instructions", entry.instructions);
                obj.put("timestamp", entry.timestamp);
                array.put(obj);
            }

            File parent = skillFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            File temp = new File(parent != null ? parent : new File("."), skillFile.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!temp.renameTo(skillFile)) {
                try (FileOutputStream fos = new FileOutputStream(skillFile)) {
                    fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!skillFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(skillFile)) {
            byte[] data = new byte[(int) skillFile.length()];
            int read = fis.read(data);
            if (read <= 0) return;
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            entries.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new SkillEntry(
                        obj.getString("name"),
                        obj.getString("trigger"),
                        obj.getString("instructions"),
                        obj.optLong("timestamp", 0)
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
