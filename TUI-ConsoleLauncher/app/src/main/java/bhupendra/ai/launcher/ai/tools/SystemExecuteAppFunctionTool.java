package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemExecuteAppFunctionTool extends BaseAITool {

    private static final String TAG = "AppExecutionTool";

    public SystemExecuteAppFunctionTool() {
        super("system.execute_app_function",
              "Execute a specific discovered App Function (shortcut) within another app. You MUST run 'system.discover_app_capabilities' first to get the correct 'function_id'.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("package_name", "The package name of the target app.");
        params.put("function_id", "The ID of the function (shortcut) to execute.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String pkg = args.getString("package_name");
        String functionId = args.getString("function_id");
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
            return "[error: App Function execution requires Android 7.1 or higher]";
        }

        try {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps == null) return "[error: LauncherApps service not available]";

            launcherApps.startShortcut(pkg, functionId, null, null, Process.myUserHandle());
            
            return "[App Function '" + functionId + "' triggered successfully for " + pkg + "]";
        } catch (SecurityException e) {
            return "[error: Launcher permission required. Please ensure this app is set as the default launcher.]";
        } catch (Exception e) {
            Log.e(TAG, "Execution failed", e);
            return "[error: " + e.getMessage() + "]";
        }
    }
}
