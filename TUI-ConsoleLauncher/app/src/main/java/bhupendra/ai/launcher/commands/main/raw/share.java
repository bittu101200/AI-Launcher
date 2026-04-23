package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.content.Intent;

import java.io.File;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.Tuils;

public class share implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        File f = info.get(File.class);
        if (f.isDirectory())
            return info.getResources().getString(R.string.output_isdirectory);

        Intent sharingIntent = FileSystemManager.shareFile(pack.getContext(), f);
        info.getContext().startActivity(Intent.createChooser(sharingIntent, info.getResources().getString(R.string.share_label)));

        return Tuils.EMPTYSTRING;
    }

    @Override
    public int helpRes() {
        return R.string.help_share;
    }

    @Override
    public int[] argType() {
        return new int[]{CommandAbstraction.FILE};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.getResources().getString(helpRes());
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;
        return info.getResources().getString(R.string.output_filenotfound);
    }

}
