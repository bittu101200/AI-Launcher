package bhupendra.ai.launcher.commands.tuixt.raw;

import bhupendra.ai.launcher.managers.TextProcessor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.tuixt.TuixtPack;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 24/01/2017.
 */

public class help implements CommandAbstraction {

    @Override
    public String exec(ExecutePack info) throws Exception {
        TuixtPack pack = (TuixtPack) info;

        CommandAbstraction cmd = info.get(CommandAbstraction.class);
        int res = cmd == null ? R.string.output_commandnotfound : cmd.helpRes();
        return pack.resources.getString(res);
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return R.string.help_tuixt_help;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return onNotArgEnough(info, 0);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        TuixtPack info = (TuixtPack) pack;
        List<String> toPrint = new ArrayList<>(Arrays.asList(info.commandGroup.getCommandNames()));

        Collections.sort(toPrint, TextProcessor::alphabeticCompare);

        TextProcessor.addPrefix(toPrint, Tuils.DOUBLE_SPACE);
        TextProcessor.addSeparator(toPrint, Tuils.TRIBLE_SPACE);
        Tuils.insertHeaders(toPrint, true);

        return TextProcessor.toPlanString(toPrint, Tuils.EMPTYSTRING);
    }
}
