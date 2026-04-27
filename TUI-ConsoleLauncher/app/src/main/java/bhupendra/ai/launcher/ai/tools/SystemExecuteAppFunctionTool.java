package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.content.pm.LauncherApps;
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
              "Execute a specific discovered App Function (shortcut) within another app. You can provide either the raw shortcut ID or the full 'id' from the Dynamic Library.",
              createParams(),
              ToolRiskClass.LAUNCH_ONLY);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("package_name", "The package name of the target app.");
        params.put("function_id", "The ID of the function (shortcut) to execute.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String pkg = args.has("package_name") ? args.getString("package_name") : 
                     (args.has("package") ? args.getString("package") : null);
        String functionId = args.has("function_id") ? args.getString("function_id") : 
                            (args.has("id") ? args.getString("id") : null);
        
        if (pkg == null || functionId == null) {
            return "[error: Missing required arguments. Expected 'package_name' and 'function_id']";
        }
        
        // Clean up library ID format if present (shortcut:pkg:id)
        if (functionId.startsWith("shortcut:" + pkg + ":")) {
            functionId = functionId.substring(("shortcut:" + pkg + ":").length());
        } else if (functionId.startsWith("shortcut:")) {
            // Fallback for generic shortcut: prefix
            String[] parts = functionId.split(":");
            if (parts.length >= 3) functionId = parts[2];
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
