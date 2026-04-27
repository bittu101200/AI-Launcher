package bhupendra.ai.launcher.commands;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CommandRegistryTest {

    @Test
    public void builtInRegistryCoversEveryRawCommandClass() {
        Set<String> expected = new HashSet<>(Arrays.asList(
            "ai", "alias", "apps", "backup", "beep", "bluetooth", "brightness", "calc",
            "call", "changelog", "clear", "cleararchive", "clearhistory", "compact",
            "config", "ctrlc", "devutils", "donate", "exit", "flash", "help",
            "htmlextract", "integrate", "kill", "location", "music", "notes",
            "notifications", "open", "pin", "rate", "refresh", "regex", "reply",
            "requirements", "restart", "restore", "rss", "search", "share", "shortcut",
            "sms", "status", "termux", "theme", "time", "timer", "tui", "tuiweather",
            "tuixt", "tutorial", "uninstall", "username", "vibrate", "volume", "wifi"
        ));

        Set<String> actual = new HashSet<>();
        for (CommandAbstraction command : CommandRegistry.getBuiltInCommands()) {
            actual.add(command.getClass().getSimpleName());
        }

        assertEquals(expected, actual);
    }

    @Test
    public void builtInRegistryHasExplicitMetadataForEveryCommand() throws Exception {
        for (CommandAbstraction command : CommandRegistry.getBuiltInCommands()) {
            assertTrue(
                command.getClass().getSimpleName() + " must override getMetadata",
                command.getClass().getMethod("getMetadata", android.content.Context.class).getDeclaringClass() != CommandAbstraction.class
            );
        }
    }
}
