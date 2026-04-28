package bhupendra.ai.launcher.commands;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CommandGroupTest {

    @Test
    public void mainGroupLoadsBuiltInCommands() {
        CommandGroup group = new CommandGroup(null, "bhupendra.ai.launcher.commands.main.raw");

        assertNotNull(group.getCommandByName("config"));
        assertNotNull(group.getCommandByName("tuixt"));
        assertTrue(group.getCommandByName("save") == null);
    }

    @Test
    public void tuixtGroupLoadsOnlyEditorCommands() {
        CommandGroup group = new CommandGroup(null, "bhupendra.ai.launcher.commands.tuixt.raw");

        assertEquals("exit", group.getCommandByName("exit").getClass().getSimpleName());
        assertEquals("help", group.getCommandByName("help").getClass().getSimpleName());
        assertEquals("save", group.getCommandByName("save").getClass().getSimpleName());
        assertTrue(group.getCommandByName("config") == null);
    }
}
