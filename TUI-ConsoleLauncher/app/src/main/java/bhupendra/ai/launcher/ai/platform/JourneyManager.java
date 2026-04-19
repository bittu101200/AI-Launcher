package bhupendra.ai.launcher.ai.platform;

import android.app.Notification;
import android.os.Build;

public class JourneyManager {

    public boolean isJourneyNotification(Notification notification) {
        if (Build.VERSION.SDK_INT >= 36 && notification != null && notification.extras != null) {
            return notification.extras.containsKey(Notification.EXTRA_PROGRESS);
        }
        return false;
    }
}