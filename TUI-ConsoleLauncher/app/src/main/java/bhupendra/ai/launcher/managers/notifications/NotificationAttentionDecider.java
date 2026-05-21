package bhupendra.ai.launcher.managers.notifications;

import android.app.Notification;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class NotificationAttentionDecider {

    private static final Set<String> HARD_BLACKLIST = new HashSet<>(Arrays.asList(
        "android",
        "com.android.systemui",
        "com.google.android.gms",
        "com.android.vending",
        "org.fdroid.fdroid",
        "com.apple.android.music",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.bluetooth.aptxmode"
    ));

    private static final Set<String> MESSAGING_ALLOWLIST = new HashSet<>(Arrays.asList(
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

    private static final Pattern GROUP_SUMMARY_PATTERN = Pattern.compile(
        "^(\\d+\\s+(new\\s+)?messages?(\\s+from\\s+\\d+\\s+chats?)?|\\d+\\s+chats?)$",
        Pattern.CASE_INSENSITIVE
    );

    public static final class Candidate {
        public final String source;
        public final String packageName;
        public final String category;
        public final int priority;
        public final String title;
        public final String text;
        public final CharSequence renderedText;
        public final boolean isInternal;
        public final boolean isGroupSummary;
        public final boolean hasProgress;
        public final boolean hasReplyAction;

        private Candidate(Builder builder) {
            this.source = builder.source;
            this.packageName = builder.packageName;
            this.category = builder.category;
            this.priority = builder.priority;
            this.title = builder.title;
            this.text = builder.text;
            this.renderedText = builder.renderedText;
            this.isInternal = builder.isInternal;
            this.isGroupSummary = builder.isGroupSummary;
            this.hasProgress = builder.hasProgress;
            this.hasReplyAction = builder.hasReplyAction;
        }

        public static final class Builder {
            private String source = "system";
            private String packageName;
            private String category;
            private int priority = Notification.PRIORITY_DEFAULT;
            private String title;
            private String text;
            private CharSequence renderedText;
            private boolean isInternal;
            private boolean isGroupSummary;
            private boolean hasProgress;
            private boolean hasReplyAction;

            public Builder setSource(String source) {
                this.source = source;
                return this;
            }

            public Builder setPackageName(String packageName) {
                this.packageName = packageName;
                return this;
            }

            public Builder setCategory(String category) {
                this.category = category;
                return this;
            }

            public Builder setPriority(int priority) {
                this.priority = priority;
                return this;
            }

            public Builder setTitle(String title) {
                this.title = title;
                return this;
            }

            public Builder setText(String text) {
                this.text = text;
                return this;
            }

            public Builder setRenderedText(CharSequence renderedText) {
                this.renderedText = renderedText;
                return this;
            }

            public Builder setInternal(boolean internal) {
                this.isInternal = internal;
                return this;
            }

            public Builder setGroupSummary(boolean groupSummary) {
                this.isGroupSummary = groupSummary;
                return this;
            }

            public Builder setHasProgress(boolean hasProgress) {
                this.hasProgress = hasProgress;
                return this;
            }

            public Builder setHasReplyAction(boolean hasReplyAction) {
                this.hasReplyAction = hasReplyAction;
                return this;
            }

            public Candidate build() {
                return new Candidate(this);
            }
        }
    }

    public boolean shouldDisplay(StatusBarNotification sbn, CharSequence renderedText, NotificationContentResolver.ResolvedContent resolvedContent) {
        if (sbn == null) return false;

        Notification notification = sbn.getNotification();
        if (notification == null) return false;

        String title = resolvedContent.title;
        String text = resolvedContent.text;

        Candidate candidate = new Candidate.Builder()
            .setSource("system")
            .setPackageName(sbn.getPackageName())
            .setCategory(notification.category)
            .setPriority(notification.priority)
            .setTitle(title)
            .setText(text)
            .setRenderedText(renderedText)
            .setGroupSummary(isGroupSummary(notification, title, text) && !resolvedContent.resolvedFromMessages)
            .setHasProgress(hasProgress(notification))
            .setHasReplyAction(hasReplyAction(notification))
            .build();

        return shouldDisplay(candidate);
    }

    public boolean shouldDisplayInternalNotification(String source, String packageName, String title, String text, int priority) {
        Candidate candidate = new Candidate.Builder()
            .setSource(source)
            .setPackageName(packageName)
            .setPriority(priority)
            .setTitle(normalize(title))
            .setText(normalize(text))
            .setInternal(true)
            .build();
        return shouldDisplay(candidate);
    }

    public boolean shouldDisplay(Candidate candidate) {
        if (candidate == null) return false;

        String packageName = safeLower(candidate.packageName);
        String title = safeLower(candidate.title);
        String text = safeLower(candidate.text);
        String category = safeLower(candidate.category);

        if (!candidate.isInternal && HARD_BLACKLIST.contains(packageName)) return false;
        if (candidate.isGroupSummary) return false;

        if (MESSAGING_ALLOWLIST.contains(packageName)) {
            return shouldDisplayMessaging(candidate, title, text);
        }

        if (candidate.hasProgress) return false;
        if ("progress".equals(category) || "transport".equals(category)
                || "service".equals(category) || "status".equals(category)) {
            return false;
        }

        if (candidate.hasReplyAction) return true;
        if (candidate.isInternal) return !isLowSignal(candidate, title, text);

        if (candidate.priority < Notification.PRIORITY_DEFAULT) {
            return false;
        }

        return true;
    }

    private boolean shouldDisplayMessaging(Candidate candidate, String title, String text) {
        if (title.length() == 0 && text.length() == 0) return false;
        if (GROUP_SUMMARY_PATTERN.matcher(text).matches() || GROUP_SUMMARY_PATTERN.matcher(title).matches()) {
            return false;
        }
        if (candidate.hasReplyAction) return true;
        return !isLowSignal(candidate, title, text);
    }

    private boolean isLowSignal(Candidate candidate, String title, String text) {
        if (title.length() == 0 && text.length() == 0) return true;

        String combined = (title + " " + text).trim();
        if (combined.length() == 0) {
            combined = normalize(candidate.renderedText);
        }
        combined = safeLower(combined);

        return combined.length() == 0
            || GROUP_SUMMARY_PATTERN.matcher(text).matches()
            || combined.contains("running in background")
            || combined.contains("tap to configure")
            || combined.contains("checking for updates")
            || combined.contains("update available")
            || combined.contains("syncing")
            || combined.contains("backup in progress");
    }

    private boolean isGroupSummary(Notification notification, String title, String text) {
        if (notification == null) return false;
        if (notification.flags != 0) {
            if ((notification.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return true;
        }
        return GROUP_SUMMARY_PATTERN.matcher(safeLower(text)).matches()
            || GROUP_SUMMARY_PATTERN.matcher(safeLower(title)).matches();
    }

    private boolean hasProgress(Notification notification) {
        if (notification == null || notification.extras == null) return false;
        return notification.extras.containsKey(Notification.EXTRA_PROGRESS)
            || notification.extras.containsKey(Notification.EXTRA_PROGRESS_MAX);
    }

    private boolean hasReplyAction(Notification notification) {
        if (notification == null || notification.actions == null) return false;
        for (Notification.Action action : notification.actions) {
            if (action != null && action.getRemoteInputs() != null && action.getRemoteInputs().length > 0) {
                return true;
            }
        }
        return false;
    }

    private String normalize(CharSequence value) {
        if (value == null) return "";
        return value.toString().trim();
    }

    private String safeLower(CharSequence value) {
        String normalized = normalize(value);
        if (TextUtils.isEmpty(normalized)) return "";
        return normalized.toLowerCase(Locale.US);
    }
}
