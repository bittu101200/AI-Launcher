package bhupendra.ai.launcher.commands.main.raw;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.TermuxManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class termux implements CommandAbstraction {

    @Override
    public String exec(final ExecutePack pack) throws Exception {
        final Context context = pack.getContext();

        if (!TermuxManager.isTermuxInstalled(context)) {
            Tuils.sendOutput(Color.YELLOW, context, "Termux is not installed. Opening Play Store...");
            TermuxManager.openTermuxInPlayStore(context);
            return null;
        }

        List<String> argsList = (List<String>) pack.args[0];
        if (argsList.isEmpty()) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(TermuxManager.TERMUX_PACKAGE);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return "Opening Termux...";
            }
            return "Failed to open Termux.";
        }

        String command = argsList.get(0);
        String[] args = new String[argsList.size() - 1];
        for (int i = 1; i < argsList.size(); i++) {
            args[i - 1] = argsList.get(i);
        }

        TermuxManager.runCommand(context, command, args, null, true);
        return "Running '" + command + "'...";
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.TEXTLIST};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_termux;
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
