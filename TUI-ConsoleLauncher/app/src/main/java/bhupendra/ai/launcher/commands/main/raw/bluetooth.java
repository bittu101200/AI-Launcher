package bhupendra.ai.launcher.commands.main.raw;

import android.bluetooth.BluetoothAdapter;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;

import bhupendra.ai.launcher.commands.CommandMetadata;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;

public class bluetooth implements CommandAbstraction {

    @Override
    public CommandMetadata getMetadata(Context context) {
        String description = "Toggle Bluetooth on or off.";
        List<String> examples = new ArrayList<>();
        examples.add("bluetooth");

        return new CommandMetadata("bluetooth", description, null, null, examples);
    }

    @Override
    public String exec(ExecutePack pack) {
        MainPack info = (MainPack) pack;

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if(adapter == null) return info.getContext().getString(R.string.output_bluetooth_unavailable);

        if(adapter.isEnabled()) {
            adapter.disable();
            return info.getContext().getString(R.string.output_bluetooth) + " false";
        } else {
            adapter.enable();
            return info.getContext().getString(R.string.output_bluetooth) + " true";
        }
    }

    @Override
    public int helpRes() {
        return R.string.help_bluetooth;
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
