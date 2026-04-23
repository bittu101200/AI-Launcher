package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.NotificationHookManager;

public class SystemAddNotificationHookTool extends BaseAITool {

    public SystemAddNotificationHookTool() {
        super("system.add_notification_hook",
              "Add an automated reply rule for notifications. Supports temporal windows, static replies, AI-generated replies, and update logging.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("package", "Optional: Filter by app package (e.g., 'com.whatsapp')");
        params.put("sender_regex", "Optional: Regex to match notification title/sender (e.g., 'Poorab')");
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
        NotificationHookManager.Hook hook = new NotificationHookManager.Hook();
        hook.id = UUID.randomUUID().toString().substring(0, 8);
        hook.packageName = args.optString("package", null);
        hook.senderRegex = args.optString("sender_regex", null);
        hook.contentRegex = args.optString("content_regex", null);
        hook.replyText = args.optString("reply_text", null);
        
        hook.useAI = args.optBoolean("use_ai", false);
        hook.aiInstruction = args.optString("ai_instruction", null);
        hook.logUpdate = args.optBoolean("log_update", false);
        hook.timeWindow = args.optString("time_window", null);
        
        NotificationHookManager.getInstance(context).addHook(hook);
        
        return "Powerful hook added successfully with ID: " + hook.id;
    }
}
