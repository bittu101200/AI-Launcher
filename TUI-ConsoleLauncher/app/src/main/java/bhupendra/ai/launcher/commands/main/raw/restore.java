package bhupendra.ai.launcher.commands.main.raw;

import android.graphics.Color;
import android.os.Environment;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.RedirectCommand;
import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;

public class restore extends RedirectCommand {

    private List<File> backups;

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack info = (MainPack) pack;
        
        backups = findBackups(info.getContext());
        if (backups.isEmpty()) {
            return "No backups found in Downloads folder.";
        }

        Tuils.sendOutput(Color.CYAN, info.context, "\n--- TUI RESTORE ---", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(info.context, "Found " + backups.size() + " backups:", TerminalManager.CATEGORY_OUTPUT);
        for (int i = 0; i < backups.size(); i++) {
            Tuils.sendOutput(Color.GREEN, info.context, (i + 1) + ". " + backups.get(i).getName(), TerminalManager.CATEGORY_OUTPUT);
        }
        Tuils.sendOutput(info.context, "\nType a number to restore, or 'cancel'.", TerminalManager.CATEGORY_OUTPUT);
        
        info.redirectator.prepareRedirection(this);
        return null;
    }

    private List<File> findBackups(android.content.Context context) {
        List<File> backups = new ArrayList<>();
        File publicDownload = new File(Environment.getExternalStorageDirectory(), "Download");
        File privateDownload = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        
        File[] searchDirs = {publicDownload, privateDownload};
        for (File dir : searchDirs) {
            if (dir != null && dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory() && f.getName().startsWith("TUI_Backup_")) {
                            backups.add(f);
                        }
                    }
                }
            }
        }
        return backups;
    }

    @Override
    public String onRedirect(ExecutePack pack) {
        if (afterObjects.isEmpty()) return null;
        String input = String.valueOf(afterObjects.get(0)).trim().toLowerCase();
        
        if (input.equals("cancel")) {
            cleanup();
            return "Restore cancelled.";
        }

        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < backups.size()) {
                File selectedBackup = backups.get(index);
                File tuiFolder = FileSystemManager.getFolder();
                
                Tuils.sendOutput(pack.context, "Restoring from " + selectedBackup.getName() + "...", TerminalManager.CATEGORY_OUTPUT);
                
                FileSystemManager.deleteContentOnly(tuiFolder);
                FileSystemManager.copyDirectory(selectedBackup, tuiFolder);
                
                cleanup();
                
                Tuils.sendOutput(Color.CYAN, pack.context, "Restore Complete!", TerminalManager.CATEGORY_OUTPUT);
                Tuils.sendOutput(pack.context, "Restarting...", TerminalManager.CATEGORY_OUTPUT);
                
                if (pack.context instanceof Reloadable) {
                    ((Reloadable) pack.context).reload();
                }
                return null;
            }
        } catch (Exception e) {}

        afterObjects.clear();
        return "Invalid selection. Enter a number (1-" + backups.size() + ") or 'cancel'.";
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
        return R.string.help_restore;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return pack.getContext().getString(helpRes());
    }
    
    @Override public int getHint() { return R.string.restore_hint; }
    @Override public boolean isWaitingPermission() { return false; }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("restore", "Restore configuration from a backup in Downloads/TUI_Backup", null, null, null);
    }
}
