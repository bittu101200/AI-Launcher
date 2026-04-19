package bhupendra.ai.launcher.ai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
import bhupendra.ai.launcher.ai.platform.LauncherIndex;

public class LauncherIndexTest {

    @Test
    public void search_returnsMatchingLocalEntries() {
        LauncherIndex index = new LauncherIndex();
        index.put("cmd:status", "status", "command");
        index.put("tool:spotify", "Spotify search", "shortcut");
        index.put("app:maps", "Maps", "app");

        List<LauncherIndex.Entry> results = index.search("spot", 5);
        assertEquals(1, results.size());
        assertEquals("Spotify search", results.get(0).label);
    }
}