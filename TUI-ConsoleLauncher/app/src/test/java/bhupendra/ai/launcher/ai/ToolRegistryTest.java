package bhupendra.ai.launcher.ai;

import org.junit.Before;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ToolRegistryTest {

    private ToolRegistry registry;

    @Before public void setup() { registry = new ToolRegistry(); }

    @Test
    public void registerAndLookup() {
        Tool def = new Tool("apps", "List apps", null, ToolRiskClass.READ_ONLY);
        registry.register(ToolRegistry.Tier.TUI_COMMAND, def);
        assertNotNull(registry.lookup("apps"));
        assertEquals(ToolRiskClass.READ_ONLY, registry.lookup("apps").riskClass);
    }

    @Test
    public void unregister_removes() {
        registry.register(ToolRegistry.Tier.TUI_COMMAND,
            new Tool("wifi", "Toggle", null, ToolRiskClass.STATE_CHANGING));
        registry.unregister("wifi");
        assertNull(registry.lookup("wifi"));
    }

    @Test
    public void tierOrdering_tuiFirst() {
        registry.register(ToolRegistry.Tier.APP_INTENT,
            new Tool("open_spotify", "Open Spotify", null, ToolRiskClass.LAUNCH_ONLY));
        registry.register(ToolRegistry.Tier.TUI_COMMAND,
            new Tool("apps", "List", null, ToolRiskClass.READ_ONLY));

        List<Tool> tools = registry.getTools();
        assertEquals("apps", tools.get(0).name);
        assertEquals("open_spotify", tools.get(1).name);
    }

    @Test
    public void compressedTools_dropAppTier() {
        registry.register(ToolRegistry.Tier.TUI_COMMAND,
            new Tool("apps", "List", null, ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.APP_INTENT,
            new Tool("open_spotify", "Open", null, ToolRiskClass.LAUNCH_ONLY));

        List<Tool> compressed = registry.getToolsCompressed();
        assertEquals(1, compressed.size());
        assertEquals("apps", compressed.get(0).name);
    }
}