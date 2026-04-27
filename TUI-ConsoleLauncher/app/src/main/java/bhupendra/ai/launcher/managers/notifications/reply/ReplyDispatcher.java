package bhupendra.ai.launcher.managers.notifications.reply;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcelable;

import bhupendra.ai.launcher.tuils.Tuils;

public final class ReplyDispatcher {
    private ReplyDispatcher() {}

    public static boolean dispatch(Context context, CharSequence text, Bundle bundle, Parcelable[] remoteInputs,
                                   PendingIntent pendingIntent, int requestCode) {
        if (bundle == null) {
            Tuils.sendOutput(Color.RED, context, "The bundle is null");
            return false;
        }

        if (remoteInputs == null || remoteInputs.length == 0) {
            Tuils.sendOutput(Color.RED, context, "No remote inputs");
            return false;
        }

        if (pendingIntent == null) {
            Tuils.sendOutput(Color.RED, context, "The pending intent couldn't be found");
            return false;
        }

        android.app.RemoteInput[] inputs = new android.app.RemoteInput[remoteInputs.length];
        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = (android.app.RemoteInput) remoteInputs[i];
        }

        Intent replyIntent = new Intent();
        replyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        for (android.app.RemoteInput input : inputs) {
            bundle.putCharSequence(input.getResultKey(), text);
        }

        android.app.RemoteInput.addResultsToIntent(inputs, replyIntent, bundle);
        try {
            pendingIntent.send(context.getApplicationContext(), requestCode, replyIntent);
            return true;
        } catch (PendingIntent.CanceledException e) {
            Tuils.sendOutput(Color.RED, context, e.toString());
            Tuils.log(e);
            return false;
        }
    }
}
