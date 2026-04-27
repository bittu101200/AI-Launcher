package bhupendra.ai.launcher.ai.tools;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRiskClass;

public class SystemAddAliasTool extends BaseAITool {

    private static final String TAG = "SystemAddAliasTool";
    private MainPack mainPack;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SystemAddAliasTool(String name, String description, Map<String, String> parameters, ToolRiskClass riskClass) {
        super(name, description, parameters, riskClass);
    }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
    }

    @Override
    public String execute(Context context, JSONObject args) throws Exception {
            final String name = args.optString("name", null);
            final String value = args.optString("value", null);
            
            if (name == null || value == null) return "[error: name and value are required]";
            
            if (mainPack == null) {
                AISubsystem ai = AISubsystem.getInstance();
                if (ai != null) mainPack = ai.getMainPack();
            }
            
            if (mainPack == null) return "[error: mainPack not available]";
            if (mainPack.aliasManager == null) return "[error: aliasManager not available]";

            final CountDownLatch latch = new CountDownLatch(1);
            final AtomicReference<String> resultRef = new AtomicReference<>();
            final AtomicReference<Exception> errorRef = new AtomicReference<>();

            mainHandler.post(() -> {
                try {
                    mainPack.aliasManager.add(context, name, value);
                    resultRef.set("[alias added: " + name + " -> " + value + "]");
                } catch (Exception e) {
                    Log.e(TAG, "Alias creation failed", e);
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
