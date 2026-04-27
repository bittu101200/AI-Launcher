package bhupendra.ai.launcher.commands;

import android.content.Context;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class CommandHelpRegistry {
    private static CommandHelpRegistry instance;
    private final Map<String, CommandMetadata> registry = new HashMap<>();

    public static synchronized CommandHelpRegistry getInstance(Context context) {
        if (instance == null) {
            instance = new CommandHelpRegistry(context);
        }
        return instance;
    }

    private CommandHelpRegistry(Context context) {
        List<CommandAbstraction> commands = CommandRegistry.getBuiltInCommands();
        for (CommandAbstraction cmd : commands) {
            CommandMetadata meta = cmd.getMetadata(context);
            if (meta != null) {
                registry.put(meta.name.toLowerCase(), meta);
            }
        }
    }

    public CommandMetadata getCommandHelp(String name) {
        return registry.get(name.toLowerCase());
    }

    public List<CommandMetadata> searchCommands(String query) {
        List<CommandMetadata> results = new ArrayList<>();
        String q = query.toLowerCase();
        for (CommandMetadata meta : registry.values()) {
            if (meta.name.toLowerCase().contains(q) || 
                meta.description.toLowerCase().contains(q)) {
                results.add(meta);
            }
        }
        return results;
    }

    public String getAllCommandNames() {
        return String.join(", ", registry.keySet());
    }
}
