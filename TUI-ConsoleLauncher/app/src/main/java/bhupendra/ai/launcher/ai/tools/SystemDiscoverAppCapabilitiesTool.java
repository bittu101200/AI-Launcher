package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Process;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemDiscoverAppCapabilitiesTool extends BaseAITool {

    private static final String TAG = "AppDiscoveryTool";

    public SystemDiscoverAppCapabilitiesTool() {
        super("system.discover_app_capabilities",
              "Scan an installed app to discover its 'App Functions' (Shortcuts and deep links). Use this to see what specific tasks you can execute inside other apps.",
              Collections.singletonMap("package_name", "The package name of the app to scan (e.g., 'com.whatsapp')."),
              ToolRiskClass.READ_ONLY);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String pkg = args.getString("package_name");
        
        try {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (launcherApps == null) return "[error: LauncherApps service not available]";

            // Query dynamic and manifest shortcuts
            LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
            query.setPackage(pkg);
            query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC | 
                               LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST | 
                               LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);

            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle());
            
            if (shortcuts == null || shortcuts.isEmpty()) {
                return "[No specific App Functions discovered for " + pkg + ". You can still try to launch the app using 'system.execute_command' with 'open " + pkg + "']";
            }

            JSONArray results = new JSONArray();
            for (ShortcutInfo s : shortcuts) {
                JSONObject item = new JSONObject();
                item.put("function_id", s.getId());
                item.put("label", s.getShortLabel());
                if (s.getLongLabel() != null) item.put("description", s.getLongLabel());
                results.put(item);
            }

            return "Discovered " + shortcuts.size() + " App Functions for " + pkg + ":\n" + results.toString(2);
        } catch (SecurityException e) {
            return "[error: Launcher permission required to scan app functions. Please set this app as your default launcher.]";
        } catch (Exception e) {
            Log.e(TAG, "Discovery failed", e);
            return "[error: " + e.getMessage() + "]";
        }
    }
}
