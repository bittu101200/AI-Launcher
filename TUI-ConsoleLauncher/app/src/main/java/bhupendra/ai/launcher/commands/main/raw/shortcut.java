package bhupendra.ai.launcher.commands.main.raw;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.os.Build;

import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 24/03/2018.
 */

@TargetApi(Build.VERSION_CODES.N_MR1)
public class shortcut extends ParamCommand {

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        use {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.NO_SPACE_STRING, CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                String id = pack.getString();
                AppsManager.LaunchInfo li = pack.getLaunchInfo();

                ShortcutInfo shortcut = null;
                int index;
                try {
                    index = Integer.parseInt(id);

                    if(li.shortcuts == null || li.shortcuts.size() == 0) return "[]";
                    if(index >= li.shortcuts.size()) return pack.getContext().getString(R.string.shortcut_index_greater);
                    shortcut = li.shortcuts.get(index);
                } catch (Exception e) {
                    if(li != null) {
                        if(li.shortcuts == null || li.shortcuts.size() == 0) return pack.getContext().getString(R.string.app_shortcut_not_found);

                        for(ShortcutInfo i : li.shortcuts) {
                            if(i.getId().equals(id)) {
                                shortcut = i;
                                break;
                            }
                        }
                    }
                }

                if(shortcut == null) return pack.getContext().getString(R.string.id_notfound);

                startShortcut(shortcut, pack.getContext());
                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                if(n == 1) return pack.getContext().getString(R.string.help_shortcut);

                pack.get();
                String id = pack.getString();

                ShortcutInfo info = null;

                Out:
                for(AppsManager.LaunchInfo l : pack.getAppsManager().shownApps()) {
                    if(l.shortcuts == null || l.shortcuts.size() == 0) continue;

                    for(ShortcutInfo i : l.shortcuts) {
                        if(i.getId().equals(id)) {
                            info = i;

                            break Out;
                        }
                    }
                }

                return startShortcut(info, pack.getContext());
            }

            private String startShortcut(ShortcutInfo info, Context context) {
                if(info == null) return context.getString(R.string.app_shortcut_not_found);

                LauncherApps apps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                apps.startShortcut(info, null, null);

                return null;
            }
        },
        ls {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo li = pack.getLaunchInfo();
                if(li.shortcuts == null || li.shortcuts.size() == 0) return "[]";

                StringBuilder builder = new StringBuilder();
                append(builder, li.shortcuts, Tuils.EMPTYSTRING);

                return builder.toString();
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                List<AppsManager.LaunchInfo> infos = pack.getAppsManager().shownApps();
                StringBuilder builder = new StringBuilder();

                for(AppsManager.LaunchInfo l : infos) {
                    if(l.shortcuts == null || l.shortcuts.size() == 0) continue;

                    builder.append(l.publicLabel).append(Tuils.NEWLINE);
                    append(builder, l.shortcuts, Tuils.DOUBLE_SPACE);
                }

                String s = builder.toString().trim();
                if(s.length() == 0) return "[]";

                return s;
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
            return pack.getContext().getString(R.string.help_shortcut);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.getContext().getString(R.string.output_appnotfound);
        }

        private static void append(StringBuilder builder, List<ShortcutInfo> shortcuts, final String prefix) {
            for(ShortcutInfo i : shortcuts) {
                builder.append(prefix).append("- ").append(i.getShortLabel()).append(" (ID: ").append(i.getId()).append(")").append(Tuils.NEWLINE);
            }
        }
    }

    @Override
    protected bhupendra.ai.launcher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_shortcut;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    protected String doThings(ExecutePack pack) {
        return null;
    }


    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("shortcut", "-ls [appName] -> show the shortcuts of this app", null, null, null);
    }
}
