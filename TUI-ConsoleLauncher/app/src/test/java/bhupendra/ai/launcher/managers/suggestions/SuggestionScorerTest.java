package bhupendra.ai.launcher.managers.suggestions;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SuggestionScorerTest {

    @Test
    public void acceptsCompactAndAcronymMatches() {
        assertTrue(SuggestionScorer.score("wa", "WhatsApp") >= SuggestionScorer.minimumScore("wa"));
        assertTrue(SuggestionScorer.score("ps", "Play Store") >= SuggestionScorer.minimumScore("ps"));
        assertTrue(SuggestionScorer.score("playst", "Play Store") >= SuggestionScorer.minimumScore("playst"));
    }

    @Test
    public void ranksTyposAboveUnrelatedCandidates() {
        int typo = SuggestionScorer.score("notifcations", "notifications");
        int unrelated = SuggestionScorer.score("notifcations", "calendar");

        assertTrue(typo >= SuggestionScorer.minimumScore("notifcations"));
        assertTrue(typo > unrelated);
    }

    @Test
    public void rankStringsReturnsBestMatchesFirst() {
        String[] values = {"Calendar", "Play Store", "WhatsApp", "Settings"};

        List<String> ranked = SuggestionScorer.rankStrings("wapp", values, 3);

        assertFalse(ranked.isEmpty());
        assertEquals("WhatsApp", ranked.get(0));
    }

    @Test
    public void normalizesSeparatorsCaseAndCamelCase() {
        assertTrue(SuggestionScorer.score("notif hook", "notification_hook") >= SuggestionScorer.minimumScore("notif hook"));
        assertTrue(SuggestionScorer.score("fs man", "FileSystemManager") >= SuggestionScorer.minimumScore("fs man"));
    }
}
