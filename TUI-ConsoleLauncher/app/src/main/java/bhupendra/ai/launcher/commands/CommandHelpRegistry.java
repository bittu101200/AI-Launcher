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
        refresh(context);
    }

    public void refresh(Context context) {
        registry.clear();
        List<CommandAbstraction> commands = CommandRegistry.getBuiltInCommands();
        for (CommandAbstraction cmd : commands) {
            try {
                CommandMetadata meta = cmd.getMetadata(context);
                if (meta != null) {
                    registry.put(meta.name.toLowerCase(), meta);
                }
            } catch (Exception e) {
                android.util.Log.e("CommandHelpRegistry", "Failed to index command: " + cmd.getClass().getSimpleName(), e);
            }
        }
    }

    public void register(CommandMetadata meta) {
        if (meta != null) {
            registry.put(meta.name.toLowerCase(), meta);
        }
    }

    public void registerAliases(List<bhupendra.ai.launcher.managers.AliasManager.Alias> aliases) {
        for (bhupendra.ai.launcher.managers.AliasManager.Alias alias : aliases) {
            String desc = "Alias for: " + alias.value;
            if (alias.isParametrized) {
                desc += " (Supports parameters)";
            }
            register(new CommandMetadata(alias.name, desc, null, null, java.util.Arrays.asList(alias.name)));
        }
    }

    public CommandMetadata getCommandHelp(String name) {
        return registry.get(name.toLowerCase());
    }

    public List<CommandMetadata> searchCommands(String query) {
        List<CommandMetadata> results = new ArrayList<>();
        String q = query.toLowerCase();
        
        // Priority 1: Exact name match
        CommandMetadata exact = registry.get(q);
        if (exact != null) results.add(exact);

        for (CommandMetadata meta : registry.values()) {
            if (meta == exact) continue;

            // Priority 2: Keyword match
            boolean keywordMatch = false;
            for (String kw : meta.keywords) {
                if (kw.toLowerCase().equals(q)) {
                    results.add(meta);
                    keywordMatch = true;
                    break;
                }
            }
            if (keywordMatch) continue;

            // Priority 3: Contains match (name or description)
            if (meta.name.toLowerCase().contains(q) || 
                meta.description.toLowerCase().contains(q)) {
                results.add(meta);
            } else {
                // Priority 4: Fuzzy match (simple Levenshtein distance < 3 for short queries)
                if (q.length() > 3 && (levenshtein(q, meta.name.toLowerCase()) < 3)) {
                    results.add(meta);
                }
            }
        }
        return results;
    }

    private int levenshtein(String s1, String s2) {
        int[] prev = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) prev[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            int[] curr = new int[s2.length() + 1];
            curr[0] = i;
            for (int j = 1; j <= s2.length(); j++) {
                int d = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + d);
            }
            prev = curr;
        }
        return prev[s2.length()];
    }

    public String getAllCommandNames() {
        return String.join(", ", registry.keySet());
    }
}
