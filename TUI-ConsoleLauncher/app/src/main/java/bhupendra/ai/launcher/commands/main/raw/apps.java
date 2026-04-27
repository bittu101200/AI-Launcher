package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.DeviceStateManager;


import bhupendra.ai.launcher.managers.TextProcessor;


import bhupendra.ai.launcher.managers.FileSystemManager;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.File;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Apps;
import bhupendra.ai.launcher.tuils.Tuils;

import bhupendra.ai.launcher.commands.CommandMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class apps extends ParamCommand {

    @Override
    public CommandMetadata getMetadata(Context context) {
        String description = "Manage your applications: list, show, hide, group, and more.";
        Map<String, String> flags = new HashMap<>();
        flags.put("-ls", "List all visible applications.");
        flags.put("-lsh", "List all hidden applications.");
        flags.put("-show", "Make a hidden application visible.");
        flags.put("-hide", "Hide an application from the list.");
        flags.put("-l", "Show detailed information about an application.");
        flags.put("-ps", "Open an application's page on the Google Play Store.");
        flags.put("-st", "Open an application's system settings page.");
        flags.put("-default_app", "Set a default application for a specific index.");
        flags.put("-frc", "Force launch an application.");
        flags.put("-file", "Open the apps.xml configuration file.");
        flags.put("-reset", "Reset an application's launch count to zero.");
        flags.put("-mkgp", "Create a new application group.");
        flags.put("-rmgp", "Delete an existing application group.");
        flags.put("-gp_bg_color", "Set the background color for an application group.");
        flags.put("-gp_fore_color", "Set the foreground color for an application group.");
        flags.put("-lsgp", "List all groups or applications within a specific group.");
        flags.put("-addtogp", "Add an application to a group.");
        flags.put("-rmfromgp", "Remove an application from a group.");
        flags.put("-tutorial", "Open the applications tutorial on GitHub.");

        List<String> examples = new ArrayList<>();
        examples.add("apps -ls");
        examples.add("apps -hide com.android.chrome");
        examples.add("apps -mkgp Social");

        return new CommandMetadata("apps", description, flags, "[-flag] [args]", examples, 
            java.util.Arrays.asList("applications", "launch", "group", "hide", "uninstall"), 
            "system.get_app_functions");
        }

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        ls {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return pack.getAppsManager().printApps(AppsManager.SHOWN_APPS, pack.getString());
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.getAppsManager().printApps(AppsManager.SHOWN_APPS);
            }
        },
        lsh {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return pack.getAppsManager().printApps(AppsManager.HIDDEN_APPS, pack.getString());
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.getAppsManager().printApps(AppsManager.HIDDEN_APPS);
            }
        },
        show {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.HIDDEN_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo i = pack.getLaunchInfo();
                pack.getAppsManager().showActivity(i);
                return null;
            }
        },
        hide {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo i = pack.getLaunchInfo();
                pack.getAppsManager().hideActivity(i);
                return null;
            }
        },
        l {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                try {
                    AppsManager.LaunchInfo i = pack.getLaunchInfo();

                    PackageInfo info = pack.getContext().getPackageManager().getPackageInfo(i.componentName.getPackageName(), PackageManager.GET_PERMISSIONS | PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_RECEIVERS);
                    return AppsManager.AppUtils.format(i, info);
                } catch (PackageManager.NameNotFoundException e) {
                    return e.toString();
                }
            }
        },
        ps {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                openPlaystore(pack.getContext(), pack.getLaunchInfo().componentName.getPackageName());
                return null;
            }
        },
        default_app {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.DEFAULT_APP};
            }

            @Override
            public String exec(ExecutePack pack) {
                int index = pack.getInt();

                Object o = pack.get();

                String marker;
                if(o instanceof AppsManager.LaunchInfo) {
                    AppsManager.LaunchInfo i = (AppsManager.LaunchInfo) o;
                    marker = i.componentName.getPackageName() + "-" + i.componentName.getClassName();
                } else {
                    marker = (String) o;
                }

                try {
                    XMLPrefsSave save = Apps.valueOf("default_app_n" + index);
                    save.parent().write(save, marker);
                    return null;
                } catch (Exception e) {
                    return pack.getContext().getString(R.string.invalid_integer);
                }
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                int res;
                if(index == 1) res = R.string.invalid_integer;
                else res = R.string.output_appnotfound;

                return pack.getContext().getString(res);
            }
        },
        st {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                openSettings(pack.getContext(), pack.getLaunchInfo().componentName.getPackageName());
                return null;
            }
        },
        frc {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.ALL_PACKAGES};
            }

            @Override
            public String exec(ExecutePack pack) {
                Intent intent = pack.getAppsManager().getIntent(pack.getLaunchInfo());
                pack.getContext().startActivity(intent);

                return null;
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.getContext().startActivity(FileSystemManager.openFile(pack.getContext(), new File(FileSystemManager.getFolder(), AppsManager.PATH)));
                return null;
            }
        },
//        services {
//            @Override
//            public int[] args() {
//                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
//            }
//
//            @Override
//            public String exec(ExecutePack pack) {
//                AppsManager.LaunchInfo info = pack.get(AppsManager.LaunchInfo.class, 1);
//
//                List<String> services = new ArrayList<>();
//
//                ActivityManager activityManager = (ActivityManager) pack.getContext().getSystemService(Context.ACTIVITY_SERVICE);
//                for(ActivityManager.RunningServiceInfo i : activityManager.getRunningServices(Integer.MAX_VALUE)) {
//                    ComponentName name = i.service;
//
//                    if(info.equals(name.getPackageName())) {
//                        services.add(name.getClassName().replace(name.getPackageName(), Tuils.EMPTYSTRING));
//                    }
//                }
//
//                if(services.size() == 0) return "[]";
//                Collections.sort(services);
//                return TextProcessor.toPlanString(services, Tuils.NEWLINE);
//            }
//
//            @Override
//            public String onNotArgEnough(ExecutePack pack, int n) {
//
//                List<SimpleMutableEntry<String, ArrayList<String>>> services = new ArrayList<>();
//
//                ActivityManager activityManager = (ActivityManager) pack.getContext().getSystemService(Context.ACTIVITY_SERVICE);
//                Tuils.log(activityManager.getRunningServices(Integer.MAX_VALUE).toString());
//                for(ActivityManager.RunningServiceInfo i : activityManager.getRunningServices(Integer.MAX_VALUE)) {
//
//                    boolean check = false;
//                    for(SimpleMutableEntry<String, ArrayList<String>> s : services) {
//                        if(s.getKey().equals(i.service.getPackageName())) {
//                            s.getValue().add(i.service.getClassName().replace(i.service.getPackageName(), Tuils.EMPTYSTRING));
//
//                            check = true;
//                            break;
//                        }
//                    }
//
//                    if(!check) {
//                        SimpleMutableEntry<String,ArrayList<String>> s = new SimpleMutableEntry<>(i.service.getPackageName(), new ArrayList<String>());
//                        s.getValue().add(i.service.getClassName().replace(i.service.getPackageName(), Tuils.EMPTYSTRING));
//                        services.add(s);
//                    }
//                }
//
//                if(services.size() == 0) return "[]";
//                Collections.sort(services, new Comparator<SimpleMutableEntry<String, ArrayList<String>>>() {
//                    @Override
//                    public int compare(SimpleMutableEntry<String, ArrayList<String>> o1, SimpleMutableEntry<String, ArrayList<String>> o2) {
//                        return o1.getKey().compareTo(o2.getKey());
//                    }
//                });
//
//                PackageManager manager = pack.getContext().getPackageManager();
//                StringBuilder b = new StringBuilder();
//                for(SimpleMutableEntry<String, ArrayList<String>> s : services) {
//                    String appName = null;
//                    try {
//                        appName = manager.getApplicationInfo(s.getKey(), 0).loadLabel(manager).toString();
//                    } catch (PackageManager.NameNotFoundException e) {}
//
//                    if(appName != null) b.append(appName).append(Tuils.SPACE).append("(").append(s.getKey()).append(")");
//                    else b.append(s.getKey());
//                    b.append(Tuils.NEWLINE);
//
//                    for(String st : s.getValue()) {
//                        b.append(" - ").append(st).append(Tuils.NEWLINE);
//                    }
//                    b.append(Tuils.NEWLINE);
//                }
//                return b.toString().trim();
//            }
//        },
        reset {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                AppsManager.LaunchInfo app = pack.getLaunchInfo();
                app.launchedTimes = 0;
                pack.getAppsManager().writeLaunchTimes(app);

                return null;
            }
        },
        mkgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.NO_SPACE_STRING};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                return pack.getAppsManager().createGroup(name);
            }
        },
        rmgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                return pack.getAppsManager().removeGroup(name);
            }
        },
        gp_bg_color {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.COLOR};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                String color = pack.getString();
                return pack.getAppsManager().groupBgColor(name, color);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                if(n == 2) {
                    String name = pack.getString();
                    return pack.getAppsManager().groupBgColor(name, Tuils.EMPTYSTRING);
                }
                return super.onNotArgEnough(pack, n);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.getContext().getString(R.string.output_invalidcolor);
            }
        },
        gp_fore_color {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.COLOR};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                String color = pack.getString();
                return pack.getAppsManager().groupForeColor(name, color);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                if(n == 2) {
                    String name = pack.getString();
                    return pack.getAppsManager().groupForeColor(name, Tuils.EMPTYSTRING);
                }
                return super.onNotArgEnough(pack, n);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.getContext().getString(R.string.output_invalidcolor);
            }
        },
        lsgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                return pack.getAppsManager().listGroup(name);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                return pack.getAppsManager().listGroups();
            }
        },
        addtogp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.VISIBLE_PACKAGE};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                AppsManager.LaunchInfo app = pack.getLaunchInfo();
                return pack.getAppsManager().addAppToGroup(name, app);
            }
        },
        rmfromgp {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.APP_GROUP, CommandAbstraction.APP_INSIDE_GROUP};
            }

            @Override
            public String exec(ExecutePack pack) {
                String name = pack.getString();
                AppsManager.LaunchInfo app = pack.getLaunchInfo();
                return pack.getAppsManager().removeAppFromGroup(name, app);
            }
        },
        tutorial {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.getContext().startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Apps"));
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
            return pack.getContext().getString(R.string.help_apps);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.getContext().getString(R.string.output_appnotfound);
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

    private static void openSettings(Context context, String packageName) {
        DeviceStateManager.openSettingsPage(context, packageName);
    }

    private static void openPlaystore(Context context, String packageName) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName)));
        } catch (Exception e) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName)));
        }
    }

    @Override
    public int helpRes() {
        return R.string.help_apps;
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }
}
