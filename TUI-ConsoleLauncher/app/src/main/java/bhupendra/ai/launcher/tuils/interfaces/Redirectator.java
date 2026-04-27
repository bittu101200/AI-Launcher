package bhupendra.ai.launcher.tuils.interfaces;

import bhupendra.ai.launcher.commands.main.specific.RedirectCommand;

/**
 * Created by francescoandreuzzi on 03/03/2017.
 */

public interface Redirectator {

    void prepareRedirection(RedirectCommand cmd);
    void cleanup();
    RedirectCommand getRedirect();
}
