package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.ai.AISubsystem;
import android.graphics.Color;
import bhupendra.ai.launcher.managers.TerminalManager;

public class kill implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = AISubsystem.getInstance();
        if (ai != null) {
            ai.hardKill();
            return "AI processes terminated and history cleared.";
        }
        return "AI Subsystem not active.";
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return R.string.help_kill;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }
}
