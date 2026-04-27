package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;

import bhupendra.ai.launcher.BuildConfig;
import bhupendra.ai.launcher.LauncherActivity;
import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.UIManager;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.CommandsPreferences;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;
import bhupendra.ai.launcher.tuils.stuff.PolicyReceiver;

/**
 * Created by francescoandreuzzi on 10/06/2017.
 */

public class tui extends ParamCommand {

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        rm {
            @Override
            public String exec(ExecutePack pack) {
                MainPack info = (MainPack) pack;

                DevicePolicyManager policy = (DevicePolicyManager) info.getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
                ComponentName name = new ComponentName(info.getContext(), PolicyReceiver.class);
                policy.removeActiveAdmin(name);

                Uri packageURI = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                Intent uninstallIntent = new Intent(Intent.ACTION_DELETE, packageURI);
                info.getContext().startActivity(uninstallIntent);

                return null;
            }
        },
        about {
            @Override
            public String exec(ExecutePack pack) {
                MainPack info = (MainPack) pack;
                return "Version:" + Tuils.SPACE + BuildConfig.VERSION_NAME + " (code: " + BuildConfig.VERSION_CODE + ")" +
                        (BuildConfig.DEBUG ? Tuils.NEWLINE + BuildConfig.BUILD_TYPE : Tuils.EMPTYSTRING) +
                        Tuils.NEWLINE + Tuils.NEWLINE + info.getResources().getString(R.string.output_about);
            }
        },
        log {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent i = new Intent(UIManager.ACTION_LOGTOFILE);
                i.putExtra(UIManager.FILE_NAME, pack.getString());
                LocalBroadcastManager.getInstance(pack.getContext().getApplicationContext()).sendBroadcast(i);

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.getContext().getString(R.string.help_tui);
            }
        },
        priority {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.COMMAND, CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(FileSystemManager.getFolder(), "cmd.xml");
                return XMLPrefsManager.set(file, pack.get().getClass().getSimpleName() + CommandsPreferences.PRIORITY_SUFFIX, new String[] {XMLPrefsManager.VALUE_ATTRIBUTE}, new String[] {String.valueOf(pack.getInt())});
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.getContext().getString(R.string.help_tui);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.getContext().getString(R.string.output_invalidarg);
            }
        },
        sourcecode {
            @Override
            public String exec(ExecutePack pack) {
                pack.getContext().startActivity(Tuils.webPage("https://github.com/cycloarcane/TUI-ConsoleLauncher"));
                return null;
            }
        },
        reset {
            @Override
            public String exec(ExecutePack pack) {
                FileSystemManager.deleteContentOnly(FileSystemManager.getFolder());

                ((LauncherActivity) pack.getContext()).addMessage(pack.getContext().getString(R.string.tui_reset), null);
                ((Reloadable) pack.getContext()).reload();
                return null;
            }
        },
        folder {
            @Override
            public String exec(ExecutePack pack) {

                Uri selectedUri = Uri.parse(FileSystemManager.getFolder().getAbsolutePath());
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/folder");

                if (intent.resolveActivityInfo(pack.getContext().getPackageManager(), 0) != null) {
                    pack.getContext().startActivity(intent);
                } else {
                    return FileSystemManager.getFolder().getAbsolutePath();
                }

                return null;
            }
        }
//        ,
//        exclude_message {
//            @Override
//            public int[] args() {
//                return new int[] {CommandAbstraction.INT};
//            }
//
//            @Override
//            public String exec(ExecutePack pack) {
//                return null;
//            }
//
//            @Override
//            public String onNotArgEnough(ExecutePack pack, int n) {
//
//            }
//        }
        ;

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
            return null;
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return null;
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
        return R.string.help_tui;
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("tui", "-rm -> uninstall t-ui", null, null, null);
    }
}
