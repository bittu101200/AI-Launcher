package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
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
        String prompt = args.optString("prompt", "Choose an option");
        List<String> options = parseOptions(args);

        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null) return "[error: AI subsystem not available]";

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> selectionResult = new AtomicReference<>();

        // 1. Send the prompt to the terminal
        Tuils.sendOutput(Color.rgb(255, 165, 0), context, "[Choice Required] " + prompt, TerminalManager.CATEGORY_OUTPUT);

        // 2. Push options to the Suggestions Bar via the UI Manager
        // We'll implement this bridge in the next steps
        ai.requestUserChoice(prompt, options, (selection) -> {
            selectionResult.set(selection);
            latch.countDown();
        });

        // 3. Block the AI thread until the user taps a choice
        latch.await();

        return selectionResult.get();
    }

    private List<String> parseOptions(JSONObject args) throws Exception {
        List<String> options = new ArrayList<>();

        Object rawOptions = args.opt("options");
        if (rawOptions instanceof JSONArray) {
            JSONArray optionsJson = (JSONArray) rawOptions;
            for (int i = 0; i < optionsJson.length(); i++) {
                Object item = optionsJson.get(i);
                if (item instanceof JSONObject) {
                    JSONObject optionObject = (JSONObject) item;
                    String label = optionObject.optString("label",
                            optionObject.optString("title",
                                    optionObject.optString("value", null)));
                    if (label != null && label.trim().length() > 0) {
                        options.add(label.trim());
                    }
                } else if (item != null) {
                    String label = String.valueOf(item).trim();
                    if (label.length() > 0) options.add(label);
                }
            }
        } else if (rawOptions instanceof String) {
            String optionsString = ((String) rawOptions).trim();
            if (optionsString.startsWith("[") && optionsString.endsWith("]")) {
                JSONArray optionsJson = new JSONArray(optionsString);
                for (int i = 0; i < optionsJson.length(); i++) {
                    String label = optionsJson.optString(i, "").trim();
                    if (label.length() > 0) options.add(label);
                }
            } else if (optionsString.length() > 0) {
                for (String part : optionsString.split("\\s*,\\s*|\\s*\\|\\s*|\\s*/\\s*")) {
                    String label = part.trim();
                    if (label.length() > 0) options.add(label);
                }
            }
        }

        if (options.isEmpty()) {
            String yesNo = args.optString("choice", args.optString("choices", "")).trim();
            if (yesNo.length() > 0) {
                options.addAll(Arrays.asList(yesNo.split("\\s*,\\s*|\\s*\\|\\s*|\\s*/\\s*")));
            }
        }

        if (options.isEmpty()) {
            options.add("yes");
            options.add("no");
        }

        List<String> cleaned = new ArrayList<>();
        for (String option : options) {
            if (option == null) continue;
            String label = option.trim();
            if (label.length() > 0 && !cleaned.contains(label)) cleaned.add(label);
        }
        return cleaned;
    }
}
