package bhupendra.ai.launcher.tuils;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TermuxManager {

    public static final String TERMUX_PACKAGE = "com.termux";
    public static final String TERMUX_PREFIX = "/data/data/com.termux/files/usr";
    public static final String TERMUX_BIN = TERMUX_PREFIX + "/bin";
    
    public static final String ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND";
    public static final String RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService";
    
    public static final String EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH";
    public static final String EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS";
    public static final String EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR";
    public static final String EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND";
    public static final String EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION";
    public static final String EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT";

    public static final String ACTION_TERMUX_RESULT = "bhupendra.ai.launcher.TERMUX_RESULT";

    private static final Map<Integer, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public static boolean isTermuxInstalled(Context context) {
        try {
            context.getPackageManager().getPackageInfo(TERMUX_PACKAGE, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String runCommandSync(Context context, String command, String[] args, String workdir, long timeoutMs) {
        int requestCode = (int) (System.currentTimeMillis() & 0xfffffff);
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRequests.put(requestCode, future);

        runCommandInternal(context, command, args, workdir, true, requestCode);

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            pendingRequests.remove(requestCode);
            return "[error: Termux command timed out or failed: " + e.getMessage() + "]";
        }
    }

    public static void onResultReceived(int requestCode, String result) {
        CompletableFuture<String> future = pendingRequests.remove(requestCode);
        if (future != null) {
            future.complete(result);
        }
    }

    public static void cancelAll() {
        for (Map.Entry<Integer, CompletableFuture<String>> entry : pendingRequests.entrySet()) {
            entry.getValue().complete("[error: cancelled by user]");
        }
        pendingRequests.clear();
    }

    public static void runCommand(Context context, String command, String[] args, String workdir, boolean background) {
        runCommandInternal(context, command, args, workdir, background, (int) (System.currentTimeMillis() & 0xfffffff));
    }

    private static void runCommandInternal(Context context, String command, String[] args, String workdir, boolean background, int requestCode) {
        if (command == null || command.trim().isEmpty()) return;

        Tuils.log("TermuxManager: runCommand (Service): " + command + " (RQ:" + requestCode + ")");

        String cleanCommand = command.trim();
        if (cleanCommand.startsWith("\"") && cleanCommand.endsWith("\"")) {
            cleanCommand = cleanCommand.substring(1, cleanCommand.length() - 1).trim();
        } else if (cleanCommand.startsWith("'") && cleanCommand.endsWith("'")) {
            cleanCommand = cleanCommand.substring(1, cleanCommand.length() - 1).trim();
        }

        String[] parts = cleanCommand.split("\\s+");
        String execName = parts[0];
        
        List<String> finalArgs = new ArrayList<>();
        if (parts.length > 1) {
            finalArgs.addAll(Arrays.asList(parts).subList(1, parts.length));
        }
        if (args != null) {
            finalArgs.addAll(Arrays.asList(args));
        }

        String commandPath = execName;
        if (!execName.startsWith("/")) {
            commandPath = TERMUX_BIN + "/" + execName;
        }

        // Result Intent
        Intent resultIntent = new Intent(ACTION_TERMUX_RESULT);
        resultIntent.setPackage(context.getPackageName());
        resultIntent.setComponent(new ComponentName(context.getPackageName(), TermuxResultReceiver.class.getName()));
        resultIntent.putExtra("request_code", requestCode);
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, resultIntent, flags);

        // Build Intent for Service
        Intent intent = new Intent(ACTION_RUN_COMMAND);
        intent.setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE);
        intent.putExtra(EXTRA_COMMAND_PATH, commandPath);
        if (!finalArgs.isEmpty()) {
            intent.putExtra(EXTRA_ARGUMENTS, finalArgs.toArray(new String[0]));
        }
        intent.putExtra(EXTRA_WORKDIR, workdir != null ? workdir : "/data/data/com.termux/files/home");
        intent.putExtra(EXTRA_BACKGROUND, background);
        intent.putExtra(EXTRA_SESSION_ACTION, background ? "2" : "0");
        intent.putExtra(EXTRA_PENDING_INTENT, pendingIntent);

        try {
            // Prefer startService for RUN_COMMAND
            context.startService(intent);
            Tuils.log("TermuxManager: startService sent");
        } catch (Exception e) {
            Tuils.log("TermuxManager: startService failed, trying broadcast: " + e.getMessage());
            intent.setComponent(null);
            intent.setPackage(TERMUX_PACKAGE);
            context.sendBroadcast(intent);
        }
    }

    public static void openTermuxInPlayStore(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + TERMUX_PACKAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + TERMUX_PACKAGE)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }
}
