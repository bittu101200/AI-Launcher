# AI System And Config Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add built-in AI-accessible system and constrained config tools, plus a visible AI thinking indicator, without changing the existing `integrate` flow for app-launch tools.

**Architecture:** Keep app discovery tools in `integrate`, but register built-in system/config tools at AI subsystem startup using `ToolRegistry.Tier.SYSTEM`. Route all built-in execution through a single executor that validates arguments, delegates to existing launcher behavior where possible, and uses the existing confirmation gate in `AISubsystem` based on `ToolRiskClass`.

**Tech Stack:** Java 8, Android SDK APIs already used by the launcher, org.json, JUnit 4

---

## File Map

### Create
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/BuiltInTools.java`
  Registers startup `Tier.SYSTEM` tools and owns their schemas.
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/BuiltInToolExecutor.java`
  Executes `system.*` and `config.*` tools, delegating `launch:` tools to `AndroidToolExecutor`.
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/ConfigToolCatalog.java`
  Central allowlist for readable/writable config entries and their type normalization rules.
- `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolsTest.java`
  Verifies startup tool registration contents and risk classification.
- `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolExecutorTest.java`
  Verifies built-in executor validation, read/write behavior, and error cases.
- `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/ConfigToolCatalogTest.java`
  Verifies allowlisted config entries and normalization behavior.

### Modify
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/AISubsystem.java`
  Register built-in tools during construction and swap in the new executor when app context is available.
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/LauncherActivity.java`
  Expose a small UI-facing AI status API to show/hide the thinking indicator.
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/AITrigger.java`
  Replace the current append-only `[thinking...]` line with the shared indicator lifecycle.
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/ai.java`
  Use the same indicator lifecycle for explicit `ai ...` requests.
- `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/UIManager.java`
  Bind a dedicated AI status view and add a setter that updates it without polluting terminal history.
- `TUI-ConsoleLauncher/app/src/main/res/layout/input_up_layout.xml`
  Add a small AI status text view near the terminal/input area.
- `TUI-ConsoleLauncher/app/src/main/res/layout/input_down_layout.xml`
  Add the same AI status text view for bottom-input mode.
- `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/AISubsystemToolExecutionTest.java`
  Add startup registration and read-only auto-run coverage.

---

### Task 1: Register Built-In AI Tools At Startup

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/BuiltInTools.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/AISubsystem.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolsTest.java`

- [ ] **Step 1: Write the failing registration test**

Create `BuiltInToolsTest.java` with:

```java
package bhupendra.ai.launcher.ai;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BuiltInToolsTest {

    @Test
    public void register_addsExpectedSystemAndConfigTools() {
        ToolRegistry registry = new ToolRegistry();

        BuiltInTools.register(registry);

        List<Tool> tools = registry.getTools();
        assertNotNull(registry.lookup("system.get_brightness"));
        assertNotNull(registry.lookup("system.set_brightness"));
        assertNotNull(registry.lookup("system.get_wifi"));
        assertNotNull(registry.lookup("system.set_wifi"));
        assertNotNull(registry.lookup("system.get_bluetooth"));
        assertNotNull(registry.lookup("system.set_bluetooth"));
        assertNotNull(registry.lookup("system.get_volume"));
        assertNotNull(registry.lookup("system.set_volume"));
        assertNotNull(registry.lookup("config.list_sections"));
        assertNotNull(registry.lookup("config.list_entries"));
        assertNotNull(registry.lookup("config.get"));
        assertNotNull(registry.lookup("config.set"));
        assertTrue(tools.size() >= 12);
    }

    @Test
    public void register_marksReadOnlyAndMutatingToolsDifferently() {
        ToolRegistry registry = new ToolRegistry();

        BuiltInTools.register(registry);

        assertEquals(ToolRiskClass.READ_ONLY, registry.lookup("system.get_brightness").riskClass);
        assertEquals(ToolRiskClass.STATE_CHANGING, registry.lookup("system.set_brightness").riskClass);
        assertEquals(ToolRiskClass.READ_ONLY, registry.lookup("config.get").riskClass);
        assertEquals(ToolRiskClass.STATE_CHANGING, registry.lookup("config.set").riskClass);
    }
}
```

- [ ] **Step 2: Run the new test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.BuiltInToolsTest"
```

Expected: FAIL because `BuiltInTools` does not exist yet.

- [ ] **Step 3: Implement the built-in tool catalog**

Create `BuiltInTools.java` with:

```java
package bhupendra.ai.launcher.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BuiltInTools {

    private BuiltInTools() {}

    public static void register(ToolRegistry registry) {
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.get_brightness", "get brightness", noArgs(), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.set_brightness", "set brightness", mapOf("percentage", "Brightness percentage 0..100"), ToolRiskClass.STATE_CHANGING));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.get_wifi", "get wifi state", noArgs(), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.set_wifi", "set wifi state", mapOf("enabled", "true/false or on/off"), ToolRiskClass.STATE_CHANGING));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.get_bluetooth", "get bluetooth state", noArgs(), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.set_bluetooth", "set bluetooth state", mapOf("enabled", "true/false or on/off"), ToolRiskClass.STATE_CHANGING));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.get_volume", "get volume level", mapOf("stream", "voice_call, system, ring, media, alarm, notifications"), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("system.set_volume", "set volume level", mapOf("stream", "voice_call, system, ring, media, alarm, notifications", "percentage", "Volume percentage 0..100"), ToolRiskClass.STATE_CHANGING));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("config.list_sections", "list config sections", noArgs(), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("config.list_entries", "list allowed config entries", mapOf("section", "Config section name"), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("config.get", "get config value", mapOf("section", "Config section name", "key", "Allowed config key"), ToolRiskClass.READ_ONLY));
        registry.register(ToolRegistry.Tier.SYSTEM,
            new Tool("config.set", "set config value", mapOf("section", "Config section name", "key", "Allowed config key", "value", "New value"), ToolRiskClass.STATE_CHANGING));
    }

    private static Map<String, String> noArgs() {
        return new LinkedHashMap<String, String>();
    }

    private static Map<String, String> mapOf(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
```

- [ ] **Step 4: Register built-in tools when the subsystem is created**

Update `AISubsystem.java` constructor setup to register tools immediately after creating `toolRegistry`:

```java
this.toolRegistry = new ToolRegistry();
BuiltInTools.register(this.toolRegistry);
```

Also swap the default executor selection to the new built-in executor:

```java
this.toolExecutor = toolExecutor != null
    ? toolExecutor
    : (this.appContext != null ? new BuiltInToolExecutor() : new ToolExecutor() {
        @Override
        public String execute(Context context, Tool tool, ToolCall toolCall) {
            throw new IllegalStateException("Tool execution requires app context");
        }
    });
```

- [ ] **Step 5: Run the registration test again**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.BuiltInToolsTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/bhupendra/ai/launcher/ai/BuiltInTools.java app/src/main/java/bhupendra/ai/launcher/ai/AISubsystem.java app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolsTest.java
git commit -m "feat: register built-in AI system and config tools"
```

---

### Task 2: Implement Validated System Tool Execution

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/BuiltInToolExecutor.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolExecutorTest.java`

- [ ] **Step 1: Write failing tests for system validation and launch delegation**

Create `BuiltInToolExecutorTest.java` with:

```java
package bhupendra.ai.launcher.ai;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class BuiltInToolExecutorTest {

    @Test
    public void execute_rejectsBrightnessOutsideRange() {
        BuiltInToolExecutor executor = new BuiltInToolExecutor();
        Tool tool = new Tool("system.set_brightness", "set brightness", Collections.singletonMap("percentage", "Brightness percentage 0..100"), ToolRiskClass.STATE_CHANGING);
        ToolCall call = new ToolCall("c1", "system.set_brightness", "{\"percentage\":\"150\"}");

        try {
            executor.execute(null, tool, call);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("0..100"));
        }
    }

    @Test
    public void execute_rejectsUnknownVolumeStream() {
        BuiltInToolExecutor executor = new BuiltInToolExecutor();
        Tool tool = new Tool("system.set_volume", "set volume", Collections.<String, String>emptyMap(), ToolRiskClass.STATE_CHANGING);
        ToolCall call = new ToolCall("c1", "system.set_volume", "{\"stream\":\"podcast\",\"percentage\":\"10\"}");

        try {
            executor.execute(null, tool, call);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("stream"));
        }
    }

    @Test
    public void execute_delegatesLaunchToolsToAndroidExecutor() throws Exception {
        BuiltInToolExecutor executor = new BuiltInToolExecutor(new ToolExecutor() {
            @Override
            public String execute(android.content.Context context, Tool tool, ToolCall toolCall) {
                return "delegated:" + tool.name;
            }
        });

        Tool tool = new Tool("launch:com.apple.android.music", "Apple Music", Collections.<String, String>emptyMap(), ToolRiskClass.LAUNCH_ONLY);
        String output = executor.execute(null, tool, new ToolCall("c1", tool.name, "{}"));

        assertEquals("delegated:launch:com.apple.android.music", output);
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.BuiltInToolExecutorTest"
```

Expected: FAIL because `BuiltInToolExecutor` does not exist yet.

- [ ] **Step 3: Implement the built-in executor skeleton and validation helpers**

Create `BuiltInToolExecutor.java` with:

```java
package bhupendra.ai.launcher.ai;

import android.content.Context;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BuiltInToolExecutor implements ToolExecutor {

    private static final Set<String> VOLUME_STREAMS = new HashSet<>(Arrays.asList(
        "voice_call", "system", "ring", "media", "alarm", "notifications"));

    private final ToolExecutor launchDelegate;

    public BuiltInToolExecutor() {
        this(new AndroidToolExecutor());
    }

    BuiltInToolExecutor(ToolExecutor launchDelegate) {
        this.launchDelegate = launchDelegate;
    }

    @Override
    public String execute(Context context, Tool tool, ToolCall toolCall) throws Exception {
        if (tool.name.startsWith("launch:")) {
            return launchDelegate.execute(context, tool, toolCall);
        }

        JSONObject args = new JSONObject(toolCall.arguments == null || toolCall.arguments.isEmpty() ? "{}" : toolCall.arguments);

        if ("system.set_brightness".equals(tool.name)) {
            int percentage = parsePercentage(args.getString("percentage"));
            return setBrightness(context, percentage);
        }
        if ("system.set_volume".equals(tool.name)) {
            String stream = normalizeStream(args.getString("stream"));
            int percentage = parsePercentage(args.getString("percentage"));
            return setVolume(context, stream, percentage);
        }

        throw new IllegalArgumentException("Tool not supported by BuiltInToolExecutor: " + tool.name);
    }

    private int parsePercentage(String raw) {
        int value = Integer.parseInt(raw);
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("percentage must be in range 0..100");
        }
        return value;
    }

    private String normalizeStream(String raw) {
        String value = raw.trim().toLowerCase();
        if (!VOLUME_STREAMS.contains(value)) {
            throw new IllegalArgumentException("Unknown stream: " + raw);
        }
        return value;
    }

    private String setBrightness(Context context, int percentage) {
        return "TODO replace in task step 4";
    }

    private String setVolume(Context context, String stream, int percentage) {
        return "TODO replace in task step 4";
    }
}
```

- [ ] **Step 4: Replace the temporary system stubs with actual command-backed behavior**

In `BuiltInToolExecutor.java`, replace the temporary methods with real command-backed methods. Keep them small and explicit:

```java
private String setBrightness(Context context, int percentage) throws Exception {
    bhupendra.ai.launcher.commands.main.raw.brightness command = new bhupendra.ai.launcher.commands.main.raw.brightness();
    bhupendra.ai.launcher.commands.ExecutePack pack = new bhupendra.ai.launcher.commands.ExecutePack();
    pack.context = context;
    pack.args = new Object[] { percentage };
    String output = command.exec(pack);
    return output != null ? output : "[brightness set to " + percentage + "%]";
}

private String setVolume(Context context, String stream, int percentage) throws Exception {
    int streamIndex = mapStream(stream);
    bhupendra.ai.launcher.commands.main.raw.volume command = new bhupendra.ai.launcher.commands.main.raw.volume();
    bhupendra.ai.launcher.commands.ExecutePack pack = new bhupendra.ai.launcher.commands.ExecutePack();
    pack.context = context;
    pack.args = new Object[] { "-set", streamIndex, percentage };
    String output = command.exec(pack);
    return output != null ? output : "[volume set: " + stream + " -> " + percentage + "%]";
}
```

Then add the same pattern for:

```java
system.get_brightness
system.get_wifi
system.set_wifi
system.get_bluetooth
system.set_bluetooth
system.get_volume
```

Use current launcher behavior as the source of truth for permissions and settings intents.

- [ ] **Step 5: Run the executor test again**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.BuiltInToolExecutorTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/bhupendra/ai/launcher/ai/BuiltInToolExecutor.java app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolExecutorTest.java
git commit -m "feat: add validated AI system tool execution"
```

---

### Task 3: Add A Constrained Config Allowlist And Executor Paths

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/ConfigToolCatalog.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/BuiltInToolExecutor.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/ConfigToolCatalogTest.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolExecutorTest.java`

- [ ] **Step 1: Write failing tests for the config allowlist**

Create `ConfigToolCatalogTest.java` with:

```java
package bhupendra.ai.launcher.ai;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigToolCatalogTest {

    @Test
    public void find_returnsAllowlistedEntry() {
        ConfigToolCatalog.Entry entry = ConfigToolCatalog.find("ai", "model");

        assertNotNull(entry);
        assertEquals("ai", entry.section);
        assertEquals("model", entry.key);
        assertEquals(ConfigToolCatalog.ValueType.TEXT, entry.valueType);
    }

    @Test
    public void find_returnsNullForUnknownEntry() {
        assertNull(ConfigToolCatalog.find("ui", "font_path"));
    }

    @Test
    public void normalizeBooleanAcceptsOnOff() {
        assertEquals("true", ConfigToolCatalog.normalize(ConfigToolCatalog.ValueType.BOOLEAN, "on"));
        assertEquals("false", ConfigToolCatalog.normalize(ConfigToolCatalog.ValueType.BOOLEAN, "off"));
    }
}
```

- [ ] **Step 2: Run the allowlist test to verify it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.ConfigToolCatalogTest"
```

Expected: FAIL because `ConfigToolCatalog` does not exist yet.

- [ ] **Step 3: Implement a conservative config catalog**

Create `ConfigToolCatalog.java` with:

```java
package bhupendra.ai.launcher.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigToolCatalog {

    public enum ValueType { TEXT, INTEGER, BOOLEAN }

    public static final class Entry {
        public final String section;
        public final String key;
        public final ValueType valueType;
        public final String description;

        Entry(String section, String key, ValueType valueType, String description) {
            this.section = section;
            this.key = key;
            this.valueType = valueType;
            this.description = description;
        }
    }

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        add("ai", "provider", ValueType.TEXT, "AI provider");
        add("ai", "model", ValueType.TEXT, "AI model");
        add("ai", "always_on_fallback", ValueType.BOOLEAN, "AI shell fallback behavior");
        add("ai", "max_tokens", ValueType.INTEGER, "AI max response tokens");
        add("ai", "confirm_state_changing", ValueType.BOOLEAN, "Confirmation for state-changing tools");
    }

    private ConfigToolCatalog() {}

    public static Entry find(String section, String key) {
        return ENTRIES.get(section.toLowerCase() + ":" + key.toLowerCase());
    }

    public static String normalize(ValueType type, String raw) {
        String value = raw.trim();
        switch (type) {
            case BOOLEAN:
                if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) return "true";
                if ("off".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return "false";
                throw new IllegalArgumentException("Expected boolean value");
            case INTEGER:
                Integer.parseInt(value);
                return value;
            default:
                return value;
        }
    }

    private static void add(String section, String key, ValueType type, String description) {
        ENTRIES.put(section + ":" + key, new Entry(section, key, type, description));
    }
}
```

- [ ] **Step 4: Extend the built-in executor for config.list/get/set**

In `BuiltInToolExecutor.java`, add config handling using the catalog:

```java
if ("config.list_sections".equals(tool.name)) {
    return "ai";
}
if ("config.list_entries".equals(tool.name)) {
    String section = args.optString("section", "ai");
    return ConfigToolCatalog.listEntries(section);
}
if ("config.get".equals(tool.name)) {
    String section = args.getString("section");
    String key = args.getString("key");
    ConfigToolCatalog.Entry entry = requireConfigEntry(section, key);
    return readConfigValue(entry);
}
if ("config.set".equals(tool.name)) {
    String section = args.getString("section");
    String key = args.getString("key");
    ConfigToolCatalog.Entry entry = requireConfigEntry(section, key);
    String value = ConfigToolCatalog.normalize(entry.valueType, args.getString("value"));
    writeConfigValue(context, entry, value);
    return "[config updated: " + entry.section + "." + entry.key + " -> " + value + "]";
}
```

Also add helpers that map the allowlisted `ai` section to the existing `Ai` enum:

```java
private String readConfigValue(ConfigToolCatalog.Entry entry) {
    if ("ai".equals(entry.section) && "model".equals(entry.key)) {
        return bhupendra.ai.launcher.managers.xml.XMLPrefsManager.get(
            bhupendra.ai.launcher.managers.xml.options.Ai.model);
    }
    throw new IllegalArgumentException("Unsupported config entry: " + entry.section + "." + entry.key);
}

private void writeConfigValue(Context context, ConfigToolCatalog.Entry entry, String value) {
    if ("ai".equals(entry.section) && "model".equals(entry.key)) {
        bhupendra.ai.launcher.managers.xml.options.Ai.model.parent().write(
            bhupendra.ai.launcher.managers.xml.options.Ai.model, value);
        return;
    }
    throw new IllegalArgumentException("Unsupported config entry: " + entry.section + "." + entry.key);
}
```

Implement the same pattern for each allowlisted `Ai` enum entry from `ConfigToolCatalog`.

- [ ] **Step 5: Add executor tests for allowed and rejected config access**

Append to `BuiltInToolExecutorTest.java`:

```java
@Test
public void execute_rejectsConfigEntryOutsideAllowlist() {
    BuiltInToolExecutor executor = new BuiltInToolExecutor();
    Tool tool = new Tool("config.set", "set config", Collections.<String, String>emptyMap(), ToolRiskClass.STATE_CHANGING);
    ToolCall call = new ToolCall("c1", "config.set", "{\"section\":\"ui\",\"key\":\"font_path\",\"value\":\"bad\"}");

    try {
        executor.execute(null, tool, call);
        fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
        assertTrue(expected.getMessage().contains("Unsupported config entry"));
    }
}
```

- [ ] **Step 6: Run the config-focused tests**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.ConfigToolCatalogTest" --tests "bhupendra.ai.launcher.ai.BuiltInToolExecutorTest"
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/bhupendra/ai/launcher/ai/ConfigToolCatalog.java app/src/main/java/bhupendra/ai/launcher/ai/BuiltInToolExecutor.java app/src/test/java/bhupendra/ai/launcher/ai/ConfigToolCatalogTest.java app/src/test/java/bhupendra/ai/launcher/ai/BuiltInToolExecutorTest.java
git commit -m "feat: add constrained AI config tools"
```

---

### Task 4: Extend AI Tool Execution Tests For Startup And Read-Only Auto-Run

**Files:**
- Modify: `TUI-ConsoleLauncher/app/src/test/java/bhupendra/ai/launcher/ai/AISubsystemToolExecutionTest.java`

- [ ] **Step 1: Add a failing test for built-in startup availability**

Append to `AISubsystemToolExecutionTest.java`:

```java
@Test
public void constructor_registersBuiltInSystemTools() {
    AISubsystem subsystem = new AISubsystem(new bhupendra.ai.launcher.ai.providers.MockProvider("ok"));

    assertNotNull(subsystem.getToolRegistry().lookup("system.get_brightness"));
    assertNotNull(subsystem.getToolRegistry().lookup("config.get"));
}
```

- [ ] **Step 2: Add a failing test for read-only tools auto-running without confirmation**

Append to `AISubsystemToolExecutionTest.java`:

```java
@Test
public void submit_autoRunsReadOnlyToolWithoutConfirmation() throws InterruptedException {
    ToolCall call = new ToolCall("call-1", "config.get", "{\"section\":\"ai\",\"key\":\"model\"}");
    bhupendra.ai.launcher.ai.providers.MockProvider provider = new bhupendra.ai.launcher.ai.providers.MockProvider(Collections.singletonList(call));
    java.util.concurrent.atomic.AtomicInteger executeCount = new java.util.concurrent.atomic.AtomicInteger();

    AISubsystem subsystem = new AISubsystem(provider, null, new ToolExecutor() {
        @Override
        public String execute(android.content.Context context, Tool tool, ToolCall toolCall) {
            executeCount.incrementAndGet();
            return "gemini-flash-latest";
        }
    });

    subsystem.getToolRegistry().register(
        ToolRegistry.Tier.SYSTEM,
        new Tool("config.get", "get config", Collections.<String, String>emptyMap(), ToolRiskClass.READ_ONLY));

    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<String> responseText = new AtomicReference<>();

    subsystem.submit("what model are you using", new AICallback() {
        @Override public void onToken(String rid, String token) {}
        @Override public void onResponse(AIResponse response) {
            responseText.set(response.text);
            latch.countDown();
        }
        @Override public void onStateChange(String rid, AIRequestState state) {}
    });

    assertTrue(latch.await(1, TimeUnit.SECONDS));
    assertEquals(1, executeCount.get());
    assertFalse(subsystem.isAwaitingConfirmation());
    assertEquals("gemini-flash-latest", responseText.get());
}
```

- [ ] **Step 3: Run the subsystem execution tests**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.AISubsystemToolExecutionTest"
```

Expected: PASS once Tasks 1-3 are in place.

- [ ] **Step 4: Commit**

```bash
git add app/src/test/java/bhupendra/ai/launcher/ai/AISubsystemToolExecutionTest.java
git commit -m "test: cover built-in AI tool startup and read-only execution"
```

---

### Task 5: Add A Real AI Thinking Indicator In The Launcher UI

**Files:**
- Modify: `TUI-ConsoleLauncher/app/src/main/res/layout/input_up_layout.xml`
- Modify: `TUI-ConsoleLauncher/app/src/main/res/layout/input_down_layout.xml`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/UIManager.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/LauncherActivity.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/ai/AITrigger.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/bhupendra/ai/launcher/commands/main/raw/ai.java`

- [ ] **Step 1: Add a dedicated status view to both input layouts**

In `input_up_layout.xml`, insert this `TextView` between `tools_view` and `ScrollView`:

```xml
<bhupendra.ai.launcher.tuils.OutlineTextView
    android:id="@+id/ai_status_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone"
    android:text="[AI is thinking...]"
    android:gravity="start" />
```

In `input_down_layout.xml`, insert the same view above `input_group` and below the `ScrollView` anchor:

```xml
<bhupendra.ai.launcher.tuils.OutlineTextView
    android:id="@+id/ai_status_view"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:visibility="gone"
    android:text="[AI is thinking...]"
    android:layout_above="@id/input_group" />
```

- [ ] **Step 2: Bind the status view in `UIManager` and add explicit show/hide methods**

In `UIManager.java`, add a field and bind it when the layout is inflated:

```java
private TextView aiStatusView;
```

```java
aiStatusView = (TextView) inputOutputView.findViewById(R.id.ai_status_view);
if (aiStatusView != null) {
    applyBgRect(aiStatusView, bgRectColors[OUTPUT_BGCOLOR_INDEX], bgColors[OUTPUT_BGCOLOR_INDEX], margins[OUTPUT_MARGINS_INDEX], strokeWidth, cornerRadius);
    applyShadow(aiStatusView, outlineColors[OUTPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);
}
```

Add methods:

```java
public void showAiStatus(final String text) {
    if (aiStatusView == null) return;
    aiStatusView.post(() -> {
        aiStatusView.setText(text);
        aiStatusView.setVisibility(View.VISIBLE);
    });
}

public void hideAiStatus() {
    if (aiStatusView == null) return;
    aiStatusView.post(() -> aiStatusView.setVisibility(View.GONE));
}
```

- [ ] **Step 3: Expose the status API through `LauncherActivity`**

In `LauncherActivity.java`, add:

```java
public void showAiStatus(String text) {
    if (ui != null) {
        ui.showAiStatus(text);
    }
}

public void hideAiStatus() {
    if (ui != null) {
        ui.hideAiStatus();
    }
}
```

- [ ] **Step 4: Replace append-only thinking output in `AITrigger`**

Update `AITrigger.java` so request state controls the indicator instead of appending a terminal line at the start:

```java
private void showThinking() {
    if (context instanceof bhupendra.ai.launcher.LauncherActivity) {
        ((bhupendra.ai.launcher.LauncherActivity) context).showAiStatus("[AI is thinking...]");
    }
}

private void hideThinking() {
    if (context instanceof bhupendra.ai.launcher.LauncherActivity) {
        ((bhupendra.ai.launcher.LauncherActivity) context).hideAiStatus();
    }
}
```

Then use them:

```java
showThinking();
```

and inside callbacks:

```java
@Override public void onResponse(AIResponse response) {
    hideThinking();
    ...
}

@Override public void onStateChange(String rid, AIRequestState state) {
    switch (state) {
        case EXECUTING_TOOLS:
        case COMPLETED:
        case FAILED:
        case CANCELLED:
        case TIMED_OUT_CONNECT:
        case TIMED_OUT_INACTIVITY:
            hideThinking();
            break;
        default:
            break;
    }
    ...
}
```

- [ ] **Step 5: Apply the same indicator lifecycle to the explicit `ai` command**

In `commands/main/raw/ai.java`, show the indicator before submit and hide it in both `onResponse` and terminal states:

```java
if (pack.context instanceof bhupendra.ai.launcher.LauncherActivity) {
    ((bhupendra.ai.launcher.LauncherActivity) pack.context).showAiStatus("[AI is thinking...]");
}
```

and:

```java
private void hideAiStatus() {
    if (pack.context instanceof bhupendra.ai.launcher.LauncherActivity) {
        ((bhupendra.ai.launcher.LauncherActivity) pack.context).hideAiStatus();
    }
}
```

Call `hideAiStatus()` in:

```java
onResponse(...)
FAILED
CANCELLED
TIMED_OUT_CONNECT
TIMED_OUT_INACTIVITY
EXECUTING_TOOLS
COMPLETED
```

- [ ] **Step 6: Build the app and verify the layouts compile**

Run:

```bash
./gradlew assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/res/layout/input_up_layout.xml app/src/main/res/layout/input_down_layout.xml app/src/main/java/bhupendra/ai/launcher/UIManager.java app/src/main/java/bhupendra/ai/launcher/LauncherActivity.java app/src/main/java/bhupendra/ai/launcher/ai/AITrigger.java app/src/main/java/bhupendra/ai/launcher/commands/main/raw/ai.java
git commit -m "feat: add launcher AI thinking indicator"
```

---

### Task 6: Full Verification On Device

**Files:**
- Modify: none
- Test: existing tests plus manual device verification

- [ ] **Step 1: Run all focused AI tests together**

Run:

```bash
./gradlew testDebugUnitTest --tests "bhupendra.ai.launcher.ai.BuiltInToolsTest" --tests "bhupendra.ai.launcher.ai.BuiltInToolExecutorTest" --tests "bhupendra.ai.launcher.ai.ConfigToolCatalogTest" --tests "bhupendra.ai.launcher.ai.AISubsystemToolExecutionTest"
```

Expected: `BUILD SUCCESSFUL` with all targeted tests passing.

- [ ] **Step 2: Build and install the APK on device**

Run:

```bash
./gradlew assembleDebug
/usr/local/share/android-commandlinetools/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: `Success` from `adb install`.

- [ ] **Step 3: Verify startup tool availability and thinking indicator manually**

On device, run these in order:

```text
ai list all available tools to you
ai show my ai config options
ai set brightness to 10%
ai open Apple Music
```

Expected:

```text
[AI is thinking...]
```

appears while each request is pending and disappears once output or confirmation is shown.

- [ ] **Step 4: Verify risk behavior and config writes manually**

On device, confirm these expectations:

```text
ai what model are you using
```

Expected: no confirmation, read-only output.

```text
ai change ai model to gemini-flash-latest
```

Expected: confirmation prompt before write.

```text
ai set wifi off
```

Expected: confirmation prompt before state change.

- [ ] **Step 5: Capture logs for any mismatch**

Run if behavior differs from expectation:

```bash
/usr/local/share/android-commandlinetools/platform-tools/adb logcat -d -s "AI_CMD:*" "OpenAIProvider:*" "AISubsystem:*"
```

- [ ] **Step 6: Commit final verification-ready state**

```bash
git add app/src/main/java app/src/main/res/layout app/src/test/java docs/superpowers/specs/2026-04-19-ai-system-and-config-tools-design.md docs/superpowers/plans/2026-04-19-ai-system-and-config-tools.md
git commit -m "feat: add AI system tools, config tools, and thinking indicator"
```

---

## Self-Review

### Spec Coverage
- Built-in startup registration: Task 1
- System tools: Task 2
- Constrained config tools: Task 3
- Existing confirmation model by risk: Tasks 1, 4, and 6
- Thinking indicator: Task 5
- Device verification: Task 6

### Placeholder Scan
- No `TBD` or `implement later` markers remain in the plan.
- The temporary `TODO replace in task step 4` string appears only inside Task 2 Step 3 as an intentional intermediate state and is explicitly removed in the next step.

### Type Consistency
- Built-in tool names use the spec-approved structured names: `system.*` and `config.*`.
- Risk handling consistently uses `ToolRiskClass.READ_ONLY` for reads and `ToolRiskClass.STATE_CHANGING` for writes.
- Config access consistently routes through `ConfigToolCatalog.Entry` and the existing `Ai` XML enum.
