package bhupendra.ai.launcher.ai;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Process;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class AppCapabilityScanner {

    private static final String TAG = "AppCapabilityScanner";
    private static final String LIB_FILENAME = "app_capabilities.json";

    public static class Capability {
        public final String id;
        public final String label;
        public final String packageName;
        public final String category;
        public final String description;
        public long usageTime = 0;

        public Capability(String id, String label, String packageName, String category, String description) {
            this.id = id;
            this.label = label;
            this.packageName = packageName;
            this.category = category;
            this.description = description;
        }

        public JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("id", id);
            json.put("label", label);
            json.put("package", packageName);
            json.put("category", category);
            json.put("description", description);
            return json;
        }
    }

    public static class PendingIntegration {
        private static PendingIntegration active;
        private final List<Capability> capabilities;
        private final AISubsystem aiSubsystem;
        private final Context context;

        public PendingIntegration(List<Capability> capabilities, AISubsystem aiSubsystem, Context context) {
            this.capabilities = capabilities;
            this.aiSubsystem = aiSubsystem;
            this.context = context;
            active = this;
        }

        public static boolean isActive() { return active != null; }

        public static void processSelection(String input) {
            if (active == null) return;
            PendingIntegration current = active;
            active = null;

            List<Capability> selected = new ArrayList<>();
            if (input.trim().equalsIgnoreCase("all")) {
                selected.addAll(current.capabilities);
            } else {
                String[] parts = input.trim().split("\\s+");
                for (String part : parts) {
                    try {
                        int idx = Integer.parseInt(part) - 1;
                        if (idx >= 0 && idx < current.capabilities.size()) {
                            selected.add(current.capabilities.get(idx));
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }

            saveLibrary(current.context, selected);
            Tuils.sendOutput(Color.GREEN, current.context, "[integrated " + selected.size() + " capabilities into Dynamic Library]");
        }
    }

    private final Context context;
    private static List<Capability> cachedScan = null;
    private static long cachedScanTime = 0;
    private static final long SCAN_CACHE_TTL_MS = 60_000L;

    public AppCapabilityScanner(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Capability> scanInstalledApps() {
        long now = System.currentTimeMillis();
        if (cachedScan != null && (now - cachedScanTime) < SCAN_CACHE_TTL_MS) {
            return new ArrayList<>(cachedScan);
        }
        List<Capability> result = doScan();
        cachedScan = result;
        cachedScanTime = now;
        return new ArrayList<>(result);
    }

    public static void invalidateCache() {
        cachedScan = null;
        cachedScanTime = 0;
    }

    private List<Capability> doScan() {
        List<Capability> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        
        // 1. Get Usage Stats for sorting
        Map<String, Long> usageMap = getUsageStats();

        // 2. Scan Launcher Apps (Base Capabilities)
        Intent launchIntent = new Intent(Intent.ACTION_MAIN, null);
        launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(launchIntent, 0)) {
            String pkg = info.activityInfo.packageName;
            String label = info.loadLabel(pm).toString();
            Capability cap = new Capability("launch:" + pkg, label, pkg, "APP", "Launch " + label);
            cap.usageTime = usageMap.getOrDefault(pkg, 0L);
            result.add(cap);

            // 3. Deep Scan Shortcuts (App Functions)
            try {
                LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
                query.setPackage(pkg);
                query.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC | LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST);

                List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, Process.myUserHandle());
                if (shortcuts != null) {
                    for (ShortcutInfo s : shortcuts) {
                        String desc = s.getLongLabel() != null ? s.getLongLabel().toString() : s.getShortLabel().toString();
                        Capability subCap = new Capability("shortcut:" + pkg + ":" + s.getId(), s.getShortLabel().toString(), pkg, "FUNCTION", desc);
                        subCap.usageTime = cap.usageTime; // Inherit parent app usage for sorting
                        result.add(subCap);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to scan shortcuts for " + pkg, e);
            }
        }

        // 4. Sort by usage time (Descending)
        Collections.sort(result, (a, b) -> Long.compare(b.usageTime, a.usageTime));
        
        return result;
    }

    private Map<String, Long> getUsageStats() {
        Map<String, Long> map = new HashMap<>();
        UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        long now = System.currentTimeMillis();
        List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, now - (1000 * 60 * 60 * 24 * 7), now);
        if (stats != null) {
            for (UsageStats s : stats) {
                map.put(s.getPackageName(), s.getTotalTimeInForeground());
            }
        }
        return map;
    }

    public static void saveLibrary(Context context, List<Capability> selected) {
        try {
            JSONArray array = new JSONArray();
            for (Capability c : selected) {
                array.put(c.toJson());
            }
            File file = new File(FileSystemManager.getFolder(), LIB_FILENAME);
            FileSystemManager.saveFile(file, array.toString());
            invalidateCache();
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public static String getLibraryContent(Context context) {
        try {
            File file = new File(FileSystemManager.getFolder(), LIB_FILENAME);
            if (!file.exists()) return "[]";
            return FileSystemManager.readFile(file);
        } catch (Exception e) {
            return "[]";
        }
    }
}
