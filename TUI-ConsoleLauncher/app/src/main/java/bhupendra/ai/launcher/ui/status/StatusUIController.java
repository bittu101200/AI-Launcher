package bhupendra.ai.launcher.ui.status;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.UIManager;
import bhupendra.ai.launcher.LabelUpdater;
import bhupendra.ai.launcher.managers.DeviceStateManager;
import bhupendra.ai.launcher.managers.NotesManager;
import bhupendra.ai.launcher.managers.TextProcessor;
import bhupendra.ai.launcher.managers.TimeManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.NetworkUtils;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.OnBatteryUpdate;

public class StatusUIController {

    private final Context mContext;
    private final Handler handler;
    private final SharedPreferences preferences;
    private final LabelUpdater labelUpdater;
    private final TextView notesView;
    private final Runnable onLockAction;

    private final int RAM_DELAY = 10000;
    private final int TIME_DELAY = 1000;
    private final int STORAGE_DELAY = 60 * 1000;

    // RAM properties
    private ActivityManager.MemoryInfo memory;
    private ActivityManager activityManager;
    private RamRunnable ramRunnable;

    // Storage properties
    private StorageRunnable storageRunnable;

    // Time properties
    private TimeRunnable timeRunnable;

    // Network properties
    private NetworkRunnable networkRunnable;

    // Notes properties
    private int notesMaxLines;
    private NotesManager notesManager;

    // Battery properties
    private BatteryUpdate batteryUpdate;
    private int mediumPercentage, lowPercentage;
    private String batteryFormat;

    // Unlock properties
    private int unlockColor, unlockTimeOrder;
    private int unlockTimes, unlockHour, unlockMinute, cycleDuration = 1000 * 60 * 60 * 24;
    private long lastUnlockTime = -1, nextUnlockCycleRestart;
    private String unlockFormat, notAvailableText, unlockTimeDivider;
    private long[] lastUnlocks;
    private BroadcastReceiver lockReceiver = null;
    private final int UP_DOWN = 1;
    public static final String UNLOCK_KEY = "unlockTimes", NEXT_UNLOCK_CYCLE_RESTART = "nextUnlockRestart";
    private final int UNLOCK_RUNNABLE_DELAY = 1000 * 60 * 60; // cycleDuration / 24

    private final Pattern timePattern = Pattern.compile("(%t\\d*)(?:\\(([^\\)]*)\\))?(\\d+)?");
    private final Pattern unlockCount = Pattern.compile("%c", Pattern.CASE_INSENSITIVE);
    private final Pattern advancement = Pattern.compile("%a(\\d+)(.)");
    private final Pattern indexPattern = Pattern.compile("%i", Pattern.CASE_INSENSITIVE);
    private final String whenPattern = "%w";

    public StatusUIController(Context context, Handler handler, SharedPreferences preferences, 
                              LabelUpdater labelUpdater, TextView notesView, Runnable onLockAction) {
        this.mContext = context;
        this.handler = handler;
        this.preferences = preferences;
        this.labelUpdater = labelUpdater;
        this.notesView = notesView;
        this.onLockAction = onLockAction;
    }

    public void init(boolean[] show) {
        // Ram Setup
        if (show[UIManager.Label.ram.ordinal()]) {
            ramRunnable = new RamRunnable();
            memory = new ActivityManager.MemoryInfo();
            activityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            handler.post(ramRunnable);
        }

        // Storage Setup
        if (show[UIManager.Label.storage.ordinal()]) {
            storageRunnable = new StorageRunnable();
            handler.post(storageRunnable);
        }

        // Device Setup
        if (show[UIManager.Label.device.ordinal()]) {
            applyDeviceVisibility(true);
        }

        // Time Setup
        if (show[UIManager.Label.time.ordinal()]) {
            timeRunnable = new TimeRunnable();
            handler.post(timeRunnable);
        }

        // Battery Setup
        if (show[UIManager.Label.battery.ordinal()]) {
            batteryUpdate = new BatteryUpdate();
            mediumPercentage = XMLPrefsManager.getInt(Behavior.battery_medium);
            lowPercentage = XMLPrefsManager.getInt(Behavior.battery_low);
            DeviceStateManager.registerBatteryReceiver(mContext, batteryUpdate);
        } else {
            batteryUpdate = null;
        }

        // Network Setup
        if (show[UIManager.Label.network.ordinal()]) {
            networkRunnable = new NetworkRunnable();
            handler.post(networkRunnable);
        }

        // Notes Setup
        notesManager = new NotesManager(mContext, notesView);
        notesManager.setOnNotesChangedListener(() -> {
            if (notesView != null && handler != null) {
                handler.post(() -> {
                    labelUpdater.updateText(UIManager.Label.notes, TextProcessor.span(mContext, labelUpdater.getLabelSize(UIManager.Label.notes), notesManager.getNotes()));
                });
            }
        });
        if (show[UIManager.Label.notes.ordinal()]) {
            if (notesView != null) {
                notesView.setMovementMethod(new LinkMovementMethod());
                notesMaxLines = XMLPrefsManager.getInt(Ui.notes_max_lines);
                if (notesMaxLines > 0) {
                    notesView.setMaxLines(notesMaxLines);
                    notesView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                }
            }
            labelUpdater.updateText(UIManager.Label.notes, TextProcessor.span(mContext, labelUpdater.getLabelSize(UIManager.Label.notes), notesManager.getNotes()));
        }

        // Unlock Counter Setup
        if (show[UIManager.Label.unlock.ordinal()]) {
            unlockTimes = preferences.getInt(UNLOCK_KEY, 0);
            unlockColor = XMLPrefsManager.getColor(Theme.unlock_counter_color);
            unlockFormat = XMLPrefsManager.get(Behavior.unlock_counter_format);
            notAvailableText = XMLPrefsManager.get(Behavior.not_available_text);
            unlockTimeDivider = XMLPrefsManager.get(Behavior.unlock_time_divider);
            unlockTimeDivider = Tuils.patternNewline.matcher(unlockTimeDivider).replaceAll(Tuils.NEWLINE);

            String start = XMLPrefsManager.get(Behavior.unlock_counter_cycle_start);
            Pattern p = Pattern.compile("(\\d{1,2}).(\\d{1,2})");
            Matcher m = p.matcher(start);
            if (!m.find()) {
                m = p.matcher(Behavior.unlock_counter_cycle_start.defaultValue());
                m.find();
            }

            unlockHour = Integer.parseInt(m.group(1));
            unlockMinute = Integer.parseInt(m.group(2));
            unlockTimeOrder = XMLPrefsManager.getInt(Behavior.unlock_time_order);
            nextUnlockCycleRestart = preferences.getLong(NEXT_UNLOCK_CYCLE_RESTART, 0);

            m = timePattern.matcher(unlockFormat);
            if (m.find()) {
                String s = m.group(3);
                if (s == null || s.length() == 0) s = "1";
                lastUnlocks = new long[Integer.parseInt(s)];
                for (int c = 0; c < lastUnlocks.length; c++) {
                    lastUnlocks[c] = -1;
                }
                registerLockReceiver();
                handler.post(unlockTimeRunnable);
            } else {
                lastUnlocks = null;
            }
        }
    }

    public void pause() {
        if (handler != null) {
            if (timeRunnable != null) handler.removeCallbacks(timeRunnable);
            if (ramRunnable != null) handler.removeCallbacks(ramRunnable);
            if (storageRunnable != null) handler.removeCallbacks(storageRunnable);
            if (networkRunnable != null) handler.removeCallbacks(networkRunnable);
            if (unlockTimeRunnable != null) handler.removeCallbacks(unlockTimeRunnable);
        }
    }

    public void resume() {
        if (handler != null) {
            if (timeRunnable != null && XMLPrefsManager.getBoolean(Ui.show_time)) {
                handler.removeCallbacks(timeRunnable);
                handler.post(timeRunnable);
            }
            if (ramRunnable != null && XMLPrefsManager.getBoolean(Ui.show_ram)) {
                handler.removeCallbacks(ramRunnable);
                handler.post(ramRunnable);
            }
            if (storageRunnable != null && XMLPrefsManager.getBoolean(Ui.show_storage_info)) {
                handler.removeCallbacks(storageRunnable);
                handler.post(storageRunnable);
            }
            if (networkRunnable != null && XMLPrefsManager.getBoolean(Ui.show_network_info)) {
                handler.removeCallbacks(networkRunnable);
                handler.post(networkRunnable);
            }
            if (XMLPrefsManager.getBoolean(Ui.show_notes)) {
                labelUpdater.updateText(UIManager.Label.notes, TextProcessor.span(mContext, labelUpdater.getLabelSize(UIManager.Label.notes), notesManager.getNotes()));
            }
            if (unlockTimeRunnable != null && XMLPrefsManager.getBoolean(Ui.show_unlock_counter) && lastUnlocks != null) {
                handler.removeCallbacks(unlockTimeRunnable);
                handler.post(unlockTimeRunnable);
            }
        }
    }

    public void dispose() {
        pause();
        if (notesManager != null) {
            notesManager.dispose(mContext);
        }
        DeviceStateManager.unregisterBatteryReceiver(mContext);
        unregisterLockReceiver();
    }

    public boolean applyNotesVisibility(boolean visible) {
        if (notesView == null || notesManager == null || handler == null) return false;

        if (!visible) {
            labelUpdater.updateText(UIManager.Label.notes, Tuils.EMPTYSTRING);
            return true;
        }

        notesView.setMovementMethod(new LinkMovementMethod());
        notesMaxLines = XMLPrefsManager.getInt(Ui.notes_max_lines);
        if (notesMaxLines > 0) {
            notesView.setMaxLines(notesMaxLines);
            notesView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        } else {
            notesView.setMaxLines(Integer.MAX_VALUE);
            notesView.setEllipsize(null);
        }
        labelUpdater.updateText(UIManager.Label.notes, TextProcessor.span(mContext, labelUpdater.getLabelSize(UIManager.Label.notes), notesManager.getNotes()));
        return true;
    }

    public boolean applySimpleLabelVisibility(UIManager.Label label, boolean visible) {
        if (handler == null) return false;

        if (!visible) {
            Runnable runnable = getRunnableForLabel(label);
            if (runnable != null) handler.removeCallbacks(runnable);
            labelUpdater.updateText(label, Tuils.EMPTYSTRING);
            return true;
        }

        switch (label) {
            case ram:
                if (ramRunnable == null) ramRunnable = new RamRunnable();
                handler.removeCallbacks(ramRunnable);
                handler.post(ramRunnable);
                return true;
            case time:
                if (timeRunnable == null) timeRunnable = new TimeRunnable();
                handler.removeCallbacks(timeRunnable);
                handler.post(timeRunnable);
                return true;
            case storage:
                if (storageRunnable == null) storageRunnable = new StorageRunnable();
                handler.removeCallbacks(storageRunnable);
                handler.post(storageRunnable);
                return true;
            case network:
                if (networkRunnable == null) networkRunnable = new NetworkRunnable();
                handler.removeCallbacks(networkRunnable);
                handler.post(networkRunnable);
                return true;
            case unlock:
                handler.removeCallbacks(unlockTimeRunnable);
                handler.post(unlockTimeRunnable);
                return true;
            default:
                return false;
        }
    }

    private Runnable getRunnableForLabel(UIManager.Label label) {
        switch (label) {
            case ram: return ramRunnable;
            case time: return timeRunnable;
            case storage: return storageRunnable;
            case network: return networkRunnable;
            case unlock: return unlockTimeRunnable;
            default: return null;
        }
    }

    public boolean applyDeviceVisibility(boolean visible) {
        if (!visible) {
            labelUpdater.updateText(UIManager.Label.device, Tuils.EMPTYSTRING);
            return true;
        }

        Pattern USERNAME = Pattern.compile("%u", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        Pattern DV = Pattern.compile("%d", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        String deviceFormat = XMLPrefsManager.get(Behavior.device_format);
        String username = XMLPrefsManager.get(Ui.username);
        String deviceName = XMLPrefsManager.get(Ui.deviceName);
        if (deviceName == null || deviceName.length() == 0) {
            deviceName = Build.DEVICE;
        }
        deviceFormat = USERNAME.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(username != null ? username : "null"));
        deviceFormat = DV.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(deviceName));
        deviceFormat = Tuils.patternNewline.matcher(deviceFormat).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));
        labelUpdater.updateText(UIManager.Label.device, TextProcessor.span(mContext, deviceFormat, XMLPrefsManager.getColor(Theme.device_color), labelUpdater.getLabelSize(UIManager.Label.device)));
        return true;
    }

    public boolean applyBatteryVisibility(boolean visible) {
        if (!visible) {
            labelUpdater.updateText(UIManager.Label.battery, Tuils.EMPTYSTRING);
            DeviceStateManager.unregisterBatteryReceiver(mContext);
            batteryUpdate = null;
            return true;
        }

        mediumPercentage = XMLPrefsManager.getInt(Behavior.battery_medium);
        lowPercentage = XMLPrefsManager.getInt(Behavior.battery_low);
        if (batteryUpdate == null) {
            batteryUpdate = new BatteryUpdate();
            DeviceStateManager.registerBatteryReceiver(mContext, batteryUpdate);
        } else {
            batteryUpdate.update(-1);
        }
        return true;
    }

    // Lock/Unlock functionality
    private void registerLockReceiver() {
        if (lockReceiver != null) return;

        final IntentFilter theFilter = new IntentFilter();
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);
        theFilter.addAction(Intent.ACTION_USER_PRESENT);

        lockReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();
                if (strAction == null) return;

                KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                if (strAction.equals(Intent.ACTION_USER_PRESENT) || strAction.equals(Intent.ACTION_SCREEN_OFF) || strAction.equals(Intent.ACTION_SCREEN_ON)) {
                    if (myKM != null && myKM.inKeyguardRestrictedInputMode()) {
                        onLock();
                    } else {
                        onUnlock();
                    }
                }
            }
        };

        mContext.getApplicationContext().registerReceiver(lockReceiver, theFilter);
    }

    private void unregisterLockReceiver() {
        if (lockReceiver != null) {
            mContext.getApplicationContext().unregisterReceiver(lockReceiver);
            lockReceiver = null;
        }
    }

    private void onLock() {
        if (onLockAction != null) {
            onLockAction.run();
        }
    }

    private void onUnlock() {
        if (System.currentTimeMillis() - lastUnlockTime < 1000 || lastUnlocks == null) return;
        lastUnlockTime = System.currentTimeMillis();

        unlockTimes++;

        System.arraycopy(lastUnlocks, 0, lastUnlocks, 1, lastUnlocks.length - 1);
        lastUnlocks[0] = lastUnlockTime;

        preferences.edit()
                .putInt(UNLOCK_KEY, unlockTimes)
                .apply();

        invalidateUnlockText();
    }

    private void invalidateUnlockText() {
        String cp = unlockFormat;
        if (cp == null) return;

        cp = unlockCount.matcher(cp).replaceAll(String.valueOf(unlockTimes));
        cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE);

        Matcher m = advancement.matcher(cp);
        if (m.find()) {
            int denominator = Integer.parseInt(m.group(1));
            String divider = m.group(2);

            long lastCycleStart = nextUnlockCycleRestart - cycleDuration;
            int elapsed = (int) (System.currentTimeMillis() - lastCycleStart);
            int numerator = denominator * elapsed / cycleDuration;

            cp = m.replaceAll(numerator + divider + denominator);
        }

        CharSequence s = TextProcessor.span(mContext, cp, unlockColor, labelUpdater.getLabelSize(UIManager.Label.unlock));

        Matcher timeMatcher = timePattern.matcher(cp);
        if (timeMatcher.find()) {
            String timeGroup = timeMatcher.group(1);
            String text = timeMatcher.group(2);
            if (text == null) text = whenPattern;

            CharSequence cs = Tuils.EMPTYSTRING;
            int c, change;
            if (unlockTimeOrder == UP_DOWN) {
                c = 0;
                change = +1;
            } else {
                c = lastUnlocks.length - 1;
                change = -1;
            }

            for (int counter = 0; counter < lastUnlocks.length; counter++, c += change) {
                String t = text;
                t = indexPattern.matcher(t).replaceAll(String.valueOf(c + 1));
                cs = TextUtils.concat(cs, t);

                CharSequence time;
                if (lastUnlocks[c] > 0) {
                    time = TimeManager.instance.getCharSequence(timeGroup, lastUnlocks[c]);
                } else {
                    time = notAvailableText;
                }

                if (time == null) continue;

                cs = TextUtils.replace(cs, new String[] {whenPattern}, new CharSequence[] {time});
                if (counter != lastUnlocks.length - 1) {
                    cs = TextUtils.concat(cs, unlockTimeDivider);
                }
            }

            s = TextUtils.replace(s, new String[] {timeMatcher.group(0)}, new CharSequence[] {cs});
        }

        labelUpdater.updateText(UIManager.Label.unlock, s);
    }

    private final Runnable unlockTimeRunnable = new Runnable() {
        @Override
        public void run() {
            long delay = nextUnlockCycleRestart - System.currentTimeMillis();
            if (delay <= 0) {
                unlockTimes = 0;
                if (lastUnlocks != null) {
                    for (int c = 0; c < lastUnlocks.length; c++) {
                        lastUnlocks[c] = -1;
                    }
                }

                Calendar now = Calendar.getInstance();
                int hour = now.get(Calendar.HOUR_OF_DAY), minute = now.get(Calendar.MINUTE);
                if (unlockHour < hour || (unlockHour == hour && unlockMinute <= minute)) {
                    now.set(Calendar.DAY_OF_YEAR, now.get(Calendar.DAY_OF_YEAR) + 1);
                }
                Calendar nextRestart = now;
                nextRestart.set(Calendar.HOUR_OF_DAY, unlockHour);
                nextRestart.set(Calendar.MINUTE, unlockMinute);
                nextRestart.set(Calendar.SECOND, 0);

                nextUnlockCycleRestart = nextRestart.getTimeInMillis();

                preferences.edit()
                        .putLong(NEXT_UNLOCK_CYCLE_RESTART, nextUnlockCycleRestart)
                        .putInt(UNLOCK_KEY, 0)
                        .apply();

                delay = nextUnlockCycleRestart - System.currentTimeMillis();
                if (delay < 0) delay = 0;
            }

            invalidateUnlockText();
            delay = Math.min(delay, UNLOCK_RUNNABLE_DELAY);
            handler.postDelayed(this, delay);
        }
    };


    // Battery Update class
    private class BatteryUpdate implements OnBatteryUpdate {
        Pattern optionalCharging;
        final Pattern value = Pattern.compile("%v", Pattern.LITERAL | Pattern.CASE_INSENSITIVE);
        boolean manyStatus, loaded;
        int colorHigh, colorMedium, colorLow;
        boolean charging;
        float last = -1;

        @Override
        public void update(float p) {
            if (batteryFormat == null) {
                batteryFormat = XMLPrefsManager.get(Behavior.battery_format);
                Intent intent = mContext.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                if (intent == null) {
                    charging = false;
                } else {
                    int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
                }

                String optionalSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator);
                String optional = "%\\(([^" + optionalSeparator + "]*)" + optionalSeparator + "([^)]*)\\)";
                optionalCharging = Pattern.compile(optional, Pattern.CASE_INSENSITIVE);
            }

            if (p == -1) p = last;
            last = p;

            if (!loaded) {
                loaded = true;
                manyStatus = XMLPrefsManager.getBoolean(Ui.enable_battery_status);
                colorHigh = XMLPrefsManager.getColor(Theme.battery_color_high);
                colorMedium = XMLPrefsManager.getColor(Theme.battery_color_medium);
                colorLow = XMLPrefsManager.getColor(Theme.battery_color_low);
            }

            int percentage = (int) p;
            int color;
            if (manyStatus) {
                if (percentage > mediumPercentage) color = colorHigh;
                else if (percentage > lowPercentage) color = colorMedium;
                else color = colorLow;
            } else {
                color = colorHigh;
            }

            String cp = batteryFormat;
            Matcher m = optionalCharging.matcher(cp);
            while (m.find()) {
                cp = cp.replace(m.group(0), m.groupCount() == 2 ? m.group(charging ? 1 : 2) : Tuils.EMPTYSTRING);
            }

            cp = value.matcher(cp).replaceAll(String.valueOf(percentage));
            cp = Tuils.patternNewline.matcher(cp).replaceAll(Tuils.NEWLINE);

            labelUpdater.updateText(UIManager.Label.battery, TextProcessor.span(mContext, cp, color, labelUpdater.getLabelSize(UIManager.Label.battery)));
        }

        @Override
        public void onCharging() {
            charging = true;
            update(-1);
        }

        @Override
        public void onNotCharging() {
            charging = false;
            update(-1);
        }
    }

    // Storage Runnable
    private class StorageRunnable implements Runnable {
        private final String INT_AV = "%iav";
        private final String INT_TOT = "%itot";
        private final String EXT_AV = "%eav";
        private final String EXT_TOT = "%etot";
        private List<Pattern> storagePatterns;
        private String storageFormat;
        int color;

        @Override
        public void run() {
            if (storageFormat == null) {
                storageFormat = XMLPrefsManager.get(Behavior.storage_format);
                color = XMLPrefsManager.getColor(Theme.storage_color);
            }

            if (storagePatterns == null) {
                storagePatterns = new ArrayList<>();
                storagePatterns.add(Pattern.compile(INT_AV + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_AV + "%", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Pattern.compile(INT_TOT + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Pattern.compile(EXT_AV + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV + "%", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Pattern.compile(EXT_TOT + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                storagePatterns.add(Tuils.patternNewline);

                storagePatterns.add(Pattern.compile(INT_AV, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(INT_TOT, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_AV, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                storagePatterns.add(Pattern.compile(EXT_TOT, Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
            }

            double iav = DeviceStateManager.getAvailableInternalMemorySize(Tuils.BYTE);
            double itot = DeviceStateManager.getTotalInternalMemorySize(Tuils.BYTE);
            double eav = DeviceStateManager.getAvailableExternalMemorySize(Tuils.BYTE);
            double etot = DeviceStateManager.getTotalExternalMemorySize(Tuils.BYTE);

            String copy = storageFormat;
            copy = storagePatterns.get(0).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.TERA))));
            copy = storagePatterns.get(1).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.GIGA))));
            copy = storagePatterns.get(2).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.MEGA))));
            copy = storagePatterns.get(3).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.KILO))));
            copy = storagePatterns.get(4).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.BYTE))));
            copy = storagePatterns.get(5).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.percentage(iav, itot))));

            copy = storagePatterns.get(6).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.TERA))));
            copy = storagePatterns.get(7).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.GIGA))));
            copy = storagePatterns.get(8).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.MEGA))));
            copy = storagePatterns.get(9).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.KILO))));
            copy = storagePatterns.get(10).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.BYTE))));

            copy = storagePatterns.get(11).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.TERA))));
            copy = storagePatterns.get(12).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.GIGA))));
            copy = storagePatterns.get(13).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.MEGA))));
            copy = storagePatterns.get(14).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.KILO))));
            copy = storagePatterns.get(15).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.BYTE))));
            copy = storagePatterns.get(16).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.percentage(eav, etot))));

            copy = storagePatterns.get(17).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.TERA))));
            copy = storagePatterns.get(18).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.GIGA))));
            copy = storagePatterns.get(19).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.MEGA))));
            copy = storagePatterns.get(20).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.KILO))));
            copy = storagePatterns.get(21).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.BYTE))));

            copy = storagePatterns.get(22).matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));
            copy = storagePatterns.get(23).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) iav, Tuils.GIGA))));
            copy = storagePatterns.get(24).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) itot, Tuils.GIGA))));
            copy = storagePatterns.get(25).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) eav, Tuils.GIGA))));
            copy = storagePatterns.get(26).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) etot, Tuils.GIGA))));

            labelUpdater.updateText(UIManager.Label.storage, TextProcessor.span(mContext, copy, color, labelUpdater.getLabelSize(UIManager.Label.storage)));
            handler.postDelayed(this, STORAGE_DELAY);
        }
    }

    // Time Runnable
    private class TimeRunnable implements Runnable {
        boolean active;

        @Override
        public void run() {
            if (!active) {
                active = true;
            }
            labelUpdater.updateText(UIManager.Label.time, TimeManager.instance.getCharSequence(mContext, labelUpdater.getLabelSize(UIManager.Label.time), "%t0"));
            
            long delay = TIME_DELAY;
            java.text.SimpleDateFormat format = TimeManager.instance.getDateFormat(0);
            if (format != null) {
                String pattern = format.toPattern();
                if (!pattern.contains("s") && !pattern.contains("S")) {
                    delay = 60000 - (System.currentTimeMillis() % 60000);
                }
            }
            handler.postDelayed(this, delay);
        }
    }

    // Ram Runnable
    private class RamRunnable implements Runnable {
        private final String AV = "%av";
        private final String TOT = "%tot";
        List<Pattern> ramPatterns;
        String ramFormat;
        int color;

        @Override
        public void run() {
            if (ramFormat == null) {
                ramFormat = XMLPrefsManager.get(Behavior.ram_format);
                color = XMLPrefsManager.getColor(Theme.ram_color);
            }

            if (ramPatterns == null) {
                ramPatterns = new ArrayList<>();
                ramPatterns.add(Pattern.compile(AV + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(AV + "%", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                ramPatterns.add(Pattern.compile(TOT + "tb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "gb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "mb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "kb", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));
                ramPatterns.add(Pattern.compile(TOT + "b", Pattern.CASE_INSENSITIVE | Pattern.LITERAL));

                ramPatterns.add(Tuils.patternNewline);
            }

            String copy = ramFormat;
            double av = DeviceStateManager.freeRam(activityManager, memory);
            double tot = DeviceStateManager.totalRam() * 1024L;

            copy = ramPatterns.get(0).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.TERA))));
            copy = ramPatterns.get(1).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.GIGA))));
            copy = ramPatterns.get(2).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.MEGA))));
            copy = ramPatterns.get(3).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.KILO))));
            copy = ramPatterns.get(4).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) av, Tuils.BYTE))));
            copy = ramPatterns.get(5).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.percentage(av, tot))));

            copy = ramPatterns.get(6).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.TERA))));
            copy = ramPatterns.get(7).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.GIGA))));
            copy = ramPatterns.get(8).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.MEGA))));
            copy = ramPatterns.get(9).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.KILO))));
            copy = ramPatterns.get(10).matcher(copy).replaceAll(Matcher.quoteReplacement(String.valueOf(Tuils.formatSize((long) tot, Tuils.BYTE))));

            copy = ramPatterns.get(11).matcher(copy).replaceAll(Matcher.quoteReplacement(Tuils.NEWLINE));

            labelUpdater.updateText(UIManager.Label.ram, TextProcessor.span(mContext, copy, color, labelUpdater.getLabelSize(UIManager.Label.ram)));
            handler.postDelayed(this, RAM_DELAY);
        }
    }

    // Network Runnable
    private class NetworkRunnable implements Runnable {
        final String zero = "0";
        final String one = "1";
        final String on = "on";
        final String off = "off";
        final String ON = on.toUpperCase();
        final String OFF = off.toUpperCase();
        final String _true = "true";
        final String _false = "false";
        final String TRUE = _true.toUpperCase();
        final String FALSE = _false.toUpperCase();

        final Pattern w0 = Pattern.compile("%w0", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w1 = Pattern.compile("%w1", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w2 = Pattern.compile("%w2", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w3 = Pattern.compile("%w3", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern w4 = Pattern.compile("%w4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern wn = Pattern.compile("%wn", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d0 = Pattern.compile("%d0", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d1 = Pattern.compile("%d1", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d2 = Pattern.compile("%d2", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d3 = Pattern.compile("%d3", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern d4 = Pattern.compile("%d4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b0 = Pattern.compile("%b0", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b1 = Pattern.compile("%b1", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b2 = Pattern.compile("%b2", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b3 = Pattern.compile("%b3", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern b4 = Pattern.compile("%b4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern ip4 = Pattern.compile("%ip4", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern ip6 = Pattern.compile("%ip6", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
        final Pattern dt = Pattern.compile("%dt", Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

        Pattern optionalWifi, optionalData, optionalBluetooth;
        String format, optionalValueSeparator;
        int color;

        WifiManager wifiManager;
        BluetoothAdapter mBluetoothAdapter;
        ConnectivityManager connectivityManager;

        Class cmClass;
        Method method;
        int maxDepth;
        int updateTime;

        @Override
        public void run() {
            if (format == null) {
                format = XMLPrefsManager.get(Behavior.network_info_format);
                color = XMLPrefsManager.getColor(Theme.network_info_color);
                maxDepth = XMLPrefsManager.getInt(Behavior.max_optional_depth);

                updateTime = XMLPrefsManager.getInt(Behavior.network_info_update_ms);
                if (updateTime < 1000) {
                    updateTime = Integer.parseInt(Behavior.network_info_update_ms.defaultValue());
                }

                connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

                optionalValueSeparator = "\\" + XMLPrefsManager.get(Behavior.optional_values_separator);

                String wifiRegex = "%\\(([^" + optionalValueSeparator + "]*)" + optionalValueSeparator + "([^)]*)\\)";
                String dataRegex = "%\\[([^" + optionalValueSeparator + "]*)" + optionalValueSeparator + "([^\\]]*)\\]";
                String bluetoothRegex = "%\\{([^" + optionalValueSeparator + "]*)" + optionalValueSeparator + "([^}]*)\\}";

                optionalWifi = Pattern.compile(wifiRegex, Pattern.CASE_INSENSITIVE);
                optionalBluetooth = Pattern.compile(bluetoothRegex, Pattern.CASE_INSENSITIVE);
                optionalData = Pattern.compile(dataRegex, Pattern.CASE_INSENSITIVE);

                try {
                    cmClass = Class.forName(connectivityManager.getClass().getName());
                    method = cmClass.getDeclaredMethod("getMobileDataEnabled");
                    method.setAccessible(true);
                } catch (Exception e) {
                    cmClass = null;
                    method = null;
                }
            }

            boolean wifiOn = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
            String wifiName = null;
            if (wifiOn) {
                WifiInfo connectionInfo = wifiManager.getConnectionInfo();
                if (connectionInfo != null) {
                    wifiName = connectionInfo.getSSID();
                }
            }

            boolean mobileOn = false;
            try {
                mobileOn = method != null && connectivityManager != null && (Boolean) method.invoke(connectivityManager);
            } catch (Exception e) {
                // ignore
            }

            String mobileType;
            if (mobileOn) {
                mobileType = DeviceStateManager.getNetworkType(mContext);
            } else {
                mobileType = "unknown";
            }

            boolean bluetoothOn = mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
            String copy = format;

            if (maxDepth > 0) {
                copy = apply(1, copy, new boolean[]{wifiOn, mobileOn, bluetoothOn}, optionalWifi, optionalData, optionalBluetooth);
                copy = apply(1, copy, new boolean[]{mobileOn, wifiOn, bluetoothOn}, optionalData, optionalWifi, optionalBluetooth);
                copy = apply(1, copy, new boolean[]{bluetoothOn, wifiOn, mobileOn}, optionalBluetooth, optionalWifi, optionalData);
            }

            copy = w0.matcher(copy).replaceAll(wifiOn ? one : zero);
            copy = w1.matcher(copy).replaceAll(wifiOn ? on : off);
            copy = w2.matcher(copy).replaceAll(wifiOn ? ON : OFF);
            copy = w3.matcher(copy).replaceAll(wifiOn ? _true : _false);
            copy = w4.matcher(copy).replaceAll(wifiOn ? TRUE : FALSE);
            copy = wn.matcher(copy).replaceAll(wifiName != null ? wifiName.replaceAll("\"", Tuils.EMPTYSTRING) : "null");
            copy = d0.matcher(copy).replaceAll(mobileOn ? one : zero);
            copy = d1.matcher(copy).replaceAll(mobileOn ? on : off);
            copy = d2.matcher(copy).replaceAll(mobileOn ? ON : OFF);
            copy = d3.matcher(copy).replaceAll(mobileOn ? _true : _false);
            copy = d4.matcher(copy).replaceAll(mobileOn ? TRUE : FALSE);
            copy = b0.matcher(copy).replaceAll(bluetoothOn ? one : zero);
            copy = b1.matcher(copy).replaceAll(bluetoothOn ? on : off);
            copy = b2.matcher(copy).replaceAll(bluetoothOn ? ON : OFF);
            copy = b3.matcher(copy).replaceAll(bluetoothOn ? _true : _false);
            copy = b4.matcher(copy).replaceAll(bluetoothOn ? TRUE : FALSE);
            copy = ip4.matcher(copy).replaceAll(NetworkUtils.getIPAddress(true));
            copy = ip6.matcher(copy).replaceAll(NetworkUtils.getIPAddress(false));
            copy = dt.matcher(copy).replaceAll(mobileType);
            copy = Tuils.patternNewline.matcher(copy).replaceAll(Tuils.NEWLINE);

            labelUpdater.updateText(UIManager.Label.network, TextProcessor.span(mContext, copy, color, labelUpdater.getLabelSize(UIManager.Label.network)));
            handler.postDelayed(this, updateTime);
        }

        private String apply(int depth, String s, boolean[] on, Pattern... ps) {
            if (ps.length == 0) return s;

            Matcher m = ps[0].matcher(s);
            while (m.find()) {
                if (m.groupCount() < 2) {
                    s = s.replace(m.group(0), Tuils.EMPTYSTRING);
                    continue;
                }

                String g1 = m.group(1);
                String g2 = m.group(2);

                if (depth < maxDepth) {
                    for (int c = 0; c < ps.length - 1; c++) {
                        boolean[] subOn = new boolean[on.length - 1];
                        subOn[0] = on[c + 1];

                        Pattern[] subPs = new Pattern[ps.length - 1];
                        subPs[0] = ps[c + 1];

                        for (int j = 1, k = 1; j < subOn.length; j++, k++) {
                            if (k == c + 1) {
                                j--;
                                continue;
                            }
                            subOn[j] = on[k];
                            subPs[j] = ps[k];
                        }

                        g1 = apply(depth + 1, g1, subOn, subPs);
                        g2 = apply(depth + 1, g2, subOn, subPs);
                    }
                }

                s = s.replace(m.group(0), on[0] ? g1 : g2);
            }

            return s;
        }
    }
}
