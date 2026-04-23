package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;

public class username implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) {
        String newUser = pack.getString();
        String newDevice = pack.getString();

        if (newUser == null || newDevice == null) {
            return onNotArgEnough(pack, 0);
        }

        XMLPrefsManager.XMLPrefsRoot.UI.write(Ui.username, newUser);
        XMLPrefsManager.XMLPrefsRoot.UI.write(Ui.deviceName, newDevice);

        try {
            if (pack.getContext() instanceof Reloadable) {
                ((Reloadable) pack.getContext()).reload();
            }
        } catch (Exception e) {}

        return "Username and Device updated!";
    }

    @Override
    public int[] argType() {
        return new int[] {CommandAbstraction.NO_SPACE_STRING, CommandAbstraction.NO_SPACE_STRING};
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_username;
    }

    @Override
    public String onNotArgEnough(ExecutePack pack, int n) {
        return pack.getContext().getString(R.string.help_username);
    }

    @Override
    public String onArgNotFound(ExecutePack pack, int index) {
        return null;
    }
}
