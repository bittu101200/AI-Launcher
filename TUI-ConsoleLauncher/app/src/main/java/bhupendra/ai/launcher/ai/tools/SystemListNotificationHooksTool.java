package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Collections;
import java.util.List;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.NotificationHookManager;

public class SystemListNotificationHooksTool extends BaseAITool {

    public SystemListNotificationHooksTool() {
        super("system.list_notification_hooks",
              "List all active automated notification reply hooks.",
              Collections.emptyMap(),
              ToolRiskClass.READ_ONLY);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        List<NotificationHookManager.Hook> hooks = NotificationHookManager.getInstance(context).getHooks();
        if (hooks.isEmpty()) return "No notification hooks found.";

        JSONArray result = new JSONArray();
        for (NotificationHookManager.Hook h : hooks) {
            result.put(h.toJson());
        }
        return result.toString();
    }
}
