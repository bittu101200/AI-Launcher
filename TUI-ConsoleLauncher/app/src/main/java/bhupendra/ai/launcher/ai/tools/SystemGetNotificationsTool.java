package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.service.notification.StatusBarNotification;
import android.app.Notification;
import android.os.Bundle;
import androidx.core.app.NotificationCompat;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.Collections;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.NotificationService;

public class SystemGetNotificationsTool extends BaseAITool {

    public SystemGetNotificationsTool() {
        super("system.get_notifications",
              "Get a list of active notifications including app name, title, and text content.",
              Collections.emptyMap(),
              ToolRiskClass.READ_ONLY);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        NotificationService service = NotificationService.instance;
        if (service == null) {
            return "Notification access is not active. The user might need to enable Notification Listener permission.";
        }

        StatusBarNotification[] sbns = service.getActiveNotifications();
        if (sbns == null || sbns.length == 0) {
            return "No active notifications.";
        }

        JSONArray result = new JSONArray();
        for (int i = 0; i < sbns.length; i++) {
            StatusBarNotification sbn = sbns[i];
            Notification n = sbn.getNotification();
            Bundle extras = NotificationCompat.getExtras(n);

            JSONObject item = new JSONObject();
            item.put("id", i);
            item.put("package", sbn.getPackageName());
            
            String appName = sbn.getPackageName();
            try {
                appName = context.getPackageManager().getApplicationLabel(
                    context.getPackageManager().getApplicationInfo(sbn.getPackageName(), 0)).toString();
            } catch (Exception ignored) {}
            item.put("app_name", appName);

            if (extras != null) {
                item.put("title", String.valueOf(extras.getCharSequence(Notification.EXTRA_TITLE)));
                item.put("text", String.valueOf(extras.getCharSequence(Notification.EXTRA_TEXT)));
                item.put("sub_text", String.valueOf(extras.getCharSequence(Notification.EXTRA_SUB_TEXT)));
            }
            
            result.put(item);
        }

        return result.toString();
    }
}
