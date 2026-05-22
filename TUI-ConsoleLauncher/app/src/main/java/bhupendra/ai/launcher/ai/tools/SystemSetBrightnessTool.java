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
import bhupendra.ai.launcher.integration.termux.TermuxManager;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.ai.WebFetcher;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

public class SystemSetBrightnessTool extends BaseAITool {

    private static final String TAG = "SystemSetBrightnessTool";
    private MainPack mainPack;

    public SystemSetBrightnessTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            int percentage = args.optInt("percentage", -1);
            if (percentage < 0 || percentage > 100) {
                return "[error: brightness must be 0-100]";
            }

            if (!Settings.System.canWrite(context)) {
                context.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return "[waiting for write settings permission]";
            }

            final int b = percentage * 255 / 100;
            ContentResolver cResolver = context.getContentResolver();
            
            try {
                int autobrightnessState = Settings.System.getInt(cResolver, SCREEN_BRIGHTNESS_MODE);
                if (autobrightnessState == SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                    Settings.System.putInt(cResolver, SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not check/set brightness mode", e);
            }

            Settings.System.putInt(cResolver, SCREEN_BRIGHTNESS, b);

            if (context instanceof Activity) {
                final Activity activity = (Activity) context;
                activity.runOnUiThread(() -> {
                    Window window = activity.getWindow();
                    WindowManager.LayoutParams lp = window.getAttributes();
                    lp.screenBrightness = (float) b / 255f;
                    window.setAttributes(lp);
                });
            }

            return "[brightness set to " + percentage + "%]";
    }
}
