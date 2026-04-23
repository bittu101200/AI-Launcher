package bhupendra.ai.launcher.managers.notifications;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class NotificationUpdateManager {
    private static final String FILENAME = "notification_updates.json";
    private static NotificationUpdateManager instance;
    private final List<Update> updates = new ArrayList<>();

    public static class Update {
        public long timestamp;
        public String sender;
        public String message;
        public String packageName;

        public JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("timestamp", timestamp);
            json.put("sender", sender);
            json.put("message", message);
            json.put("package", packageName);
            return json;
        }

        public static Update fromJson(JSONObject json) throws Exception {
            Update u = new Update();
            u.timestamp = json.getLong("timestamp");
            u.sender = json.getString("sender");
            u.message = json.getString("message");
            u.packageName = json.optString("package", "");
            return u;
        }
    }

    private NotificationUpdateManager() {
        load();
    }

    public static synchronized NotificationUpdateManager getInstance() {
        if (instance == null) instance = new NotificationUpdateManager();
        return instance;
    }

    private void load() {
        try {
            File file = new File(FileSystemManager.getFolder(), FILENAME);
            if (!file.exists()) return;
            String content = FileSystemManager.readFile(file);
            JSONArray array = new JSONArray(content);
            updates.clear();
            for (int i = 0; i < array.length(); i++) {
                updates.add(Update.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (Update u : updates) array.put(u.toJson());
            File file = new File(FileSystemManager.getFolder(), FILENAME);
            FileSystemManager.saveFile(file, array.toString());
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public void logUpdate(String sender, String message, String packageName) {
        Update u = new Update();
        u.timestamp = System.currentTimeMillis();
        u.sender = sender;
        u.message = message;
        u.packageName = packageName;
        updates.add(u);
        save();
    }

    public List<Update> getUpdates() {
        return new ArrayList<>(updates);
    }

    public void clearUpdates() {
        updates.clear();
        save();
    }
}
