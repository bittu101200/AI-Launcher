package bhupendra.ai.launcher.commands;

import java.util.ArrayList;
import java.util.List;

import bhupendra.ai.launcher.commands.main.raw.*;

public class CommandRegistry {
    public static List<CommandAbstraction> getBuiltInCommands() {
        List<CommandAbstraction> list = new ArrayList<>();
        list.add(new ai());
        list.add(new alias());
        list.add(new apps());
        list.add(new backup());
        list.add(new beep());
        list.add(new bluetooth());
        list.add(new brightness());
        list.add(new calc());
        list.add(new call());
        list.add(new changelog());
        list.add(new clear());
        list.add(new cleararchive());
        list.add(new clearhistory());
        list.add(new compact());
        list.add(new config());
        list.add(new ctrlc());
        list.add(new devutils());
        list.add(new donate());
        list.add(new exit());
        list.add(new flash());
        list.add(new help());
        list.add(new htmlextract());
        list.add(new integrate());
        list.add(new kill());
        list.add(new location());
        list.add(new music());
        list.add(new notes());
        list.add(new notifications());
        list.add(new open());
        list.add(new pin());
        list.add(new rate());
        list.add(new refresh());
        list.add(new regex());
        list.add(new reply());
        list.add(new requirements());
        list.add(new restart());
        list.add(new restore());
        list.add(new rss());
        list.add(new search());
        list.add(new share());
        list.add(new shortcut());
        list.add(new sms());
        list.add(new status());
        list.add(new termux());
        list.add(new theme());
        list.add(new time());
        list.add(new timer());
        list.add(new tui());
        list.add(new tuiweather());
        list.add(new tuixt());
        list.add(new tutorial());
        list.add(new uninstall());
        list.add(new username());
        list.add(new vibrate());
        list.add(new volume());
        list.add(new wifi());
        return list;
    }
}
