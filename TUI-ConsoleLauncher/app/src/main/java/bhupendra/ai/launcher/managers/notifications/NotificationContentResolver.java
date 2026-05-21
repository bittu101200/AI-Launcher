package bhupendra.ai.launcher.managers.notifications;

import android.app.Notification;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class NotificationContentResolver {

    private static final Set<String> MESSAGING_PACKAGES = new HashSet<>(Arrays.asList(
        "com.whatsapp",
        "org.telegram.messenger",
        "org.thoughtcrime.securesms",
        "com.facebook.orca",
        "com.google.android.apps.messaging",
        "com.google.android.apps.googlevoice",
        "com.discord",
        "com.instagram.android",
        "com.snapchat.android",
        "com.android.mms",
        "com.android.messaging",
        "com.samsung.android.messaging",
        "com.oneplus.messaging",
        "com.oneplus.mms",
        "com.sonyericsson.conversations",
        "com.motorola.messaging",
        "com.textra",
        "com.jb.gosms",
        "com.handcent.nextsms",
        "com.chompry.chomp",
        "org.fossify.messages"
    ));

    private static final Pattern SINGLE_MESSAGE_SUMMARY_PATTERN = Pattern.compile(
        "^1\\s+(new\\s+)?message$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MULTI_MESSAGE_SUMMARY_PATTERN = Pattern.compile(
        "^(\\d+\\s+(new\\s+)?messages?(\\s+from\\s+\\d+\\s+chats?)?|\\d+\\s+chats?)$",
        Pattern.CASE_INSENSITIVE
    );

    public static final class ResolvedContent {
        public final String originalTitle;
        public final String originalText;
        public final String title;
        public final String text;
        public final boolean resolvedFromMessages;

        public ResolvedContent(String originalTitle, String originalText, String title, String text, boolean resolvedFromMessages) {
            this.originalTitle = originalTitle;
            this.originalText = originalText;
            this.title = title;
            this.text = text;
            this.resolvedFromMessages = resolvedFromMessages;
        }
    }

    private NotificationContentResolver() {}

    public static ResolvedContent resolve(StatusBarNotification sbn) {
        if (sbn == null) return new ResolvedContent("", "", "", "", false);

        Notification notification = sbn.getNotification();
        Bundle extras = notification != null ? notification.extras : null;

        String originalTitle = extras != null ? normalize(extras.getCharSequence(Notification.EXTRA_TITLE)) : "";
        String originalText = extras != null ? normalize(extras.getCharSequence(Notification.EXTRA_TEXT)) : "";

        if (notification == null || extras == null) {
            return new ResolvedContent(originalTitle, originalText, originalTitle, originalText, false);
        }

        String packageName = safeLower(sbn.getPackageName());
        if (!isMessagingPackage(packageName) || !looksLikeSummary(notification, originalTitle, originalText)) {
            return new ResolvedContent(originalTitle, originalText, originalTitle, originalText, false);
        }

        ResolvedContent fromMessages = resolveFromMessages(extras, originalTitle, originalText);
        if (fromMessages != null) {
            return fromMessages;
        }

        ResolvedContent fromLines = resolveFromTextLines(extras, originalTitle, originalText);
        if (fromLines != null) {
            return fromLines;
        }

        return new ResolvedContent(originalTitle, originalText, originalTitle, originalText, false);
    }

    static boolean isMessagingSummary(StatusBarNotification sbn) {
        if (sbn == null) return false;
        Notification notification = sbn.getNotification();
        if (notification == null) return false;
        Bundle extras = notification.extras;
        String title = extras != null ? normalize(extras.getCharSequence(Notification.EXTRA_TITLE)) : "";
        String text = extras != null ? normalize(extras.getCharSequence(Notification.EXTRA_TEXT)) : "";
        return isMessagingPackage(safeLower(sbn.getPackageName())) && looksLikeSummary(notification, title, text);
    }

    static boolean isGroupSummary(StatusBarNotification sbn) {
        if (sbn == null) return false;
        Notification notification = sbn.getNotification();
        if (notification == null) return false;
        return (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0;
    }

    static boolean isMessagingPackage(String packageName) {
        return MESSAGING_PACKAGES.contains(safeLower(packageName));
    }

    private static ResolvedContent resolveFromMessages(Bundle extras, String originalTitle, String originalText) {
        Parcelable[] messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
        if (messages == null || messages.length == 0) {
            messages = extras.getParcelableArray("android.messages");
        }
        if (messages == null || messages.length == 0) return null;

        for (int i = messages.length - 1; i >= 0; i--) {
            Bundle message = asBundle(messages[i]);
            if (message == null) continue;

            String text = normalize(message.getCharSequence("text"));
            if (text.length() == 0) continue;

            String sender = normalize(message.getCharSequence("sender"));
            if (sender.length() == 0) {
                sender = normalize(message.getCharSequence("name"));
            }
            if (sender.length() == 0) {
                sender = normalize(extras.getCharSequence("android.conversationTitle"));
            }
            if (sender.length() == 0) {
                sender = originalTitle;
            }

            return new ResolvedContent(originalTitle, originalText, sender, text, true);
        }

        return null;
    }

    private static ResolvedContent resolveFromTextLines(Bundle extras, String originalTitle, String originalText) {
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if (lines == null || lines.length == 0) {
            lines = extras.getCharSequenceArray("android.textLines");
        }
        if (lines == null || lines.length == 0) return null;

        for (int i = lines.length - 1; i >= 0; i--) {
            String line = normalize(lines[i]);
            if (line.length() == 0) continue;

            String sender = originalTitle;
            String text = line;

            int colon = line.indexOf(':');
            if (colon > 0 && colon < line.length() - 1) {
                sender = line.substring(0, colon).trim();
                text = line.substring(colon + 1).trim();
            }

            if (sender.length() == 0) {
                sender = normalize(extras.getCharSequence("android.conversationTitle"));
            }
            if (sender.length() == 0) {
                sender = originalTitle;
            }

            if (text.length() == 0) continue;
            return new ResolvedContent(originalTitle, originalText, sender, text, true);
        }

        return null;
    }

    private static Bundle asBundle(Parcelable value) {
        if (value instanceof Bundle) {
            return (Bundle) value;
        }
        return null;
    }

    private static boolean looksLikeSummary(Notification notification, String title, String text) {
        boolean flagSummary = notification != null
            && (notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0;

        return flagSummary
            || SINGLE_MESSAGE_SUMMARY_PATTERN.matcher(safeLower(text)).matches()
            || MULTI_MESSAGE_SUMMARY_PATTERN.matcher(safeLower(text)).matches();
    }

    private static String normalize(CharSequence value) {
        if (value == null) return "";
        return TextUtils.isEmpty(value) ? "" : value.toString().trim();
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
