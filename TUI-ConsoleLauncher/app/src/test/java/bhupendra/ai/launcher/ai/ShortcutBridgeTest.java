package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import bhupendra.ai.launcher.ai.platform.ShortcutBridge;

public class ShortcutBridgeTest {

    @Test
    public void sortPreferred_putsPublishedShortcutsFirst() {
        ShortcutBridge bridge = new ShortcutBridge();
        List<ShortcutBridge.ShortcutInfoCompat> sorted = bridge.sortPreferred(Arrays.asList(
            new ShortcutBridge.ShortcutInfoCompat("spotify_launch", "Open Spotify", false),
            new ShortcutBridge.ShortcutInfoCompat("spotify_search", "Spotify Search", true)
        ));
        assertEquals("spotify_search", sorted.get(0).id);
    }
}