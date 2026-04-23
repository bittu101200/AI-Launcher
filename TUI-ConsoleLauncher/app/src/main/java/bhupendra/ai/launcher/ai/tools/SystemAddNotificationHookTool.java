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
              "Add an automated reply rule for notifications. If all match criteria (package, sender, content) are met, the reply is sent automatically.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("package", "Optional: Filter by app package (e.g., 'com.whatsapp')");
        params.put("sender_regex", "Optional: Regex to match notification title/sender (e.g., 'Poorab')");
        params.put("content_regex", "Optional: Regex to match notification message content (e.g., 'hello')");
        params.put("reply_text", "The message to send back automatically.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        NotificationHookManager.Hook hook = new NotificationHookManager.Hook();
        hook.id = UUID.randomUUID().toString().substring(0, 8);
        hook.packageName = args.optString("package", null);
        hook.senderRegex = args.optString("sender_regex", null);
        hook.contentRegex = args.optString("content_regex", null);
        hook.replyText = args.getString("reply_text");
        
        NotificationHookManager.getInstance(context).addHook(hook);
        
        return "Hook added successfully with ID: " + hook.id;
    }
}
