package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.util.LinkedHashMap;
import java.util.Map;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.tuils.system.BeepPlayer;

public class SystemBeepTool extends BaseAITool {

    private static final String TAG = "SystemBeepTool";

    public SystemBeepTool() {
        super("system.beep",
              "Trigger an adjustable audible alert for user attention. Supports safe pitch, duration, volume, repeats, and urgent repeat-until-acknowledged alerts.",
              createParams(),
              ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("action", "Optional: 'play' or 'stop'. Use 'stop' to cancel a repeating urgent alert. Default: 'play'.");
        params.put("urgency", "Optional: 'normal' or 'urgent'. Urgent defaults to longer, higher-pitch repeating beeps.");
        params.put("pitch_hz", "Optional: beep pitch in Hz. Safe range is clamped to 500-4500. Higher values are more attention-grabbing.");
        params.put("duration_ms", "Optional: duration of each beep pulse. Safe range is clamped to 50-1500.");
        params.put("volume", "Optional: perceived volume from 0.0-1.0. Internally capped to a speaker-safe maximum.");
        params.put("repeat_count", "Optional: number of pulses per alert cycle. Safe range is clamped to 1-24.");
        params.put("gap_ms", "Optional: silence between pulses/cycles in milliseconds. Safe range is clamped to 25-5000.");
        params.put("repeat_until_ack", "Optional boolean: repeat urgent alert cycles until the user enters a launcher command or action='stop' is called.");
        params.put("max_total_ms", "Optional: safety cap for repeat_until_ack. Maximum is 300000 ms.");
        params.put("force_audible", "Optional boolean: for urgent alerts only, use alarm audio and temporarily raise alarm volume so sound works even if media/ring volume is muted.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        try {
            String action = args.optString("action", "play").toLowerCase();
            if ("stop".equals(action)) {
                BeepPlayer.stopRepeatingAlert();
                return "[Beep stopped]";
            }
            if (!"play".equals(action)) {
                return "[error: action must be 'play' or 'stop']";
            }

            String urgency = args.optString("urgency", "normal").toLowerCase();
            boolean urgent = "urgent".equals(urgency);
            boolean repeatUntilAck = args.optBoolean("repeat_until_ack", urgent);
            boolean forceAudible = args.optBoolean("force_audible", urgent || repeatUntilAck);
            int defaultPitchHz = urgent ? 3900 : 3200;
            int defaultDurationMs = urgent ? 350 : 180;
            int defaultRepeatCount = urgent ? 5 : 3;
            int defaultGapMs = urgent ? 120 : 55;
            float defaultVolume = urgent ? 0.75f : 0.60f;
            int defaultMaxTotalMs = repeatUntilAck ? 120000 : 30000;

            BeepPlayer.Options options = new BeepPlayer.Options(
                    args.optInt("pitch_hz", defaultPitchHz),
                    args.optInt("duration_ms", defaultDurationMs),
                    args.optInt("gap_ms", defaultGapMs),
                    args.optInt("repeat_count", defaultRepeatCount),
                    (float) args.optDouble("volume", defaultVolume),
                    repeatUntilAck,
                    args.optInt("max_total_ms", defaultMaxTotalMs),
                    forceAudible);

            BeepPlayer.playAlert(context, options);
            Log.d(TAG, "Audible beep triggered successfully");
            return "[Beep sounded: urgency=" + urgency + ", repeat_until_ack=" + repeatUntilAck + ", force_audible=" + forceAudible + "]";
        } catch (Exception e) {
            Log.e(TAG, "Failed to play beep tone", e);
            return "[error: could not trigger audio beep: " + e.getMessage() + "]";
        }
    }
}
