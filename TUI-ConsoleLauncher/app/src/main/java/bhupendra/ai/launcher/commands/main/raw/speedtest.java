package bhupendra.ai.launcher.commands.main.raw;

import java.util.Arrays;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.managers.InternetSpeedTestManager;

public class speedtest implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        InternetSpeedTestManager.Result result = InternetSpeedTestManager.run();
        return InternetSpeedTestManager.format(result);
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public int helpRes() {
        return R.string.help_speedtest;
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int nArgs) {
        return null;
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata(
                "speedtest",
                "Run an internet speed test and report latency, download, and upload throughput.",
                null,
                null,
                Arrays.asList("speedtest"),
                Arrays.asList("internet", "network", "latency", "download", "upload"),
                null
        );
    }
}
