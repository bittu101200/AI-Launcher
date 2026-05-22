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
import bhupendra.ai.launcher.managers.ConfigChangeHandler;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.CronManager;
import bhupendra.ai.launcher.integration.termux.TermuxManager;
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

            XMLPrefsSave save = resolveConfigSave(key);

            if (save == null) return "[error: config key not found: " + key + "]";

            if ("get".equals(action)) {
                String val = XMLPrefsManager.get(String.class, save);
                if ("api_key".equals(key) && val != null && !val.isEmpty()) {
                    return key + " = " + val.substring(0, Math.min(4, val.length())) + "..." + (val.length() > 8 ? val.substring(val.length() - 4) : "");
                }
                return key + " = " + val;
            } else if ("set".equals(action)) {
                String value = args.getString("value");
                save.parent().write(save, value);

                Context liveContext = mainPack != null ? mainPack.getContext() : context;
                if (liveContext instanceof bhupendra.ai.launcher.tuils.interfaces.Reloadable) {
                    ((bhupendra.ai.launcher.tuils.interfaces.Reloadable) liveContext).addMessage(save.parent().path(), save.label() + " -> " + ("api_key".equals(key) ? "****" : value));
                }

                ConfigChangeHandler.apply(liveContext, save);
                
                return "[set " + key + " to " + ("api_key".equals(key) ? "****" : value) + "]";
            }
            return "[error: invalid action: " + action + "]";
    }

    private XMLPrefsSave resolveConfigSave(String key) {
        if (key == null) return null;

        String normalized = key.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        ArrayList<XMLPrefsSave> fuzzyMatches = new ArrayList<>();

        for (XMLPrefsManager.XMLPrefsRoot root : XMLPrefsManager.XMLPrefsRoot.values()) {
            for (XMLPrefsSave s : root.enums) {
                String label = s.label();
                String normalizedLabel = label.toLowerCase();
                if (label.equals(key) || normalizedLabel.equals(normalized)) {
                    return s;
                }
                if (!normalized.startsWith("show_") && normalizedLabel.equals("show_" + normalized)) {
                    return s;
                }
                if (normalizedLabel.contains(normalized)) {
                    fuzzyMatches.add(s);
                }
            }
        }

        return fuzzyMatches.size() == 1 ? fuzzyMatches.get(0) : null;
    }
}
