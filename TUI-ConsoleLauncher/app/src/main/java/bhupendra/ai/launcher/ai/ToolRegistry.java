package bhupendra.ai.launcher.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    public enum Tier { TUI_COMMAND, APP_INTENT, SYSTEM }

    private final Map<String, ToolEntry> entries = new LinkedHashMap<>();

    private static class ToolEntry {
        final Tier tier;
        final Tool definition;
        ToolEntry(Tier tier, Tool definition) {
            this.tier = tier;
            this.definition = definition;
        }
    }

    public synchronized void register(Tier tier, Tool definition) {
        entries.put(definition.name, new ToolEntry(tier, definition));
    }

    public synchronized void unregister(String name) {
        entries.remove(name);
    }

    public synchronized Tool lookup(String name) {
        ToolEntry e = entries.get(name);
        return e != null ? e.definition : null;
    }

    public synchronized List<Tool> getTools() {
        List<Tool> result = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            for (ToolEntry e : entries.values()) {
                if (e.tier == tier) result.add(e.definition);
            }
        }
        return result;
    }

    public synchronized List<Tool> getToolsCompressed() {
        List<Tool> result = new ArrayList<>();
        for (ToolEntry e : entries.values()) {
            if (e.tier == Tier.TUI_COMMAND) result.add(e.definition);
        }
        return result;
    }
}