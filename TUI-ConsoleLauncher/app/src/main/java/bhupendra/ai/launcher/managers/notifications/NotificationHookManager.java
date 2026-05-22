package bhupendra.ai.launcher.managers.notifications;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.graphics.Color;
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
import bhupendra.ai.launcher.tuils.system.BeepPlayer;

public class NotificationHookManager {

    private static final String TAG = "NotificationHook";
    private static final String FILENAME = "notification_hooks.json";
    private static NotificationHookManager instance;
    private final List<Hook> hooks = new ArrayList<>();
    private final Context context;

    private final Map<String, String> lastProcessedContent = new HashMap<>();
    private final Map<String, Long> lastTriggerTime = new HashMap<>();
    private static final long COOLDOWN_MS = 10000; 

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

    public boolean removeHook(String id) {
        boolean removed = hooks.removeIf(h -> h.id.equals(id));
        if (removed) save();
        return removed;
    }

    public void processNotification(final StatusBarNotification sbn) {
        if (hooks.isEmpty()) return;

        Notification n = sbn.getNotification();
        Bundle extras = n.extras;
        NotificationContentResolver.ResolvedContent resolvedContent = NotificationContentResolver.resolve(sbn);
        final String title = firstNonEmpty(resolvedContent.title, resolvedContent.originalTitle,
                extras != null ? safe(extras.getCharSequence(Notification.EXTRA_TITLE)) : "");
        final String text = firstNonEmpty(resolvedContent.text, resolvedContent.originalText,
                extras != null ? safe(extras.getCharSequence(Notification.EXTRA_TEXT)) : "");
        final String pkg = sbn.getPackageName();

        if (text.length() == 0 || text.matches("\\d+ new messages")) return;

        for (final Hook h : hooks) {
            if (!h.enabled) continue;
            if (!isWithinTimeWindow(h.timeWindow)) continue;

            if (h.packageName != null && !h.packageName.isEmpty() && !h.packageName.equals(pkg)) continue;
            if (h.senderRegex != null && !h.senderRegex.isEmpty()) {
                if (!Pattern.compile(h.senderRegex, Pattern.CASE_INSENSITIVE).matcher(title).find()) continue;
            }
            if (h.contentRegex != null && !h.contentRegex.isEmpty()) {
                if (!Pattern.compile(h.contentRegex, Pattern.CASE_INSENSITIVE).matcher(text).find()) continue;
            }

            String stateKey = h.id + "_" + title;
            String lastText = lastProcessedContent.get(stateKey);
            long now = System.currentTimeMillis();
            long lastTime = lastTriggerTime.getOrDefault(stateKey, 0L);

            if (text.equals(lastText) || (now - lastTime < COOLDOWN_MS)) continue;

            lastProcessedContent.put(stateKey, text);
            lastTriggerTime.put(stateKey, now);

            if (h.logUpdate) {
                NotificationUpdateManager.getInstance().logUpdate(title, text, pkg);
            }

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

        String query = String.format("BACKGROUND AUTOMATION: Generate a short, natural reply to a notification.\\n" +
                "From: %s\\nMessage: %s\\nGoal: %s\\n\\n" +
                "INSTRUCTIONS:\\n" +
                "1. Output the reply text for the sender.\\n" +
                "2. If URGENT, prefix with [URGENT: <reason>] AND follow with a calming reply saying you've alerted the owner.\\n" +
                "3. Do NOT use tools or add meta-commentary.\\n" +
                "4. Reply in the same language as the message.",
                sender, message, instruction);

        ai.submitAutomation(sender, query, new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) {
                if (r.type == AIResponse.Type.TEXT && r.text != null && !r.text.isEmpty()) {
                    String cleanReply = r.text.trim().replaceAll("^\"|\"$", "");
                    boolean isUrgent = false;
                    
                    if (cleanReply.contains("[URGENT")) {
                        isUrgent = true;
                        String reason = "Urgent message detected";
                        if (cleanReply.contains("[URGENT:") && cleanReply.contains("]")) {
                            int start = cleanReply.indexOf("[URGENT:") + 8;
                            int end = cleanReply.indexOf("]", start);
                            if (end > start) {
                                reason = cleanReply.substring(start, end).trim();
                                cleanReply = cleanReply.substring(0, cleanReply.indexOf("[URGENT:")) + cleanReply.substring(end + 1);
                            } else {
                                cleanReply = cleanReply.replace("[URGENT:", "").trim();
                            }
                        } else {
                            cleanReply = cleanReply.replace("[URGENT]", "").trim();
                        }
                        triggerUrgentAlert(reason);
                    } else {
                        if (cleanReply.startsWith("{") || cleanReply.startsWith("[") || cleanReply.contains("tool_")) return;
                    }
                    
                    cleanReply = cleanReply.trim();
                    if (cleanReply.isEmpty() && isUrgent) {
                        // Fallback calming reply if AI only provided the tag
                        cleanReply = "I have alerted my owner that this is urgent. They will get back to you as soon as possible.";
                    }
                    
                    if (!cleanReply.isEmpty()) {
                        triggerReply(sbn, cleanReply);
                    }
                }
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });
    }

    private void triggerUrgentAlert(String reason) {
        try {
            BeepPlayer.Options options = new BeepPlayer.Options(
                    3900,
                    350,
                    120,
                    5,
                    0.75f,
                    true,
                    180000,
                    true);
            BeepPlayer.playAlert(context, options);
            
            int orange = Color.rgb(255, 165, 0);
            Tuils.sendOutput(orange, context, "[URGENT] BEEP REASON: " + reason.toUpperCase(), 0);
        } catch (Exception e) {
            Log.e(TAG, "Urgent alert failed", e);
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
            LocalBroadcastManager.getInstance(context).sendBroadcast(i);
        }
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            String safeValue = safe(value);
            if (safeValue.length() > 0) return safeValue;
        }
        return "";
    }

    private static String safe(CharSequence value) {
        if (value == null) return "";
        String text = value.toString().trim();
        return "null".equalsIgnoreCase(text) ? "" : text;
    }
}
