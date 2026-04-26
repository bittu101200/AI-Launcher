package bhupendra.ai.launcher.managers.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class NotificationDisplayManager {

    private static final long DUPLICATE_WINDOW_MS = 15_000L;
    private static final String DEBUG_TAG = "TUI_NOTIFY";

    private static volatile NotificationDisplayManager instance;

    private final Context appContext;
    private final NotificationAttentionDecider attentionDecider;
    private final Map<String, Long> recentDisplays = new HashMap<>();
    private final Map<String, String> appLabelCache = new HashMap<>();

    private NotificationDisplayManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.attentionDecider = new NotificationAttentionDecider();
    }

    public static synchronized NotificationDisplayManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationDisplayManager(context);
        }
        return instance;
    }

    public boolean dispatchSystemNotification(StatusBarNotification sbn, CharSequence renderedText, Parcelable action, Parcelable longAction, NotificationContentResolver.ResolvedContent resolvedContent) {
        if (!attentionDecider.shouldDisplay(sbn, renderedText, resolvedContent)) {
            debug("suppressed attention", sbn, renderedText, resolvedContent);
            return false;
        }
        if (shouldSuppressDuplicate(buildSystemFingerprint(sbn, renderedText))) {
            debug("suppressed duplicate", sbn, renderedText, resolvedContent);
            return false;
        }
        debug("dispatch", sbn, renderedText, resolvedContent);
        Tuils.sendOutput(appContext, renderedText, TerminalManager.CATEGORY_NO_COLOR, action, longAction);
        return true;
    }

    public boolean dispatchJourneyNotification(StatusBarNotification sbn) {
        if (sbn == null) return false;

        Notification notification = sbn.getNotification();
        if (notification == null) return false;

        Bundle extras = notification.extras;
        String packageName = sbn.getPackageName();
        String appName = resolveAppName(packageName);
        String title = extras != null ? safe(extras.getCharSequence(Notification.EXTRA_TITLE)) : "";
        String text = extras != null ? safe(extras.getCharSequence(Notification.EXTRA_TEXT)) : "";

        if (!attentionDecider.shouldDisplayInternalNotification("journey", packageName, title, text, notification.priority)) {
            return false;
        }

        StringBuilder message = new StringBuilder("[journey] ");
        if (appName.length() > 0) {
            message.append(appName);
        } else {
            message.append(packageName);
        }
        if (title.length() > 0) {
            message.append(": ").append(title);
        }
        if (text.length() > 0 && !text.equalsIgnoreCase(title)) {
            message.append(" --- ").append(text);
        }

        if (shouldSuppressDuplicate(buildJourneyFingerprint(packageName, title, text))) return false;
        Tuils.sendOutput(appContext, message.toString(), TerminalManager.CATEGORY_NO_COLOR, notification.contentIntent);
        return true;
    }

    private synchronized boolean shouldSuppressDuplicate(String fingerprint) {
        if (fingerprint == null || fingerprint.length() == 0) return false;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = recentDisplays.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > DUPLICATE_WINDOW_MS) {
                it.remove();
            }
        }

        Long previous = recentDisplays.get(fingerprint);
        if (previous != null && now - previous <= DUPLICATE_WINDOW_MS) {
            recentDisplays.put(fingerprint, now);
            return true;
        }

        recentDisplays.put(fingerprint, now);
        return false;
    }

    private String buildSystemFingerprint(StatusBarNotification sbn, CharSequence renderedText) {
        if (sbn == null) return "";
        return "system|" + safe(sbn.getPackageName()) + "|" + safe(renderedText);
    }

    private String buildJourneyFingerprint(String packageName, String title, String text) {
        return "journey|" + safe(packageName) + "|" + safe(title) + "|" + safe(text);
    }

    private String resolveAppName(String packageName) {
        if (packageName == null || packageName.length() == 0) return "";
        String cached = appLabelCache.get(packageName);
        if (cached != null) return cached;
        try {
            String label = appContext.getPackageManager().getApplicationInfo(packageName, 0).loadLabel(appContext.getPackageManager()).toString();
            appLabelCache.put(packageName, label);
            return label;
        } catch (PackageManager.NameNotFoundException e) {
            appLabelCache.put(packageName, packageName);
            return packageName;
        }
    }

    private String safe(CharSequence value) {
        if (value == null) return "";
        return TextUtils.isEmpty(value) ? "" : value.toString().trim();
    }

    private void debug(String stage, StatusBarNotification sbn, CharSequence renderedText, NotificationContentResolver.ResolvedContent resolvedContent) {
        if (sbn == null) return;
        String packageName = sbn.getPackageName();
        if (!NotificationContentResolver.isMessagingPackage(packageName)) return;
        Log.d(
            DEBUG_TAG,
            stage
                + " pkg=" + packageName
                + " key=" + sbn.getKey()
                + " title=" + (resolvedContent != null ? resolvedContent.title : "")
                + " text=" + (resolvedContent != null ? resolvedContent.text : "")
                + " rendered=" + safe(renderedText)
        );
    }
}
