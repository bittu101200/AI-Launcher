package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.tuils.BeepPlayer;

/**
 * Created by francescoandreuzzi on 29/04/2017.
 */

public class beep implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        try {
            BeepPlayer.playAlert(pack.getContext(), BeepPlayer.Options.defaults());
        } catch (Exception e) {
            return e.toString();
        }

        return null;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public int helpRes() {
        return R.string.help_beep;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return pack.getContext().getString(helpRes());
    }
}
