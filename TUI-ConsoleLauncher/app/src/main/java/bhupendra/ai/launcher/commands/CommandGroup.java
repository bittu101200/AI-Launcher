package bhupendra.ai.launcher.commands;

import android.content.Context;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import bhupendra.ai.launcher.tuils.Tuils;

public class CommandGroup {

    private String packageName;
    private CommandAbstraction[] commands;
    private String[] commandNames;

    public CommandGroup(Context c, String packageName) {
        this.packageName = packageName;

        List<CommandAbstraction> cmdAbs = CommandRegistry.getBuiltInCommands();
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
