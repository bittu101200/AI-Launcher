package bhupendra.ai.launcher.managers.notifications.reply;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.os.Bundle;

/**
 * Created by francescoandreuzzi on 24/01/2018.
 */

public class NotificationWear {

    public BoundApp app;

    public PendingIntent pendingIntent;
    public RemoteInput[] remoteInputs;
    public Bundle bundle;
    public int id;

    public CharSequence text;
    public String title;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof NotificationWear)) return false;
        NotificationWear h = (NotificationWear) obj;
        if (app == null || h.app == null) return false;
        if (!app.packageName.equals(h.app.packageName)) return false;
        if (title == null) return h.title == null;
        return title.equalsIgnoreCase(h.title);
    }

    @Override
    public int hashCode() {
        int result = app != null ? app.packageName.hashCode() : 0;
        result = 31 * result + (title != null ? title.toLowerCase().hashCode() : 0);
        return result;
    }
}