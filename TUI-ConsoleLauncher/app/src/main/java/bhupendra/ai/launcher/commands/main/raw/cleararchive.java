package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AISubsystem;

public class cleararchive implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack mp = (MainPack) pack;
        AISubsystem ai = mp.aiSubsystem;
        if (ai == null) return "[AI not initialized]";
        int deleted = ai.getConversationManager().clearArchives();
        return "[deleted " + deleted + " archive file(s)]";
    }

    @Override public int[] argType() { return new int[0]; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_cleararchive; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return null; }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return null; }
}