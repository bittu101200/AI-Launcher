package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.util.Log;
import org.json.JSONObject;
import java.util.Collections;

import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemBeepTool extends BaseAITool {

    private static final String TAG = "SystemBeepTool";

    public SystemBeepTool() {
        super("system.beep",
              "Trigger an audible alert. Use this to get the user's attention when a task is finished or a decision is needed.",
              Collections.emptyMap(),
              ToolRiskClass.STATE_CHANGING);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        try {
            // Use ToneGenerator directly for immediate audible feedback
            // Volume set to 100 (max) for STREAM_NOTIFICATION
            ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
            
            // TONE_CDMA_PIP is a sharp, clear double-beep often used for alerts
            toneG.startTone(ToneGenerator.TONE_CDMA_PIP, 300);
            
            Log.d(TAG, "Audible beep triggered successfully");
            return "[Beep sounded]";
        } catch (Exception e) {
            Log.e(TAG, "Failed to play beep tone", e);
            return "[error: could not trigger audio beep: " + e.getMessage() + "]";
        }
    }
}
