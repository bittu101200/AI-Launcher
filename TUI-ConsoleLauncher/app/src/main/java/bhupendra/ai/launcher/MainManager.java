package bhupendra.ai.launcher;

import bhupendra.ai.launcher.managers.TextProcessor;


import bhupendra.ai.launcher.managers.FileSystemManager;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Parcelable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.ai.AITrigger;
import bhupendra.ai.launcher.commands.Command;
import bhupendra.ai.launcher.commands.CommandGroup;
import bhupendra.ai.launcher.commands.CommandTuils;
import bhupendra.ai.launcher.commands.CommandExecutionController;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.raw.location;
import bhupendra.ai.launcher.commands.main.specific.RedirectCommand;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ChangelogManager;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.HTMLExtractManager;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.ThemeManager;
import bhupendra.ai.launcher.managers.TimeManager;
import bhupendra.ai.launcher.managers.TuiLocationManager;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.managers.music.MusicService;
import bhupendra.ai.launcher.managers.notifications.KeeperService;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.tuils.BeepPlayer;
import bhupendra.ai.launcher.tuils.TermuxManager;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;

import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.CommandExecuter;
import bhupendra.ai.launcher.tuils.interfaces.OnRedirectionListener;
import bhupendra.ai.launcher.tuils.interfaces.Redirectator;
import bhupendra.ai.launcher.tuils.libsuperuser.Shell;
import bhupendra.ai.launcher.tuils.libsuperuser.ShellHolder;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

public class MainManager {

    public static String ACTION_EXEC = BuildConfig.APPLICATION_ID + ".main_exec";
    public static String CMD = "cmd", NEED_WRITE_INPUT = "writeInput", ALIAS_NAME = "aliasName", PARCELABLE = "parcelable", CMD_COUNT = "cmdCount", MUSIC_SERVICE = "musicService", REQUEST_ID = "requestId";

    private RedirectCommand redirect;
    private Redirectator redirectator = new Redirectator() {
        @Override
        public void prepareRedirection(RedirectCommand cmd) {
            redirect = cmd;

            if(redirectionListener != null) {
                redirectionListener.onRedirectionRequest(cmd);
            }
        }

        @Override
        public void cleanup() {
            if(redirect != null) {
                redirect.beforeObjects.clear();
                redirect.afterObjects.clear();

                if(redirectionListener != null) {
                    redirectionListener.onRedirectionEnd(redirect);
                }

                redirect = null;
            }
        }
    };
    private OnRedirectionListener redirectionListener;
    public void setRedirectionListener(OnRedirectionListener redirectionListener) {
        this.redirectionListener = redirectionListener;
    }

    private final String COMMANDS_PKG = "bhupendra.ai.launcher.commands.main.raw";

    private CommandExecutionController commandController;
    private MainPack mainPack;

    private LauncherActivity mContext;

    public static Shell.Interactive interactive;

    private AliasManager aliasManager;
    private RssManager rssManager;
    private AppsManager appsManager;
    private ContactManager contactManager;
    private MusicManager2 musicManager2;
    private ThemeManager themeManager;
    private HTMLExtractManager htmlExtractManager;

    private BroadcastReceiver receiver;
    private String activeRequestId;
    private boolean platformReceiverRegistered;

    public static int commandCount = 0;

    protected MainManager(LauncherActivity c) {
        mContext = c;

        CommandGroup group = new CommandGroup(mContext, COMMANDS_PKG);

        try {
            contactManager = new ContactManager(mContext);
        } catch (NullPointerException e) {
            Tuils.log(e);
        }

        appsManager = new AppsManager(c);
        aliasManager = new AliasManager(mContext);

        final OkHttpClient client = new OkHttpClient.Builder()
                .cache(new Cache(mContext.getCacheDir(), 10*1024*1024))
                .build();

        rssManager = new RssManager(mContext, client);
        themeManager = new ThemeManager(client, mContext, c);
        musicManager2 = XMLPrefsManager.getBoolean(Behavior.enable_music) ? new MusicManager2(mContext) : null;
        htmlExtractManager = new HTMLExtractManager(mContext, client);

        mainPack = new MainPack(mContext, group, aliasManager, appsManager, musicManager2, contactManager, redirectator, rssManager, client);
        commandController = new CommandExecutionController(mContext, mainPack);

        if (mainPack.aiSubsystem != null) {
            mainPack.aiSubsystem.setMainPack(mainPack);
            AITrigger aiTrigger = new AITrigger(
                mainPack.aiSubsystem,
                mContext,
                rawInput -> {
                    try {
                        commandController.getShellCommandTrigger().trigger(mainPack, rawInput, null);
                    } catch (Exception e) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    }
                }
            );
            commandController.setAITrigger(aiTrigger);
        }

        ShellHolder shellHolder = new ShellHolder(mContext);
        interactive = shellHolder.build();
        mainPack.shellHolder = shellHolder;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_EXEC);
        filter.addAction(location.ACTION_LOCATION_CMD_GOT);
        filter.addAction(AliasManager.ACTION_RELOAD);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(AliasManager.ACTION_RELOAD)) {
                    if (aliasManager != null) aliasManager.reload();
                    return;
                }
                if (action.equals(ACTION_EXEC)) {
                    String cmd = intent.getStringExtra(CMD);
                    if (cmd == null) cmd = intent.getStringExtra(PrivateIOReceiver.TEXT);

                    if (cmd == null) {
                        return;
                    }

                    int cmdCount = intent.getIntExtra(CMD_COUNT, -1);
                    if (cmdCount < commandCount && cmdCount != -1) return;
                    commandCount++;

                    String previousRequestId = activeRequestId;
                    activeRequestId = intent.getStringExtra(REQUEST_ID);
                    try {
                        String aliasName = intent.getStringExtra(ALIAS_NAME);
                        boolean needWriteInput = intent.getBooleanExtra(NEED_WRITE_INPUT, false);
                        Parcelable p = intent.getParcelableExtra(PARCELABLE);

                        if(needWriteInput) {
                            Tuils.sendInput(context.getApplicationContext(), cmd);
                        }

                        if(p != null && p instanceof AppsManager.LaunchInfo) {
                            onCommand(cmd, (AppsManager.LaunchInfo) p, intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false));
                        } else {
                            onCommand(cmd, aliasName, intent.getBooleanExtra(MainManager.MUSIC_SERVICE, false));
                        }
                    } finally {
                        activeRequestId = previousRequestId;
                    }
                } else if(action.equals(location.ACTION_LOCATION_CMD_GOT)) {
                    if (intent.getBooleanExtra(TuiLocationManager.FAIL, false)) {
                        Tuils.sendOutput(context, context.getString(R.string.location_error));
                    } else {
                        Tuils.sendOutput(context, "Lat: " + intent.getDoubleExtra(TuiLocationManager.LATITUDE, 0) + "; Long: " + intent.getDoubleExtra(TuiLocationManager.LONGITUDE, 0));
                    }
                    TuiLocationManager.instance(context).rm(location.ACTION_LOCATION_CMD_GOT);
                }
            }
        };

        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).registerReceiver(receiver, filter);
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            mContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            mContext.registerReceiver(receiver, filter);
        }
        platformReceiverRegistered = true;
    }

    public void onCommand(String input, AppsManager.LaunchInfo launchInfo, boolean wasMusicService) {
        commandController.onCommand(input, launchInfo, wasMusicService, activeRequestId);
    }

    public void onCommand(String input, String alias, boolean wasMusicService) {
        if(redirect != null) {
            if(!redirect.isWaitingPermission()) {
                redirect.afterObjects.add(input);
            }
            String output = redirect.onRedirect(mainPack);
            Tuils.sendOutput(mContext, output);

            return;
        }

        commandController.onCommand(input, alias, wasMusicService, activeRequestId);
    }

    public void onLongBack() {
        Tuils.sendInput(mContext, Tuils.EMPTYSTRING);
    }

    public void sendPermissionNotGrantedWarning() {
        redirectator.cleanup();
    }

    public void dispose() {
        mainPack.dispose();
    }

    public void destroy() {
        mainPack.destroy();
        TuiLocationManager.disposeStatic();

        themeManager.dispose();
        htmlExtractManager.dispose(mContext);
        aliasManager.dispose();
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).unregisterReceiver(receiver);
        if (platformReceiverRegistered) {
            try {
                mContext.unregisterReceiver(receiver);
            } catch (Exception e) {
                Tuils.log(e);
            }
            platformReceiverRegistered = false;
        }

        bhupendra.ai.launcher.tuils.LauncherExecutors.bgExecutor.execute(() -> {
                try {
                    interactive.kill();
                    interactive.close();
                } catch (Exception e) {
                    Tuils.log(e);
                    FileSystemManager.toFile(e);
                }
        });
    }

    public void setAISubsystem(bhupendra.ai.launcher.ai.AISubsystem ai) {
        if (ai != null) {
            mainPack.aiSubsystem = ai;
            ai.setMainPack(mainPack);
            AITrigger aiTrigger = new AITrigger(
                ai,
                mContext,
                rawInput -> {
                    try {
                        commandController.getShellCommandTrigger().trigger(mainPack, rawInput, null);
                    } catch (Exception e) {
                        Tuils.sendOutput(mContext, Tuils.getStackTrace(e));
                    }
                }
            );
            commandController.setAITrigger(aiTrigger);
        }
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    public CommandExecutionController getCommandController() {
        return commandController;
    }

    public CommandExecuter executer() {
        return (input, obj) -> {
            AppsManager.LaunchInfo li = obj instanceof AppsManager.LaunchInfo ? (AppsManager.LaunchInfo) obj : null;

            onCommand(input, li, false);
        };
    }
}
