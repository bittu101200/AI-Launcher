package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.TextProcessor;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.Tuils;

public class help implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack info = (MainPack) pack;
        CommandAbstraction cmd = info.get(CommandAbstraction.class);
        if (cmd == null) return info.getResources().getString(R.string.output_commandnotfound);

        bhupendra.ai.launcher.commands.CommandMetadata meta = bhupendra.ai.launcher.commands.CommandHelpRegistry.getInstance(pack.getContext()).getCommandHelp(cmd.getClass().getSimpleName());
        if (meta != null) {
            return meta.toString();
        }

        int res = cmd.helpRes();
        return "Priority: " + info.getCmdPrefs().getPriority(cmd) + Tuils.NEWLINE + info.getResources().getString(res);
    }

    @Override
    public int helpRes() {
        return R.string.help_help;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.COMMAND};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        List<String> toPrint = new ArrayList<>(Arrays.asList(info.commandGroup.getCommandNames()));

        Collections.sort(toPrint, TextProcessor::alphabeticCompare);

        TextProcessor.addPrefix(toPrint, Tuils.DOUBLE_SPACE);
        TextProcessor.addSeparator(toPrint, Tuils.TRIBLE_SPACE);
        Tuils.insertHeaders(toPrint, true);

        return TextProcessor.toPlanString(toPrint, "");
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        return info.getResources().getString(R.string.output_commandnotfound);
    }


    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("help", "Print the available commands, or info about a command", null, null, null);
    }
}
