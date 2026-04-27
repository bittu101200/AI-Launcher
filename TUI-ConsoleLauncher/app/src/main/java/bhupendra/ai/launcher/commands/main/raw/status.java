package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.managers.DeviceStateManager;
import bhupendra.ai.launcher.managers.TextProcessor;
import bhupendra.ai.launcher.tuils.Tuils;

public class status implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        StringBuilder builder = new StringBuilder();
        builder
                .append(pack.getResources().getString(R.string.battery_label)).append(Tuils.SPACE).append(DeviceStateManager.getBatteryLevel(pack.getContext())).append("%").append(Tuils.NEWLINE)
                .append(pack.getResources().getString(R.string.wifi_label)).append(Tuils.SPACE).append(DeviceStateManager.isWifiConnected(pack.getContext())).append(Tuils.NEWLINE)
                .append(pack.getResources().getString(R.string.mobile_data_label)).append(Tuils.SPACE).append(DeviceStateManager.isMobileDataEnabled(pack.getContext())).append(Tuils.NEWLINE)
                .append(pack.getResources().getString(R.string.bluetooth_label)).append(Tuils.SPACE).append(DeviceStateManager.isBluetoothEnabled()).append(Tuils.NEWLINE)
                .append(pack.getResources().getString(R.string.location_label)).append(Tuils.SPACE).append(DeviceStateManager.isLocationEnabled(pack.getContext())).append(Tuils.NEWLINE)
                .append(pack.getResources().getString(R.string.brightness_label)).append(Tuils.SPACE).append(DeviceStateManager.isAutoBrightnessEnabled(pack.getContext()) ? "(auto) " : Tuils.EMPTYSTRING).append(DeviceStateManager.getBrightnessPercentage(pack.getContext())).append("%");

        return builder.toString();
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
        return R.string.help_status;
    }

    @Override
    public String onArgNotFound(ExecutePack info, int index) {
        return null;
    }

    @Override
    public String onNotArgEnough(ExecutePack info, int nArgs) {
        return null;
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("status", "Get info about battery charge, wifi status and mobile data", null, null, null);
    }
}
