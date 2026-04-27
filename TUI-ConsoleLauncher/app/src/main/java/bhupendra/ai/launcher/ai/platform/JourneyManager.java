package bhupendra.ai.launcher.ai.platform;

import android.app.Notification;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.util.HashSet;
import java.util.Set;

public class JourneyManager {

    private static final String TAG = "JourneyManager";
    
    // Packages that are known to be noisy or low-priority for terminal display
    private static final Set<String> DISPLAY_BLACKLIST = new HashSet<>();
    static {
        DISPLAY_BLACKLIST.add("com.apple.android.music");
        DISPLAY_BLACKLIST.add("com.spotify.music");
        DISPLAY_BLACKLIST.add("com.google.android.apps.youtube.music");
        DISPLAY_BLACKLIST.add("com.bluetooth.aptxmode");
        DISPLAY_BLACKLIST.add("com.google.android.gms"); // General play services noise
        DISPLAY_BLACKLIST.add("android"); // System noise
        DISPLAY_BLACKLIST.add("com.android.systemui");
        DISPLAY_BLACKLIST.add("com.android.vending"); // Play Store updates
        DISPLAY_BLACKLIST.add("org.fdroid.fdroid");
    }

    public boolean isJourneyNotification(Notification notification) {
        if (notification == null) return false;

        // Modern Android progress check
        if (notification.extras != null) {
            if (notification.extras.containsKey(Notification.EXTRA_PROGRESS) ||
                notification.extras.containsKey(Notification.EXTRA_PROGRESS_MAX)) {
                return true;
            }
        }
        
        // Categorical noise
        if (Notification.CATEGORY_PROGRESS.equals(notification.category) ||
            Notification.CATEGORY_TRANSPORT.equals(notification.category) ||
            Notification.CATEGORY_SERVICE.equals(notification.category) ||
            Notification.CATEGORY_STATUS.equals(notification.category)) {
            return true;
        }

        return false;
    }

    public void processNotification(StatusBarNotification sbn) {
        if (sbn != null) {
            Log.d(TAG, "Notification processed: " + sbn.getPackageName());
        }
    }
}
