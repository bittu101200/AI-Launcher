package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.graphics.Color;
import org.json.JSONObject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AppCapabilityScanner;
import bhupendra.ai.launcher.ai.AppCapabilityScanner.Capability;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.managers.TerminalManager;

public class SystemIntegrateTool extends BaseAITool {

    public SystemIntegrateTool() {
        super("system.integrate",
              "Scan the system to discover and index all App Functions (shortcuts). Use this to update your knowledge of what apps can do.",
              Collections.emptyMap(),
              ToolRiskClass.READ_ONLY);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null) return "[error: AI subsystem not available]";

        AppCapabilityScanner scanner = new AppCapabilityScanner(context);
        List<Capability> caps = scanner.scanInstalledApps();
        
        if (caps.isEmpty()) return "[No capabilities found on device]";

        // Group by package for a cleaner display
        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (Capability cap : caps) {
            counts.put(cap.packageName, counts.getOrDefault(cap.packageName, 0) + 1);
            if (!labels.containsKey(cap.packageName) || cap.id.startsWith("launch:")) {
                labels.put(cap.packageName, cap.label);
            }
        }

        StringBuilder sb = new StringBuilder("\n--- SYSTEM INTEGRATION ---\n");
        sb.append("Discovered ").append(caps.size()).append(" capabilities across ").append(counts.size()).append(" apps.\n\n");
        
        int appCount = 0;
        for (String pkg : counts.keySet()) {
            if (appCount >= 20) {
                sb.append("... and ").append(counts.size() - 20).append(" more apps.\n");
                break;
            }
            sb.append("• ").append(labels.get(pkg)).append(" (").append(counts.get(pkg)).append(" functions)\n");
            appCount++;
        }
        
        sb.append("\nIntegrating all capabilities into Dynamic Library...");
        
        // Auto-integrate everything for the AI
        AppCapabilityScanner.saveLibrary(context, caps);
        
        return sb.toString();
    }
}
