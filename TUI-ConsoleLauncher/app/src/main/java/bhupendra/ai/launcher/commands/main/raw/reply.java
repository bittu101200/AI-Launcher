package bhupendra.ai.launcher.commands.main.raw;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.notifications.reply.ReplyManager;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 05/11/2017.
 */

public class reply extends ParamCommand {

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        to {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.BOUND_REPLY_APP, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = new Intent(ReplyManager.ACTION);

                // Use the string directly from pack (which is the package name from suggestion)
                intent.putExtra(ReplyManager.ID, pack.getString());
                intent.putExtra(ReplyManager.WHAT, pack.getString());

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(intent);
                return null;
            }
        },
        tutorial {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.getContext().startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Reply"));
                return null;
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps)
                if (p.endsWith(p1.label()))
                    return p1;
            return null;
        }

        static String[] labels() {
            Param[] ps = values();
            String[] ss = new String[ps.length];

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.getContext().getString(R.string.help_reply);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.getContext().getString(R.string.help_reply);
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_reply;
    }


    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("reply", "-to [ID or packageName] [what] -> reply to the last notification from the application bound with the given ID", null, null, null);
    }
}
