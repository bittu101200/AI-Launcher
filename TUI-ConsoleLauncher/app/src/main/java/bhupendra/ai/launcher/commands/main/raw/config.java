package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.TextProcessor;


import bhupendra.ai.launcher.managers.FileSystemManager;


import android.content.ActivityNotFoundException;
import android.content.SharedPreferences;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.UIManager;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ConfigChangeHandler;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.notifications.NotificationManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsElement;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Apps;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Notifications;
import bhupendra.ai.launcher.managers.xml.options.Rss;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;
import android.content.Context;

import static bhupendra.ai.launcher.UIManager.PREFS_NAME;

import bhupendra.ai.launcher.commands.CommandMetadata;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by francescoandreuzzi on 11/06/2017.
 */

public class config extends ParamCommand {

    @Override
    public CommandMetadata getMetadata(Context context) {
        String description = "Configure T-UI settings, colors, and behavior.";
        Map<String, String> flags = new HashMap<>();
        flags.put("-set", "Set the value of a configuration option.");
        flags.put("-file", "Open a configuration file for editing.");
        flags.put("-append", "Append a value to a configuration option.");
        flags.put("-erase", "Clear the value of a configuration option.");
        flags.put("-get", "Get the current value of a configuration option.");
        flags.put("-ls", "List available configuration files or options.");
        flags.put("-fontsize", "Change the global font size.");
        flags.put("-reset", "Reset a configuration option to its default value.");
        flags.put("-apply", "Apply a configuration file from a local path.");
        flags.put("-tutorial", "Open the customization tutorial on GitHub.");

        List<String> examples = new ArrayList<>();
        examples.add("config -ls");
        examples.add("config -set ui_color #ffffff");
        examples.add("config -info max_lines");

        return new CommandMetadata("config", description, flags, "[-flag] [args]", examples,
            java.util.Arrays.asList("settings", "preferences", "options", "setup", "customize"),
            "system.search_config");
    }

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        set {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY, CommandAbstraction.CONFIG_VALUE};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                String value = pack.getString();

                if (save == Notifications.notification_whitelist || save == Notifications.notification_blacklist) {
                    AppsManager apps = ((MainPack) pack).appsManager;
                    String[] items = value.split(",");
                    StringBuilder resolved = new StringBuilder();
                    for (int i = 0; i < items.length; i++) {
                        String item = items[i].trim();
                        AppsManager.LaunchInfo info = apps.findLaunchInfoWithLabel(item, AppsManager.SHOWN_APPS);
                        if (info == null) info = apps.findLaunchInfoWithLabel(item, AppsManager.HIDDEN_APPS);
                        
                        if (info != null) {
                            resolved.append(info.componentName.getPackageName());
                        } else {
                            resolved.append(item);
                        }
                        if (i < items.length - 1) resolved.append(",");
                    }
                    value = resolved.toString();
                }

                save.parent().write(save, value);

                ((Reloadable) pack.getContext()).addMessage(save.parent().path(), save.label() + " -> " + value);

                ConfigChangeHandler.apply(pack.getContext(), save);

                if(save.label().startsWith("default_app_n")) {
                    return pack.getContext().getString(R.string.output_usedefapp);
                } else if(save == Behavior.unlock_counter_cycle_start) {
                    SharedPreferences preferences = pack.getContext().getSharedPreferences(PREFS_NAME, 0);
                    preferences.edit()
                            .putLong(UIManager.NEXT_UNLOCK_CYCLE_RESTART, 0)
                            .putInt(UIManager.UNLOCK_KEY, 0)
                            .apply();
                }

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                pack.args = new Object[] {pack.args[1], Tuils.EMPTYSTRING};
                return set.exec(pack);
            }
        },
        info {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();

                return "Type:" + Tuils.SPACE + save.type() + Tuils.NEWLINE
                        + "Default:" + Tuils.SPACE + save.defaultValue() + Tuils.NEWLINE
                        + save.info();
            }
        },
        file {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(FileSystemManager.getFolder(), pack.getString());

                try {
                    pack.getContext().startActivity(FileSystemManager.openFile(pack.getContext(), file));
                } catch (ActivityNotFoundException e) {
                    Tuils.log("nf");
                    FileSystemManager.toFile(e);
                } catch (Exception ex) {
                    Tuils.log(ex);
                    FileSystemManager.toFile(ex);
                }

                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.getContext().getString(R.string.output_filenotfound);
            }
        },
        append {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY, CommandAbstraction.CONFIG_VALUE};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                String value = XMLPrefsManager.get(save) + pack.getString();

                save.parent().write(save, value);

                ((Reloadable) pack.getContext()).addMessage(save.parent().path(), save.label() + " -> " + value);

                ConfigChangeHandler.apply(pack.getContext(), save);

                return null;
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                pack.args = new Object[] {pack.args[0], pack.args[1], Tuils.EMPTYSTRING};
                return set.exec(pack);
            }
        },
        erase {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                save.parent().write(save, Tuils.EMPTYSTRING);

                ((Reloadable) pack.getContext()).addMessage(save.parent().path(), save.label() + " -> " + "\"\"");

                ConfigChangeHandler.apply(pack.getContext(), save);

                return null;
            }
        },
        get {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                String s = XMLPrefsManager.get(String.class, save);
                if(s.length() == 0) return "\"\"";
                return s;
            }
        },
        ls {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = new File(FileSystemManager.getFolder(), pack.getString());
                String name = file.getName();

                for(XMLPrefsManager.XMLPrefsRoot r : XMLPrefsManager.XMLPrefsRoot.values()) {
                    if(name.equalsIgnoreCase(r.path)) {
                        List<String> strings = r.getValues().values();
                        TextProcessor.addPrefix(strings, Tuils.DOUBLE_SPACE);
                        strings.add(0, r.path);
                        return TextProcessor.toPlanString(strings, Tuils.NEWLINE);
                    }
                }

                if(name.equalsIgnoreCase(AppsManager.PATH)) {
                    List<String> strings = AppsManager.instance.getValues().values();
                    TextProcessor.addPrefix(strings, Tuils.DOUBLE_SPACE);
                    strings.add(0, AppsManager.PATH);
                    return TextProcessor.toPlanString(strings, Tuils.NEWLINE);
                }

                if(name.equalsIgnoreCase(NotificationManager.PATH)) {
                    List<String> strings = NotificationManager.instance.getValues().values();
                    TextProcessor.addPrefix(strings, Tuils.DOUBLE_SPACE);
                    strings.add(0, NotificationManager.PATH);
                    return TextProcessor.toPlanString(strings, Tuils.NEWLINE);
                }

                if(name.equalsIgnoreCase(RssManager.PATH)) {
                    List<String> strings = NotificationManager.instance.getValues().values();
                    TextProcessor.addPrefix(strings, Tuils.DOUBLE_SPACE);
                    strings.add(0, RssManager.PATH);
                    return TextProcessor.toPlanString(strings, Tuils.NEWLINE);
                }

                return "[]";
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                return pack.getContext().getString(R.string.output_filenotfound);
            }

            @Override
            public String onNotArgEnough(ExecutePack pack, int n) {
                List<String> ss = new ArrayList<>();

                for(XMLPrefsManager.XMLPrefsRoot element : XMLPrefsManager.XMLPrefsRoot.values()) {
                    ss.add(element.path);
                    for(XMLPrefsSave save : element.enums) {
                        ss.add(Tuils.DOUBLE_SPACE + save.label());
                    }
                }
                ss.add(AppsManager.PATH);
                for(XMLPrefsSave save : Apps.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }
                ss.add(NotificationManager.PATH);
                for(XMLPrefsSave save : Notifications.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }
                ss.add(RssManager.PATH);
                for(XMLPrefsSave save : Rss.values()) {
                    ss.add(Tuils.DOUBLE_SPACE + save.label());
                }

                return TextProcessor.toPlanString(ss);
            }
        },
        fontsize {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsElement parent = Ui.device_size.parent();

                int size = pack.getInt();

                parent.write(Ui.device_size, String.valueOf(size));
                parent.write(Ui.ram_size, String.valueOf(size));
                parent.write(Ui.network_size, String.valueOf(size));
                parent.write(Ui.storage_size, String.valueOf(size));
                parent.write(Ui.battery_size, String.valueOf(size));
                parent.write(Ui.notes_size, String.valueOf(size));
                parent.write(Ui.time_size, String.valueOf(size));
                parent.write(Ui.weather_size, String.valueOf(size));
                parent.write(Ui.unlock_size, String.valueOf(size));
                parent.write(Ui.input_output_size, String.valueOf(size));

                ConfigChangeHandler.apply(pack.getContext(), Ui.device_size);

                return null;
            }
        },
        reset {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.CONFIG_ENTRY};
            }

            @Override
            public String exec(ExecutePack pack) {
                XMLPrefsSave save = pack.getPrefsSave();
                save.parent().write(save, save.defaultValue());

                ((Reloadable) pack.getContext()).addMessage(save.parent().path(), save.label() + " -> " + save.defaultValue());

                ConfigChangeHandler.apply(pack.getContext(), save);

                return null;
            }
        },
        apply {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.FILE};
            }

            @Override
            public String exec(ExecutePack pack) {
                File file = pack.get(File.class);

                if(!file.getName().endsWith(".xml")) {
//                    is font
                    if(Tuils.fontPath != null) {
                        File font = new File(Tuils.fontPath);
                        if (font.exists()) {
                            File[] files = font.listFiles();
                            if (files.length > 0) FileSystemManager.insertOld(files[0]);
                            FileSystemManager.deleteContentOnly(font);
                        } else {
                            font.mkdir();
                        }
                    }
                } else {
                    File toPutInsideOld = new File(FileSystemManager.getFolder(), file.getName());
                    FileSystemManager.insertOld(toPutInsideOld);
                }

                File dest = new File(FileSystemManager.getFolder(), file.getName());
                file.renameTo(dest);

                if (pack.getContext() instanceof Reloadable) {
                    ((Reloadable) pack.getContext()).reload();
                }

                return "Path: " + dest.getAbsolutePath();
            }
        },
        tutorial {
            @Override
            public int[] args() {
                return new int[0];
            }

            @Override
            public String exec(ExecutePack pack) {
                pack.getContext().startActivity(Tuils.webPage("https://github.com/Andre1299/TUI-ConsoleLauncher/wiki/Customize-T_UI"));
                return null;
            }
        };

        static Param get(String p) {
            p = p.toLowerCase();
            Param[] ps = values();
            for (Param p1 : ps) if (p.endsWith(p1.label())) return p1;
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
            return pack.getContext().getString(R.string.help_config);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.getContext().getString(R.string.output_invalidarg);
        }
    }

    @Override
    public String[] params() {
        return Param.labels();
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
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_config;
    }
}
