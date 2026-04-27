package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.managers.FileSystemManager;


import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.TimeManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Rss;
import bhupendra.ai.launcher.tuils.Tuils;

/**
 * Created by francescoandreuzzi on 30/09/2017.
 */

public class rss extends ParamCommand {

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {

        add {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                long tm = pack.get(long.class);
                String url = pack.getString();

                return pack.getRssManager().add(id, tm, url);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.LONG, CommandAbstraction.PLAIN_TEXT};
            }
        },
        rm {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();

                return pack.getRssManager().rm(id);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }


        },
        ls {
            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().list();
            }

            @Override
            public int[] args() {
                return new int[0];
            }
        },
        l {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                return pack.getRssManager().l(id);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }
        },
        show {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                boolean show = pack.getBoolean();

                return pack.getRssManager().setShow(id, show);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.BOOLEAN};
            }
        },
        update_time {
            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                long tm = pack.get(long.class);

                return pack.getRssManager().setTime(id, tm);
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.LONG};
            }
        },
        time_format {
            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().setTimeFormat(pack.getInt(), pack.getString());
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }
        },
        format {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String s = pack.getString();

                return pack.getRssManager().setFormat(id, s);
            }
        },
        color {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.COLOR};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String c = pack.getString();

                return pack.getRssManager().setColor(id, c);
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int index) {
                if(index == 2) return pack.getContext().getString(R.string.output_invalidcolor);
                return super.onArgNotFound(pack, index);
            }
        },
        entry_tag {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().setEntryTag(pack.getInt(), pack.getString());
            }
        },
        date_tag {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().setDateTag(pack.getInt(), pack.getString());
            }
        },
        last_check {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                Node n = XMLPrefsManager.findNode(new File(FileSystemManager.getFolder(), RssManager.PATH), RssManager.RSS_LABEL, new String[] {RssManager.ID_ATTRIBUTE}, new String[] {String.valueOf(pack.getInt())});
                if(n == null) return pack.getContext().getString(R.string.id_notfound);

                Element el = (Element) n;

                String value = el.hasAttribute(RssManager.LASTCHECKED_ATTRIBUTE) ? el.getAttribute(RssManager.LASTCHECKED_ATTRIBUTE) : null;
                if(value == null) return pack.getContext().getString(R.string.rss_never_checked);

                try {
                    return TimeManager.instance.replace(XMLPrefsManager.get(Rss.rss_time_format), Long.parseLong(value),
                            Integer.MAX_VALUE).toString();
                } catch (Exception e) {
                    Tuils.log(e);
                    return pack.getContext().getString(R.string.output_error);
                }
            }
        },
        frc {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                if(!pack.getRssManager().updateRss(pack.getInt(), false, true)) return pack.getContext().getString(R.string.id_notfound);
                return null;
            }
        },
        info {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                RssManager.Rss rss = pack.getRssManager().findId(pack.getInt());
                if(rss == null) return pack.getContext().getString(R.string.id_notfound);

                return rss.toString();
            }
        },
        include_if_matches {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String r = pack.getString();

                return pack.getRssManager().setIncludeIfMatches(id, r);
            }
        },
        exclude_if_matches {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                String r = pack.getString();

                return pack.getRssManager().setExcludeIfMatches(id, r);
            }
        },
        add_command {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.NO_SPACE_STRING, CommandAbstraction.NO_SPACE_STRING, CommandAbstraction.PLAIN_TEXT};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();

                String on = pack.getString();
                String regex = pack.getString();
                String cmd = pack.getString();

                try {
                    Pattern.compile(regex);
                } catch (Exception e) {
                    return e.toString();
                }

                return pack.getRssManager().addRegexCommand(id, on, regex, cmd);
            }
        },
        rm_command {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().rmRegexCommand(pack.getInt());
            }
        },
        wifi_only {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.BOOLEAN};
            }

            @Override
            public String exec(ExecutePack pack) {
                int id = pack.getInt();
                boolean w = pack.getBoolean();

                return pack.getRssManager().setWifiOnly(id, w);
            }
        },
        add_format {
            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().addFormat(pack.getInt(), pack.getString());
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT, CommandAbstraction.PLAIN_TEXT};
            }
        },
        rm_format {
            @Override
            public String exec(ExecutePack pack) {
                return pack.getRssManager().removeFormat(pack.getInt());
            }

            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }
        },
        file {
            @Override
            public String exec(ExecutePack pack) {
                pack.getContext().startActivity(FileSystemManager.openFile(pack.getContext(), new File(FileSystemManager.getFolder(), RssManager.PATH)));
                return null;
            }

            @Override
            public int[] args() {
                return new int[0];
            }
        };

        @Override
        public String label() {
            return Tuils.MINUS + name();
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

            for (int count = 0; count < ps.length; count++) {
                ss[count] = ps[count].label();
            }

            return ss;
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.getContext().getString(R.string.help_rss);
        }

        @Override
        public String onArgNotFound(ExecutePack pack, int index) {
            return pack.getContext().getString(R.string.invalid_integer);
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
    public int priority() {
        return 3;
    }

    @Override
    public int helpRes() {
        return R.string.help_rss;
    }

    @Override
    public String[] params() {
        return Param.labels();
    }

    @Override
    public bhupendra.ai.launcher.commands.CommandMetadata getMetadata(android.content.Context context) {
        return new bhupendra.ai.launcher.commands.CommandMetadata("rss", "-add [ID] [update_time_in_seconds] [url] -> add a new RSS feed", null, null, null);
    }
}
