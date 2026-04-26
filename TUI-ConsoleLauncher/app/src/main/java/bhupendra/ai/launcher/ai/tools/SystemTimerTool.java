package bhupendra.ai.launcher.ai.tools;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;

import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemTimerTool extends BaseAITool {

    public SystemTimerTool() {
        super("system.timer",
              "Set a countdown timer or a specific clock alarm.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("action", "Mandatory: 'timer' to start a countdown, or 'alarm' to set a specific time of day.");
        params.put("duration_seconds", "Required for 'timer': total seconds for the countdown.");
        params.put("hour", "Required for 'alarm': hour of the day (0-23).");
        params.put("minutes", "Required for 'alarm': minute of the hour (0-59).");
        params.put("label", "Optional: a descriptive name for the timer or alarm.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String action = args.getString("action").toLowerCase();
        String label = args.optString("label", "AI Task");

        if ("timer".equals(action)) {
            int seconds = args.getInt("duration_seconds");
            Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Timer started for " + seconds + " seconds: " + label;
        } else if ("alarm".equals(action)) {
            int hour = args.getInt("hour");
            int minutes = args.getInt("minutes");
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, hour)
                    .putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                    .putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return "Alarm set for " + String.format("%02d:%02d", hour, minutes) + ": " + label;
        } else {
            return "[error: invalid action. Use 'timer' or 'alarm']";
        }
    }
}
