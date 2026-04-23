package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;

import java.util.Map;

import bhupendra.ai.launcher.ai.Tool;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.commands.main.MainPack;

public abstract class BaseAITool extends Tool {

    public BaseAITool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {}

    /**
     * Executes the tool logic.
     * @param context The application or activity context.
     * @param args The JSON object containing parsed arguments for this tool.
     * @return The string output to return to the AI.
     * @throws Exception If any error occurs during execution.
     */
    public abstract String execute(Context context, JSONObject args) throws Exception;
}