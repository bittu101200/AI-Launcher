package bhupendra.ai.launcher.commands.main.raw;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.AlarmClock;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import bhupendra.ai.launcher.LauncherActivity;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;

public class timer implements CommandAbstraction {
    @Override
    public String exec(ExecutePack pack) throws Exception {
        if (ContextCompat.checkSelfPermission(pack.context, Manifest.permission.SET_ALARM) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) pack.context, new String[]{Manifest.permission.SET_ALARM}, LauncherActivity.COMMAND_REQUEST_PERMISSION);
            return pack.context.getString(R.string.output_waitingpermission);
        }
        ArrayList<String> args = pack.getList();
        if (args.size() < 1) {
            return pack.context.getString(R.string.timer_arg_error);
        }

        String time = args.get(0);
        char lastChar = Character.toLowerCase(time.charAt(time.length()-1));
        String substring = time.substring(0, time.length() - 1);
        String invalidFormat = String.format(pack.context.getString(R.string.timer_format_error), substring);

        int seconds;
        try {
            seconds = Integer.parseInt(substring);
        } catch (NumberFormatException e) {
            return invalidFormat;
        }
        if (lastChar == 's') {
            // seconds = seconds;
        } else if (lastChar == 'm') {
            seconds = seconds * 60;
        } else if (lastChar == 'h') {
            seconds = seconds * 3600;
        } else {
            return invalidFormat;
        }

        if (seconds > (60 * 60 * 24)) {
            return pack.context.getString(R.string.timer_limit_error);
        }
        
        String alarmTitle = pack.context.getString(R.string.timer_default_title);
        if (args.size() == 2) {
            alarmTitle = args.get(1);
        }
        Intent timerIntent = new Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                .putExtra(AlarmClock.EXTRA_MESSAGE, alarmTitle)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true);
        pack.context.startActivity(timerIntent);
        return String.format(pack.context.getString(R.string.timer_started), time);
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.TEXTLIST};
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public int helpRes() {
        return R.string.help_timer;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int indexNotFound) {
        return pack.context.getString(R.string.output_appnotfound);
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return pack.context.getString(helpRes());
    }
}
