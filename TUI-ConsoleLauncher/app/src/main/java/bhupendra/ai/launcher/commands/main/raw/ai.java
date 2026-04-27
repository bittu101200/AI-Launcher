package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AITrigger;
import android.util.Log;

public class ai implements CommandAbstraction {

    private static final String TAG = "AI_CMD";

    @Override
    public String exec(ExecutePack pack) throws Exception {
        String query = pack.getString();
        Log.d(TAG, "exec: query=" + query);
        if (query == null || query.trim().isEmpty()) {
            return pack.getContext().getString(R.string.help_ai);
        }
        MainPack mp = (MainPack) pack;
        AISubsystem aiSubsystem = mp.getAiSubsystem();
        Log.d(TAG, "exec: aiSubsystem=" + aiSubsystem + " available=" + (aiSubsystem != null && aiSubsystem.isAvailable()));
        if (aiSubsystem == null || !aiSubsystem.isAvailable()) {
            return "[AI subsystem not available — check ai.xml]";
        }

        AITrigger trigger = new AITrigger(aiSubsystem, pack.getContext(), null);
        trigger.triggerDirect(query.trim());
        return null;
    }

    @Override public int[] argType() { return new int[]{CommandAbstraction.PLAIN_TEXT}; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_ai; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return pack.getContext().getString(R.string.help_ai); }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return pack.getContext().getString(R.string.help_ai); }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("ai", "ai [query] — send a query directly to AI", null, null, null);
    }
}
