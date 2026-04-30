package bhupendra.ai.launcher.ai;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SkillRegistryTest {

    @Test
    public void storeAndLoadSkillByTrigger() {
        File file = new File(System.getProperty("java.io.tmpdir"), "ai_skills_test_" + System.nanoTime() + ".json");
        SkillRegistry registry = new SkillRegistry(file);

        registry.store(
                "subtitle_pipeline",
                "when user asks to process subtitles with gemma",
                "Run the subtitle pipeline with gemma 4 and keep timestamps intact."
        );

        String loaded = registry.loadSkill("process subtitles with gemma");

        assertTrue(loaded.contains("subtitle_pipeline"));
        assertTrue(loaded.contains("keep timestamps intact"));
        file.delete();
    }

    @Test
    public void promptReminderContainsOnlyTriggerIndex() {
        File file = new File(System.getProperty("java.io.tmpdir"), "ai_skills_test_" + System.nanoTime() + ".json");
        SkillRegistry registry = new SkillRegistry(file);

        registry.store("release_flow", "when publishing Alpha builds", "Build release, install, tag, and upload.");

        String reminder = registry.promptReminder();

        assertTrue(reminder.contains("release_flow"));
        assertTrue(reminder.contains("when publishing Alpha builds"));
        assertTrue(!reminder.contains("Build release, install"));
        assertEquals(1, registry.size());
        file.delete();
    }
}
