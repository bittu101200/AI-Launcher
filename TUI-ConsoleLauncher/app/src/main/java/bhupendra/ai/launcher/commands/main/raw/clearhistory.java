package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AISubsystem;

public class clearhistory implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack mp = (MainPack) pack;
        AISubsystem ai = mp.getAiSubsystem();
        if (ai == null) return "[AI not initialized]";
        ai.getConversationManager().clear();
        return "[conversation history cleared]";
    }

    @Override public int[] argType() { return new int[0]; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_clearhistory; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return null; }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return null; }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("clearhistory", "clearhistory — clear all AI conversation history", null, null, null);
    }
}