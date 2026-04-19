package bhupendra.ai.launcher.ai;

import android.content.Context;

public interface ToolExecutor {
    String execute(Context context, Tool tool, String arguments) throws Exception;
}
