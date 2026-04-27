package bhupendra.ai.launcher.commands.main.raw;

import android.graphics.Color;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AppCapabilityScanner;
import bhupendra.ai.launcher.ai.AppCapabilityScanner.Capability;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.managers.TerminalManager;

public class integrate implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = ((bhupendra.ai.launcher.commands.main.MainPack) pack).aiSubsystem;
        if (ai == null || !ai.isAvailable()) return "[AI subsystem not available]";

        AppCapabilityScanner scanner = new AppCapabilityScanner(pack.getContext());
        List<Capability> caps = scanner.scanInstalledApps();
        if (caps.isEmpty()) return "[no capabilities found]";

        // Group by package for cleaner display
        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> labels = new HashMap<>();
        for (Capability cap : caps) {
            counts.put(cap.packageName, counts.getOrDefault(cap.packageName, 0) + 1);
            if (!labels.containsKey(cap.packageName) || cap.id.startsWith("launch:")) {
                labels.put(cap.packageName, cap.label);
            }
        }

        StringBuilder list = new StringBuilder("\n--- CAPABILITY HARVEST ---\n");
        list.append("Found ").append(caps.size()).append(" total functions.\n\n");
        
        int appCount = 0;
        for (String pkg : counts.keySet()) {
            if (appCount >= 25) {
                list.append("... and ").append(counts.size() - 25).append(" more apps.\n");
                break;
            }
            list.append("• ").append(labels.get(pkg)).append(" (").append(counts.get(pkg)).append(" functions)\n");
            appCount++;
        }

        list.append("\nType 'all' to integrate everything into the AI Mental Map, or 'cancel'.");
        Tuils.sendOutput(Color.WHITE, pack.getContext(), list.toString());

        new AppCapabilityScanner.PendingIntegration(caps, ai, pack.getContext());
        return null;
    }

    @Override public int[] argType() { return new int[0]; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_integrate; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return null; }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return null; }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("integrate", "integrate — discover installed apps and add as AI tools", null, null, null);
    }
}
