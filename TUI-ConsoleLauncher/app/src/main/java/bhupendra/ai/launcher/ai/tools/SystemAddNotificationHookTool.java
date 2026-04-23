package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.notifications.NotificationHookManager;

public class SystemAddNotificationHookTool extends BaseAITool {

    private static final String TAG = "SystemAddHookTool";
    private MainPack mainPack;

    public SystemAddNotificationHookTool() {
        super("system.add_notification_hook",
              "Add an automated reply rule for notifications. Supports temporal windows, static replies, AI-generated replies, and update logging.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("package", "Optional: Filter by app package (e.g., 'com.whatsapp')");
        params.put("sender_regex", "Optional: Regex or Name to match notification title/sender (e.g., 'Poorab')");
        params.put("content_regex", "Optional: Regex to match notification message content (e.g., 'hello')");
        params.put("reply_text", "Static reply text (optional if use_ai is true)");
        params.put("use_ai", "Boolean: If true, use AI to generate the reply based on ai_instruction");
        params.put("ai_instruction", "The instruction for the AI to generate a reply (e.g., 'let him know I am busy')");
        params.put("log_update", "Boolean: If true, log the notification details for later review");
        params.put("time_window", "Optional: Time window in HH:mm-HH:mm format (e.g., '17:00-20:00')");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        if (mainPack == null) {
            AISubsystem ai = AISubsystem.getInstance();
            if (ai != null) mainPack = ai.getMainPack();
        }

        NotificationHookManager.Hook hook = new NotificationHookManager.Hook();
        hook.id = UUID.randomUUID().toString().substring(0, 8);
        hook.packageName = args.optString("package", null);
        
        String senderRegex = args.optString("sender_regex", null);
        String finalSender = senderRegex;

        // Contact matching logic
        if (senderRegex != null && !senderRegex.isEmpty() && mainPack != null && mainPack.contacts != null) {
            // Check if it's likely a simple name and not a complex regex
            if (!senderRegex.contains(".*") && !senderRegex.contains("[") && !senderRegex.contains("(")) {
                List<ContactManager.Contact> matches = new ArrayList<>();
                String query = senderRegex.toLowerCase();
                
                for (ContactManager.Contact c : mainPack.contacts.getContacts()) {
                    String contactName = c.name.toLowerCase();
                    if (contactName.equals(query)) {
                        matches.clear();
                        matches.add(c);
                        break;
                    } else if (contactName.contains(query)) {
                        matches.add(c);
                    }
                }
                
                if (matches.size() == 1) {
                    finalSender = matches.get(0).name;
                    Log.d(TAG, "Matched sender '" + senderRegex + "' to contact '" + finalSender + "'");
                } else if (matches.size() > 1) {
                    // Pick the one that starts with the query if possible, or just the first one
                    for (ContactManager.Contact c : matches) {
                        if (c.name.toLowerCase().startsWith(query)) {
                            finalSender = c.name;
                            break;
                        }
                    }
                    if (finalSender.equals(senderRegex)) {
                        finalSender = matches.get(0).name;
                    }
                    Log.d(TAG, "Multiple matches for '" + senderRegex + "', picked '" + finalSender + "'");
                }
            }
        }

        hook.senderRegex = finalSender;
        hook.contentRegex = args.optString("content_regex", null);
        hook.replyText = args.optString("reply_text", null);
        
        hook.useAI = args.optBoolean("use_ai", false);
        hook.aiInstruction = args.optString("ai_instruction", null);
        hook.logUpdate = args.optBoolean("log_update", false);
        hook.timeWindow = args.optString("time_window", null);
        
        NotificationHookManager.getInstance(context).addHook(hook);
        
        String msg = "Powerful hook added successfully with ID: " + hook.id;
        if (finalSender != null && !finalSender.equals(senderRegex)) {
            msg += "\nNote: Matched '" + senderRegex + "' to contact '" + finalSender + "'";
        }
        return msg;
    }
}
