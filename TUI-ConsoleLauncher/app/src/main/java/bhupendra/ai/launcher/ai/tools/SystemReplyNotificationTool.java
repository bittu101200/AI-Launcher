package bhupendra.ai.launcher.ai.tools;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.NotificationService;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;

public class SystemReplyNotificationTool extends BaseAITool {

    public SystemReplyNotificationTool() {
        super("system.reply_notification",
              "Reply to a notification by ID. Get IDs from system.get_notifications.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("id", "The ID of the notification from system.get_notifications list");
        params.put("text", "The reply message text");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        int id = args.getInt("id");
        String text = args.getString("text");

        NotificationService service = NotificationService.instance;
        if (service == null) {
            return "Notification service not active.";
        }

        StatusBarNotification[] sbns = service.getActiveNotifications();
        if (sbns == null || id < 0 || id >= sbns.length) {
            return "Invalid notification ID.";
        }

        StatusBarNotification sbn = sbns[id];
        Notification n = sbn.getNotification();
        
        // Find reply action
        Notification.Action replyAction = null;
        if (n.actions != null) {
            for (Notification.Action action : n.actions) {
                if (action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                    replyAction = action;
                    break;
                }
            }
        }
        
        // Try WearableExtender if not found in main actions
        if (replyAction == null) {
            Notification.WearableExtender wearableExtender = new Notification.WearableExtender(n);
            for (Notification.Action action : wearableExtender.getActions()) {
                if (action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                    replyAction = action;
                    break;
                }
            }
        }

        if (replyAction == null) {
            return "This notification does not support direct replies.";
        }

        Intent i = new Intent(PrivateIOReceiver.ACTION_REPLY);
        i.putExtra(PrivateIOReceiver.BUNDLE, n.extras);
        i.putExtra(PrivateIOReceiver.REMOTE_INPUTS, replyAction.getRemoteInputs());
        i.putExtra(PrivateIOReceiver.TEXT, text);
        i.putExtra(PrivateIOReceiver.PENDING_INTENT, replyAction.actionIntent);
        i.putExtra(PrivateIOReceiver.ID, sbn.getId());
        i.putExtra(PrivateIOReceiver.CURRENT_ID, PrivateIOReceiver.currentId);

        LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);

        return "Reply sent successfully.";
    }
}
