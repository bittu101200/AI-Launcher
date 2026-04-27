package bhupendra.ai.launcher.commands;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.ai.AITrigger;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.RedirectCommand;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.TimeManager;
import bhupendra.ai.launcher.managers.music.MusicService;
import bhupendra.ai.launcher.managers.notifications.KeeperService;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.tuils.BeepPlayer;

import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.libsuperuser.Shell;
import bhupendra.ai.launcher.managers.TextProcessor;

public class CommandExecutionController {

    private final Context mContext;
    private final MainPack mainPack;

    private boolean showAliasValue;
    private boolean showAppHistory;
    private int aliasContentColor;
    private String multipleCmdSeparator;
    private boolean keeperServiceRunning;

    private CmdTrigger[] triggers;
    private ShellCommandTrigger shellCommandTrigger;
    private AITrigger aiTrigger;

    private String appFormat;
    private int outputColor;

    private Pattern pa = Pattern.compile("%a", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private Pattern pp = Pattern.compile("%p", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private Pattern pl = Pattern.compile("%l", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
    private Pattern colorExtractor = Pattern.compile("(#[^(]{6})\\[([^\\)]*)\\]", Pattern.CASE_INSENSITIVE);

    public CommandExecutionController(Context context, MainPack mainPack) {
        this.mContext = context;
        this.mainPack = mainPack;
        
        keeperServiceRunning = XMLPrefsManager.getBoolean(Behavior.tui_notification);
        showAliasValue = XMLPrefsManager.getBoolean(Behavior.show_alias_content);
        showAppHistory = XMLPrefsManager.getBoolean(Behavior.show_launch_history);
        aliasContentColor = XMLPrefsManager.getColor(Theme.alias_content_color);
        multipleCmdSeparator = XMLPrefsManager.get(Behavior.multiple_cmd_separator);

        shellCommandTrigger = new ShellCommandTrigger();
        triggers = new CmdTrigger[] {
                new GroupTrigger(),
                new AliasTrigger(),
                new TuiCommandTrigger(),
                new AppTrigger(),
                shellCommandTrigger
        };
    }

    public void setAITrigger(AITrigger aiTrigger) {
        this.aiTrigger = aiTrigger;
    }
    
    public ShellCommandTrigger getShellCommandTrigger() {
        return shellCommandTrigger;
    }

    public boolean onAICommand(String input, String requestId) {
        return onCommandInternal(input, null, false, requestId, false, false);
    }

    private void logCommandFinished(String requestId) {
        if (requestId == null || requestId.length() == 0) {
            android.util.Log.i("AI_OUTPUT", "CMD_FINISHED");
        } else {
            android.util.Log.i("AI_OUTPUT", "CMD_FINISHED requestId=" + requestId);
        }
    }

    private void updateServices(String cmd, boolean wasMusicService) {
        if(keeperServiceRunning) {
            Intent i = new Intent(mContext, KeeperService.class);
            i.putExtra(KeeperService.CMD_KEY, cmd);
            i.putExtra(KeeperService.PATH_KEY, mainPack.currentDirectory.getAbsolutePath());
            mContext.startService(i);
        }

        if(wasMusicService) {
            Intent i = new Intent(mContext, MusicService.class);
            mContext.startService(i);
        }
    }

    public void onCommand(String input, AppsManager.LaunchInfo launchInfo, boolean wasMusicService, String requestId) {
        if(launchInfo == null) {
            onCommand(input, (String) null, wasMusicService, requestId);
            return;
        }

        updateServices(input, wasMusicService);

        if(launchInfo.unspacedLowercaseLabel.equals(TextProcessor.removeSpaces(input.toLowerCase()))) {
            performLaunch(mainPack, launchInfo, input);
        } else {
            onCommand(input, (String) null, wasMusicService, requestId);
        }
    }

    public void onCommand(String input, String alias, boolean wasMusicService, String requestId) {
        onCommandInternal(input, alias, wasMusicService, requestId, true, true);
    }

    private boolean onCommandInternal(String input, String alias, boolean wasMusicService, String requestId, boolean allowShellFallback, boolean allowAITrigger) {
        BeepPlayer.stopRepeatingAlert();

        if (bhupendra.ai.launcher.ai.AppCapabilityScanner.PendingIntegration.isActive()) {
            bhupendra.ai.launcher.ai.AppCapabilityScanner.PendingIntegration.processSelection(input);
            return true;
        }

        if (mainPack.aiSubsystem != null) {
            if (mainPack.aiSubsystem.isInFlight()) {
                String trimmed = input.trim();
                if (trimmed.equalsIgnoreCase("stop") || trimmed.equalsIgnoreCase("cancel")) {
                    mainPack.aiSubsystem.cancel();
                    return true;
                } else if (trimmed.equalsIgnoreCase("kill")) {
                    mainPack.aiSubsystem.hardKill();
                    Tuils.sendOutput(mContext, "AI processes terminated and history cleared.");
                    return true;
                }
            }
            if (mainPack.aiSubsystem.isAwaitingConfirmation()) {
                if (input.trim().isEmpty()) {
                    mainPack.aiSubsystem.confirmCurrentTool();
                } else {
                    mainPack.aiSubsystem.declineCurrentTool();
                }
                return true;
            }
        }

        input = TextProcessor.removeUnncesarySpaces(input);

        if(alias == null) updateServices(input, wasMusicService);

        if(mainPack.redirectator != null) {
            RedirectCommand redirect = null; // Need to access redirect state from MainPack or pass it.
            // Wait, redirectator state is in MainPack or MainManager?
            // In MainManager, it's `redirect`. Let's handle it later or modify how redirect works.
        }

        if(alias != null && showAliasValue) {
           Tuils.sendOutput(aliasContentColor, mContext, mainPack.aliasManager.formatLabel(alias, input));
        }

        String[] cmds;
        if(multipleCmdSeparator.length() > 0) {
            cmds = input.split(multipleCmdSeparator);
        } else {
            cmds = new String[] {input};
        }

        int[] colors = new int[cmds.length];
        for(int c = 0; c < colors.length; c++) {
            Matcher m = colorExtractor.matcher(cmds[c]);
            if(m.matches()) {
                try {
                    colors[c] = Color.parseColor(m.group(1));
                    cmds[c] = m.group(2);
                } catch (Exception e) {
                    colors[c] = TerminalManager.NO_COLOR;
                }
            } else colors[c] = TerminalManager.NO_COLOR;
        }

        boolean agentic = XMLPrefsManager.getBoolean(bhupendra.ai.launcher.managers.xml.options.Ai.agentic_mode);
        boolean alwaysOnFallback = XMLPrefsManager.getBoolean(bhupendra.ai.launcher.managers.xml.options.Ai.always_on_fallback);

        for(int c = 0; c < cmds.length; c++) {
            mainPack.clear();
            mainPack.commandColor = colors[c];

            boolean matched = false;
            for (int i = 0; i < triggers.length - 1; i++) {
                CmdTrigger trigger = triggers[i];
                try {
                    if (trigger.trigger(mainPack, cmds[c], requestId)) {
                        matched = true;
                        break;
                    }
                } catch (Exception e) {
                    Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    matched = true;
                    break;
                }
            }

            if (matched) continue;

            if (allowAITrigger && agentic && aiTrigger != null) {
                if (aiTrigger.trigger(cmds[c])) {
                    continue;
                }
            }

            if (allowShellFallback) {
                try {
                    if (shellCommandTrigger.trigger(mainPack, cmds[c], requestId)) {
                        continue;
                    }
                } catch (Exception e) {
                    Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                }
            }

            if (allowAITrigger && !agentic && alwaysOnFallback && aiTrigger != null) {
                aiTrigger.trigger(cmds[c]);
            }

            return false;
        }

        return true;
    }

    public boolean performLaunch(MainPack mainPack, AppsManager.LaunchInfo i, String input) {
        Intent intent = mainPack.appsManager.getIntent(i);
        if (intent == null) {
            return false;
        }

        if(showAppHistory) {
            if(appFormat == null) {
                appFormat = XMLPrefsManager.get(Behavior.app_launch_format);
                outputColor = XMLPrefsManager.getColor(Theme.output_color);
            }

            String a = new String(appFormat);
            a = pa.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getClassName()));
            a = pp.matcher(a).replaceAll(Matcher.quoteReplacement(intent.getComponent().getPackageName()));
            a = pl.matcher(a).replaceAll(Matcher.quoteReplacement(i.publicLabel));
            a = Tuils.patternNewline.matcher(a).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            SpannableString text = new SpannableString(a);
            text.setSpan(new ForegroundColorSpan(outputColor), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            CharSequence s = TimeManager.instance.replace(text);

            Tuils.sendOutput(mainPack, s, TerminalManager.CATEGORY_OUTPUT);
        }

        mainPack.context.startActivity(intent);
        return true;
    }

    public interface CmdTrigger {
        boolean trigger(MainPack info, String input, String requestId) throws Exception;
    }

    private class AliasTrigger implements CmdTrigger {
        @Override
        public boolean trigger(MainPack info, String input, String requestId) {
            String alias[] = mainPack.aliasManager.getAlias(input, true);

            String aliasValue = alias[0];
            if (alias[0] == null) {
                return false;
            }

            String aliasName = alias[1];
            String residual = alias[2];

            aliasValue = mainPack.aliasManager.format(aliasValue, residual);

            onCommand(aliasValue, aliasName, false, requestId);

            return true;
        }
    }

    private class GroupTrigger implements CmdTrigger {
        @Override
        public boolean trigger(MainPack info, String input, String requestId) throws Exception {
            int index = input.indexOf(Tuils.SPACE);
            String name;

            if(index != -1) {
                name = input.substring(0,index);
                input = input.substring(index + 1);
            } else {
                name = input;
                input = null;
            }

            List<? extends Group> appGroups = info.appsManager.groups;
            if(appGroups != null) {
                for(Group g : appGroups) {
                    if(name.equals(g.name())) {
                        if(input == null) {
                            Tuils.sendOutput(mContext, AppsManager.AppUtils.printApps(AppsManager.AppUtils.labelList((List<AppsManager.LaunchInfo>) g.members(), false)));
                            return true;
                        } else {
                            return g.use(mainPack, input);
                        }
                    }
                }
            }

            return false;
        }
    }

    public class ShellCommandTrigger implements CmdTrigger {
        final int CD_CODE = 10;
        final int PWD_CODE = 11;

        final Shell.OnCommandResultListener result = new Shell.OnCommandResultListener() {
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if(commandCode == CD_CODE) {
                    if (bhupendra.ai.launcher.MainManager.interactive != null) {
                        bhupendra.ai.launcher.MainManager.interactive.addCommand("pwd", PWD_CODE, this);
                    }
                } else if(commandCode == PWD_CODE && output.size() == 1) {
                    File f = new File(output.get(0));
                    if(f.exists()) {
                        mainPack.currentDirectory = f;
                        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent("bhupendra.ai.launcher.update_hint"));
                    }
                }
            }
        };

        @Override
        public boolean trigger(final MainPack info, final String input, final String requestId) throws Exception {
            final String trimmed = input.trim();
            final String cmd = trimmed.split(" ")[0];

            if (bhupendra.ai.launcher.tuils.TermuxManager.isTermuxInstalled(mContext)) {
                String[] common = {"ping", "echo", "ls", "grep", "cat", "vi", "top", "ps", "ip", "pkg", "git", "python", "node", "vim", "sed", "awk", "find"};
                for (String c : common) {
                    if (cmd.equalsIgnoreCase(c)) {
                        String argsStr = input.length() > cmd.length() ? input.substring(cmd.length()).trim() : "";
                        String[] finalArgs = argsStr.isEmpty() ? new String[0] : argsStr.split(" ");
                        bhupendra.ai.launcher.tuils.TermuxManager.runCommand(mContext, cmd, finalArgs, null, false);
                        
                        if (mainPack.aiSubsystem == null || !mainPack.aiSubsystem.isInFlight()) {
                            logCommandFinished(requestId);
                        }
                        
                        return true;
                    }
                }
            } else {
                String[] common = {"ping", "echo", "ls", "grep", "cat", "vi", "top", "ps", "ip"};
                for (String c : common) {
                    if (cmd.equalsIgnoreCase(c)) {
                        Tuils.sendOutput(mContext, "Command not found. Install Termux for a full Linux environment: termux", TerminalManager.CATEGORY_OUTPUT);
                        return true;
                    }
                }
            }

            bhupendra.ai.launcher.tuils.LauncherExecutors.commandExecutor.execute(() -> {
                try {
                    Shell.Interactive interactive = bhupendra.ai.launcher.MainManager.interactive;
                    if (interactive == null) return;
                    
                    if(input.trim().equalsIgnoreCase("su")) {
                        if(Shell.SU.available()) LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent("bhupendra.ai.launcher.root"));
                        interactive.addCommand("su");

                    } else if(input.contains("cd ")) {
                        interactive.addCommand(input, CD_CODE, result);
                    } else interactive.addCommand(input);

                    if (mainPack.aiSubsystem == null || !mainPack.aiSubsystem.isInFlight()) {
                        logCommandFinished(requestId);
                    }
                } catch (Exception e) {
                    Tuils.log(e);
                    bhupendra.ai.launcher.managers.FileSystemManager.toFile(e);
                }
            });

            return true;
        }
    }

    private class AppTrigger implements CmdTrigger {
        @Override
        public boolean trigger(MainPack info, String input, String requestId) {
            AppsManager.LaunchInfo i = mainPack.appsManager.findLaunchInfoWithLabel(input, AppsManager.SHOWN_APPS);
            return i != null && performLaunch(info, i, input);
        }
    }

    private class TuiCommandTrigger implements CmdTrigger {
        @Override
        public boolean trigger(final MainPack info, final String input, final String requestId) throws Exception {
            final Command command = CommandTuils.parse(input, info);
            if(command == null) return false;

            mainPack.lastCommand = input;

            bhupendra.ai.launcher.tuils.LauncherExecutors.commandExecutor.execute(() -> {
                    try {
                        String output = command.exec(info);
                        if(output != null) {
                            Tuils.sendOutput(info, output, TerminalManager.CATEGORY_OUTPUT);
                        }

                        boolean isAI = command.getClass().getSimpleName().equals("ai") || input.trim().startsWith("ai ");
                        if (!isAI) {
                            logCommandFinished(requestId);
                        }
                    } catch (Exception e) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                        Tuils.log(e);
                    }
            });

            return true;
        }
    }

    public interface Group {
        List<? extends Object> members();
        boolean use(MainPack mainPack, String input);
        String name();
    }
}
