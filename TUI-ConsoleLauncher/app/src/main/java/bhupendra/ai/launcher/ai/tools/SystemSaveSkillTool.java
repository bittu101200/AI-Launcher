package bhupendra.ai.launcher.ai.tools;

import android.content.Context;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemSaveSkillTool extends BaseAITool {

    public SystemSaveSkillTool() {
        super("system.save_skill",
                "Persist a reusable task skill when the user teaches a repeatable workflow or says to remember how to do a task.",
                createParams(),
                ToolRiskClass.STATE_CHANGING);
    }

    private static Map<String, String> createParams() {
        Map<String, String> params = new HashMap<>();
        params.put("name", "Short stable skill name, for example subtitle_pipeline.");
        params.put("trigger", "Concise condition for when this skill should be loaded again.");
        params.put("instructions", "The full reusable procedure, constraints, commands, and preferences.");
        return params;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null || ai.getSkillRegistry() == null) {
            return "[error: skill registry not available]";
        }

        String name = args.optString("name", "");
        String trigger = args.optString("trigger", "");
        String instructions = args.optString("instructions", "");
        ai.getSkillRegistry().store(name, trigger, instructions);
        ai.invalidatePromptCache();
        return "[skill saved: " + name + "]";
    }
}
