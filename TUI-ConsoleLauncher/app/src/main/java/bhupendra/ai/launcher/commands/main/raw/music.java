package bhupendra.ai.launcher.commands.main.raw;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.ParamCommand;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.managers.music.Song;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.libsuperuser.Shell;

public class music extends ParamCommand {

    private enum Param implements bhupendra.ai.launcher.commands.main.Param {
        next {

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) {
                    execute("NEXT");
                    return null;
                }

                String title = pack.getMusicManager().playNext();
                if(title != null) return pack.getContext().getString(R.string.output_playing) + Tuils.SPACE + title;
                return null;
            }
        },
        previous {

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) {
                    execute("PREVIOUS");
                    return null;
                }

                String title = pack.getMusicManager().playPrev();
                if(title != null) return pack.getContext().getString(R.string.output_playing) + Tuils.SPACE + title;
                return null;
            }
        },
        ls {

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) return pack.getContext().getString(R.string.output_musicdisabled);

                return pack.getMusicManager().lsSongs();
            }
        },
        play {

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) {
                    execute("PLAY_PAUSE");
                    return null;
                }

                String title = pack.getMusicManager().play();
                if(title == null) return null;
                return pack.getContext().getString(R.string.output_playing) + Tuils.SPACE + title;
            }
        },
        stop {

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) {
                    execute("CLOSE");
                    return null;
                }

                pack.getMusicManager().stop();
                return null;
            }
        },
        select {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.SONG};
            }

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) return pack.getContext().getString(R.string.output_musicdisabled);

                String s = pack.getString();
                pack.getMusicManager().select(s);
                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int indexNotFound) {
                return pack.getContext().getString(R.string.output_songnotfound);
            }
        },
        info {

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) return pack.getContext().getString(R.string.output_musicdisabled);

                StringBuilder builder = new StringBuilder();

                MusicManager2 m = pack.getMusicManager();
                Song song = m.get(m.getSongIndex());
                if(song == null) return pack.getContext().getString(R.string.output_songnotfound);

                builder.append("Name: ").append(song.getTitle()).append(Tuils.NEWLINE);
                if(song.getID() == -1) builder.append("Path: ").append(song.getPath()).append(Tuils.NEWLINE);
                builder.append(Tuils.NEWLINE);

                int curS = m.getCurrentPosition() / 1000;
                int curMin = 0;
                if(curS >= 60) {
                    curMin = curS / 60;
                    curS = curS % 60;
                }

                int s = m.getDuration() / 1000;
                int min = 0;
                if(s >= 60) {
                    min = s / 60;
                    s = s % 60;
                }

                builder.append(curMin > 0 ? curMin + "." + curS : curS + "s").append(" of ").append(min > 0 ? min + "." + s : s + "s").append(" (").append(Tuils.percentage(m.getCurrentPosition(), m.getDuration())).append("%)");
                return builder.toString();
            }
        },
        seekto {
            @Override
            public int[] args() {
                return new int[] {CommandAbstraction.INT};
            }

            @Override
            public String exec(ExecutePack pack) {
                if(pack.getMusicManager() == null) return pack.getContext().getString(R.string.output_musicdisabled);

                pack.getMusicManager().seekTo(pack.getInt() * 1000);
                return null;
            }

            @Override
            public String onArgNotFound(ExecutePack pack, int indexNotFound) {
                return pack.getContext().getString(R.string.invalid_integer);
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
        public String onArgNotFound(ExecutePack pack, int indexNotFound) {
            return null;
        }

        @Override
        public String onNotArgEnough(ExecutePack pack, int n) {
            return pack.getContext().getString(R.string.help_music);
        }

        @Override
        public int[] args() {
            return new int[0];
        }
    }

    private static void execute(String code) {
        Shell.SH.run("input keyevent KEYCODE_MEDIA_" + code);
    }

    @Override
    protected bhupendra.ai.launcher.commands.main.Param paramForString(MainPack pack, String param) {
        return Param.get(param);
    }

    @Override
    public int priority() {
        return 4;
    }

    @Override
    public int helpRes() {
        return R.string.help_music;
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
        return new bhupendra.ai.launcher.commands.CommandMetadata("music", "-next -> play the next song", null, null, null);
    }
}
