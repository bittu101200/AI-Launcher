package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class SystemRequestUserChoiceTool extends BaseAITool {

    private static final String TAG = "UserChoiceTool";

    public SystemRequestUserChoiceTool() {
        super("system.request_user_choice",
              "Present a set of options to the user via the Suggestions Bar and wait for their selection. Use this for disambiguation or multi-step decisions.",
              createParams(),
              ToolRiskClass.SENSITIVE);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("prompt", "The question to ask the user (displayed in terminal).");
        params.put("options", "A JSON array of strings representing the choices (displayed in suggestions bar).");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String prompt = args.getString("prompt");
        JSONArray optionsJson = args.getJSONArray("options");
        List<String> options = new ArrayList<>();
        for (int i = 0; i < optionsJson.length(); i++) {
            options.add(optionsJson.getString(i));
        }

        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null) return "[error: AI subsystem not available]";

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> selectionResult = new AtomicReference<>();

        // 1. Send the prompt to the terminal
        Tuils.sendOutput(context, "[Choice Required] " + prompt);

        // 2. Push options to the Suggestions Bar via the UI Manager
        // We'll implement this bridge in the next steps
        ai.requestUserChoice(options, (selection) -> {
            selectionResult.set(selection);
            latch.countDown();
        });

        // 3. Block the AI thread until the user taps a choice
        latch.await();

        return selectionResult.get();
    }
}
