package bhupendra.ai.launcher.managers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class AppsManagerLaunchScoringTest {

    @Test
    public void prefixMatchBeatsLooseContainsMatch() {
        int linkedinScore = AppsManager.AppUtils.scoreLaunchCandidate(
                "LinkedIn", "com.linkedin.android", "com.linkedin.android.Main", 4, "lin");
        int locationScore = AppsManager.AppUtils.scoreLaunchCandidate(
                "Location", "com.android.location", "com.android.location.Main", 12, "lin");

        assertTrue(linkedinScore > locationScore);
    }

    @Test
    public void usageBoostStillKeepsExactMatchOnTop() {
        int exactScore = AppsManager.AppUtils.scoreLaunchCandidate(
                "LinkedIn", "com.linkedin.android", "com.linkedin.android.Main", 0, "linkedin");
        int noisyScore = AppsManager.AppUtils.scoreLaunchCandidate(
                "Other", "com.example.other", "com.example.other.Main", 999, "linkedin");

        assertTrue(exactScore > noisyScore);
    }
}
