package bhupendra.ai.launcher.managers;

import android.app.Activity;
import android.content.Context;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;

public final class ConfigChangeHandler {

    private ConfigChangeHandler() {}

    public static void apply(Context context, XMLPrefsSave save) {
        if (save == null) return;

        if (save.parent() == XMLPrefsManager.XMLPrefsRoot.AI) {
            AISubsystem ai = AISubsystem.getInstance();
            if (ai != null) ai.refresh();
            return;
        }

        if (!(context instanceof Reloadable)) return;

        Reloadable reloadable = (Reloadable) context;
        Runnable applyChange = () -> {
            if (!reloadable.applyLiveConfigChange(save.label())) {
                reloadable.reload();
            }
        };

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(applyChange);
        } else {
            applyChange.run();
        }
    }
}
