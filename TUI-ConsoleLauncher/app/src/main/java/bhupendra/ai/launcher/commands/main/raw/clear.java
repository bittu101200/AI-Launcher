package bhupendra.ai.launcher.commands.main.raw;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.UIManager;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;

/**
 * Created by andre on 07/11/15.
 */
public class clear implements CommandAbstraction {

    @Override
    public String exec(ExecutePack info) throws Exception {
        LocalBroadcastManager.getInstance(info.getContext().getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_CLEAR));
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
        return R.string.help_clear;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("clear", "Clear the screen", null, null, null);
    }
}
