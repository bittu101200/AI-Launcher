package bhupendra.ai.launcher.ai.tools;

import android.content.Context;

import org.json.JSONObject;

import java.util.Collections;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.AppsManager;

public class SystemLaunchAppTool extends BaseAITool {

    private MainPack mainPack;

    public SystemLaunchAppTool() {
        super("system.launch_app",
              "Launch an installed app by name or package. Use this for any request to open or start an app.",
              Collections.singletonMap("query", "App name, package name, or launch label"),
              ToolRiskClass.LAUNCH_ONLY);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String query = args.optString("query", null);
        if (query == null || query.trim().isEmpty()) {
            return "[error: query is required]";
        }

        if (mainPack == null) {
            AISubsystem ai = AISubsystem.getInstance();
            if (ai != null) mainPack = ai.getMainPack();
        }

        if (mainPack == null || mainPack.appsManager == null) {
            return "[error: apps manager not available]";
        }

        AppsManager.LaunchInfo info = mainPack.appsManager.findLaunchInfo(query);
        if (info == null) {
            return "[error: app not found for query: " + query + "]";
        }

        if (mainPack.commandController != null) {
            mainPack.commandController.performLaunch(mainPack, info, query);
        } else {
            context.startActivity(mainPack.appsManager.getIntent(info));
        }

        return "[launched " + info.publicLabel + "]";
    }
}
