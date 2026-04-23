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

public class SystemExecuteCommandTool extends BaseAITool {

    private static final String TAG = "SystemExecuteCommandTool";
    private MainPack mainPack;

    public SystemExecuteCommandTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            String commandLine = args.optString("command", null);
            if (commandLine == null) {
                if ("system.backup".equals(this.name)) {
                    commandLine = "backup";
                } else if ("system.restore".equals(this.name)) {
                    commandLine = "restore";
                }
            }
            
            if (commandLine == null) return "[error: command is required]";
            
            if (mainPack == null) {
                // Try to get from AISubsystem if not set directly
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null) return "[error: mainPack not available]";

            Command command = CommandTuils.parse(commandLine, mainPack);
            if (command == null) return "[error: unknown command: " + commandLine + "]";

            if (command.mArgs != null) {
                mainPack.clear();
                mainPack.set(command.mArgs);
            }
            String output = command.exec(mainPack);
            return output != null ? output : "[executed: " + commandLine + "]";
    }
}
