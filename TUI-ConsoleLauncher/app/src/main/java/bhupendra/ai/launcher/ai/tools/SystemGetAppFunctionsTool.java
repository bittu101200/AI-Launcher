package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import org.json.JSONObject;
import java.util.Collections;
import bhupendra.ai.launcher.ai.AppCapabilityScanner;
import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemGetAppFunctionsTool extends BaseAITool {

    public SystemGetAppFunctionsTool() {
        super("system.get_app_functions",
              "Retrieve the full Dynamic Library of integrated App Functions (shortcuts). Use this to see what tasks you can execute inside other apps without searching.",
              Collections.emptyMap(),
              ToolRiskClass.READ_ONLY);
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
        String content = AppCapabilityScanner.getLibraryContent(context);
        if (content.equals("[]")) {
            return "[The Dynamic Library is empty. User must run the 'integrate' command first to harvest App Functions.]";
        }
        return "DYNAMIC CAPABILITY LIBRARY:\n" + content;
    }
}
