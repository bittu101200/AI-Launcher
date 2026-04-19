package bhupendra.ai.launcher.ai;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LongTermMemory {
    private static final String FILE_NAME = "ai_memory.json";
    private final File memoryFile;
    private final List<MemoryEntry> entries = new ArrayList<>();

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
        entries.removeIf(e -> e.key.equalsIgnoreCase(key));
        entries.add(new MemoryEntry(key, value, System.currentTimeMillis()));
        save();
    }

    public String retrieve(String query) {
        StringBuilder sb = new StringBuilder();
        for (MemoryEntry entry : entries) {
            if (entry.key.toLowerCase().contains(query.toLowerCase()) || 
                entry.value.toLowerCase().contains(query.toLowerCase())) {
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
            FileOutputStream fos = new FileOutputStream(memoryFile);
            fos.write(array.toString().getBytes(StandardCharsets.UTF_8));
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        if (!memoryFile.exists()) return;
        try {
            FileInputStream fis = new FileInputStream(memoryFile);
            byte[] data = new byte[(int) memoryFile.length()];
            fis.read(data);
            fis.close();
            JSONArray array = new JSONArray(new String(data, StandardCharsets.UTF_8));
            entries.clear();
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                entries.add(new MemoryEntry(obj.getString("key"), obj.getString("value"), obj.getLong("timestamp")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
