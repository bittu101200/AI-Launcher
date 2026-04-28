package bhupendra.ai.launcher.commands;

import android.content.Context;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandGroup {

    private static final String MAIN_COMMANDS_PACKAGE = "bhupendra.ai.launcher.commands.main.raw";
    private static final String TUIXT_COMMANDS_PACKAGE = "bhupendra.ai.launcher.commands.tuixt.raw";

    private String packageName;
    private CommandAbstraction[] commands;
    private String[] commandNames;

    public CommandGroup(Context c, String packageName) {
        this.packageName = packageName;

        List<CommandAbstraction> cmdAbs = loadCommands(packageName);
        List<String> names = new ArrayList<>();
        for (CommandAbstraction ca : cmdAbs) {
            names.add(ca.getClass().getSimpleName());
        }

        Collections.sort(names);
        commandNames = new String[names.size()];
        names.toArray(commandNames);

        Collections.sort(cmdAbs, (o1, o2) -> o2.priority() - o1.priority());
        commands = new CommandAbstraction[cmdAbs.size()];
        cmdAbs.toArray(commands);
    }

    private List<CommandAbstraction> loadCommands(String packageName) {
        if (MAIN_COMMANDS_PACKAGE.equals(packageName)) {
            return CommandRegistry.getBuiltInCommands();
        }

        if (TUIXT_COMMANDS_PACKAGE.equals(packageName)) {
            List<CommandAbstraction> list = new ArrayList<>();
            list.add(new bhupendra.ai.launcher.commands.tuixt.raw.exit());
            list.add(new bhupendra.ai.launcher.commands.tuixt.raw.help());
            list.add(new bhupendra.ai.launcher.commands.tuixt.raw.save());
            return list;
        }

        throw new IllegalArgumentException("Unsupported command package: " + packageName);
    }

    public CommandAbstraction getCommandByName(String name) {
        for(CommandAbstraction c : commands) {
            if(c.getClass().getSimpleName().equals(name)) {
                return c;
            }
        }

        return null;
    }

    public CommandAbstraction[] getCommands() {
        return commands;
    }

    public String[] getCommandNames() {
        return commandNames;
    }

}
