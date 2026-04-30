package bhupendra.ai.launcher.ai.tools;

import android.content.Context;

import org.json.JSONObject;

import java.util.Collections;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemLoadSkillTool extends BaseAITool {

    public SystemLoadSkillTool() {
        super("system.load_skill",
                "Load the full instructions for a previously saved task skill when the current task matches its trigger.",
                Collections.singletonMap("query", "Current task or skill name to match"),
                ToolRiskClass.READ_ONLY);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        AISubsystem ai = AISubsystem.getInstance();
        if (ai == null || ai.getSkillRegistry() == null) {
            return "[error: skill registry not available]";
        }
        return ai.getSkillRegistry().loadSkill(args.optString("query", ""));
    }
}
