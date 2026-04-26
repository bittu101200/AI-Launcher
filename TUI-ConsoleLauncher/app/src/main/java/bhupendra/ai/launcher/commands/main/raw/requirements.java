package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.managers.PermissionManager;
import android.app.Activity;

public class requirements implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        PermissionManager.PermissionRequirement requesting = null;
        if (!(pack.context instanceof Activity)) {
            return PermissionManager.buildRequirementsReport(pack.context, null);
        }

        requesting = PermissionManager.requestNextMissing((Activity) pack.context);
        return PermissionManager.buildRequirementsReport(pack.context, requesting);
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_requirements;
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
