package bhupendra.ai.launcher.ai.platform;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.specific.RedirectCommand;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Ai;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;
import bhupendra.ai.launcher.managers.FileSystemManager;

public class AIOnboardingManager {

    private static boolean onboardingStarted = false;

    public static void checkAndStart(bhupendra.ai.launcher.commands.main.MainPack pack) {
        if (onboardingStarted) return;
        
        String provider = XMLPrefsManager.get(Ai.provider);
        String key = XMLPrefsManager.get(Ai.api_key);

        if (provider.equals("mock") || key.isEmpty()) {
            onboardingStarted = true;
            
            List<File> backups = findBackups(pack.getContext());
            if (!backups.isEmpty()) {
                startBackupDetection(pack, backups);
            } else {
                startOnboarding(pack);
            }
        }
    }

    private static List<File> findBackups(android.content.Context context) {
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

    private static void startBackupDetection(bhupendra.ai.launcher.commands.main.MainPack pack, List<File> backups) {
        Tuils.sendOutput(Color.CYAN, pack.context, "\n--- TUI RESTORE ---", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(pack.context, "Existing backups were found in your Downloads folder:", TerminalManager.CATEGORY_OUTPUT);
        for (int i = 0; i < backups.size(); i++) {
            Tuils.sendOutput(Color.GREEN, pack.context, (i + 1) + ". " + backups.get(i).getName(), TerminalManager.CATEGORY_OUTPUT);
        }
        Tuils.sendOutput(pack.context, "\nType a number to restore, or type 'fresh' to start a new configuration.", TerminalManager.CATEGORY_OUTPUT);
        
        pack.redirectator.prepareRedirection(new BackupDetectionOnboarding(backups));
    }

    public static class BackupDetectionOnboarding extends RedirectCommand {
        private final List<File> backups;

        public BackupDetectionOnboarding(List<File> backups) {
            this.backups = backups;
        }

        @Override
        public String onRedirect(ExecutePack pack) {
            if (afterObjects.isEmpty()) return null;
            String input = String.valueOf(afterObjects.get(0)).trim().toLowerCase();
            
            if (input.equals("fresh")) {
                cleanup();
                startOnboarding((bhupendra.ai.launcher.commands.main.MainPack) pack);
                return null;
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
            return "Invalid selection. Enter a number (1-" + backups.size() + ") or 'fresh'.";
        }

        @Override public int getHint() { return R.string.restore_hint; }
        @Override public boolean isWaitingPermission() { return false; }
        @Override public String exec(ExecutePack pack) { return null; }
        @Override public int[] argType() { return new int[0]; }
        @Override public int priority() { return 0; }
        @Override public int helpRes() { return R.string.help_restore; }
        @Override public String onArgNotFound(ExecutePack pack, int index) { return null; }
        @Override public String onNotArgEnough(ExecutePack pack, int n) { return null; }
    }

    private static void startOnboarding(bhupendra.ai.launcher.commands.main.MainPack pack) {
        Tuils.sendOutput(Color.CYAN, pack.context, "\n--- AI LAUNCHER SETUP ---", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(pack.context, "Welcome! Let's configure your AI assistant.", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(pack.context, "Please choose an AI provider by typing its name:", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, pack.context, "- gemini (Recommended, fast & free tier)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, pack.context, "- claude (Anthropic, powerful reasoning)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, pack.context, "- openai (requires GPT-4o API key)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(Color.GREEN, pack.context, "- ollama (local, requires server URL)", TerminalManager.CATEGORY_OUTPUT);
        Tuils.sendOutput(pack.context, "\nType your choice below (or type 'help' for instructions):", TerminalManager.CATEGORY_OUTPUT);
        
        pack.redirectator.prepareRedirection(new ProviderOnboarding());
    }

    public static class ProviderOnboarding extends RedirectCommand {
        @Override
        public String onRedirect(ExecutePack pack) {
            if (afterObjects.isEmpty()) return null;
            String input = String.valueOf(afterObjects.get(0)).toLowerCase().trim();
            
            if (input.equals("login")) {
                Tuils.sendOutput(pack.context, "Opening browser for ChatGPT login...", TerminalManager.CATEGORY_OUTPUT);
                String oauthUrl = "https://ai-launcher-proxy.vercel.app/api/auth/openai";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(oauthUrl));
                pack.context.startActivity(browserIntent);
                return "Please complete the login in your browser. The launcher will automatically update when done.";
            }

            if (input.equals("gemini") || input.equals("openai") || input.equals("ollama") || input.equals("claude")) {
                Ai.provider.parent().write(Ai.provider, input);
                cleanup();
                
                Tuils.sendOutput(pack.context, "Selected: " + input, TerminalManager.CATEGORY_OUTPUT);
                Tuils.sendOutput(pack.context, "Now, please enter your API Key:", TerminalManager.CATEGORY_OUTPUT);
                
                pack.getRedirectator().prepareRedirection(new KeyOnboarding());
                return null;
            } else {
                afterObjects.clear();
                return "Invalid choice. Please choose: gemini, claude, openai, ollama, or login.";
            }
        }

        @Override public int getHint() { return R.string.help_help; }
        @Override public boolean isWaitingPermission() { return false; }
        @Override public String exec(ExecutePack pack) { return null; }
        @Override public int[] argType() { return new int[0]; }
        @Override public int priority() { return 0; }
        @Override public int helpRes() { return R.string.help_ai; }
        @Override public String onArgNotFound(ExecutePack pack, int index) { return null; }
        @Override public String onNotArgEnough(ExecutePack pack, int n) { return null; }
    }

    public static class KeyOnboarding extends RedirectCommand {
        @Override
        public String onRedirect(ExecutePack pack) {
            if (afterObjects.isEmpty()) return null;
            String key = String.valueOf(afterObjects.get(0)).trim();
            if (key.length() < 5) {
                afterObjects.clear();
                return "That key seems too short. Please try again:";
            }
            
            Ai.api_key.parent().write(Ai.api_key, key);
            cleanup();
            
            Tuils.sendOutput(Color.CYAN, pack.context, "AI Configuration Complete!", TerminalManager.CATEGORY_OUTPUT);
            Tuils.sendOutput(pack.context, "Restarting to apply changes...", TerminalManager.CATEGORY_OUTPUT);
            
            if (pack.context instanceof Reloadable) {
                ((Reloadable) pack.context).reload();
            }
            return null;
        }

        @Override public int getHint() { return R.string.help_help; }
        @Override public boolean isWaitingPermission() { return false; }
        @Override public String exec(ExecutePack pack) { return null; }
        @Override public int[] argType() { return new int[0]; }
        @Override public int priority() { return 0; }
        @Override public int helpRes() { return R.string.help_ai; }
        @Override public String onArgNotFound(ExecutePack pack, int index) { return null; }
        @Override public String onNotArgEnough(ExecutePack pack, int n) { return null; }
    }
}
