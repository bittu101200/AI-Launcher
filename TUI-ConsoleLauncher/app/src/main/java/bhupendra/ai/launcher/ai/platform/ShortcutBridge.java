package bhupendra.ai.launcher.ai.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShortcutBridge {

    public static class ShortcutInfoCompat {
        public final String id;
        public final String shortLabel;
        public final boolean publishedShortcut;

        public ShortcutInfoCompat(String id, String shortLabel, boolean publishedShortcut) {
            this.id = id;
            this.shortLabel = shortLabel;
            this.publishedShortcut = publishedShortcut;
        }
    }

    public List<ShortcutInfoCompat> sortPreferred(List<ShortcutInfoCompat> shortcuts) {
        List<ShortcutInfoCompat> result = new ArrayList<>(shortcuts);
        Collections.sort(result, Comparator.comparing((ShortcutInfoCompat s) -> !s.publishedShortcut));
        return result;
    }
}