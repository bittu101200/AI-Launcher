package bhupendra.ai.launcher.ai.tools;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.provider.ContactsContract;
import android.content.ContentProviderOperation;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Map;

import bhupendra.ai.launcher.commands.Command;
import bhupendra.ai.launcher.commands.CommandTuils;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.CronManager;
import bhupendra.ai.launcher.managers.PermissionManager;
import bhupendra.ai.launcher.integration.termux.TermuxManager;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.ai.WebFetcher;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class SystemAddContactTool extends BaseAITool {

    private static final String TAG = "SystemAddContactTool";
    private MainPack mainPack;

    public SystemAddContactTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            String name = args.getString("name");
            String phone = args.getString("phone");

            if (context instanceof Activity && PermissionManager.requestMissingPermissions((Activity) context,
                    bhupendra.ai.launcher.LauncherActivity.COMMAND_REQUEST_PERMISSION,
                    Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS)) {
                return "[waiting for contacts permission - please grant it and try again]";
            }

            // Duplicate detection
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }

            if (mainPack != null && mainPack.contacts != null) {
                for (bhupendra.ai.launcher.managers.ContactManager.Contact c : mainPack.contacts.getContacts()) {
                    if (name.equalsIgnoreCase(c.name)) {
                        return "[error: contact '" + name + "' already exists]";
                    }
                }
            }

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();

            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            try {
                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
                return "[added contact: " + name + " (" + phone + ")]";
            } catch (Exception e) {
                return "[error adding contact: " + e.getMessage() + "]";
            }
    }
}
