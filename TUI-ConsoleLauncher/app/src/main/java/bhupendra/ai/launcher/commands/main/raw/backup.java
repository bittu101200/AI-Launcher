package bhupendra.ai.launcher.commands.main.raw;

import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class backup implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack info = (MainPack) pack;
        File srcDir = FileSystemManager.getFolder();
        
        if (srcDir == null || !srcDir.exists()) {
            return "TUI folder not found.";
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File downloadDir = info.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        
        // Use public Downloads if available, otherwise fallback to app's Downloads
        File publicDownload = new File(Environment.getExternalStorageDirectory(), "Download");
        File destParent = publicDownload.exists() ? publicDownload : downloadDir;
        
        File backupDir = new File(destParent, "TUI_Backup_" + timeStamp);
        
        try {
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                return "Could not create backup directory: " + backupDir.getAbsolutePath();
            }
            
            FileSystemManager.copyDirectory(srcDir, backupDir);
            
            return String.format(info.res.getString(R.string.output_backup_success), backupDir.getAbsolutePath());
        } catch (IOException e) {
            Tuils.log(e);
            return "Backup failed: " + e.getMessage();
        }
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
        return R.string.help_backup;
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
