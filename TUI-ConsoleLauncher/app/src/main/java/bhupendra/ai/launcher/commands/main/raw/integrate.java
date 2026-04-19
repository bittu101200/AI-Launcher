package bhupendra.ai.launcher.commands.main.raw;

import android.graphics.Color;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AppCapabilityScanner;
import bhupendra.ai.launcher.ai.AppCapabilityScanner.Capability;
import bhupendra.ai.launcher.tuils.Tuils;

import java.util.List;

public class integrate implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = ((bhupendra.ai.launcher.commands.main.MainPack) pack).aiSubsystem;
        if (ai == null || !ai.isAvailable()) return "[AI subsystem not available]";

        AppCapabilityScanner scanner = new AppCapabilityScanner(pack.context);
        List<Capability> caps = scanner.scanInstalledApps();
        if (caps.isEmpty()) return "[no capabilities found]";

        StringBuilder list = new StringBuilder("[Found " + caps.size() + " capabilities]\n");
        for (int i = 0; i < Math.min(caps.size(), 30); i++) {
            Capability cap = caps.get(i);
            list.append(i + 1).append(". [").append(cap.category).append("] ")
                .append(cap.label)
                .append(cap.preferred ? " [preferred]" : "")
                .append("\n");
        }
        list.append("\nType numbers to integrate (e.g. '1 3 5') or 'all':");
        Tuils.sendOutput(Color.WHITE, pack.context, list.toString());

        new AppCapabilityScanner.PendingIntegration(caps, ai, pack.context);
        return null;
    }

    @Override public int[] argType() { return new int[0]; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_integrate; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return null; }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return null; }
}