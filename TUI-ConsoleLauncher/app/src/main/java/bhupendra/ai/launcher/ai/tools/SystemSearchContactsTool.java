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

public class SystemSearchContactsTool extends BaseAITool {

    private static final String TAG = "SystemSearchContactsTool";
    private MainPack mainPack;

    public SystemSearchContactsTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            String query = args.optString("query", "").toLowerCase();
            
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null || mainPack.contacts == null) return "[error: contact manager not available]";
            
            java.util.List<bhupendra.ai.launcher.managers.ContactManager.Contact> contacts = mainPack.contacts.getContacts();
            StringBuilder results = new StringBuilder();
            int count = 0;
            
            for (bhupendra.ai.launcher.managers.ContactManager.Contact c : contacts) {
                String name = c.name.toLowerCase();
                int matchPercent = 0;
                if (name.equals(query)) matchPercent = 100;
                else if (name.startsWith(query)) matchPercent = 90;
                else if (name.contains(query)) matchPercent = 75;
                
                if (matchPercent >= 75) {
                    results.append("- ").append(c.name)
                           .append(" [Match: ").append(matchPercent).append("%]")
                           .append(": ").append(c.numbers.toString()).append("\n");
                    count++;
                }
            }
            
            if (count == 0) return "[no contacts found matching: " + query + "]";
            return "Found " + count + " contacts matching '" + query + "':\n" + results.toString();
    }
}
