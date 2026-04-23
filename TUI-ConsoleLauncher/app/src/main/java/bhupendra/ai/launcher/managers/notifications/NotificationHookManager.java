package bhupendra.ai.launcher.managers.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;
import bhupendra.ai.launcher.tuils.Tuils;

public class NotificationHookManager {

    private static final String FILENAME = "notification_hooks.json";
    private static NotificationHookManager instance;
    private final List<Hook> hooks = new ArrayList<>();
    private final Context context;

    public static class Hook {
        public String id;
        public String packageName; // null for any
        public String senderRegex;
        public String contentRegex;
        public String replyText;
        public boolean enabled = true;

        public JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("package", packageName);
            json.put("sender", senderRegex);
            json.put("content", contentRegex);
            json.put("reply", replyText);
            json.put("enabled", enabled);
            return json;
        }

        public static Hook fromJson(JSONObject json) throws Exception {
            Hook h = new Hook();
            h.id = json.getString("id");
            h.packageName = json.optString("package", null);
            h.senderRegex = json.optString("sender", null);
            h.contentRegex = json.optString("content", null);
            h.replyText = json.getString("reply");
            h.enabled = json.optBoolean("enabled", true);
            return h;
        }
    }

    private NotificationHookManager(Context context) {
        this.context = context.getApplicationContext();
        load();
    }

    public static synchronized NotificationHookManager getInstance(Context context) {
        if (instance == null) instance = new NotificationHookManager(context);
        return instance;
    }

    private void load() {
        try {
            File file = new File(FileSystemManager.getFolder(), FILENAME);
            if (!file.exists()) return;
            String content = FileSystemManager.readFile(file);
            JSONArray array = new JSONArray(content);
            hooks.clear();
            for (int i = 0; i < array.length(); i++) {
                hooks.add(Hook.fromJson(array.getJSONObject(i)));
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private void save() {
        try {
            JSONArray array = new JSONArray();
            for (Hook h : hooks) array.put(h.toJson());
            File file = new File(FileSystemManager.getFolder(), FILENAME);
            FileSystemManager.saveFile(file, array.toString());
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public void addHook(Hook hook) {
        hooks.add(hook);
        save();
    }

    public List<Hook> getHooks() {
        return new ArrayList<>(hooks);
    }

    public void removeHook(String id) {
        hooks.removeIf(h -> h.id.equals(id));
        save();
    }

    public void processNotification(StatusBarNotification sbn) {
        if (hooks.isEmpty()) return;

        Notification n = sbn.getNotification();
        Bundle extras = n.extras;
        String title = extras != null ? String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE)) : "";
        String text = extras != null ? String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT)) : "";
        String pkg = sbn.getPackageName();

        for (Hook h : hooks) {
            if (!h.enabled) continue;

            // Match package
            if (h.packageName != null && !h.packageName.isEmpty() && !h.packageName.equals(pkg)) continue;

            // Match sender (Title)
            if (h.senderRegex != null && !h.senderRegex.isEmpty()) {
                if (!Pattern.compile(h.senderRegex, Pattern.CASE_INSENSITIVE).matcher(title).find()) continue;
            }

            // Match content (Text)
            if (h.contentRegex != null && !h.contentRegex.isEmpty()) {
                if (!Pattern.compile(h.contentRegex, Pattern.CASE_INSENSITIVE).matcher(text).find()) continue;
            }

            // If we matched, trigger reply
            triggerReply(sbn, h.replyText);
            Tuils.sendOutput(context, "[Hook] Auto-replied to " + title + " (" + pkg + ")", 0);
            break; // Only one hook per notification
        }
    }

    private void triggerReply(StatusBarNotification sbn, String replyText) {
        Notification n = sbn.getNotification();
        Notification.Action replyAction = null;
        
        if (n.actions != null) {
            for (Notification.Action action : n.actions) {
                if (action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                    replyAction = action;
                    break;
                }
            }
        }

        if (replyAction == null) {
            Notification.WearableExtender wearableExtender = new Notification.WearableExtender(n);
            for (Notification.Action action : wearableExtender.getActions()) {
                if (action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                    replyAction = action;
                    break;
                }
            }
        }

        if (replyAction != null) {
            Intent i = new Intent(PrivateIOReceiver.ACTION_REPLY);
            i.putExtra(PrivateIOReceiver.BUNDLE, n.extras);
            i.putExtra(PrivateIOReceiver.REMOTE_INPUTS, replyAction.getRemoteInputs());
            i.putExtra(PrivateIOReceiver.TEXT, replyText);
            i.putExtra(PrivateIOReceiver.PENDING_INTENT, replyAction.actionIntent);
            i.putExtra(PrivateIOReceiver.ID, sbn.getId());
            i.putExtra(PrivateIOReceiver.CURRENT_ID, PrivateIOReceiver.currentId);

            LocalBroadcastManager.getInstance(context).sendBroadcast(i);
        }
    }
}
