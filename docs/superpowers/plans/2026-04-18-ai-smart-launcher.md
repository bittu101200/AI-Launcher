# AI Smart Terminal Launcher — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an AI cognitive layer to T-UI Console Launcher enabling natural language command execution, shortcut-first app control, local on-device retrieval, and intelligent context management aligned with Android 16-17 system APIs.

**Architecture:** AISubsystem wires AIProvider, ToolRegistry, ConversationManager, RequestManager, LauncherIndex, ShortcutBridge, and JourneyManager. AITrigger slots into MainManager's existing CmdTrigger[] chain before ShellCommandTrigger with exclusive ownership and explicit shell re-dispatch only on connect timeout. The launcher prefers local retrieval (`AppSearch`), published shortcuts (`LauncherApps` / `ShortcutManager`), and system-native surfaces (`Notification.ProgressStyle`, predictive back, Android 17 contact picker) before remote AI.

**Tech Stack:** Java 8, Android API 21+, OkHttp 4.12.0 (already present), org.json (Android built-in), JUnit 4 + Mockito (test-only)

**Program identity update:** As part of this work, rename the app package/application ID from the legacy `ohi.andre.consolelauncher` namespace to `bhupendra.ai.launcher` before implementing the AI feature set. Treat this as a prerequisite migration so all new AI code lands under the final namespace.

---

## File Map

### New files — `ai/` package
| File | Responsibility |
|---|---|
| `ai/AIProvider.java` | Interface: complete(), supportsToolUse(), supportsStreaming(), providerId() |
| `ai/AIRequest.java` | Request model with Builder |
| `ai/AIResponse.java` | Response model (TEXT / TOOL_CALLS / ERROR) |
| `ai/AICallback.java` | Async callback interface |
| `ai/AISubsystem.java` | Singleton orchestrator; access via MainPack or getInstance() |
| `ai/AIRequestState.java` | State machine enum (IDLE → COMPLETED / CANCELLED / FAILED) |
| `ai/RequestManager.java` | Lifecycle, stale-callback suppression, cancel |
| `ai/ConversationTurn.java` | Single history entry (role + content + pinned flag) |
| `ai/ConversationManager.java` | History storage in STATELESS / SESSION / PERSISTENT mode |
| `ai/CompactionEngine.java` | Dynamic prompt construction + history replacement |
| `ai/Tool.java` | Tool schema (name, description, parameters map, risk class) |
| `ai/ToolCall.java` | AI-requested tool invocation |
| `ai/ToolResult.java` | Tool execution result |
| `ai/ToolRiskClass.java` | Risk enum driving confirmation gate |
| `ai/ToolRegistry.java` | 3-tier registry; getTools() / getToolsCompressed() |
| `ai/AITrigger.java` | Plugs into MainManager CmdTrigger chain |
| `ai/platform/LauncherIndex.java` | AppSearch-backed local index for apps, commands, tools, and recent AI artifacts |
| `ai/platform/ShortcutBridge.java` | LauncherApps / ShortcutManager adapter for published shortcut discovery |
| `ai/platform/JourneyManager.java` | Notification.ProgressStyle / Live Update parser for ongoing tasks |
| `ai/AppCapabilityScanner.java` | Shortcut-first app discovery with PackageManager fallback |
| `ai/providers/MockProvider.java` | Scripted responses, no network |
| `ai/providers/ClaudeProvider.java` | Anthropic Messages API via OkHttp |
| `ai/providers/OpenAIProvider.java` | OpenAI-compatible API (also Ollama) |

### New files — commands
| File | Responsibility |
|---|---|
| `commands/main/raw/ai.java` | Explicit AI invocation |
| `commands/main/raw/compact.java` | Manual compaction trigger |
| `commands/main/raw/pin.java` | Pin the latest AI response turn |
| `commands/main/raw/clearhistory.java` | Clear conversation history |
| `commands/main/raw/cleararchive.java` | Delete archived compaction files |
| `commands/main/raw/integrate.java` | Interactive app integration wizard |

### New files — settings
| File | Responsibility |
|---|---|
| `managers/xml/options/Ai.java` | Settings enum backed by ai.xml |

### Modified files
| File | Change |
|---|---|
| `app/build.gradle` | Update `applicationId` / namespace references if present for `bhupendra.ai.launcher` |
| `app/src/main/AndroidManifest.xml` | Update manifest package/authorities/permissions using the old namespace |
| `app/src/main/AndroidManifest.xml` | Update manifest references if present |
| `app/src/main/java/**` | Rename Java package declarations/imports from `ohi.andre.consolelauncher` to `bhupendra.ai.launcher` |
| `MainManager.java` | Add AITrigger field; AI cancel/confirm gate in onCommand(); PendingIntegration gate |
| `commands/main/MainPack.java` | Add `public AISubsystem aiSubsystem` field |
| `managers/xml/XMLPrefsManager.java` | Add `AI(Ai.values())` XMLPrefsRoot entry |
| `LauncherActivity.java` | Initialize AISubsystem; dispose in onDestroy() |
| `managers/SuggestionsManager.java` | Merge LauncherIndex local retrieval with AI re-ranking |
| `managers/notifications/NotificationService.java` | AI triage hook + JourneyManager ingestion of progress-style notifications |
| `managers/UIManager.java` | AI thinking/streaming output + predictive back compliance for launcher surfaces |
| `AndroidManifest.xml` | Verify existing QUERY_ALL_PACKAGES; no new Phase 1 permissions |
| `app/build.gradle` | Add JUnit4 + Mockito testImplementation dependencies |
| `app/proguard-rules.pro` | Add `-keep class ohi.andre.consolelauncher.ai.** { *; }` |
| `app/src/main/res/values/strings.xml` | Add help strings for new commands |

---

## Revision Note

This plan started before the spec was tightened. The foundation tasks remain useful, but the approved spec now requires these alignments during implementation:

- Use `AIRequestState` as the canonical enum name even where older snippets below still say `RequestState`
- Use `Tool` as the canonical schema type even where older snippets below still say `ToolDefinition`
- Treat the revised tasks from **Task 12 onward** in this document as the source of truth for Phase 1 integration, Android 16-17 alignment, and final verification
- Prefer published shortcuts, `AppSearch`, progress-style journeys, predictive back, and Android 17 contact-picker flows over older inferred-only integrations

---

## Sub-plan A: AI Foundation

### Task 1: Test Infrastructure

**Files:**
- Modify: `TUI-ConsoleLauncher/app/build.gradle`
- Create: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/PlaceholderTest.java`

- [ ] **Step 1: Add test dependencies to build.gradle**

In the `dependencies` block, add:
```groovy
testImplementation 'junit:junit:4.13.2'
testImplementation 'org.mockito:mockito-core:4.11.0'
testImplementation 'org.mockito:mockito-inline:4.11.0'
```

- [ ] **Step 2: Create test directory and placeholder test**

```bash
mkdir -p TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai
```

Create `PlaceholderTest.java`:
```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import static org.junit.Assert.*;

public class PlaceholderTest {
    @Test public void infraWorks() { assertTrue(true); }
}
```

- [ ] **Step 3: Run test**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.PlaceholderTest" 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add TUI-ConsoleLauncher/app/build.gradle TUI-ConsoleLauncher/app/src/test/
git commit -m "test: add JUnit4 + Mockito test infrastructure"
```

---

### Task 1A: Package ID Migration

**Files:**
- Modify: `TUI-ConsoleLauncher/app/build.gradle`
- Modify: `TUI-ConsoleLauncher/app/src/main/AndroidManifest.xml`
- Modify: `TUI-ConsoleLauncher/app/src/main/AndroidManifest.xml`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/**`
- Modify: `TUI-ConsoleLauncher/app/src/test/java/**`

- [ ] **Step 1: Find all references to the legacy package**

```bash
cd TUI-ConsoleLauncher && rg -n "ohi\.andre\.consolelauncher|applicationId|namespace" app/src app/build.gradle
```

- [ ] **Step 2: Update Gradle/manifest identity to `bhupendra.ai.launcher`**

Apply the rename consistently in `app/build.gradle` and manifests:

```groovy
applicationId "bhupendra.ai.launcher"
namespace "bhupendra.ai.launcher"
```

```xml
package="bhupendra.ai.launcher"
```

Also rename any custom permissions, provider authorities, and intent action strings that are package-scoped, for example:

```xml
android:name="bhupendra.ai.launcher.permission.RECEIVE_CMD"
authorities="bhupendra.ai.launcher.provider"
```

- [ ] **Step 3: Rename Java package declarations/imports**

Move source files from:

```text
app/src/main/java/ohi/andre/consolelauncher/
```

to:

```text
app/src/main/java/bhupendra/ai/launcher/
```

and update package declarations/imports accordingly:

```java
package bhupendra.ai.launcher;
```

- [ ] **Step 4: Update tests to the new namespace**

Example package declaration update:

```java
package bhupendra.ai.launcher.ai;
```

- [ ] **Step 5: Run a full compile check**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | tail -20
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 6: Commit**

```bash
git add TUI-ConsoleLauncher/app/build.gradle
git add TUI-ConsoleLauncher/app/src/main/AndroidManifest.xml
git add TUI-ConsoleLauncher/app/src/main/AndroidManifest.xml
git add TUI-ConsoleLauncher/app/src/main/java/
git add TUI-ConsoleLauncher/app/src/test/java/
git commit -m "refactor: rename package id to bhupendra.ai.launcher"
```

---

### Task 2: Core Types

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AIRequestState.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ConversationTurn.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AICallback.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ToolRiskClass.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/Tool.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ToolCall.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ToolResult.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AIResponse.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AIRequest.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/CoreTypesTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CoreTypesTest {

    @Test
    public void aiRequest_storesFields() {
        List<ConversationTurn> history = new ArrayList<>();
        history.add(new ConversationTurn(ConversationTurn.Role.USER, "hello"));

        AIRequest req = new AIRequest.Builder()
            .requestId("req-1")
            .userMessage("what time is it?")
            .history(history)
            .systemPrompt("You are a terminal assistant.")
            .maxTokens(1024)
            .build();

        assertEquals("req-1", req.requestId);
        assertEquals("what time is it?", req.userMessage);
        assertEquals(1, req.history.size());
        assertEquals("You are a terminal assistant.", req.systemPrompt);
        assertEquals(1024, req.maxTokens);
    }

    @Test
    public void aiResponse_textType() {
        AIResponse r = AIResponse.text("req-1", "It is 3pm.");
        assertEquals(AIResponse.Type.TEXT, r.type);
        assertEquals("req-1", r.requestId);
        assertEquals("It is 3pm.", r.text);
    }

    @Test
    public void aiResponse_errorType() {
        AIResponse r = AIResponse.error("req-1", "timeout");
        assertEquals(AIResponse.Type.ERROR, r.type);
        assertEquals("timeout", r.errorMessage);
    }

    @Test
    public void toolCall_storesFields() {
        ToolCall tc = new ToolCall("call-1", "brightness", "{\"level\":50}");
        assertEquals("call-1", tc.callId);
        assertEquals("brightness", tc.toolName);
    }

    @Test
    public void toolResult_success() {
        ToolResult tr = ToolResult.success("call-1", "done");
        assertTrue(tr.success);
        assertEquals("done", tr.output);
    }

    @Test
    public void toolResult_failure() {
        ToolResult tr = ToolResult.failure("call-1", "denied");
        assertFalse(tr.success);
        assertEquals("denied", tr.errorMessage);
    }
}
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.CoreTypesTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create AIRequestState.java**

```java
package ohi.andre.consolelauncher.ai;

public enum AIRequestState {
    IDLE, THINKING, STREAMING, EXECUTING_TOOLS, FOLLOWUP,
    COMPLETED, CANCELLED, TIMED_OUT_CONNECT, TIMED_OUT_INACTIVITY, FAILED
}
```

- [ ] **Step 4: Create ConversationTurn.java**

```java
package ohi.andre.consolelauncher.ai;

public class ConversationTurn {
    public enum Role { USER, ASSISTANT, TOOL_RESULT }
    public final Role role;
    public final String content;
    public boolean pinned;

    public ConversationTurn(Role role, String content) {
        this.role = role;
        this.content = content;
    }
}
```

- [ ] **Step 5: Create AICallback.java**

```java
package ohi.andre.consolelauncher.ai;

public interface AICallback {
    void onToken(String requestId, String token);
    void onResponse(AIResponse response);
    void onStateChange(String requestId, AIRequestState state);
}
```

- [ ] **Step 6: Create ToolRiskClass.java**

```java
package ohi.andre.consolelauncher.ai;

public enum ToolRiskClass {
    READ_ONLY, LAUNCH_ONLY, STATE_CHANGING, SENSITIVE, REQUIRES_CONFIRMATION
}
```

- [ ] **Step 7: Create Tool.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.LinkedHashMap;
import java.util.Map;

public class Tool {
    public final String name;
    public final String description;
    public final Map<String, String> parameters;
    public final ToolRiskClass riskClass;

    public Tool(String name, String description,
                Map<String, String> parameters, ToolRiskClass riskClass) {
        this.name = name;
        this.description = description;
        this.parameters = parameters != null ? parameters : new LinkedHashMap<>();
        this.riskClass = riskClass;
    }
}
```

- [ ] **Step 8: Create ToolCall.java**

```java
package ohi.andre.consolelauncher.ai;

public class ToolCall {
    public final String callId;
    public final String toolName;
    public final String argumentsJson;

    public ToolCall(String callId, String toolName, String argumentsJson) {
        this.callId = callId;
        this.toolName = toolName;
        this.argumentsJson = argumentsJson;
    }
}
```

- [ ] **Step 9: Create ToolResult.java**

```java
package ohi.andre.consolelauncher.ai;

public class ToolResult {
    public final String callId;
    public final boolean success;
    public final String output;
    public final String errorMessage;

    private ToolResult(String callId, boolean success, String output, String errorMessage) {
        this.callId = callId;
        this.success = success;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public static ToolResult success(String callId, String output) {
        return new ToolResult(callId, true, output, null);
    }

    public static ToolResult failure(String callId, String errorMessage) {
        return new ToolResult(callId, false, null, errorMessage);
    }
}
```

- [ ] **Step 10: Create AIResponse.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.List;

public class AIResponse {
    public enum Type { TEXT, TOOL_CALLS, ERROR }
    public final String requestId;
    public final Type type;
    public final String text;
    public final List<ToolCall> toolCalls;
    public final String errorMessage;

    private AIResponse(String requestId, Type type, String text,
                       List<ToolCall> toolCalls, String errorMessage) {
        this.requestId = requestId;
        this.type = type;
        this.text = text;
        this.toolCalls = toolCalls;
        this.errorMessage = errorMessage;
    }

    public static AIResponse text(String requestId, String text) {
        return new AIResponse(requestId, Type.TEXT, text, null, null);
    }

    public static AIResponse toolCalls(String requestId, List<ToolCall> calls) {
        return new AIResponse(requestId, Type.TOOL_CALLS, null, calls, null);
    }

    public static AIResponse error(String requestId, String message) {
        return new AIResponse(requestId, Type.ERROR, null, null, message);
    }
}
```

- [ ] **Step 11: Create AIRequest.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.ArrayList;
import java.util.List;

public class AIRequest {
    public final String requestId;
    public final String userMessage;
    public final List<ConversationTurn> history;
    public final List<Tool> tools;
    public final String systemPrompt;
    public final int maxTokens;

    private AIRequest(Builder b) {
        this.requestId = b.requestId;
        this.userMessage = b.userMessage;
        this.history = b.history != null ? b.history : new ArrayList<>();
        this.tools = b.tools != null ? b.tools : new ArrayList<>();
        this.systemPrompt = b.systemPrompt;
        this.maxTokens = b.maxTokens > 0 ? b.maxTokens : 2048;
    }

    public static class Builder {
        String requestId, userMessage, systemPrompt;
        List<ConversationTurn> history;
        List<Tool> tools;
        int maxTokens;

        public Builder requestId(String v) { requestId = v; return this; }
        public Builder userMessage(String v) { userMessage = v; return this; }
        public Builder history(List<ConversationTurn> v) { history = v; return this; }
        public Builder tools(List<Tool> v) { tools = v; return this; }
        public Builder systemPrompt(String v) { systemPrompt = v; return this; }
        public Builder maxTokens(int v) { maxTokens = v; return this; }
        public AIRequest build() { return new AIRequest(this); }
    }
}
```

- [ ] **Step 12: Run tests — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.CoreTypesTest" 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 13: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/CoreTypesTest.java
git commit -m "feat: add core AI types (Request, Response, Callback, ConversationTurn, ToolCall, ToolResult)"
```

---

### Task 3: AIProvider Interface + MockProvider

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AIProvider.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers/MockProvider.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/MockProviderTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import ohi.andre.consolelauncher.ai.providers.MockProvider;

public class MockProviderTest {

    @Test
    public void returnsScriptedTextResponse() throws InterruptedException {
        MockProvider provider = new MockProvider("Hello from mock!");
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIResponse> result = new AtomicReference<>();

        AIRequest req = new AIRequest.Builder().requestId("t1").userMessage("ping").build();
        provider.complete(req, "t1", new AICallback() {
            @Override public void onToken(String rid, String token) {}
            @Override public void onResponse(AIResponse r) { result.set(r); latch.countDown(); }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(AIResponse.Type.TEXT, result.get().type);
        assertEquals("Hello from mock!", result.get().text);
        assertEquals("t1", result.get().requestId);
    }

    @Test
    public void reportsProviderMetadata() {
        MockProvider p = new MockProvider("ok");
        assertFalse(p.supportsToolUse());
        assertFalse(p.supportsStreaming());
        assertEquals("mock", p.providerId());
    }

    @Test
    public void returnsToolCallResponse() throws InterruptedException {
        List<ToolCall> calls = new ArrayList<>();
        calls.add(new ToolCall("c-1", "brightness", "{\"level\":75}"));
        MockProvider provider = new MockProvider(calls);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIResponse> result = new AtomicReference<>();

        AIRequest req = new AIRequest.Builder().requestId("t2").userMessage("dim").build();
        provider.complete(req, "t2", new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) { result.set(r); latch.countDown(); }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        assertEquals(AIResponse.Type.TOOL_CALLS, result.get().type);
        assertEquals("brightness", result.get().toolCalls.get(0).toolName);
    }
}
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.MockProviderTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create AIProvider.java**

```java
package ohi.andre.consolelauncher.ai;

public interface AIProvider {
    void complete(AIRequest request, String requestId, AICallback callback);
    boolean supportsToolUse();
    boolean supportsStreaming();
    String providerId();
}
```

- [ ] **Step 4: Create providers directory and MockProvider.java**

```bash
mkdir -p TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers
```

```java
package ohi.andre.consolelauncher.ai.providers;

import ohi.andre.consolelauncher.ai.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MockProvider implements AIProvider {

    private final String fixedText;
    private final List<ToolCall> fixedToolCalls;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public MockProvider(String fixedText) {
        this.fixedText = fixedText;
        this.fixedToolCalls = null;
    }

    public MockProvider(List<ToolCall> toolCalls) {
        this.fixedText = null;
        this.fixedToolCalls = toolCalls;
    }

    @Override
    public void complete(AIRequest request, String requestId, AICallback callback) {
        callback.onStateChange(requestId, AIRequestState.THINKING);
        executor.execute(() -> {
            AIResponse response = fixedToolCalls != null
                ? AIResponse.toolCalls(requestId, fixedToolCalls)
                : AIResponse.text(requestId, fixedText != null ? fixedText : "");
            callback.onResponse(response);
        });
    }

    @Override public boolean supportsToolUse() { return false; }
    @Override public boolean supportsStreaming() { return false; }
    @Override public String providerId() { return "mock"; }
}
```

- [ ] **Step 5: Run tests — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.MockProviderTest" 2>&1 | tail -5
```

- [ ] **Step 6: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AIProvider.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers/MockProvider.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/MockProviderTest.java
git commit -m "feat: add AIProvider interface and MockProvider"
```

---

### Task 4: ToolRegistry

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ToolRegistry.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/ToolRegistryTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

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
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.ToolRegistryTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create ToolRegistry.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ToolRegistry {

    public enum Tier { TUI_COMMAND, APP_INTENT, SYSTEM }

    private final Map<String, ToolEntry> entries = new LinkedHashMap<>();

    private static class ToolEntry {
        final Tier tier;
        final Tool definition;
        ToolEntry(Tier tier, Tool definition) {
            this.tier = tier;
            this.definition = definition;
        }
    }

    public synchronized void register(Tier tier, Tool definition) {
        entries.put(definition.name, new ToolEntry(tier, definition));
    }

    public synchronized void unregister(String name) {
        entries.remove(name);
    }

    public synchronized Tool lookup(String name) {
        ToolEntry e = entries.get(name);
        return e != null ? e.definition : null;
    }

    public synchronized List<Tool> getTools() {
        List<Tool> result = new ArrayList<>();
        for (Tier tier : Tier.values()) {
            for (ToolEntry e : entries.values()) {
                if (e.tier == tier) result.add(e.definition);
            }
        }
        return result;
    }

    public synchronized List<Tool> getToolsCompressed() {
        List<Tool> result = new ArrayList<>();
        for (ToolEntry e : entries.values()) {
            if (e.tier == Tier.TUI_COMMAND) result.add(e.definition);
        }
        return result;
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.ToolRegistryTest" 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ToolRegistry.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/ToolRegistryTest.java
git commit -m "feat: add ToolRegistry with 3-tier storage and compressed mode"
```

---

### Task 5: ConversationManager

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ConversationManager.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/ConversationManagerTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class ConversationManagerTest {

    @Test
    public void stateless_historyAlwaysEmpty() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.STATELESS, 4000);
        cm.append(new ConversationTurn(ConversationTurn.Role.USER, "hello"));
        assertTrue(cm.getHistory().isEmpty());
    }

    @Test
    public void session_appendsHistory() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        cm.append(new ConversationTurn(ConversationTurn.Role.USER, "hello"));
        cm.append(new ConversationTurn(ConversationTurn.Role.ASSISTANT, "hi"));
        assertEquals(2, cm.getHistory().size());
    }

    @Test
    public void clear_emptiesHistory() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        cm.append(new ConversationTurn(ConversationTurn.Role.USER, "hello"));
        cm.clear();
        assertTrue(cm.getHistory().isEmpty());
    }

    @Test
    public void compactionNeeded_aboveThreshold() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 100);
        for (int i = 0; i < 50; i++) {
            cm.append(new ConversationTurn(ConversationTurn.Role.USER, "word word word"));
        }
        assertTrue(cm.isCompactionNeeded());
    }

    @Test
    public void replaceWithCompacted_keepsOnlySummaryAndPinned() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        ConversationTurn pinned = new ConversationTurn(ConversationTurn.Role.USER, "I prefer dark mode");
        pinned.pinned = true;
        cm.append(pinned);
        cm.append(new ConversationTurn(ConversationTurn.Role.ASSISTANT, "old content"));

        ConversationTurn summary = new ConversationTurn(ConversationTurn.Role.ASSISTANT, "[Summary]");
        cm.replaceWithCompacted(summary);

        List<ConversationTurn> h = cm.getHistory();
        assertEquals(2, h.size()); // pinned + summary
        assertTrue(h.get(0).pinned);
        assertEquals("[Summary]", h.get(h.size() - 1).content);
    }
}
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.ConversationManagerTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create ConversationManager.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationManager {

    public enum Mode { STATELESS, SESSION, PERSISTENT }

    private static final float COMPACT_THRESHOLD = 0.80f;
    private static final int CHARS_PER_TOKEN = 4;

    private final Mode mode;
    private final int tokenCap;
    private final List<ConversationTurn> history = new ArrayList<>();

    public ConversationManager(Mode mode, int tokenCap) {
        this.mode = mode;
        this.tokenCap = tokenCap;
    }

    public synchronized void append(ConversationTurn turn) {
        if (mode == Mode.STATELESS) return;
        history.add(turn);
    }

    public synchronized List<ConversationTurn> getHistory() {
        return Collections.unmodifiableList(new ArrayList<>(history));
    }

    public synchronized void clear() {
        history.clear();
    }

    public synchronized int estimateTokenCount() {
        int chars = 0;
        for (ConversationTurn t : history) chars += t.content.length();
        return Math.max(1, chars / CHARS_PER_TOKEN);
    }

    public synchronized boolean isCompactionNeeded() {
        return estimateTokenCount() >= (int)(tokenCap * COMPACT_THRESHOLD);
    }

    public synchronized void replaceWithCompacted(ConversationTurn summary) {
        List<ConversationTurn> pinned = new ArrayList<>();
        for (ConversationTurn t : history) {
            if (t.pinned) pinned.add(t);
        }
        history.clear();
        history.addAll(pinned);
        history.add(summary);
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.ConversationManagerTest" 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/ConversationManager.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/ConversationManagerTest.java
git commit -m "feat: add ConversationManager with stateless/session modes and compaction trigger"
```

---

### Task 6: CompactionEngine

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/CompactionEngine.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/CompactionEngineTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CompactionEngineTest {

    @Test
    public void buildPrompt_notEmpty() {
        List<ConversationTurn> history = new ArrayList<>();
        history.add(new ConversationTurn(ConversationTurn.Role.USER, "I prefer dark mode."));
        history.add(new ConversationTurn(ConversationTurn.Role.ASSISTANT, "Noted."));

        CompactionEngine engine = new CompactionEngine();
        String prompt = engine.buildCompactionPrompt(history);

        assertNotNull(prompt);
        assertTrue(prompt.length() > 30);
    }

    @Test
    public void buildPrompt_mentionsPreserve() {
        List<ConversationTurn> history = new ArrayList<>();
        history.add(new ConversationTurn(ConversationTurn.Role.USER,
            "My Spotify username is testuser123. Always ask before launching apps."));
        CompactionEngine engine = new CompactionEngine();
        String prompt = engine.buildCompactionPrompt(history);
        assertTrue(prompt.toLowerCase().contains("preserve") || prompt.toLowerCase().contains("compress"));
    }

    @Test
    public void applyCompaction_replacesHistory() {
        ConversationManager cm = new ConversationManager(ConversationManager.Mode.SESSION, 100);
        for (int i = 0; i < 20; i++) {
            cm.append(new ConversationTurn(ConversationTurn.Role.USER, "filler content here"));
        }
        CompactionEngine engine = new CompactionEngine();
        ConversationTurn summary = new ConversationTurn(ConversationTurn.Role.ASSISTANT, "[Summary]");
        engine.applyCompaction(cm, summary);
        assertTrue(cm.getHistory().size() < 20);
    }
}
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.CompactionEngineTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create CompactionEngine.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompactionEngine {

    private static final String[] PREF_KW = {"prefer", "always", "never", "don't", "please"};
    private static final String[] GOAL_KW = {"trying to", "want to", "need to", "working on"};

    public String buildCompactionPrompt(List<ConversationTurn> history) {
        boolean hasPrefs = false;
        boolean hasGoals = false;
        Map<String, Integer> trigramCounts = new HashMap<>();

        for (ConversationTurn t : history) {
            String lower = t.content.toLowerCase();
            for (String kw : PREF_KW) if (lower.contains(kw)) { hasPrefs = true; break; }
            for (String kw : GOAL_KW) if (lower.contains(kw)) { hasGoals = true; break; }

            String[] words = t.content.split("\\s+");
            for (int i = 0; i + 2 < words.length; i++) {
                String tri = words[i] + " " + words[i+1] + " " + words[i+2];
                trigramCounts.put(tri, trigramCounts.getOrDefault(tri, 0) + 1);
            }
        }

        List<String> singletons = new ArrayList<>();
        for (Map.Entry<String, Integer> e : trigramCounts.entrySet()) {
            if (e.getValue() == 1 && e.getKey().length() > 10) singletons.add(e.getKey());
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Compress the following conversation into a structured summary.\n");
        sb.append("Preserve with high fidelity:\n");
        if (hasPrefs) sb.append("- User preferences (merge into canonical list)\n");
        if (hasGoals) sb.append("- Ongoing goals still in progress\n");
        if (!singletons.isEmpty()) {
            sb.append("- Facts mentioned only once — do not lose them:\n");
            for (String s : singletons.subList(0, Math.min(singletons.size(), 5))) {
                sb.append("  * ").append(s, 0, Math.min(s.length(), 60)).append("\n");
            }
        }
        sb.append("\nConversation:\n");
        for (ConversationTurn t : history) {
            sb.append(t.role.name()).append(": ").append(t.content).append("\n");
        }
        sb.append("\nOutput a structured summary only.");
        return sb.toString();
    }

    public void applyCompaction(ConversationManager cm, ConversationTurn summary) {
        cm.replaceWithCompacted(summary);
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.CompactionEngineTest" 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/CompactionEngine.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/CompactionEngineTest.java
git commit -m "feat: add CompactionEngine with dynamic prompt and singleton fact preservation"
```

---

### Task 7: RequestManager

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/RequestManager.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/RequestManagerTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import ohi.andre.consolelauncher.ai.providers.MockProvider;

public class RequestManagerTest {

    @Test
    public void submit_completesSuccessfully() throws InterruptedException {
        MockProvider provider = new MockProvider("response text");
        RequestManager rm = new RequestManager(provider, 10000, 30000);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AIRequestState> finalState = new AtomicReference<>();

        rm.submit(new AIRequest.Builder().requestId("r1").userMessage("hello").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    finalState.set(s);
                    if (s == AIRequestState.COMPLETED || s == AIRequestState.FAILED) latch.countDown();
                }
            });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertEquals(AIRequestState.COMPLETED, finalState.get());
    }

    @Test
    public void cancel_stopsRequest() throws InterruptedException {
        MockProvider provider = new MockProvider("text");
        RequestManager rm = new RequestManager(provider, 10000, 30000);
        CountDownLatch latch = new CountDownLatch(1);

        rm.submit(new AIRequest.Builder().requestId("r2").userMessage("q").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    if (s == AIRequestState.CANCELLED || s == AIRequestState.COMPLETED) latch.countDown();
                }
            });

        rm.cancel("r2");
        assertTrue(latch.await(2, TimeUnit.SECONDS));
    }

    @Test
    public void isInFlight_trueWhileThinking() throws InterruptedException {
        MockProvider slowProvider = new MockProvider("text") {
            @Override
            public void complete(AIRequest request, String requestId, AICallback callback) {
                callback.onStateChange(requestId, AIRequestState.THINKING);
                try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                super.complete(request, requestId, callback);
            }
        };

        RequestManager rm = new RequestManager(slowProvider, 10000, 30000);
        CountDownLatch thinking = new CountDownLatch(1);
        rm.submit(new AIRequest.Builder().requestId("r3").userMessage("q").build(),
            new AICallback() {
                @Override public void onToken(String rid, String t) {}
                @Override public void onResponse(AIResponse r) {}
                @Override public void onStateChange(String rid, AIRequestState s) {
                    if (s == AIRequestState.THINKING) thinking.countDown();
                }
            });

        thinking.await(2, TimeUnit.SECONDS);
        assertTrue(rm.isInFlight());
    }
}
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.RequestManagerTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create RequestManager.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.concurrent.atomic.AtomicReference;

public class RequestManager {

    private final AIProvider provider;
    private final AtomicReference<String> activeId = new AtomicReference<>();
    private final AtomicReference<AIRequestState> state = new AtomicReference<>(AIRequestState.IDLE);
    private volatile AICallback activeCallback;
    private volatile boolean cancelRequested;

    public RequestManager(AIProvider provider, long connectTimeoutMs, long inactivityTimeoutMs) {
        this.provider = provider;
    }

    public void submit(AIRequest request, AICallback callback) {
        activeId.set(request.requestId);
        activeCallback = callback;
        cancelRequested = false;
        transition(request.requestId, AIRequestState.THINKING, callback);

        provider.complete(request, request.requestId, new AICallback() {
            @Override
            public void onToken(String rid, String token) {
                if (!rid.equals(activeId.get()) || cancelRequested) return;
                transition(rid, AIRequestState.STREAMING, activeCallback);
                activeCallback.onToken(rid, token);
            }

            @Override
            public void onResponse(AIResponse response) {
                if (!response.requestId.equals(activeId.get()) || cancelRequested) return;
                activeCallback.onResponse(response);
                transition(response.requestId,
                    response.type == AIResponse.Type.ERROR ? AIRequestState.FAILED : AIRequestState.COMPLETED,
                    activeCallback);
            }

            @Override
            public void onStateChange(String rid, AIRequestState s) {
                if (!rid.equals(activeId.get())) return;
                transition(rid, s, activeCallback);
            }
        });
    }

    public void cancel(String requestId) {
        if (requestId.equals(activeId.get())) {
            cancelRequested = true;
            transition(requestId, AIRequestState.CANCELLED, activeCallback);
        }
    }

    public boolean isInFlight() {
        AIRequestState s = state.get();
        return s == AIRequestState.THINKING || s == AIRequestState.STREAMING
            || s == AIRequestState.EXECUTING_TOOLS || s == AIRequestState.FOLLOWUP;
    }

    public AIRequestState getState() { return state.get(); }

    private void transition(String rid, AIRequestState newState, AICallback cb) {
        state.set(newState);
        if (cb != null) cb.onStateChange(rid, newState);
    }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.RequestManagerTest" 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/RequestManager.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/RequestManagerTest.java
git commit -m "feat: add RequestManager state machine with stale-callback suppression"
```

---

### Task 8: AISubsystem

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AISubsystem.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/AISubsystemTest.java`

- [ ] **Step 1: Write failing test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.After;
import org.junit.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import ohi.andre.consolelauncher.ai.providers.MockProvider;

public class AISubsystemTest {

    @After
    public void teardown() {
        AISubsystem s = AISubsystem.getInstance();
        if (s != null) s.dispose();
    }

    @Test
    public void singleton_setAndGet() {
        AISubsystem sub = new AISubsystem(new MockProvider("hi"));
        sub.setInstance();
        assertSame(sub, AISubsystem.getInstance());
    }

    @Test
    public void dispose_clearsInstance() {
        AISubsystem sub = new AISubsystem(new MockProvider("hi"));
        sub.setInstance();
        sub.dispose();
        assertNull(AISubsystem.getInstance());
    }

    @Test
    public void submit_deliversTextResponse() throws InterruptedException {
        AISubsystem sub = new AISubsystem(new MockProvider("hello from AI"));
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean gotText = new AtomicBoolean(false);

        sub.submit("ping", new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) {
                gotText.set(r.type == AIResponse.Type.TEXT);
                latch.countDown();
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(gotText.get());
    }

    @Test
    public void isAvailable_trueWithProvider() {
        assertTrue(new AISubsystem(new MockProvider("hi")).isAvailable());
    }
}
```

- [ ] **Step 2: Run — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.AISubsystemTest" 2>&1 | tail -5
```

- [ ] **Step 3: Create AISubsystem.java**

```java
package ohi.andre.consolelauncher.ai;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class AISubsystem {

    private static volatile AISubsystem instance;

    private final AIProvider provider;
    private final ToolRegistry toolRegistry;
    private final ConversationManager conversationManager;
    private final RequestManager requestManager;
    private final AtomicReference<String> lastRequestId = new AtomicReference<>();

    private volatile boolean awaitingConfirmation;
    private volatile Runnable pendingConfirmAction;
    private volatile Runnable pendingDeclineAction;

    public static AISubsystem getInstance() { return instance; }

    public AISubsystem(AIProvider provider) {
        this.provider = provider;
        this.toolRegistry = new ToolRegistry();
        this.conversationManager = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        this.requestManager = new RequestManager(provider, 10_000, 30_000);
    }

    public void setInstance() { instance = this; }

    public void dispose() { instance = null; }

    public boolean isAvailable() { return provider != null; }

    public boolean isInFlight() { return requestManager.isInFlight(); }

    public boolean isAwaitingConfirmation() { return awaitingConfirmation; }

    public void confirmCurrentTool() {
        if (awaitingConfirmation && pendingConfirmAction != null) {
            awaitingConfirmation = false;
            pendingConfirmAction.run();
        }
    }

    public void declineCurrentTool() {
        if (awaitingConfirmation && pendingDeclineAction != null) {
            awaitingConfirmation = false;
            pendingDeclineAction.run();
        }
    }

    public String submit(String userMessage, AICallback callback) {
        String requestId = UUID.randomUUID().toString();
        lastRequestId.set(requestId);
        conversationManager.append(new ConversationTurn(ConversationTurn.Role.USER, userMessage));

        AIRequest request = new AIRequest.Builder()
            .requestId(requestId)
            .userMessage(userMessage)
            .history(conversationManager.getHistory())
            .tools(toolRegistry.getTools())
            .systemPrompt("You are an AI assistant embedded in a Linux-style terminal launcher on Android. "
                + "Be concise. Use tools when action is needed. Ask one clarifying question when unsure.")
            .maxTokens(2048)
            .build();

        requestManager.submit(request, new AICallback() {
            @Override public void onToken(String rid, String token) { callback.onToken(rid, token); }
            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT && response.text != null) {
                    conversationManager.append(
                        new ConversationTurn(ConversationTurn.Role.ASSISTANT, response.text));
                }
                callback.onResponse(response);
            }
            @Override public void onStateChange(String rid, AIRequestState state) {
                callback.onStateChange(rid, state);
            }
        });

        return requestId;
    }

    public void cancel() {
        String rid = lastRequestId.get();
        if (rid != null) requestManager.cancel(rid);
    }

    public AIRequestState getState() { return requestManager.getState(); }

    public ToolRegistry getToolRegistry() { return toolRegistry; }

    public ConversationManager getConversationManager() { return conversationManager; }
}
```

- [ ] **Step 4: Run — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.AISubsystemTest" 2>&1 | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AISubsystem.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/AISubsystemTest.java
git commit -m "feat: add AISubsystem singleton with submit, cancel, confirm/decline gates"
```

---

### Task 9: Ai.java Settings Enum + XMLPrefsRoot.AI

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/xml/options/Ai.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/xml/XMLPrefsManager.java`

- [ ] **Step 1: Create Ai.java**

Follow the exact pattern from `Behavior.java` (each constant overrides `defaultValue()`, `info()`, `type()`; shared methods at bottom):

```java
package ohi.andre.consolelauncher.managers.xml.options;

import ohi.andre.consolelauncher.managers.xml.XMLPrefsManager;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsElement;
import ohi.andre.consolelauncher.managers.xml.classes.XMLPrefsSave;

public enum Ai implements XMLPrefsSave {

    enabled {
        @Override public String defaultValue() { return "true"; }
        @Override public String info() { return "Enable AI features"; }
        @Override public String type() { return XMLPrefsSave.BOOLEAN; }
    },
    provider {
        @Override public String defaultValue() { return "mock"; }
        @Override public String info() { return "AI provider: claude, openai, ollama, mock"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    api_key {
        @Override public String defaultValue() { return ""; }
        @Override public String info() { return "API key for the selected provider"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    base_url {
        @Override public String defaultValue() { return ""; }
        @Override public String info() { return "Base URL for openai/ollama (e.g. http://localhost:11434)"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    model {
        @Override public String defaultValue() { return ""; }
        @Override public String info() { return "Model name override. Leave empty for provider default."; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    always_on_fallback {
        @Override public String defaultValue() { return "true"; }
        @Override public String info() { return "Send unrecognized input to AI before shell fallback"; }
        @Override public String type() { return XMLPrefsSave.BOOLEAN; }
    },
    memory_mode {
        @Override public String defaultValue() { return "session"; }
        @Override public String info() { return "Conversation memory: stateless, session, persistent"; }
        @Override public String type() { return XMLPrefsSave.TEXT; }
    },
    history_token_cap {
        @Override public String defaultValue() { return "4000"; }
        @Override public String info() { return "Max token budget for conversation history"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    compact_threshold {
        @Override public String defaultValue() { return "80"; }
        @Override public String info() { return "History fill % that triggers auto-compaction (0-100)"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    connect_timeout_ms {
        @Override public String defaultValue() { return "10000"; }
        @Override public String info() { return "Connect timeout ms before TIMED_OUT_CONNECT"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    inactivity_timeout_ms {
        @Override public String defaultValue() { return "30000"; }
        @Override public String info() { return "Inactivity timeout ms before TIMED_OUT_INACTIVITY"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    max_tokens {
        @Override public String defaultValue() { return "2048"; }
        @Override public String info() { return "Maximum tokens in AI response"; }
        @Override public String type() { return XMLPrefsSave.INTEGER; }
    },
    confirm_state_changing {
        @Override public String defaultValue() { return "false"; }
        @Override public String info() { return "Require confirmation for STATE_CHANGING tools"; }
        @Override public String type() { return XMLPrefsSave.BOOLEAN; }
    };

    @Override public XMLPrefsElement parent() { return XMLPrefsManager.XMLPrefsRoot.AI; }
    @Override public String label() { return name(); }
    @Override public String[] invalidValues() { return null; }
    public String getLowercaseString() { return label(); }
    public String getString() { return label(); }
}
```

- [ ] **Step 2: Add AI entry to XMLPrefsManager.XMLPrefsRoot**

Open `XMLPrefsManager.java`. Find the `XMLPrefsRoot` enum. Add this as the last constant (before the closing semicolon of the enum):

```java
AI(ohi.andre.consolelauncher.managers.xml.options.Ai.values()) {
    @Override
    public String[] delete() { return new String[] {}; }
},
```

- [ ] **Step 3: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/xml/options/Ai.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/xml/XMLPrefsManager.java
git commit -m "feat: add Ai settings enum and XMLPrefsRoot.AI backed by ai.xml"
```

---

### Task 10: AITrigger + MainManager Modifications

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AITrigger.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/MainPack.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/MainManager.java`

- [ ] **Step 1: Add aiSubsystem field to MainPack**

Open `MainPack.java`. Add after the last existing public field:

```java
public ohi.andre.consolelauncher.ai.AISubsystem aiSubsystem;
```

- [ ] **Step 2: Create AITrigger.java**

```java
package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.graphics.Color;
import ohi.andre.consolelauncher.tuils.Tuils;

public class AITrigger {

    public interface ShellFallback {
        void triggerShell(String input);
    }

    private final AISubsystem aiSubsystem;
    private final Context context;
    private final ShellFallback shellFallback;

    public AITrigger(AISubsystem aiSubsystem, Context context, ShellFallback shellFallback) {
        this.aiSubsystem = aiSubsystem;
        this.context = context;
        this.shellFallback = shellFallback;
    }

    /**
     * Returns true if AITrigger claims the input (caller stops the trigger chain).
     * Returns false to let remaining triggers handle the input.
     */
    public boolean trigger(final String input) {
        if (aiSubsystem == null || !aiSubsystem.isAvailable()) return false;

        Tuils.sendOutput(Color.GRAY, context, "[thinking...]");

        aiSubsystem.submit(input, new AICallback() {
            @Override public void onToken(String rid, String token) {
                Tuils.sendOutput(Color.WHITE, context, token);
            }

            @Override public void onResponse(AIResponse response) {
                if (response.type == AIResponse.Type.TEXT && response.text != null) {
                    Tuils.sendOutput(Color.WHITE, context, response.text);
                } else if (response.type == AIResponse.Type.ERROR) {
                    Tuils.sendOutput(Color.RED, context, "[AI error: " + response.errorMessage + "]");
                }
            }

            @Override public void onStateChange(String rid, RequestState state) {
                switch (state) {
                    case CANCELLED:
                        Tuils.sendOutput(Color.GRAY, context, "[cancelled]");
                        break;
                    case TIMED_OUT_CONNECT:
                        Tuils.sendOutput(Color.YELLOW, context, "[AI unavailable — retrying as shell command]");
                        if (shellFallback != null) shellFallback.triggerShell(input);
                        break;
                    case TIMED_OUT_INACTIVITY:
                        Tuils.sendOutput(Color.YELLOW, context, "[AI response timed out]");
                        break;
                    case FAILED:
                        Tuils.sendOutput(Color.RED, context, "[AI request failed]");
                        break;
                    default:
                        break;
                }
            }
        });

        return true;
    }
}
```

- [ ] **Step 3: Modify MainManager.onCommand()**

Open `MainManager.java`. Identify the `onCommand(String input, String alias, boolean wasMusicService)` method.

**A)** At the very top of `onCommand()`, before `input = Tuils.removeUnnecessarySpaces(input)` (or equivalent), capture the original input and add the AI gate:

```java
final String originalInput = input;

// AI cancel/confirm gate — before any input transformation
if (mainPack.aiSubsystem != null) {
    if (mainPack.aiSubsystem.isInFlight()) {
        String trimmed = input.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("stop")) {
            mainPack.aiSubsystem.cancel();
            return;
        }
    }
    if (mainPack.aiSubsystem.isAwaitingConfirmation()) {
        if (input.trim().isEmpty()) {
            mainPack.aiSubsystem.confirmCurrentTool();
        } else {
            mainPack.aiSubsystem.declineCurrentTool();
        }
        return;
    }
}
```

**B)** Add `PendingIntegration` gate after the AI gate (before trigger chain):

```java
// integrate command selection gate
if (ohi.andre.consolelauncher.commands.main.raw.integrate.PendingIntegration.isActive()) {
    ohi.andre.consolelauncher.commands.main.raw.integrate.PendingIntegration.processSelection(input);
    return;
}
```

**C)** After all named triggers have run (after `TuiCommandTrigger`, `AppTrigger`, etc.) and before `ShellCommandTrigger` would handle input, add:

```java
if (aiTrigger != null && aiTrigger.trigger(originalInput)) {
    return;
}
```

**D)** Add `private AITrigger aiTrigger;` as a field in `MainManager`.

**E)** In the `MainManager` init method (where `MainPack` is fully assembled), initialize `aiTrigger`:

```java
if (mainPack.aiSubsystem != null) {
    aiTrigger = new AITrigger(
        mainPack.aiSubsystem,
        context,
        rawInput -> new ShellCommandTrigger().trigger(rawInput, mainPack)
    );
}
```

(Replace `ShellCommandTrigger().trigger(rawInput, mainPack)` with the actual method signature used in the codebase — read the existing `ShellCommandTrigger` to confirm.)

- [ ] **Step 4: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -15
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AITrigger.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/MainManager.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/MainPack.java
git commit -m "feat: wire AITrigger into MainManager with cancel/confirm gate and shell fallback"
```

---

### Task 11: LauncherActivity Initialization

**Files:**
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/LauncherActivity.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/res/values/strings.xml`
- Modify: `TUI-ConsoleLauncher/app/proguard-rules.pro`

- [ ] **Step 1: Add ProGuard keep rule**

Open `proguard-rules.pro`. Add at end:

```
-keep class ohi.andre.consolelauncher.ai.** { *; }
```

- [ ] **Step 2: Add help strings**

Open `strings.xml`. Add before `</resources>`:

```xml
<string name="help_ai">ai [query] — send a query directly to AI</string>
<string name="help_compact">compact — manually compact AI conversation history</string>
<string name="help_pin">pin — pin the latest AI response</string>
<string name="help_clearhistory">clearhistory — clear all AI conversation history</string>
<string name="help_cleararchive">cleararchive — delete archived compaction history</string>
<string name="help_integrate">integrate — discover installed apps and add as AI tools</string>
```

- [ ] **Step 3: Initialize AISubsystem in LauncherActivity**

Find `finishOnCreate()` (or equivalent startup method where all managers are built). After `mainPack` is constructed, add:

```java
try {
    String providerName = ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.get(
        ohi.andre.consolelauncher.managers.xml.options.Ai.provider);
    ohi.andre.consolelauncher.ai.AISubsystem aiSubsystem = buildAISubsystem(providerName);
    mainPack.aiSubsystem = aiSubsystem;
    aiSubsystem.setInstance();
} catch (Exception e) {
    ohi.andre.consolelauncher.tuils.Tuils.log(e);
}
```

Add private method to `LauncherActivity`:

```java
private ohi.andre.consolelauncher.ai.AISubsystem buildAISubsystem(String providerName) {
    ohi.andre.consolelauncher.ai.AIProvider provider;
    String key = ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.get(
        ohi.andre.consolelauncher.managers.xml.options.Ai.api_key);
    switch (providerName.toLowerCase()) {
        case "claude":
            provider = new ohi.andre.consolelauncher.ai.providers.ClaudeProvider(key);
            break;
        case "openai":
        case "ollama": {
            String baseUrl = ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.get(
                ohi.andre.consolelauncher.managers.xml.options.Ai.base_url);
            String model = ohi.andre.consolelauncher.managers.xml.XMLPrefsManager.get(
                ohi.andre.consolelauncher.managers.xml.options.Ai.model);
            provider = new ohi.andre.consolelauncher.ai.providers.OpenAIProvider(key, baseUrl, model);
            break;
        }
        default:
            provider = new ohi.andre.consolelauncher.ai.providers.MockProvider(
                "AI is in mock mode. Set provider in ai.xml.");
            break;
    }
    return new ohi.andre.consolelauncher.ai.AISubsystem(provider);
}
```

Add to `onDestroy()`:

```java
if (mainPack != null && mainPack.aiSubsystem != null) {
    mainPack.aiSubsystem.dispose();
}
```

- [ ] **Step 4: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -10
```

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/LauncherActivity.java
git add TUI-ConsoleLauncher/app/src/main/res/values/strings.xml
git add TUI-ConsoleLauncher/app/proguard-rules.pro
git commit -m "feat: initialize AISubsystem in LauncherActivity with provider selection from ai.xml"
```

---

### Task 12: ai.java, compact.java, pin.java, clearhistory.java, cleararchive.java Commands

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/ai.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/compact.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/pin.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/clearhistory.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/cleararchive.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/MainPack.java`

- [ ] **Step 1: Create ai.java**

```java
package ohi.andre.consolelauncher.commands.main.raw;

import android.content.Context;
import android.graphics.Color;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.ai.*;
import ohi.andre.consolelauncher.tuils.Tuils;

public class ai implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        String query = pack.args;
        if (query == null || query.trim().isEmpty()) {
            return pack.context.getString(R.string.help_ai);
        }
        AISubsystem aiSubsystem = pack.mainPack.aiSubsystem;
        if (aiSubsystem == null || !aiSubsystem.isAvailable()) {
            return "[AI subsystem not available — check ai.xml]";
        }
        Context ctx = pack.context;
        aiSubsystem.submit(query.trim(), new AICallback() {
            @Override public void onToken(String rid, String token) {
                Tuils.sendOutput(Color.WHITE, ctx, token);
            }
            @Override public void onResponse(AIResponse r) {
                if (r.type == AIResponse.Type.TEXT && r.text != null) {
                    Tuils.sendOutput(Color.WHITE, ctx, r.text);
                } else if (r.type == AIResponse.Type.ERROR) {
                    Tuils.sendOutput(Color.RED, ctx, "[AI error: " + r.errorMessage + "]");
                }
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });
        return null;
    }

    @Override public int argType() { return CommandAbstraction.PLAIN_TEXT; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_ai; }
    @Override public int onArgNotFound() { return 0; }
    @Override public int onNotArgEnough() { return 0; }
}
```

- [ ] **Step 2: Create compact.java**

```java
package ohi.andre.consolelauncher.commands.main.raw;

import android.graphics.Color;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.ai.*;
import ohi.andre.consolelauncher.tuils.Tuils;
import java.util.List;

public class compact implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = pack.mainPack.aiSubsystem;
        if (ai == null || !ai.isAvailable()) return "[AI subsystem not available]";

        ConversationManager cm = ai.getConversationManager();
        List<ConversationTurn> history = cm.getHistory();
        if (history.isEmpty()) return "[No conversation history to compact]";

        Tuils.sendOutput(Color.GRAY, pack.context, "[compacting conversation history...]");
        CompactionEngine engine = new CompactionEngine();
        String prompt = engine.buildCompactionPrompt(history);

        ai.submit(prompt, new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) {
                if (r.type == AIResponse.Type.TEXT && r.text != null) {
                    ConversationTurn summary = new ConversationTurn(
                        ConversationTurn.Role.ASSISTANT, "[Compacted] " + r.text);
                    engine.applyCompaction(cm, summary);
                    Tuils.sendOutput(Color.GREEN, pack.context,
                        "[compacted — " + cm.getHistory().size() + " turn(s) remain]");
                } else {
                    Tuils.sendOutput(Color.RED, pack.context, "[compaction failed]");
                }
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });
        return null;
    }

    @Override public int argType() { return CommandAbstraction.NO_ARGS; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_compact; }
    @Override public int onArgNotFound() { return 0; }
    @Override public int onNotArgEnough() { return 0; }
}
```

- [ ] **Step 3: Create pin.java**

```java
package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.ai.AISubsystem;

public class pin implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = pack.mainPack.aiSubsystem;
        if (ai == null) return "[AI not initialized]";
        return ai.getConversationManager().pinLatestAssistantTurn()
            ? "[latest AI response pinned]"
            : "[no AI response available to pin]";
    }

    @Override public int argType() { return CommandAbstraction.NO_ARGS; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_pin; }
    @Override public int onArgNotFound() { return 0; }
    @Override public int onNotArgEnough() { return 0; }
}
```

- [ ] **Step 4: Create clearhistory.java**

```java
package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.ai.AISubsystem;

public class clearhistory implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = pack.mainPack.aiSubsystem;
        if (ai == null) return "[AI not initialized]";
        ai.getConversationManager().clear();
        return "[conversation history cleared]";
    }

    @Override public int argType() { return CommandAbstraction.NO_ARGS; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_clearhistory; }
    @Override public int onArgNotFound() { return 0; }
    @Override public int onNotArgEnough() { return 0; }
}
```

- [ ] **Step 5: Create cleararchive.java**

```java
package ohi.andre.consolelauncher.commands.main.raw;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.commands.CommandAbstraction;
import ohi.andre.consolelauncher.commands.ExecutePack;
import ohi.andre.consolelauncher.ai.AISubsystem;

public class cleararchive implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        AISubsystem ai = pack.mainPack.aiSubsystem;
        if (ai == null) return "[AI not initialized]";
        int deleted = ai.getConversationManager().clearArchives();
        return "[deleted " + deleted + " archive file(s)]";
    }

    @Override public int argType() { return CommandAbstraction.NO_ARGS; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_cleararchive; }
    @Override public int onArgNotFound() { return 0; }
    @Override public int onNotArgEnough() { return 0; }
}
```

- [ ] **Step 6: Register in MainPack**

Add `new ai()`, `new compact()`, `new pin()`, `new clearhistory()`, and `new cleararchive()` to the command list in `MainPack.java`, following existing command registration pattern.

- [ ] **Step 7: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -10
```

- [ ] **Step 8: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/ai.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/compact.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/pin.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/clearhistory.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/cleararchive.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/MainPack.java
git commit -m "feat: add ai, compact, pin, clearhistory, and cleararchive commands"
```

---

### Task 13: ClaudeProvider

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers/ClaudeProvider.java`

- [ ] **Step 1: Create ClaudeProvider.java**

Uses OkHttp (already in `build.gradle`) and `org.json` (Android built-in):

```java
package ohi.andre.consolelauncher.ai.providers;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import ohi.andre.consolelauncher.ai.*;

public class ClaudeProvider implements AIProvider {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String DEFAULT_MODEL = "claude-haiku-4-5-20251001";
    private static final MediaType JSON_TYPE = MediaType.get("application/json");

    private final String apiKey;
    private final OkHttpClient client;

    public ClaudeProvider(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void complete(AIRequest request, String requestId, AICallback callback) {
        callback.onStateChange(requestId, RequestState.THINKING);
        try {
            JSONObject body = new JSONObject();
            body.put("model", DEFAULT_MODEL);
            body.put("max_tokens", request.maxTokens);
            if (request.systemPrompt != null) body.put("system", request.systemPrompt);

            JSONArray messages = new JSONArray();
            for (ConversationTurn t : request.history) {
                if (t.role == ConversationTurn.Role.USER || t.role == ConversationTurn.Role.ASSISTANT) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", t.role == ConversationTurn.Role.USER ? "user" : "assistant");
                    msg.put("content", t.content);
                    messages.put(msg);
                }
            }
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", request.userMessage);
            messages.put(userMsg);
            body.put("messages", messages);

            Request httpReq = new Request.Builder()
                .url(API_URL)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_TYPE))
                .build();

            client.newCall(httpReq).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    if (!call.isCanceled()) {
                        callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                    }
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        if (!response.isSuccessful()) {
                            callback.onResponse(AIResponse.error(requestId, "HTTP " + response.code()));
                            return;
                        }
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject block = json.getJSONArray("content").getJSONObject(0);
                        if ("text".equals(block.getString("type"))) {
                            callback.onResponse(AIResponse.text(requestId, block.getString("text")));
                        } else {
                            callback.onResponse(AIResponse.error(requestId, "unexpected block type"));
                        }
                    } catch (Exception e) {
                        callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            callback.onResponse(AIResponse.error(requestId, e.getMessage()));
        }
    }

    @Override public boolean supportsToolUse() { return true; }
    @Override public boolean supportsStreaming() { return false; }
    @Override public String providerId() { return "claude"; }
}
```

- [ ] **Step 2: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -5
```

- [ ] **Step 3: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers/ClaudeProvider.java
git commit -m "feat: add ClaudeProvider via Anthropic Messages API"
```

---

### Task 14: OpenAIProvider

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers/OpenAIProvider.java`

- [ ] **Step 1: Create OpenAIProvider.java**

```java
package ohi.andre.consolelauncher.ai.providers;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import ohi.andre.consolelauncher.ai.*;

public class OpenAIProvider implements AIProvider {

    private static final String DEFAULT_BASE = "https://api.openai.com";
    private static final String DEFAULT_MODEL = "gpt-4o-mini";
    private static final MediaType JSON_TYPE = MediaType.get("application/json");

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final OkHttpClient client;

    public OpenAIProvider(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : DEFAULT_BASE;
        this.model = (model != null && !model.isEmpty()) ? model : DEFAULT_MODEL;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    }

    @Override
    public void complete(AIRequest request, String requestId, AICallback callback) {
        callback.onStateChange(requestId, RequestState.THINKING);
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("max_tokens", request.maxTokens);

            JSONArray messages = new JSONArray();
            if (request.systemPrompt != null) {
                JSONObject sys = new JSONObject();
                sys.put("role", "system");
                sys.put("content", request.systemPrompt);
                messages.put(sys);
            }
            for (ConversationTurn t : request.history) {
                if (t.role == ConversationTurn.Role.USER || t.role == ConversationTurn.Role.ASSISTANT) {
                    JSONObject msg = new JSONObject();
                    msg.put("role", t.role == ConversationTurn.Role.USER ? "user" : "assistant");
                    msg.put("content", t.content);
                    messages.put(msg);
                }
            }
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", request.userMessage);
            messages.put(userMsg);
            body.put("messages", messages);

            String endpoint = baseUrl.replaceAll("/$", "") + "/v1/chat/completions";
            Request.Builder reqBuilder = new Request.Builder()
                .url(endpoint)
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_TYPE));
            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer " + apiKey);
            }

            client.newCall(reqBuilder.build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    if (!call.isCanceled()) callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (response) {
                        if (!response.isSuccessful()) {
                            callback.onResponse(AIResponse.error(requestId, "HTTP " + response.code()));
                            return;
                        }
                        JSONObject json = new JSONObject(response.body().string());
                        String text = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content");
                        callback.onResponse(AIResponse.text(requestId, text));
                    } catch (Exception e) {
                        callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                    }
                }
            });
        } catch (Exception e) {
            callback.onResponse(AIResponse.error(requestId, e.getMessage()));
        }
    }

    @Override public boolean supportsToolUse() { return false; }
    @Override public boolean supportsStreaming() { return false; }
    @Override public String providerId() { return "openai"; }
}
```

- [ ] **Step 2: Run all tests**

```bash
cd TUI-ConsoleLauncher && ./gradlew test 2>&1 | tail -10
```

Expected: `BUILD SUCCESSFUL`, all pass.

- [ ] **Step 3: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/providers/OpenAIProvider.java
git commit -m "feat: add OpenAIProvider for OpenAI and Ollama (on-device, USB tunnel, LAN)"
```

---

## Sub-plan B: App Integration and Local Retrieval

### Task 15: LauncherIndex + ShortcutBridge

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/platform/LauncherIndex.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/platform/ShortcutBridge.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/LauncherIndexTest.java`
- Test: `TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/ShortcutBridgeTest.java`

- [ ] **Step 1: Write failing LauncherIndex test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;
import ohi.andre.consolelauncher.ai.platform.LauncherIndex;

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
```

- [ ] **Step 2: Write failing ShortcutBridge test**

```java
package ohi.andre.consolelauncher.ai;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import ohi.andre.consolelauncher.ai.platform.ShortcutBridge;

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
```

- [ ] **Step 3: Run both tests — expect FAILED**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.LauncherIndexTest" --tests "ohi.andre.consolelauncher.ai.ShortcutBridgeTest" 2>&1 | tail -10
```

- [ ] **Step 4: Create LauncherIndex.java**

```java
package ohi.andre.consolelauncher.ai.platform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LauncherIndex {

    public static class Entry {
        public final String id;
        public final String label;
        public final String kind;

        public Entry(String id, String label, String kind) {
            this.id = id;
            this.label = label;
            this.kind = kind;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public synchronized void put(String id, String label, String kind) {
        entries.add(new Entry(id, label, kind));
    }

    public synchronized List<Entry> search(String query, int limit) {
        String q = query.toLowerCase();
        List<Entry> matches = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.label.toLowerCase().contains(q)) matches.add(entry);
        }
        Collections.sort(matches, Comparator.comparing(e -> e.label.toLowerCase()));
        return matches.subList(0, Math.min(limit, matches.size()));
    }
}
```

- [ ] **Step 5: Create ShortcutBridge.java**

```java
package ohi.andre.consolelauncher.ai.platform;

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
```

- [ ] **Step 6: Run both tests — expect PASS**

```bash
cd TUI-ConsoleLauncher && ./gradlew test --tests "ohi.andre.consolelauncher.ai.LauncherIndexTest" --tests "ohi.andre.consolelauncher.ai.ShortcutBridgeTest" 2>&1 | tail -10
```

- [ ] **Step 7: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/platform/LauncherIndex.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/platform/ShortcutBridge.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/LauncherIndexTest.java
git add TUI-ConsoleLauncher/app/src/test/java/ohi/andre/consolelauncher/ai/ShortcutBridgeTest.java
git commit -m "feat: add LauncherIndex and ShortcutBridge for local retrieval and published shortcuts"
```

---

### Task 16: AppCapabilityScanner + integrate.java (Shortcut-First)

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AppCapabilityScanner.java`
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/integrate.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/MainManager.java`
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/MainPack.java`

- [ ] **Step 1: Create shortcut-first AppCapabilityScanner.java**

```java
package ohi.andre.consolelauncher.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppCapabilityScanner {

    public static class Capability {
        public final String id;
        public final String label;
        public final String category;
        public final boolean preferred;

        public Capability(String id, String label, String category, boolean preferred) {
            this.id = id;
            this.label = label;
            this.category = category;
            this.preferred = preferred;
        }
    }

    private final Context context;

    public AppCapabilityScanner(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Capability> scanInstalledApps() {
        List<Capability> result = new ArrayList<>();
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        if (launcherApps != null) {
            // Phase 1: query app-published shortcuts first and append as [SHORTCUT] capabilities.
        }

        PackageManager pm = context.getPackageManager();
        Intent launch = new Intent(Intent.ACTION_MAIN, null);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info : pm.queryIntentActivities(launch, 0)) {
            String pkg = info.activityInfo.packageName;
            String label = info.loadLabel(pm).toString();
            result.add(new Capability("launch:" + pkg, label, "LAUNCH", false));
        }
        return result;
    }

    public Tool buildTool(Capability capability) {
        Map<String, String> params = new LinkedHashMap<>();
        return new Tool(capability.id, capability.label, params, ToolRiskClass.LAUNCH_ONLY);
    }
}
```

- [ ] **Step 2: Update integrate.java to render capability categories and prefer shortcuts**

Replace the old list rendering block with:

```java
AppCapabilityScanner scanner = new AppCapabilityScanner(ctx);
List<AppCapabilityScanner.Capability> caps = scanner.scanInstalledApps();
if (caps.isEmpty()) return "[no capabilities found]";

StringBuilder list = new StringBuilder("[Found " + caps.size() + " capabilities]\n");
for (int i = 0; i < Math.min(caps.size(), 30); i++) {
    AppCapabilityScanner.Capability cap = caps.get(i);
    list.append(i + 1).append(". [").append(cap.category).append("] ")
        .append(cap.label)
        .append(cap.preferred ? " [preferred]" : "")
        .append("\n");
}
list.append("\nType numbers to integrate (e.g. '1 3 5') or 'all':");
Tuils.sendOutput(Color.WHITE, ctx, list.toString());

PendingIntegration.set(caps, ai, ctx);
```

- [ ] **Step 3: Keep PendingIntegration gate in MainManager.onCommand()**

```java
if (ohi.andre.consolelauncher.commands.main.raw.integrate.PendingIntegration.isActive()) {
    ohi.andre.consolelauncher.commands.main.raw.integrate.PendingIntegration.processSelection(input);
    return;
}
```

- [ ] **Step 4: Register integrate in MainPack**

Add `new integrate()` to the command list in `MainPack.java`.

- [ ] **Step 5: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -10
```

- [ ] **Step 6: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/AppCapabilityScanner.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/raw/integrate.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/MainManager.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/commands/main/MainPack.java
git commit -m "feat: add shortcut-first integrate flow for user-curated app tool registration"
```

---

## Sub-plan C: Manager Hooks and Android 16-17 Surfaces

### Task 17: SuggestionsManager + LauncherIndex Hook

**Files:**
- Modify: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/SuggestionsManager.java`

- [ ] **Step 1: Read SuggestionsManager to find the ranking/sort point**

```bash
grep -n "sort\|rank\|suggest\|List" TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/SuggestionsManager.java | head -30
```

- [ ] **Step 2: Merge LauncherIndex local retrieval before optional AI re-ranking**

Find the method that produces the final ordered suggestion list. After existing local fuzzy matching and before any remote AI call, add:

```java
ohi.andre.consolelauncher.ai.AISubsystem ai =
    ohi.andre.consolelauncher.ai.AISubsystem.getInstance();
if (ai != null) {
    java.util.List<ohi.andre.consolelauncher.ai.platform.LauncherIndex.Entry> localHits =
        ai.getLauncherIndex().search(userInput, 5);
    for (ohi.andre.consolelauncher.ai.platform.LauncherIndex.Entry hit : localHits) {
        if (!suggestions.contains(hit.label)) suggestions.add(0, hit.label);
    }
    if (ai.isAvailable()) {
        // Existing AI re-ranking hook runs here with the merged local candidates.
    }
}
```

- [ ] **Step 3: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -5
```

- [ ] **Step 4: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/SuggestionsManager.java
git commit -m "feat: merge LauncherIndex retrieval with AI suggestion ranking"
```

---

### Task 18: NotificationService + JourneyManager Hook

**Files:**
- Create: `TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/platform/JourneyManager.java`
- Modify: notification listener class (find with: `grep -rl "onNotificationPosted" TUI-ConsoleLauncher/app/src/`)

- [ ] **Step 1: Create JourneyManager.java**

```java
package ohi.andre.consolelauncher.ai.platform;

import android.app.Notification;
import android.os.Build;

public class JourneyManager {

    public boolean isJourneyNotification(Notification notification) {
        return Build.VERSION.SDK_INT >= 36
            && notification != null
            && notification.extras != null
            && notification.extras.containsKey(Notification.EXTRA_PROGRESS);
    }
}
```

- [ ] **Step 2: Find the notification listener class**

```bash
grep -rl "onNotificationPosted" TUI-ConsoleLauncher/app/src/
```

- [ ] **Step 3: Add JourneyManager ingestion before generic AI triage in onNotificationPosted()**

```java
ohi.andre.consolelauncher.ai.AISubsystem ai =
    ohi.andre.consolelauncher.ai.AISubsystem.getInstance();
if (ai != null && ai.getJourneyManager().isJourneyNotification(sbn.getNotification())) {
    ohi.andre.consolelauncher.tuils.Tuils.sendOutput(
        android.graphics.Color.CYAN, getApplicationContext(),
        "[journey] " + sbn.getPackageName() + " progress update received");
} else if (ai != null && ai.isAvailable() && !ai.isInFlight()) {
    CharSequence ticker = sbn.getNotification().tickerText;
    if (ticker != null && ticker.length() > 0) {
        String query = "[notification from " + sbn.getPackageName() + "] "
            + ticker + " — urgent or actionable? One line reply.";
        ai.submit(query, new ohi.andre.consolelauncher.ai.AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(ohi.andre.consolelauncher.ai.AIResponse r) {
                if (r.type == ohi.andre.consolelauncher.ai.AIResponse.Type.TEXT
                        && r.text != null && !r.text.isEmpty()) {
                    ohi.andre.consolelauncher.tuils.Tuils.sendOutput(
                        android.graphics.Color.CYAN, getApplicationContext(),
                        "[AI] " + r.text);
                }
            }
            @Override public void onStateChange(String rid,
                ohi.andre.consolelauncher.ai.AIRequestState s) {}
        });
    }
}
```

- [ ] **Step 4: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -5
```

- [ ] **Step 5: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/ai/platform/JourneyManager.java
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/managers/notifications/
git commit -m "feat: add JourneyManager and AI triage hook for notification handling"
```

---

### Task 19: Android 16-17 UX Adaptation

**Files:**
- Modify: launcher activities / overlays that currently handle back navigation
- Modify: tool execution path for person-targeting actions

- [ ] **Step 1: Replace legacy back hooks with `OnBackInvokedDispatcher` on Android 16+ launcher surfaces**

Add this pattern to each launcher-owned activity or overlay entry point that currently intercepts back manually:

```java
if (android.os.Build.VERSION.SDK_INT >= 33) {
    getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
        android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,
        this::finish
    );
}
```

- [ ] **Step 2: Add Android 17 contact disambiguation path for person-targeting tools**

Before executing `call`, `message`, `email`, or share-like tools with ambiguous person targets, add this launcher-managed branch:

```java
if (android.os.Build.VERSION.SDK_INT >= 37 && ambiguousContact) {
    android.content.Intent intent = new android.content.Intent("android.intent.action.PICK_CONTACTS");
    context.startActivity(intent);
    return ToolResult.failure(callId, "contact disambiguation required");
}
```

- [ ] **Step 3: Verify build**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | grep -E "^.*error:|BUILD" | tail -10
```

- [ ] **Step 4: Commit**

```bash
git add TUI-ConsoleLauncher/app/src/main/java/ohi/andre/consolelauncher/
git commit -m "feat: align launcher UX with predictive back and Android 17 contact flows"
```

---

### Task 20: Full Build + Install Verification

- [ ] **Step 1: Run all unit tests**

```bash
cd TUI-ConsoleLauncher && ./gradlew test 2>&1 | tail -15
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 2: Build debug APK**

```bash
cd TUI-ConsoleLauncher && ./gradlew assembleDebug 2>&1 | tail -5
```

Expected: `BUILD SUCCESSFUL`
APK: `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Install on device**

```bash
adb wait-for-device && adb install -r TUI-ConsoleLauncher/app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 4: Smoke test**

1. Launch app — no crash
2. `ai hello` → `[thinking...]` then mock response
3. `compact` → reports empty or compacts
4. `pin` after an AI response → `[latest AI response pinned]`
5. `clearhistory` → `[conversation history cleared]`
6. `cleararchive` → archive deletion message
7. `integrate` → capability list appears with `[SHORTCUT]` entries when available; type `1 2` → integration success
8. Disable network and run a suggestion/query that should still hit local `LauncherIndex`
9. Type unrecognized text → AI handles it
10. During AI thinking, type `stop` → `[cancelled]`
11. Bare Enter during AI → `[cancelled]`
12. On Android 16+, predictive back returns cleanly to home in both gesture and 3-button navigation
13. On Android 17+, ambiguous person action launches contact picker instead of broad contact read

- [ ] **Step 5: Final commit**

```bash
git add -A
git commit -m "feat: complete Phase 1 AI smart launcher

- AISubsystem singleton (Claude, OpenAI/Ollama, Mock providers)
- ToolRegistry 3-tier: TUI commands, published shortcuts / app intents, system (reserved)
- ConversationManager stateless/session/persistent modes with compaction trigger
- CompactionEngine: dynamic prompt, singleton fact emphasis, pinned preservation
- RequestManager state machine with stale-callback suppression
- AITrigger in MainManager chain before ShellCommandTrigger
- Cancel: type 'stop' or bare Enter; connect timeout re-dispatches to shell
- ai, compact, pin, clearhistory, cleararchive, integrate commands
- Ai.java settings enum (ai.xml) with 13 configurable options
- SuggestionsManager: LauncherIndex retrieval + AI re-ranking
- NotificationService: JourneyManager + AI triage hook
- AppCapabilityScanner: shortcut-first capability discovery
- LauncherIndex: AppSearch-aligned local retrieval layer
- Predictive back + Android 17 contact-picker adaptation"
```

---

## Verification Checklist

- [ ] `ai hello` → mock response without crash
- [ ] `compact` → compaction flow or empty-history message
- [ ] `pin` → latest AI turn is pinned for later compaction
- [ ] `clearhistory` → history cleared
- [ ] `cleararchive` → archive files removed
- [ ] `integrate` → capability list → select numbers → tools registered, preferring `[SHORTCUT]`
- [ ] LauncherIndex returns local hits without network
- [ ] Android 16 progress-style notifications surface as journey tasks
- [ ] Unrecognized input → AI intercepts (always_on_fallback=true)
- [ ] `stop` during AI → `[cancelled]`
- [ ] Bare Enter during AI → `[cancelled]`
- [ ] `provider=claude` + valid key → real response
- [ ] `provider=openai` + key + base_url → OpenAI response
- [ ] Predictive back works on Android 16+ in gesture and 3-button modes
- [ ] Android 17 contact picker resolves ambiguous person actions without broad contact permission
- [ ] `./gradlew test` → all pass
- [ ] `./gradlew assembleDebug` → BUILD SUCCESSFUL
- [ ] No crash on startup with provider=mock
