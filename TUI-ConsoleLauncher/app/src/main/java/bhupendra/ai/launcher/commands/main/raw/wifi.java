package bhupendra.ai.launcher.commands.main.raw;

import android.content.Context;
import android.net.wifi.WifiManager;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;

import bhupendra.ai.launcher.commands.CommandMetadata;
import java.util.ArrayList;
import java.util.List;

public class wifi implements CommandAbstraction {

    @Override
    public CommandMetadata getMetadata(Context context) {
        String description = "Toggle WiFi on or off.";
        List<String> examples = new ArrayList<>();
        examples.add("wifi");

        return new CommandMetadata("wifi", description, null, null, examples);
    }

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;
        if (info.getWifiManager() == null)
            info.wifi = (WifiManager) info.getContext().getSystemService(Context.WIFI_SERVICE);
        boolean active = !info.getWifiManager().isWifiEnabled();
        info.getWifiManager().setWifiEnabled(active);
        return info.getResources().getString(R.string.output_wifi) + " " + Boolean.toString(active);
    }

    @Override
    public int helpRes() {
        return R.string.help_wifi;
    }

    @Override
    public int[] argType() {
        return new int[0];
    }

    @Override
    public int priority() {
        return 2;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

}
