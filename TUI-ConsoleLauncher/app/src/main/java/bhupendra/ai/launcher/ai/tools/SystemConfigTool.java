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
import bhupendra.ai.launcher.tuils.TermuxManager;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.ai.WebFetcher;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class SystemConfigTool extends BaseAITool {

    private static final String TAG = "SystemConfigTool";
    private MainPack mainPack;

    public SystemConfigTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            String action = args.getString("action"); // "get" or "set"
            String key = args.getString("key");
            
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null) return "[error: mainPack not available]";

            XMLPrefsSave save = null;
            // Search for the key in all known preference roots
            for (XMLPrefsManager.XMLPrefsRoot root : XMLPrefsManager.XMLPrefsRoot.values()) {
                for (XMLPrefsSave s : root.enums) {
                    if (s.label().equals(key)) {
                        save = s;
                        break;
                    }
                }
                if (save != null) break;
            }

            if (save == null) return "[error: config key not found: " + key + "]";

            if ("get".equals(action)) {
                return key + " = " + XMLPrefsManager.get(String.class, save);
            } else if ("set".equals(action)) {
                String value = args.getString("value");
                save.parent().write(save, value);
                
                if (context instanceof bhupendra.ai.launcher.tuils.interfaces.Reloadable) {
                    ((bhupendra.ai.launcher.tuils.interfaces.Reloadable) context).addMessage(save.parent().path(), save.label() + " -> " + value);
                }
                
                return "[set " + key + " to " + value + "]";
            }
            return "[error: invalid action: " + action + "]";
    }
}
