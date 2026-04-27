package bhupendra.ai.launcher.commands.main.raw;

import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.NotesManager;
import bhupendra.ai.launcher.tuils.Tuils;

import bhupendra.ai.launcher.commands.CommandMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.content.Context;

/**
 * Created by francescoandreuzzi on 12/02/2018.
 */

public class notes extends ParamCommand {

    @Override
    public CommandMetadata getMetadata(Context context) {
        String description = "Manage simple notes.";
        Map<String, String> flags = new HashMap<>();
        flags.put("-add", "Add a new note.");
        flags.put("-rm", "Remove an existing note by ID or text.");
        flags.put("-cp", "Copy a note to the clipboard.");
        flags.put("-ls", "List all notes.");
        flags.put("-clear", "Remove all unlocked notes.");
        flags.put("-lock", "Lock a note to prevent deletion.");
        flags.put("-unlock", "Unlock a note.");
        flags.put("-tutorial", "Open the notes tutorial on GitHub.");

        List<String> examples = new ArrayList<>();
        examples.add("notes -add Buy milk");
        examples.add("notes -ls");

        return new CommandMetadata("notes", description, flags, "[-flag] [args]", examples);
    }

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        add {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_ADD);
                i.putExtra(NotesManager.TEXT, pack.getString());
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
                return null;
            }
        },
        rm {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_RM);
                i.putExtra(NotesManager.TEXT, pack.getString());
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
                return null;
            }
        },
        cp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_CP);
                i.putExtra(NotesManager.TEXT, pack.getString());
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
                return null;
            }
        },
        ls {
            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_LS);
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
                return null;
            }
        },
        clear {
            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_CLEAR);
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
                return null;
            }
        },
        lock {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_LOCK);
                i.putExtra(NotesManager.TEXT, pack.getString());
                i.putExtra(NotesManager.LOCK, true);
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
                return null;
            }
        },
        unlock {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(NotesManager.ACTION_LOCK);
                i.putExtra(NotesManager.TEXT, pack.getString());
                i.putExtra(NotesManager.LOCK, false);
                i.putExtra(NotesManager.BROADCAST_COUNT, NotesManager.broadcastCount);

                LocalBroadcastManager.getInstance(pack.getContext()).sendBroadcast(i);
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
                pack.getContext().startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Notes"));
                return null;
            }
        };

        @Override
        public int[] args() {
            return new int[0];
        }

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

            for(int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String label() {
            return Tuils.MINUS + name();
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.getContext().getString(R.string.help_notes);
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.getContext().getString(R.string.help_notes);
        }
    }

    @Override
    protected bhupendra.ai.launcher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_notes;
    }
}
