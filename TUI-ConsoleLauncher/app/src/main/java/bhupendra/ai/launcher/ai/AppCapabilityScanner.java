package bhupendra.ai.launcher.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import bhupendra.ai.launcher.tuils.Tuils;

public class AppCapabilityScanner {

    public static class Capability {
        public final String id;
        public final String label;
        public final String category;
        public final boolean preferred;

        public Capability(String id, String label, String category, boolean preferred) {
            this.id = id;
            this.label = label;
            this.category = category;
            this.preferred = preferred;
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

            if (input.trim().equalsIgnoreCase("all")) {
                for (Capability cap : current.capabilities) {
                    Tool tool = new AppCapabilityScanner(current.context).buildTool(cap);
                    current.aiSubsystem.getToolRegistry().register(ToolRegistry.Tier.APP_INTENT, tool);
                }
                Tuils.sendOutput(Color.GREEN, current.context,
                    "[integrated " + current.capabilities.size() + " capabilities]");
            } else {
                String[] parts = input.trim().split("\\s+");
                int count = 0;
                for (String part : parts) {
                    try {
                        int idx = Integer.parseInt(part) - 1;
                        if (idx >= 0 && idx < current.capabilities.size()) {
                            Tool tool = new AppCapabilityScanner(current.context).buildTool(current.capabilities.get(idx));
                            current.aiSubsystem.getToolRegistry().register(ToolRegistry.Tier.APP_INTENT, tool);
                            count++;
                        }
                    } catch (NumberFormatException ignored) {}
                }
                Tuils.sendOutput(Color.GREEN, current.context, "[integrated " + count + " capabilities]");
            }
        }
    }

    public static boolean shouldUseContactPicker(String action) {
        if (android.os.Build.VERSION.SDK_INT >= 37) {
            return "call".equals(action) || "message".equals(action) || "email".equals(action);
        }
        return false;
    }

    private static boolean isPersonTargeting(String toolId) {
        return toolId.startsWith("call:") || toolId.startsWith("message:") || toolId.startsWith("email:");
    }

    private final Context context;

    public AppCapabilityScanner(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Capability> scanInstalledApps() {
        List<Capability> result = new ArrayList<>();
        PackageManager pm = context.getPackageManager();
        Intent launch = new Intent(Intent.ACTION_MAIN, null);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(launch, 0)) {
            String pkg = info.activityInfo.packageName;
            String label = info.loadLabel(pm).toString();
            result.add(new Capability("launch:" + pkg, label, "LAUNCH", false));
        }
        return result;
    }

    public Tool buildTool(Capability capability) {
        Map<String, String> params = new LinkedHashMap<>();
        return new Tool(capability.id, capability.label, params, ToolRiskClass.LAUNCH_ONLY);
    }
}