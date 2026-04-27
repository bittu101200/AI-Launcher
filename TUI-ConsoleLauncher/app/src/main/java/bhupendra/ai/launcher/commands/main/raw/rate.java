package bhupendra.ai.launcher.commands.main.raw;

import android.content.Intent;
import android.net.Uri;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;

public class rate implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        final MainPack info = (MainPack) pack;
        try {
            info.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" +
                    info.getContext().getPackageName())));
        } catch (android.content.ActivityNotFoundException anfe) {
            info.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" +
                    info.getContext().getPackageName())));
        }

        return info.getResources().getString(R.string.output_rate);
    }

    @Override
    public int helpRes() {
        return R.string.help_rate;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }


    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("rate", "Leave a feedback on the Play Store page", null, null, null);
    }
}
