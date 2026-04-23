package bhupendra.ai.launcher.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.json.JSONObject;

import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.tools.BaseAITool;

public class AndroidToolExecutor implements ToolExecutor {

    private static final String TAG = "AndroidToolExecutor";
    private MainPack mainPack;

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, Tool tool, String arguments) throws Exception {
        Log.d(TAG, "Executing tool: " + tool.name + " with args: " + arguments);
        
        if (tool.name.startsWith("launch:")) {
            String packageName = tool.name.substring("launch:".length());
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent == null) {
                return "[package not found: " + packageName + "]";
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
            return "[launched " + tool.description + "]";
        }

        if (tool instanceof BaseAITool) {
            BaseAITool baseTool = (BaseAITool) tool;
            baseTool.setMainPack(mainPack);
            JSONObject args = new JSONObject(arguments);
            return baseTool.execute(context, args);
        }

        throw new IllegalArgumentException("Unsupported tool type or name: " + tool.name);
    }
}
