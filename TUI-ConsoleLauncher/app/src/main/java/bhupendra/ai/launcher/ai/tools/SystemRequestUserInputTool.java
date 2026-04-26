package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.graphics.Color;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.tuils.Tuils;

public class SystemRequestUserInputTool extends BaseAITool {

    public SystemRequestUserInputTool() {
        super("system.request_user_input",
              "Prompt the user to enter a free-form value using the suggestions bar parameter mode.",
              createParams(),
              ToolRiskClass.SENSITIVE);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("field_name", "Short label for the field being requested, for example 'sms_message'.");
        params.put("prompt", "User-facing prompt shown in the terminal and input hint.");
        params.put("prefill", "Optional prefilled value.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String fieldName = args.optString("field_name", "value");
        String prompt = args.optString("prompt", "Enter a value");
        String prefill = args.optString("prefill", "");

        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null) return "[error: AI subsystem not available]";

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<String> inputResult = new AtomicReference<>();

        Tuils.sendOutput(Color.GRAY, context, "[Input Required] " + prompt, TerminalManager.CATEGORY_OUTPUT);

        ai.requestToolParameter(fieldName, prompt, prefill, (selection) -> {
            inputResult.set(selection);
            latch.countDown();
        });

        latch.await();

        String result = inputResult.get();
        return result != null ? result : "[user cancelled]";
    }
}
