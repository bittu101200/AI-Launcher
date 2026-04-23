package bhupendra.ai.launcher.managers.notifications;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AICallback;
import bhupendra.ai.launcher.ai.AIResponse;
import bhupendra.ai.launcher.ai.AIRequestState;

public class NotificationHookManager {

    private static final String TAG = "NotificationHook";
    private static final String FILENAME = "notification_hooks.json";
    private static NotificationHookManager instance;
    private final List<Hook> hooks = new ArrayList<>();
    private final Context context;

    // Debouncing / Spam prevention
    private final Map<String, String> lastProcessedContent = new HashMap<>();
    private final Map<String, Long> lastTriggerTime = new HashMap<>();
    private static final long COOLDOWN_MS = 10000; // 10 seconds per hook/sender

    public static class Hook {
        public String id;
        public String packageName; 
        public String senderRegex;
        public String contentRegex;
        public String replyText;
        public boolean enabled = true;
        public boolean useAI = false;
        public String aiInstruction;
        public boolean logUpdate = false;
        public String timeWindow; 

        public JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("package", packageName);
            json.put("sender", senderRegex);
            json.put("content", contentRegex);
            json.put("reply", replyText);
            json.put("enabled", enabled);
            json.put("useAI", useAI);
            json.put("aiInstruction", aiInstruction);
            json.put("logUpdate", logUpdate);
            json.put("timeWindow", timeWindow);
            return json;
        }

        public static Hook fromJson(JSONObject json) throws Exception {
            Hook h = new Hook();
            h.id = json.getString("id");
            h.packageName = json.optString("package", null);
            h.senderRegex = json.optString("sender", null);
            h.contentRegex = json.optString("content", null);
            h.replyText = json.optString("reply", null);
            h.enabled = json.optBoolean("enabled", true);
            h.useAI = json.optBoolean("useAI", false);
            h.aiInstruction = json.optString("aiInstruction", null);
            h.logUpdate = json.optBoolean("logUpdate", false);
            h.timeWindow = json.optString("timeWindow", null);
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

    public void processNotification(final StatusBarNotification sbn) {
        if (hooks.isEmpty()) return;

        Notification n = sbn.getNotification();
        Bundle extras = n.extras;
        final String title = extras != null ? String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE)) : "";
        final String text = extras != null ? String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT)) : "";
        final String pkg = sbn.getPackageName();

        // 1. Ignore generic "X new messages" notifications from WhatsApp
        if (text.matches("\\d+ new messages")) {
            return;
        }

        for (final Hook h : hooks) {
            if (!h.enabled) continue;

            // 2. Time Check
            if (!isWithinTimeWindow(h.timeWindow)) continue;

            // 3. Matching Logic
            if (h.packageName != null && !h.packageName.isEmpty() && !h.packageName.equals(pkg)) continue;
            if (h.senderRegex != null && !h.senderRegex.isEmpty()) {
                if (!Pattern.compile(h.senderRegex, Pattern.CASE_INSENSITIVE).matcher(title).find()) continue;
            }
            if (h.contentRegex != null && !h.contentRegex.isEmpty()) {
                if (!Pattern.compile(h.contentRegex, Pattern.CASE_INSENSITIVE).matcher(text).find()) continue;
            }

            // 4. Debounce - Don't reply to the exact same text twice in a row, or within cooldown
            String stateKey = h.id + "_" + title;
            String lastText = lastProcessedContent.get(stateKey);
            long now = System.currentTimeMillis();
            long lastTime = lastTriggerTime.getOrDefault(stateKey, 0L);

            if (text.equals(lastText) || (now - lastTime < COOLDOWN_MS)) {
                continue;
            }

            lastProcessedContent.put(stateKey, text);
            lastTriggerTime.put(stateKey, now);

            Log.d(TAG, "Hook " + h.id + " matched for unique content: " + text);

            // 5. Log Update if requested
            if (h.logUpdate) {
                NotificationUpdateManager.getInstance().logUpdate(title, text, pkg);
            }

            // 6. Trigger Reply (AI or Static)
            if (h.useAI) {
                triggerAIReply(sbn, title, text, h.aiInstruction);
            } else if (h.replyText != null && !h.replyText.isEmpty()) {
                triggerReply(sbn, h.replyText);
            }
            
            Tuils.sendOutput(context, "[Hook triggered] " + title, 0);
            break; 
        }
    }

    private boolean isWithinTimeWindow(String window) {
        if (window == null || window.isEmpty() || !window.contains("-")) return true;
        try {
            String[] parts = window.split("-");
            String[] start = parts[0].split(":");
            String[] end = parts[1].split(":");
            Calendar now = Calendar.getInstance();
            int nowTotal = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
            int startTotal = Integer.parseInt(start[0]) * 60 + Integer.parseInt(start[1]);
            int endTotal = Integer.parseInt(end[0]) * 60 + Integer.parseInt(end[1]);
            return (startTotal <= endTotal) ? (nowTotal >= startTotal && nowTotal <= endTotal) : (nowTotal >= startTotal || nowTotal <= endTotal);
        } catch (Exception e) { return true; }
    }

    private void triggerAIReply(final StatusBarNotification sbn, String sender, String message, String instruction) {
        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null || !ai.isAvailable()) return;

        // Use a more distinct instruction to keep the AI in "background reply mode"
        String query = String.format("BACKGROUND AUTOMATION: Generate a short, natural reply to a notification.\\n" +
                "From: %s\\nMessage: %s\\nGoal: %s\\n\\n" +
                "CRITICAL: Output ONLY the reply text. Do NOT use tools. Do NOT add meta-commentary. " +
                "Reply in the same language as the incoming message (e.g. if message is in Hindi, reply in Hindi).",
                sender, message, instruction);

        ai.submit(query, new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) {
                if (r.type == AIResponse.Type.TEXT && r.text != null && !r.text.isEmpty()) {
                    String cleanReply = r.text.trim().replaceAll("^\"|\"$", "");
                    // Skip if AI just returned JSON or tool calls despite instructions
                    if (cleanReply.startsWith("{") || cleanReply.startsWith("[") || cleanReply.contains("tool_")) {
                        Log.d(TAG, "AI returned invalid background response: " + cleanReply);
                        return;
                    }
                    triggerReply(sbn, cleanReply);
                }
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });
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
