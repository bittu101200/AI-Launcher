package bhupendra.ai.launcher.ai.tools;

import android.content.Context;

import org.json.JSONObject;

import java.util.Collections;

import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.InternetSpeedTestManager;

public class SystemSpeedTestTool extends BaseAITool {

    public SystemSpeedTestTool() {
        super(
                "system.speed_test",
                "Run an internet speed test and return latency, download, and upload measurements.",
                Collections.emptyMap(),
                ToolRiskClass.READ_ONLY
        );
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        InternetSpeedTestManager.Result result = InternetSpeedTestManager.run();
        return result.toJson().toString();
    }
}
