package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.notifications.reply.ReplyManager;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.ai.AISubsystem;

public class SystemSendAppMessageTool extends BaseAITool {

    public SystemSendAppMessageTool() {
        super("system.send_app_message",
              "Send a message via a specific app (e.g., WhatsApp, Telegram). " +
              "Supports background sending if a recent conversation is found in history. " +
              "For new WhatsApp conversations, provide the phone number to open the chat directly.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("app", "The name or package of the app (e.g., 'WhatsApp')");
        params.put("message", "The message text to send");
        params.put("contact", "Optional: The name of the person (as seen in notifications)");
        params.put("phone", "Optional: Direct phone number (with country code, no +) for WhatsApp/SMS fallback");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String appName = args.getString("app");
        String message = args.getString("message");
        String contact = args.optString("contact", null);
        String phone = args.optString("phone", null);

        ReplyManager replyManager = ReplyManager.instance;
        if (replyManager == null) {
            return "[error: ReplyManager not initialized]";
        }

        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null || ai.getMainPack() == null) return "[error: AISubsystem not ready]";

        AppsManager appsManager = ai.getMainPack().appsManager;
        String pkg = null;

        // Try to resolve app name to package
        AppsManager.LaunchInfo info = appsManager.findLaunchInfoWithPackage(appName);
        if (info == null) {
            info = appsManager.findLaunchInfoWithLabel(appName, AppsManager.SHOWN_APPS);
        }
        
        if (info != null) {
            pkg = info.componentName.getPackageName();
        } else {
            pkg = appName; // Final fallback
        }

        // Try background reply first
        try {
            replyManager.replyTo(context, pkg, contact, message);
            // Since replyTo is asynchronous (broadcast), we assume success if no immediate crash.
            // But we should warn if no notification was found.
            // We'll rely on the user to see the TUI output from ReplyManager.
            return "Attempting to send background message via " + pkg + (contact != null ? " to " + contact : "") + ".";
        } catch (Exception e) {
            // If background reply fails or is not possible, try fallback
            if (pkg.contains("whatsapp") && phone != null) {
                String cleanPhone = phone.replaceAll("[^0-9]", "");
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse("https://wa.me/" + cleanPhone + "?text=" + Uri.encode(message)));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                return "Background send failed. Opened WhatsApp chat with " + phone + ".";
            }
            return "[error: " + e.getMessage() + "]";
        }
    }
}
