package bhupendra.ai.launcher.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class LongTermMemory {
    private static final String FILE_NAME = "ai_memory.json";
    private final File memoryFile;
    private final List<MemoryEntry> entries = new ArrayList<>();
    private final HashMap<String, String> lowercaseCache = new HashMap<>();

    public static class MemoryEntry {
        public String key;
        public String value;
        public long timestamp;

        public MemoryEntry(String key, String value, long timestamp) {
            this.key = key;
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    public LongTermMemory(Context context) {
        this.memoryFile = new File(context.getFilesDir(), FILE_NAME);
        load();
    }

    public void store(String key, String value) {
        Iterator<MemoryEntry> it = entries.iterator();
        while (it.hasNext()) {
            if (it.next().key.equalsIgnoreCase(key)) it.remove();
        }
        MemoryEntry entry = new MemoryEntry(key, value, System.currentTimeMillis());
        entries.add(entry);
        lowercaseCache.put(key.toLowerCase(Locale.ROOT), key.toLowerCase(Locale.ROOT) + ":" + value.toLowerCase(Locale.ROOT));
        save();
    }

    public String retrieve(String query) {
        String queryLower = query.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        for (MemoryEntry entry : entries) {
            String cached = lowercaseCache.get(entry.key.toLowerCase(Locale.ROOT));
            if (cached != null && cached.contains(queryLower)) {
                sb.append("- ").append(entry.key).append(": ").append(entry.value).append("\n");
            }
        }
        return sb.length() > 0 ? sb.toString() : "[no matching memories found]";
    }

    public String getAll() {
        if (entries.isEmpty()) return "[memory is empty]";
        StringBuilder sb = new StringBuilder("Persistent Memories:\n");
        for (MemoryEntry entry : entries) {
            sb.append("- ").append(entry.key).append(": ").append(entry.value).append("\n");
        }
        return sb.toString();
    }

    private void rebuildLowercaseCache() {
        lowercaseCache.clear();
        for (MemoryEntry entry : entries) {
            lowercaseCache.put(entry.key.toLowerCase(Locale.ROOT), entry.key.toLowerCase(Locale.ROOT) + ":" + entry.value.toLowerCase(Locale.ROOT));
        }
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (MemoryEntry entry : entries) {
                JSONObject obj = new JSONObject();
                obj.put("key", entry.key);
                obj.put("value", entry.value);
                obj.put("timestamp", entry.timestamp);
                array.put(obj);
            }
            File temp = new File(memoryFile.getParent(), FILE_NAME + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!temp.renameTo(memoryFile)) {
                try (FileOutputStream fos = new FileOutputStream(memoryFile)) {
                    fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!memoryFile.exists()) return;
        try (FileInputStream fis = new FileInputStream(memoryFile)) {
            byte[] data = new byte[(int) memoryFile.length()];
            fis.read(data);
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            entries.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new MemoryEntry(obj.getString("key"), obj.getString("value"), obj.getLong("timestamp")));
            }
            rebuildLowercaseCache();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
