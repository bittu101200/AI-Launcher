package bhupendra.ai.launcher.commands.main.raw;

import android.app.Activity;
import android.content.Intent;

import java.io.File;
import java.io.IOException;

import bhupendra.ai.launcher.LauncherActivity;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.tuixt.TuixtActivity;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.FileManager;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 18/01/2017.
 */

public class tuixt implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        File file = info.get(File.class);
        if(file.isDirectory()) {
            return info.getResources().getString(R.string.output_isdirectory);
        }

        Intent intent = new Intent(info.getContext(), TuixtActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(TuixtActivity.PATH, file.getAbsolutePath());
        ((Activity) info.getContext()).startActivityForResult(intent, LauncherActivity.TUIXT_REQUEST);

        return Tuils.EMPTYSTRING;
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.FILE};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_tuixt;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        MainPack info = (MainPack) pack;

        String path = info.getString();
        if(path == null || path.length() == 0) {
            return onNotArgEnough(info, info.args.length);
        }

        FileManager.DirInfo dirInfo = FileManager.cd(info.getCurrentDirectory(), path);

        File file = new File(dirInfo.getCompletePath());
        if(!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            return info.getResources().getString(R.string.output_error);
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            return e.toString();
        }

        Intent intent = new Intent(info.getContext(), TuixtActivity.class);
        intent.putExtra(TuixtActivity.PATH, file.getAbsolutePath());
        ((Activity) info.getContext()).startActivityForResult(intent, LauncherActivity.TUIXT_REQUEST);

        return Tuils.EMPTYSTRING;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        MainPack info = (MainPack) pack;
        return info.getResources().getString(R.string.help_tuixt);
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("tuixt", "Open the text editor", null, null, null);
    }
}
