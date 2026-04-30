package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemExecuteCommandTool extends BaseAITool {

    private static final String TAG = "SystemExecuteCommandTool";
    private MainPack mainPack;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SystemExecuteCommandTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            
            final String commandLine;
            String rawCmd = args.optString("command", null);
            if (rawCmd == null) {
                if ("system.backup".equals(this.name)) {
                    rawCmd = "backup";
                } else if ("system.restore".equals(this.name)) {
                    rawCmd = "restore";
                }
            }
            commandLine = rawCmd;
            
            if (commandLine == null) return "[error: command is required]";
            
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null) return "[error: mainPack not available]";
            if (mainPack.commandController == null) return "[error: commandController not available]";

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> resultRef = new AtomicReference<>();
            final AtomicReference<Exception> errorRef = new AtomicReference<>();

            mainHandler.post(() -> {
                try {
                    String validationError = mainPack.commandController.validateAICommand(commandLine);
                    if (validationError != null && validationError.trim().length() > 0) {
                        resultRef.set("[command failed before execution: " + validationError + "]");
                        return;
                    }

                    String requestId = UUID.randomUUID().toString();
                    boolean dispatched = mainPack.commandController.onAICommand(commandLine, requestId);
                    if (dispatched) {
                        resultRef.set("[dispatched: " + commandLine + "]");
                    } else {
                        resultRef.set("[not dispatched: no launcher command, alias, app, or group matched '" + commandLine + "']");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Command execution failed on main thread", e);
                    errorRef.set(e);
                } finally {
                    latch.countDown();
                }
            });

            latch.await();

            if (errorRef.get() != null) {
                throw errorRef.get();
            }

            return resultRef.get();
    }
}
