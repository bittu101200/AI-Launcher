package bhupendra.ai.launcher;

import bhupendra.ai.launcher.managers.DeviceStateManager;


import bhupendra.ai.launcher.managers.TextProcessor;


import bhupendra.ai.launcher.managers.FileSystemManager;


import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AppCompatActivity;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.Spanned;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.tuixt.TuixtActivity;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.RegexManager;
import bhupendra.ai.launcher.managers.PermissionManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.TimeManager;
import bhupendra.ai.launcher.managers.TuiLocationManager;
import bhupendra.ai.launcher.managers.notifications.NotificationManager;
import bhupendra.ai.launcher.managers.notifications.KeeperService;
import bhupendra.ai.launcher.managers.notifications.NotificationService;
import bhupendra.ai.launcher.managers.suggestions.SuggestionsManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Notifications;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.Assist;
import bhupendra.ai.launcher.tuils.system.CustomExceptionHandler;
import bhupendra.ai.launcher.ui.views.LongClickableSpan;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;
import bhupendra.ai.launcher.tuils.PublicIOReceiver;
import bhupendra.ai.launcher.tuils.SimpleMutableEntry;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.Inputable;
import bhupendra.ai.launcher.tuils.interfaces.Outputable;
import bhupendra.ai.launcher.terminal.TerminalEventBus;
import bhupendra.ai.launcher.ai.AIProvider;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AndroidToolExecutor;
import bhupendra.ai.launcher.managers.xml.options.Ai;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class LauncherActivity extends AppCompatActivity implements Reloadable {
    private static CharSequence pendingReloadMessage;

    public static final int COMMAND_REQUEST_PERMISSION = 10;
    public static final int STARTING_PERMISSION = 11;
    public static final int COMMAND_SUGGESTION_REQUEST_PERMISSION = 12;
    public static final int LOCATION_REQUEST_PERMISSION = 13;

    public static final int TUIXT_REQUEST = 10;

    private UIManager ui;
    @Inject
    MainManager main;

    private PrivateIOReceiver privateIOReceiver;
    private PublicIOReceiver publicIOReceiver;
    private TerminalEventBus.Listener terminalEventListener;

    private boolean openKeyboardOnStart, canApplyTheme, backButtonEnabled;
    private boolean initialized = false;

    private Set<ReloadMessageCategory> categories = new HashSet<>();
    private Runnable stopActivity = () -> {
            CharSequence reloadMessage = Tuils.EMPTYSTRING;
            for (ReloadMessageCategory c : categories) {
                reloadMessage = TextUtils.concat(reloadMessage, Tuils.NEWLINE, c.text());
            }
            pendingReloadMessage = reloadMessage;
            overridePendingTransition(0, 0);
            recreate();
            overridePendingTransition(0, 0);
    };

    private Inputable in = new Inputable() {

        @Override
        public void in(String s) {
            if(ui != null) ui.setInput(s);
        }

        @Override
        public void changeHint(final String s) {
            runOnUiThread(() -> ui.setHint(s));
        }

        @Override
        public void resetHint() {
            runOnUiThread(() -> ui.resetHint());
        }
    };

    private Outputable out = new Outputable() {

        private final int DELAY = 500;

        Queue<SimpleMutableEntry<CharSequence,Integer>> textColor = new LinkedList<>();
        Queue<SimpleMutableEntry<CharSequence,Integer>> textCategory = new LinkedList<>();

        boolean charged = false;
        Handler handler = new Handler();

        Runnable r = new Runnable() {
            @Override
            public void run() {
                if(ui == null) {
                    if(handler != null) handler.postDelayed(this, DELAY);
                    return;
                }

                SimpleMutableEntry<CharSequence,Integer> sm;
                while (textCategory != null && (sm = textCategory.poll()) != null) {
                    ui.setOutput(sm.getKey(), sm.getValue());
                }

                while (textColor != null && (sm = textColor.poll()) != null) {
                    ui.setOutput(sm.getValue(), sm.getKey());
                }

                textCategory = null;
                textColor = null;
                handler = null;
                r = null;
            }
        };

        @Override
        public void onOutput(CharSequence output) {
            if (shouldSuppressOutput(output)) return;
            if(ui != null) ui.setOutput(output, TerminalManager.CATEGORY_OUTPUT);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, TerminalManager.CATEGORY_OUTPUT));

                if(!charged) {
                    charged = true;
                    if(handler != null) handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void onOutput(CharSequence output, int category) {
            if (shouldSuppressOutput(output)) return;
            if(ui != null) ui.setOutput(output, category);
            else {
                textCategory.add(new SimpleMutableEntry<>(output, category));

                if(!charged) {
                    charged = true;
                    if(handler != null) handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void onOutput(int color, CharSequence output) {
            if (shouldSuppressOutput(output)) return;
            if(ui != null) ui.setOutput(color, output);
            else {
                textColor.add(new SimpleMutableEntry<>(output, color));

                if(!charged) {
                    charged = true;
                    if(handler != null) handler.postDelayed(r, DELAY);
                }
            }
        }

        @Override
        public void dispose() {
            if(handler != null) handler.removeCallbacksAndMessages(null);
        }

        private boolean shouldSuppressOutput(CharSequence output) {
            if (output == null) return true;
            String text = output.toString().trim();
            return text.length() == 0 || "null".equalsIgnoreCase(text);
        }
    };

    private void dispatchTerminalEvent(TerminalEventBus.TerminalEvent event) {
        if (event instanceof TerminalEventBus.InputEvent) {
            in.in(((TerminalEventBus.InputEvent) event).text);
        } else if (event instanceof TerminalEventBus.OutputEvent) {
            TerminalEventBus.OutputEvent output = (TerminalEventBus.OutputEvent) event;
            CharSequence text = output.text;
            if (output.action != null || output.longAction != null) {
                SpannableStringBuilder builder = new SpannableStringBuilder(text);
                builder.setSpan(new LongClickableSpan(output.action, output.longAction), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                text = builder;
            }
            if (output.color != Integer.MAX_VALUE) out.onOutput(output.color, text);
            else out.onOutput(text, output.type);
        } else if (event instanceof TerminalEventBus.StartStreamEvent) {
            runOnUiThread(() -> {
                if (ui != null) ui.startStreaming();
            });
        } else if (event instanceof TerminalEventBus.UpdateStreamEvent) {
            TerminalEventBus.UpdateStreamEvent update = (TerminalEventBus.UpdateStreamEvent) event;
            runOnUiThread(() -> {
                if (ui != null) ui.updateStream(update.thinkingText, update.text, update.category);
            });
        } else if (event instanceof TerminalEventBus.FinishStreamEvent) {
            TerminalEventBus.FinishStreamEvent finish = (TerminalEventBus.FinishStreamEvent) event;
            runOnUiThread(() -> {
                if (ui != null) ui.finishStreaming(finish.thinkingText, finish.finalText);
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(0,0);

        if (isFinishing()) {
            return;
        }

        canApplyTheme = true;
        finishOnCreate();
    }

    private void finishOnCreate() {
        if (initialized) return;
        initialized = true;

        Thread.currentThread().setUncaughtExceptionHandler(new CustomExceptionHandler());

        // CRITICAL: Initialize Managers before anything else
        XMLPrefsManager.loadCommons(this);
        new RegexManager(LauncherActivity.this);
        new TimeManager(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateIOReceiver.ACTION_INPUT);
        filter.addAction(PrivateIOReceiver.ACTION_OUTPUT);
        filter.addAction(PrivateIOReceiver.ACTION_REPLY);

        privateIOReceiver = new PrivateIOReceiver(this, out, in);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(privateIOReceiver, filter);
        terminalEventListener = this::dispatchTerminalEvent;
        TerminalEventBus.get().register(terminalEventListener);

        IntentFilter filter1 = new IntentFilter();
        filter1.addAction(PublicIOReceiver.ACTION_CMD);
        filter1.addAction(PublicIOReceiver.ACTION_OUTPUT);

        publicIOReceiver = new PublicIOReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getApplicationContext().registerReceiver(publicIOReceiver, filter1, "bhupendra.ai.launcher.permission.RECEIVE_CMD", null, Context.RECEIVER_EXPORTED);
        } else {
            getApplicationContext().registerReceiver(publicIOReceiver, filter1, "bhupendra.ai.launcher.permission.RECEIVE_CMD", null);
        }

        int requestedOrientation = XMLPrefsManager.getInt(Behavior.orientation);
        if(requestedOrientation >= 0 && requestedOrientation != 2) {
            int orientation = getResources().getConfiguration().orientation;
            if(orientation != requestedOrientation) setRequestedOrientation(requestedOrientation);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
        }

        if(!XMLPrefsManager.getBoolean(Ui.ignore_bar_color)) {
            Window window = getWindow();

            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.setStatusBarColor(XMLPrefsManager.getColor(Theme.statusbar_color));
            window.setNavigationBarColor(XMLPrefsManager.getColor(Theme.navigationbar_color));
        }

        backButtonEnabled = XMLPrefsManager.getBoolean(Behavior.back_button_enabled);

        boolean fullscreen = XMLPrefsManager.getBoolean(Ui.fullscreen);
        if(fullscreen) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        boolean useSystemWP = XMLPrefsManager.getBoolean(Ui.system_wallpaper);
        if (useSystemWP) {
            setTheme(R.style.Custom_SystemWP);
        } else {
            setTheme(R.style.Custom_Solid);
        }

        try {
            NotificationManager.create(this);
        } catch (Exception e) {
            FileSystemManager.toFile(e);
        }

        boolean tuiNotification = XMLPrefsManager.getBoolean(Behavior.tui_notification);
        Intent keeperIntent = new Intent(this, KeeperService.class);
        if (tuiNotification) {
            keeperIntent.putExtra(KeeperService.PATH_KEY, XMLPrefsManager.get(Behavior.home_path));
            startForegroundService(keeperIntent);
        } else {
            stopService(keeperIntent);
        }

        boolean notifications = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");
        if(notifications) {
            try {
                ComponentName notificationComponent = new ComponentName(this, NotificationService.class);
                PackageManager pm = getPackageManager();
                pm.setComponentEnabledSetting(notificationComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

                Intent notificationIntent = new Intent(this, NotificationService.class);
                startService(notificationIntent);
            } catch (NoClassDefFoundError er) {
                Intent intent = new Intent(PrivateIOReceiver.ACTION_OUTPUT);
                intent.putExtra(PrivateIOReceiver.TEXT, getString(R.string.output_notification_error) + Tuils.SPACE + er.toString());
            }
        }

        LongClickableSpan.longPressVibrateDuration = XMLPrefsManager.getInt(Behavior.long_click_vibration_duration);

        openKeyboardOnStart = XMLPrefsManager.getBoolean(Behavior.auto_show_keyboard);
        if (!openKeyboardOnStart) {
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }

        setContentView(R.layout.base_view);

        if(XMLPrefsManager.getBoolean(Ui.show_restart_message)) {
            CharSequence s = pendingReloadMessage;
            if (s == null) {
                s = getIntent().getCharSequenceExtra(Reloadable.MESSAGE);
            }
            pendingReloadMessage = null;
            if(s != null) out.onOutput(TextProcessor.span(s, XMLPrefsManager.getColor(Theme.restart_message_color)));
        }

        // main is injected via Hilt

        try {
            String providerName = XMLPrefsManager.get(Ai.provider);
            AISubsystem aiSubsystem = buildAISubsystem(providerName);
            main.setAISubsystem(aiSubsystem);
            aiSubsystem.setInstance();
        } catch (Exception e) {
            Tuils.log(e);
        }

        ViewGroup mainView = (ViewGroup) findViewById(R.id.mainview);

        if(!XMLPrefsManager.getBoolean(Ui.ignore_bar_color) && !XMLPrefsManager.getBoolean(Ui.statusbar_light_icons)) {
            mainView.setSystemUiVisibility(mainView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        ui = new UIManager(this, mainView, main.getMainPack(), canApplyTheme, main.executer());
        
        if (main.getMainPack().aiSubsystem != null) {
            main.getMainPack().aiSubsystem.addListener(running -> {
                runOnUiThread(() -> ui.setAIState(running));
            });
        }

        main.setRedirectionListener(ui.buildRedirectionListener());
        ui.pack = main.getMainPack();

        bhupendra.ai.launcher.ai.platform.AIOnboardingManager.checkAndStart(main.getMainPack());

        in.in(Tuils.EMPTYSTRING);
        ui.focusTerminal();

        if(fullscreen) Assist.assistActivity(this);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (ui != null) ui.onStart(openKeyboardOnStart);
        PermissionManager.resumePendingSpecialFlow(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ui != null) ui.resume();
        if(main != null) main.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(ui != null) ui.pause();
        if(main != null) main.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(privateIOReceiver);
        } catch (Exception e) {}
        try {
            getApplicationContext().unregisterReceiver(publicIOReceiver);
        } catch (Exception e) {}
        if (terminalEventListener != null) {
            TerminalEventBus.get().unregister(terminalEventListener);
            terminalEventListener = null;
        }

        if(main != null) main.destroy();
        if(ui != null) ui.dispose();
        XMLPrefsManager.dispose();
    }

    @Override
    public void onBackPressed() {
        if(backButtonEnabled) {
            if(ui != null) ui.onBackPressed();
        }
    }

    @Override
    public void addMessage(String header, String message) {
        ReloadMessageCategory category = null;
        for (ReloadMessageCategory c : categories) {
            if (c.header.equals(header)) {
                category = c;
                break;
            }
        }

        if (category == null) {
            category = new ReloadMessageCategory(header);
            categories.add(category);
        }

        category.lines.add(message);
    }

    @Override
    public void reload() {
        runOnUiThread(stopActivity);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length > 0 && Manifest.permission.READ_CONTACTS.equals(permissions[0]) &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(ContactManager.ACTION_REFRESH));
        }

        try {
            switch (requestCode) {
                case COMMAND_REQUEST_PERMISSION:
                    boolean allGranted = grantResults.length > 0;
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        MainPack info = main.getMainPack();
                        main.onCommand(info.lastCommand, (String) null, false);
                    } else {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                        main.sendPermissionNotGrantedWarning();
                    }
                    break;
                case STARTING_PERMISSION:
                    canApplyTheme = true;
                    finishOnCreate();
                    break;
                case COMMAND_SUGGESTION_REQUEST_PERMISSION:
                    if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                        ui.setOutput(getString(R.string.output_nopermissions), TerminalManager.CATEGORY_OUTPUT);
                    }
                    break;
                case LOCATION_REQUEST_PERMISSION:
                    Intent i = new Intent(TuiLocationManager.ACTION_GOT_PERMISSION);
                    i.putExtra(XMLPrefsManager.VALUE_ATTRIBUTE, grantResults.length > 0 ? grantResults[0] : PackageManager.PERMISSION_DENIED);
                    LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                    break;
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    private AISubsystem buildAISubsystem(String providerId) {
        AIProvider provider = AISubsystem.buildProvider(providerId);
        AndroidToolExecutor executor = new AndroidToolExecutor();
        AISubsystem ai = new AISubsystem(provider, this, executor);
        if (main != null) executor.setMainPack(main.getMainPack());
        return ai;
    }

    private static class ReloadMessageCategory {
        String header;
        List<String> lines;

        public ReloadMessageCategory(String header) {
            this.header = header;
            lines = new ArrayList<>();
        }

        public CharSequence text() {
            CharSequence sequence = TextUtils.concat(header, Tuils.NEWLINE);

            StringBuilder builder = new StringBuilder();
            final String dash = "-";
            for(int c = 0; c < lines.size(); c++) builder.append(Tuils.SPACE).append(dash).append(Tuils.SPACE).append(lines.get(c)).append(Tuils.NEWLINE);

            return TextUtils.concat(sequence, builder.toString());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof ReloadMessageCategory && ((ReloadMessageCategory) obj).header.equals(header);
        }

        @Override
        public int hashCode() {
            return header.hashCode();
        }
    }
}
