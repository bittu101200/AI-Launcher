package bhupendra.ai.launcher.ai.tools;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.ContactManager;

public class SystemSendSMSTool extends BaseAITool {

    private static final String TAG = "SystemSendSMSTool";
    private MainPack mainPack;

    public SystemSendSMSTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String recipient = args.getString("recipient");
        String message = args.getString("message");

        if (mainPack == null) {
            AISubsystem ai = AISubsystem.getInstance();
            if (ai != null) mainPack = ai.getMainPack();
        }

        if (mainPack == null) return "[error: mainPack not available]";

        String number = null;
        
        // Check if recipient is a number
        if (recipient.matches("\\+?[0-9\\s\\-\\(\\)]{7,15}")) {
            number = recipient.replaceAll("[\\s\\-\\(\\)]", "");
        } else if (mainPack.contacts != null) {
            // Try to find contact
            List<ContactManager.Contact> matches = new ArrayList<>();
            String query = recipient.toLowerCase();
            
            for (ContactManager.Contact c : mainPack.contacts.getContacts()) {
                String name = c.name.toLowerCase();
                if (name.equals(query) || name.contains(query)) {
                    matches.add(c);
                }
            }
            
            if (matches.size() == 0) {
                return "[error: contact '" + recipient + "' not found. Try searching for contacts first or provide a direct phone number.]";
            } else if (matches.size() > 1) {
                StringBuilder sb = new StringBuilder("[error: multiple contacts found for '" + recipient + "':\n");
                for (ContactManager.Contact m : matches) {
                    sb.append("- ").append(m.name).append(" (").append(m.numbers.toString()).append(")\n");
                }
                sb.append("Please be more specific.]");
                return sb.toString();
            } else {
                ContactManager.Contact contact = matches.get(0);
                if (contact.numbers.size() > 0) {
                    number = contact.numbers.get(0);
                } else {
                    return "[error: contact '" + contact.name + "' has no phone numbers.]";
                }
            }
        }

        if (number == null) {
            return "[error: could not resolve recipient to a phone number.]";
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return "[error: SEND_SMS permission not granted. Please grant it in app settings.]";
        }

        try {
            SmsManager smsManager;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                smsManager = context.getSystemService(SmsManager.class);
            } else {
                smsManager = SmsManager.getDefault();
            }
            
            if (smsManager == null) {
                smsManager = SmsManager.getDefault();
            }

            ArrayList<String> parts = smsManager.divideMessage(message);
            if (parts.size() > 1) {
                smsManager.sendMultipartTextMessage(number, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(number, null, message, null, null);
            }

            return "SMS sent to " + recipient + " (" + number + ")";
        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS", e);
            return "[error: failed to send SMS: " + e.getMessage() + "]";
        }
    }
}
