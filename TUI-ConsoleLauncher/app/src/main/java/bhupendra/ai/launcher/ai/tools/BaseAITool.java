package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;

import java.util.Map;

import bhupendra.ai.launcher.ai.Tool;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.di.ManagerEntryPoint;
import dagger.hilt.EntryPoints;

public abstract class BaseAITool extends Tool {

    @Deprecated
    protected MainPack mainPack;

    public BaseAITool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    @Deprecated
    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    protected ManagerEntryPoint getEntryPoint(Context context) {
        return EntryPoints.get(context.getApplicationContext(), ManagerEntryPoint.class);
    }

    /**
     * Executes the tool logic.
     * @param context The application or activity context.
     * @param args The JSON object containing parsed arguments for this tool.
     * @return The string output to return to the AI.
     * @throws Exception If any error occurs during execution.
     */
    public abstract String execute(Context context, JSONObject args) throws Exception;
}