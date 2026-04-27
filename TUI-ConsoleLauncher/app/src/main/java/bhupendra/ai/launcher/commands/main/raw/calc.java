package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.TextProcessor;


import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.specific.PermanentSuggestionCommand;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 29/01/2017.
 */

public class calc implements PermanentSuggestionCommand {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        try {
            return String.valueOf(TextProcessor.eval(pack.getString()));
        } catch (Exception e) {
            return e.toString();
        }
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.PLAIN_TEXT};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_calc;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.getResources().getString(helpRes());
    }

    @Override
    public String[] permanentSuggestions() {
        return new String[] {"(", ")", "+", "-", "*", "/", "%", "^", "sqrt"};
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("calc", "Command implementation.", null, null, null);
    }
}
