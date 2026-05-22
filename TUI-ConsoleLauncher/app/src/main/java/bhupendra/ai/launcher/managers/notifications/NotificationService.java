package bhupendra.ai.launcher.managers.notifications;

import bhupendra.ai.launcher.managers.TextProcessor;


/**
 * Created by francescoandreuzzi on 27/04/2017.
 */

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.TimeManager;
import bhupendra.ai.launcher.managers.notifications.reply.ReplyManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Notifications;

import bhupendra.ai.launcher.tuils.Tuils;


@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NotificationService extends NotificationListenerService {

    private static final String TAG = "NotificationService";
    private static final String DEBUG_TAG = "TUI_NOTIFY";
    public static NotificationService instance;
    public static final String DESTROY = "destroy";

    private static final int QUEUE_CAPACITY = 128;
    private static final long POLL_TIMEOUT_MS = 250L;
    private String LINES_LABEL = "Lines";
    private String ANDROID_LABEL_PREFIX = "android.";
    private String NULL_LABEL = "";

    Handler handler = new Handler();

    String format;
    int color, maxOptionalDepth;
    boolean enabled, click, longClick, active;

    volatile List<String> whitelist, blacklist;

    LinkedBlockingQueue<StatusBarNotification> queue;
    private final Map<String, String> appLabelCache = new HashMap<>();

    final String PKG = "%pkg", APP = "%app", NEWLINE = "%n";
    final Pattern timePattern = Pattern.compile("^%t[0-9]*$");

    PackageManager manager;
    ReplyManager replyManager;
    NotificationManager notificationManager;
    NotificationDisplayManager notificationDisplayManager;

    private final Pattern formatPattern = Pattern.compile("%(?:\\[(\\d+)\\])?(?:\\[([^]]+)\\])?(?:(?:\\{)([a-zA-Z\\.\\:\\s]+)(?:\\})|([a-zA-Z\\.\\:]+))");



    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        init();
    }

    private void init() {
        try {
            notificationManager = NotificationManager.create(this);
            notificationDisplayManager = NotificationDisplayManager.getInstance(this);
            XMLPrefsManager.loadCommons(this);
        } catch (Exception e) {
            Tuils.log(e);
            return;
        }

        try {
            replyManager = new ReplyManager(this);
        } catch (VerifyError error) {
            replyManager = null;
        }

        manager = getPackageManager();
        enabled = XMLPrefsManager.getBoolean(Notifications.show_notifications) || XMLPrefsManager.get(Notifications.show_notifications).equalsIgnoreCase("enabled");
        Log.d(TAG, "NotificationService enabled: " + enabled);

        format = XMLPrefsManager.get(Notifications.notification_format);
        color = XMLPrefsManager.getColor(Notifications.default_notification_color);

        click = XMLPrefsManager.getBoolean(Notifications.click_notification);
        longClick = XMLPrefsManager.getBoolean(Notifications.long_click_notification);

        String wl = XMLPrefsManager.get(Notifications.notification_whitelist);
        if (wl != null && wl.length() > 0) {
            List<String> list = new ArrayList<>(Arrays.asList(wl.split(",")));
            for (int i = 0; i < list.size(); i++) list.set(i, list.get(i).trim());
            whitelist = list;
        } else {
            whitelist = null;
        }

        String bl = XMLPrefsManager.get(Notifications.notification_blacklist);
        if (bl != null && bl.length() > 0) {
            List<String> list = new ArrayList<>(Arrays.asList(bl.split(",")));
            for (int i = 0; i < list.size(); i++) list.set(i, list.get(i).trim());
            blacklist = list;
        } else {
            blacklist = null;
        }

        maxOptionalDepth = XMLPrefsManager.getInt(Behavior.max_optional_depth);

        queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        bhupendra.ai.launcher.tuils.LauncherExecutors.notificationExecutor.execute(() -> {
                if(!enabled) return;

                while(true) {
                    if(Thread.currentThread().isInterrupted()) return;

                    StatusBarNotification sbn = null;
                    try {
                        sbn = queue.take();
                    } catch (InterruptedException e) {
                        return;
                    }

                    if(sbn == null) continue;
                    if(Thread.currentThread().isInterrupted()) return;

                    do {
                        StatusBarNotification displaySbn = pickDisplayNotification(sbn);
                        android.app.Notification notification = displaySbn.getNotification();
                        if (notification == null) {
                            continue;
                        }

                        String pack = displaySbn.getPackageName();

                        if (blacklist != null && blacklist.contains(pack)) {
                            Log.d(DEBUG_TAG, "Filtered by blacklist: " + pack);
                            continue;
                        }

                        if (whitelist != null && !whitelist.isEmpty() && !whitelist.contains(pack)) {
                            Log.d(DEBUG_TAG, "Filtered by whitelist (not in list): " + pack + " Whitelist: " + whitelist);
                            continue;
                        }

                        NotificationManager.NotificatedApp nApp = notificationManager.getAppState(pack);
                        if ((nApp != null && !nApp.enabled)) {
                            Log.d(DEBUG_TAG, "Filtered by app state (disabled): " + pack);
                            continue;
                        }

                        NotificationHookManager.getInstance(NotificationService.this).processNotification(sbn);

                        bhupendra.ai.launcher.ai.AISubsystem ai =
                            bhupendra.ai.launcher.ai.AISubsystem.getInstance();
                        if (ai != null && ai.getJourneyManager().isJourneyNotification(notification)) {
                            ai.getJourneyManager().processNotification(sbn);
                        }

                        String appName = resolveAppName(pack);

                        String f;
                        if(nApp != null && nApp.format != null) f = nApp.format;
                        else f = format;

                        int textColor;
                        if(nApp != null && nApp.color != null) textColor = Color.parseColor(nApp.color);
                        else textColor = color;

                        CharSequence s = TextProcessor.span(f, textColor);

                        Bundle bundle = NotificationCompat.getExtras(notification);
                        NotificationContentResolver.ResolvedContent resolvedContent =
                            NotificationContentResolver.resolve(displaySbn);

                        if(bundle != null) {
                            Matcher m = formatPattern.matcher(s);
                            String match;
                            while(m.find()) {
                                match = m.group(0);
                                if (!match.startsWith(PKG) && !match.startsWith(APP) && !match.startsWith(NEWLINE) && !timePattern.matcher(match).matches()) {
                                    String length = m.group(1);
                                    String color = m.group(2);
                                    String value = m.group(3);

                                    if(value == null || value.length() == 0) value = m.group(4);

                                    if(value != null) value = value.trim();
                                    else continue;

                                    if(value.length() == 0) continue;

                                    if(value.equals("ttl")) value = "title";
                                    else if(value.equals("txt")) value = "text";

                                    String[] temp = value.split(":"), split;
                                    if(value.endsWith(":")) {
                                        split = new String[temp.length + 1];
                                        System.arraycopy(temp, 0, split, 0, temp.length);
                                        split[split.length - 1] = Tuils.EMPTYSTRING;
                                    } else split = temp;

                                    int stopAt = split.length;
                                    if(stopAt > 1) stopAt--;

                                    CharSequence text = null;
                                    String requestedField = split.length > 0 ? split[0] : "";
                                    for(int j = 0; j < stopAt; j++) {
                                        if("title".equalsIgnoreCase(split[j])) {
                                            text = resolvedContent.title;
                                        } else if("text".equalsIgnoreCase(split[j])) {
                                            text = resolvedContent.text;
                                        } else if(split[j].contains(LINES_LABEL)) {
                                            CharSequence[] array = bundle.getCharSequenceArray(ANDROID_LABEL_PREFIX + split[j]);
                                            if(array != null) {
                                                for(CharSequence c : array) {
                                                    if(text == null) text = c;
                                                    else text = TextUtils.concat(text, Tuils.NEWLINE, c);
                                                }
                                            }
                                        } else {
                                            text = bundle.getCharSequence(ANDROID_LABEL_PREFIX + split[j]);
                                        }

                                        if(text != null && text.length() > 0) break;
                                    }

                                    if(text == null || text.length() == 0) {
                                        if ("title".equalsIgnoreCase(requestedField)) {
                                            text = firstNonEmpty(resolvedContent.title, resolvedContent.originalTitle);
                                        } else if ("text".equalsIgnoreCase(requestedField)) {
                                            text = firstNonEmpty(resolvedContent.text, resolvedContent.originalText);
                                        }
                                    }

                                    if(text == null || text.length() == 0) {
                                        text = split.length == 1
                                            ? ("title".equalsIgnoreCase(requestedField) || "text".equalsIgnoreCase(requestedField) ? "" : NULL_LABEL)
                                            : split[split.length - 1];
                                    }

                                    String stringed = text.toString().trim();

                                    try {
                                        int l = Integer.parseInt(length);
                                        stringed = stringed.substring(0,l);
                                    } catch (Exception e) {}

                                    try {
                                        text = TextProcessor.span(stringed, Color.parseColor(color));
                                    } catch (Exception e) {
                                        text = stringed;
                                    }

                                    s = TextUtils.replace(s, new String[] {m.group(0)}, new CharSequence[] {text});
                                }
                            }
                        }

                        String text = s.toString();

                        if(notificationManager.match(text)) continue;

                        Notification n = new Notification(System.currentTimeMillis(), text, pack, notification.contentIntent);

                        s = TextUtils.replace(s, new String[]{PKG, APP, NEWLINE}, new CharSequence[]{pack, appName, Tuils.NEWLINE});
                        String st = s.toString();
                        while (st.contains(NEWLINE)) {
                            s = TextUtils.replace(s,
                                    new String[]{NEWLINE},
                                    new CharSequence[]{Tuils.NEWLINE});
                            st = s.toString();
                        }

                        try {
                            s = TimeManager.instance.replace(s);
                        } catch (Exception e) {
                            Tuils.log(e);
                        }

                        if (NotificationContentResolver.isMessagingPackage(pack)) {
                            Log.d(
                                DEBUG_TAG,
                                "candidate pkg=" + pack
                                    + " key=" + displaySbn.getKey()
                                    + " title=" + resolvedContent.title
                                    + " text=" + resolvedContent.text
                                    + " rendered=" + s
                            );
                        }

                        if (notificationDisplayManager != null) {
                            notificationDisplayManager.dispatchSystemNotification(
                                displaySbn,
                                s,
                                click ? n : null,
                                longClick ? n : null,
                                resolvedContent
                            );
                        }

                        if(replyManager != null) replyManager.onNotification(sbn, s, resolvedContent.title);
                    } while ((sbn = queue.poll()) != null && !Thread.currentThread().isInterrupted());
                }
        });

        active = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null) {
            boolean destroy = intent.getBooleanExtra(DESTROY, false);
            if(destroy) dispose();
        }

        if(!active) init();

        return START_STICKY;
    }

    private void dispose() {
        if(replyManager != null) {
            replyManager.dispose(this);
            replyManager = null;
        }

        if(notificationManager != null) {
            notificationManager.dispose();
            notificationManager = null;
        }

        notificationDisplayManager = null;



        if(queue != null) {
            queue.clear();
            queue = null;
        }

        active = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
//        ondestroy won't ever be called
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;
        String pack = sbn.getPackageName();
        if (blacklist != null && blacklist.contains(pack)) {
            return;
        }
        if (whitelist != null && !whitelist.isEmpty() && !whitelist.contains(pack)) {
            return;
        }
        Log.d(TAG, "onNotificationPosted: " + pack);
        if(!enabled) return;

        if (!queue.offer(sbn)) {
            queue.poll();
            queue.offer(sbn);
        }
    }

    public void updateFiltering() {
        String wl = XMLPrefsManager.get(Notifications.notification_whitelist);
        if (wl != null && wl.length() > 0) {
            List<String> list = new ArrayList<>(Arrays.asList(wl.split(",")));
            for (int i = 0; i < list.size(); i++) list.set(i, list.get(i).trim());
            whitelist = list;
        } else {
            whitelist = null;
        }

        String bl = XMLPrefsManager.get(Notifications.notification_blacklist);
        if (bl != null && bl.length() > 0) {
            List<String> list = new ArrayList<>(Arrays.asList(bl.split(",")));
            for (int i = 0; i < list.size(); i++) list.set(i, list.get(i).trim());
            blacklist = list;
        } else {
            blacklist = null;
        }
    }

    private String resolveAppName(String packageName) {
        String cached = appLabelCache.get(packageName);
        if (cached != null) return cached;
        try {
            String label = manager.getApplicationInfo(packageName, 0).loadLabel(manager).toString();
            appLabelCache.put(packageName, label);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            String fallback = packageName != null ? packageName : "";
            appLabelCache.put(packageName, fallback);
            return fallback;
        }
    }

    private StatusBarNotification pickDisplayNotification(StatusBarNotification sbn) {
        if (!NotificationContentResolver.isMessagingSummary(sbn)) {
            return sbn;
        }

        try {
            StatusBarNotification[] activeNotifications = getActiveNotifications();
            if (activeNotifications == null || activeNotifications.length == 0) {
                return sbn;
            }

            String packageName = sbn.getPackageName();
            String groupKey = sbn.getGroupKey();
            StatusBarNotification best = null;

            for (StatusBarNotification candidate : activeNotifications) {
                if (candidate == null) continue;
                if (!packageName.equals(candidate.getPackageName())) continue;
                if (NotificationContentResolver.isGroupSummary(candidate)) continue;
                if (groupKey != null && groupKey.length() > 0 && !groupKey.equals(candidate.getGroupKey())) {
                    continue;
                }

                if (best == null || candidate.getPostTime() > best.getPostTime()) {
                    best = candidate;
                }
            }

            if (best != null) {
                Log.d(DEBUG_TAG, "promoted summary pkg=" + packageName + " summaryKey=" + sbn.getKey() + " childKey=" + best.getKey());
                return best;
            }
        } catch (Exception e) {
            Tuils.log(e);
        }

        return sbn;
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (trimmed.length() > 0 && !"null".equalsIgnoreCase(trimmed)) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {}

    public static class Notification implements Parcelable {
        public long time;
        public String text, pkg;
        public PendingIntent pendingIntent;

        public Notification(long time, String text, String pkg, PendingIntent pi) {
            this.time = time;
            this.text = text;
            this.pkg = pkg;
            this.pendingIntent = pi;
        }

        protected Notification(Parcel in) {
            time = in.readLong();
            text = in.readString();
            pkg = in.readString();
            pendingIntent = in.readParcelable(PendingIntent.class.getClassLoader());
        }

        public static final Creator<Notification> CREATOR = new Creator<Notification>() {
            @Override
            public Notification createFromParcel(Parcel in) {
                return new Notification(in);
            }

            @Override
            public Notification[] newArray(int size) {
                return new Notification[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(time);
            dest.writeString(text);
            dest.writeString(pkg);
            dest.writeParcelable(pendingIntent, flags);
        }
    }
}
