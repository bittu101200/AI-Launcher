package bhupendra.ai.launcher.managers.notifications.reply;

import android.app.PendingIntent;
import android.app.RemoteInput;
import android.os.Bundle;

/**
 * Created by francescoandreuzzi on 24/01/2018.
 */

public class NotificationWear {

    public PendingIntent pendingIntent;
    public RemoteInput[] remoteInputs;
    public Bundle bundle;
    public int id;

    public CharSequence text;

    @Override
    public boolean equals(Object obj) {
        try {
            NotificationWear h = (NotificationWear) obj;
            return h.pendingIntent.equals(pendingIntent);
        } catch (Exception e) {
            return false;
        }
    }
}
