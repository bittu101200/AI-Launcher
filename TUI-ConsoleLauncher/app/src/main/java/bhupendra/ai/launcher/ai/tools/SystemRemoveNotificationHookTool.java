package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;
import java.util.Collections;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.NotificationHookManager;

public class SystemRemoveNotificationHookTool extends BaseAITool {

    public SystemRemoveNotificationHookTool() {
        super("system.remove_notification_hook",
              "Remove an automated notification reply hook by ID.",
              Collections.singletonMap("id", "The 8-character ID of the hook to remove"),
              ToolRiskClass.STATE_CHANGING);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String id = args.getString("id");
        NotificationHookManager.getInstance(context).removeHook(id);
        return "Hook " + id + " removed.";
    }
}
