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

public class SystemRemoveContactTool extends BaseAITool {

    private static final String TAG = "SystemRemoveContactTool";
    private MainPack mainPack;

    public SystemRemoveContactTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            String name = args.getString("name");

            if (context instanceof Activity && PermissionManager.requestMissingPermissions((Activity) context,
                    bhupendra.ai.launcher.LauncherActivity.COMMAND_REQUEST_PERMISSION,
                    Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_CONTACTS)) {
                return "[waiting for contacts permission - please grant it and try again]";
            }

            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }

            if (mainPack == null || mainPack.contacts == null) return "[error: contact manager not available]";

            try {
                mainPack.contacts.delete(name);
                return "[removed contact: " + name + "]";
            } catch (Exception e) {
                return "[error removing contact: " + e.getMessage() + "]";
            }
    }
}
