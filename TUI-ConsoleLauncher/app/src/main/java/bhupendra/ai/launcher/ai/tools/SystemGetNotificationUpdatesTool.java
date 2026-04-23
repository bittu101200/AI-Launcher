package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Collections;
import java.util.List;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.NotificationUpdateManager;

public class SystemGetNotificationUpdatesTool extends BaseAITool {

    public SystemGetNotificationUpdatesTool() {
        super("system.get_notification_updates",
              "Retrieve logged notification updates (messages caught by hooks with log_update=true).",
              Collections.singletonMap("clear", "Boolean: If true, clear the logs after retrieval"),
              ToolRiskClass.STATE_CHANGING);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        boolean clear = args.optBoolean("clear", false);
        List<NotificationUpdateManager.Update> updates = NotificationUpdateManager.getInstance().getUpdates();
        
        if (updates.isEmpty()) return "No new notification updates.";

        JSONArray result = new JSONArray();
        for (NotificationUpdateManager.Update u : updates) {
            result.put(u.toJson());
        }

        if (clear) {
            NotificationUpdateManager.getInstance().clearUpdates();
        }

        return result.toString();
    }
}
