package bhupendra.ai.launcher.tuils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.core.app.RemoteInput;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import bhupendra.ai.launcher.BuildConfig;
import bhupendra.ai.launcher.MainManager;
import bhupendra.ai.launcher.managers.notifications.reply.ReplyDispatcher;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.interfaces.Inputable;
import bhupendra.ai.launcher.tuils.interfaces.Outputable;

/**
 * Created by francescoandreuzzi on 18/08/2017.
 */

public class PrivateIOReceiver extends BroadcastReceiver {

//    this class handles incoming intent to
//    process input
//    show custom output

    public static final String ACTION_OUTPUT = BuildConfig.APPLICATION_ID + ".action_output";
    public static final String ACTION_INPUT = BuildConfig.APPLICATION_ID + ".action_input";
    public static final String ACTION_REPLY = BuildConfig.APPLICATION_ID + ".action_reply";

    public static final String TEXT = BuildConfig.APPLICATION_ID + ".text";
    public static final String TYPE = BuildConfig.APPLICATION_ID + ".type";
    public static final String COLOR = BuildConfig.APPLICATION_ID + ".color";
    public static final String ACTION = BuildConfig.APPLICATION_ID + ".action";
    public static final String LONG_ACTION = BuildConfig.APPLICATION_ID + ".longaction";
    public static final String REMOTE_INPUTS = BuildConfig.APPLICATION_ID + ".remote_inputs";
    public static final String BUNDLE = BuildConfig.APPLICATION_ID + ".bundle";
    public static final String PENDING_INTENT = BuildConfig.APPLICATION_ID + ".pending_intent";
    public static final String ID = BuildConfig.APPLICATION_ID + ".id";
    public static final String INFO_AREA = BuildConfig.APPLICATION_ID + ".info_area";

    Outputable outputable;
    Inputable inputable;

    Activity activity;

    public PrivateIOReceiver(Activity activity, Outputable outputable, Inputable inputable) {
        this.outputable = outputable;
        this.inputable = inputable;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
        if(remoteInput == null || remoteInput.size() == 0) {
            CharSequence text = intent.getCharSequenceExtra(TEXT);
            if(text == null) text = intent.getStringExtra(TEXT);
            if(text == null) return;

            if(intent.getAction().equals(ACTION_OUTPUT)) {
                boolean infoArea = intent.getBooleanExtra(INFO_AREA, false);
                int color = intent.getIntExtra(COLOR, Integer.MAX_VALUE);

                Object singleClickExtraObject, longClickExtraObject;

                singleClickExtraObject = intent.getStringExtra(ACTION);
                longClickExtraObject = intent.getStringExtra(LONG_ACTION);

                if(singleClickExtraObject == null) singleClickExtraObject = intent.getParcelableExtra(ACTION);
                if(longClickExtraObject == null) longClickExtraObject = intent.getParcelableExtra(LONG_ACTION);

                if(singleClickExtraObject != null || longClickExtraObject != null) {
                    text = new SpannableStringBuilder(text);
                    ((SpannableStringBuilder) text).setSpan(new LongClickableSpan(singleClickExtraObject, longClickExtraObject), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                if(color != Integer.MAX_VALUE) outputable.onOutput(color, text);
                else {
                    int type = intent.getIntExtra(TYPE, -1);
                    if(type != -1) outputable.onOutput(text, type);
                    else outputable.onOutput(text, TerminalManager.CATEGORY_OUTPUT);
                }
            }
            else if(intent.getAction().equals(ACTION_INPUT)) {
                inputable.in(text.toString());
            } else if(intent.getAction().equals(ACTION_REPLY)) {
                Bundle b = intent.getBundleExtra(BUNDLE);
                Parcelable[] ps = intent.getParcelableArrayExtra(REMOTE_INPUTS);
                PendingIntent pi = intent.getParcelableExtra(PENDING_INTENT);
                int id = intent.getIntExtra(ID, 0);
                ReplyDispatcher.dispatch(context, text, b, ps, pi, id);
            }
        } else {
            String cmd = remoteInput.getString(TEXT);
            Intent i = new Intent(MainManager.ACTION_EXEC);
            i.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
            i.putExtra(MainManager.CMD, cmd);
            LocalBroadcastManager.getInstance(context.getApplicationContext()).sendBroadcast(i);
        }
    }
}
