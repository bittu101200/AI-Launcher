package bhupendra.ai.launcher.ai;

import bhupendra.ai.launcher.UIManager;
import bhupendra.ai.launcher.tuils.Tuils;
import android.graphics.Color;
import bhupendra.ai.launcher.managers.FileSystemManager;
import android.content.Context;
import android.content.Intent;
import android.app.ActivityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.ArrayList;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Ai;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.TermuxManager;
import bhupendra.ai.launcher.ai.tools.*;
import bhupendra.ai.launcher.ai.providers.MockProvider;
import bhupendra.ai.launcher.managers.DeviceStateManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class AISubsystem {
    private static final String PROMPT_APPENDIX =
        "\nFINAL EXECUTION RULES:\n" +
        "1. No preamble: execute tool first, then report. No 'Sure' or 'I will'.\n" +
        "2. Direct Path: call system.config directly for known visibility keys (e.g. show_ram=true).\n" +
        "3. Ambiguity: If intent is unclear, ask one short question before tool use.\n";

    private static volatile AISubsystem instance;

    private AIProvider provider;
    private final Context appContext;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final ConversationManager conversationManager;
    private final RequestManager requestManager;
    
    private final ConversationManager automationConversationManager;
    private final RequestManager automationRequestManager;

    private final LongTermMemory longTermMemory;
    private final bhupendra.ai.launcher.ai.platform.LauncherIndex launcherIndex = new bhupendra.ai.launcher.ai.platform.LauncherIndex();
    private final AtomicReference<String> lastRequestId = new AtomicReference<>();
    private final bhupendra.ai.launcher.ai.platform.JourneyManager journeyManager = new bhupendra.ai.launcher.ai.platform.JourneyManager();

    private MainPack mainPack;
    private volatile boolean awaitingConfirmation;
    private volatile Runnable pendingConfirmAction;
    private volatile Runnable pendingDeclineAction;
    private volatile ChoiceCallback pendingChoiceCallback;

    public interface AIListener {
        void onAIStateChanged(boolean running);
    }
    
    private final List<AIListener> listeners = new java.util.ArrayList<>();

    public synchronized void addListener(AIListener l) { listeners.add(l); }
    public synchronized void removeListener(AIListener l) { listeners.remove(l); }
    private synchronized void notifyListeners(boolean running) {
        for (AIListener l : listeners) l.onAIStateChanged(running);
    }
    
    public synchronized void requestUserChoice(List<String> options, ChoiceCallback callback) {
        requestUserChoice("Choose an option", options, callback);
    }

    public synchronized void requestUserChoice(String prompt, List<String> options, ChoiceCallback callback) {
        pendingChoiceCallback = callback;
        if (appContext == null) {
            if (callback != null) callback.onSelection(null);
            return;
        }

        ArrayList<String> choiceOptions = new ArrayList<>(options != null ? options : Collections.emptyList());
        Intent intent = new Intent(UIManager.ACTION_SHOW_CHOICE_SUGGESTIONS);
        intent.putStringArrayListExtra(UIManager.EXTRA_SUGGESTION_OPTIONS, choiceOptions);
        intent.putExtra(UIManager.EXTRA_SUGGESTION_PROMPT, prompt);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
        Tuils.sendOutput(Color.GRAY, appContext, "[interaction] choice mode: " + prompt, TerminalManager.CATEGORY_OUTPUT);
    }

    public synchronized void requestToolParameter(String fieldName, String prompt, String prefill, ChoiceCallback callback) {
        pendingChoiceCallback = callback;
        if (appContext == null) {
            if (callback != null) callback.onSelection(null);
            return;
        }

        Intent intent = new Intent(UIManager.ACTION_SHOW_PARAMETER_SUGGESTIONS);
        intent.putExtra(UIManager.EXTRA_SUGGESTION_FIELD, fieldName);
        intent.putExtra(UIManager.EXTRA_SUGGESTION_PROMPT, prompt);
        intent.putExtra(UIManager.EXTRA_SUGGESTION_PREFILL, prefill);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
        Tuils.sendOutput(Color.GRAY, appContext, "[interaction] parameter mode: " + fieldName, TerminalManager.CATEGORY_OUTPUT);
    }

    public synchronized void resolvePendingUserInteraction(String value) {
        ChoiceCallback callback = pendingChoiceCallback;
        pendingChoiceCallback = null;
        broadcastSuggestionModeReset();
        if (callback != null) callback.onSelection(value);
    }

    public synchronized void cancelPendingUserInteraction() {
        ChoiceCallback callback = pendingChoiceCallback;
        pendingChoiceCallback = null;
        broadcastSuggestionModeReset();
        if (callback != null) callback.onSelection(null);
    }

    private void broadcastSuggestionModeReset() {
        if (appContext == null) return;
        Intent intent = new Intent(UIManager.ACTION_RESET_SUGGESTIONS_MODE);
        LocalBroadcastManager.getInstance(appContext).sendBroadcast(intent);
    }

    public interface ChoiceCallback {
        void onSelection(String choice);
    }

    public boolean hasPendingUserInteraction() {
        return pendingChoiceCallback != null;
    }

    public static AISubsystem getInstance() { return instance; }

    public AISubsystem(AIProvider provider) {
        this(provider, null, null);
    }

    public AISubsystem(AIProvider provider, Context context, ToolExecutor toolExecutor) {
        this.provider = provider;
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.toolExecutor = toolExecutor != null ? toolExecutor : new AndroidToolExecutor();
        this.toolRegistry = new ToolRegistry();
        this.conversationManager = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        this.requestManager = new RequestManager(provider, 10_000, 30_000);
        this.automationConversationManager = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        this.automationRequestManager = new RequestManager(provider, 10_000, 30_000);
        this.longTermMemory = this.appContext != null ? new LongTermMemory(this.appContext) : null;
        
        if (this.appContext != null) {
            registerSystemTools();
        }

        instance = this;
    }

    public static AIProvider buildProvider(String providerName) {
        String key = XMLPrefsManager.get(Ai.api_key);
        String model = XMLPrefsManager.get(Ai.model);
        String normalizedProvider = providerName != null ? providerName.trim().toLowerCase(Locale.US) : "";

        switch (normalizedProvider) {
            case "opencode_zen": {
                String zenModel = (model != null && !model.isEmpty()) ? model : "minimax-m2.5-free";
                return new bhupendra.ai.launcher.ai.providers.OpenCodeZenProvider(key, zenModel);
            }
            case "openai":
            case "ollama": {
                String baseUrl = XMLPrefsManager.get(Ai.base_url);
                return new bhupendra.ai.launcher.ai.providers.OpenAIProvider(key, baseUrl, model);
            }
            case "gemini": {
                String geminiModel = (model != null && !model.isEmpty()) ? model : "gemini-flash-latest";
                return new bhupendra.ai.launcher.ai.providers.OpenAIProvider(key, "https://generativelanguage.googleapis.com/v1beta/openai", geminiModel);
            }
            case "claude":
                return new bhupendra.ai.launcher.ai.providers.ClaudeProvider(key);
            default:
                return new MockProvider("AI is in mock mode. Set provider in ai.xml.");
        }
    }

    private void registerSystemTools() {
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSetBrightnessTool(
            "system.set_brightness",
            "Set screen brightness percentage (0-100)",
            Collections.singletonMap("percentage", "integer from 0 to 100"),
            ToolRiskClass.STATE_CHANGING));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetBrightnessTool(
            "system.get_brightness",
            "Get current screen brightness percentage",
            Collections.emptyMap(),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemExecuteCommandTool(
            "system.execute_command",
            "Execute a raw TUI command (e.g., 'wifi -on', 'status').",
            Collections.singletonMap("command", "The full command string to execute"),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetNotificationsTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemReplyNotificationTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemAddNotificationHookTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemListNotificationHooksTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRemoveNotificationHookTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetNotificationUpdatesTool());

        Map<String, String> configArgs = new HashMap<>();
        configArgs.put("action", "get or set");
        configArgs.put("key", "The config key to read/write");
        configArgs.put("value", "The value to set (optional if action is get)");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemConfigTool(
            "system.config",
            "Read or update application settings when the target key is already known or obvious. Prefer this over search for direct requests like show_notes=false.",
            configArgs,
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchConfigTool(
            "system.search_config",
            "Search configuration keys only when the exact key is genuinely unknown.",
            Collections.singletonMap("query", "Search term for settings"),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchContactsTool(
            "system.search_contacts",
            "Search the device contacts by name.",
            Collections.singletonMap("query", "Name or partial name of the contact"),
            ToolRiskClass.READ_ONLY));

        Map<String, String> addContactArgs = new HashMap<>();
        addContactArgs.put("name", "Contact name");
        addContactArgs.put("phone", "Phone number");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemAddContactTool(
            "system.add_contact",
            "Add a new contact to the device.",
            addContactArgs,
            ToolRiskClass.STATE_CHANGING));
        
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRemoveContactTool(
            "system.remove_contact",
            "Remove an existing contact from the device.",
            Collections.singletonMap("name", "The full name of the contact to remove."),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemBeepTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemTimerTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRequestUserChoiceTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRequestUserInputTool());

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetAppFunctionsTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemExecuteAppFunctionTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemIntegrateTool());

        Map<String, String> smsArgs = new HashMap<>();
        smsArgs.put("recipient", "The name of the contact or a direct phone number.");
        smsArgs.put("message", "The content of the SMS message.");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSendSMSTool(
            "system.send_sms",
            "Send an SMS message to a contact or phone number.",
            smsArgs,
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new TermuxExecuteTool(
            "termux.execute",
            "Execute a shell command within the Termux environment.",
            new HashMap<String, String>() {{
                put("command", "The base command (e.g., 'git', 'ls').");
                put("args", "The command arguments (optional).");
            }},
            ToolRiskClass.STATE_CHANGING));

        Map<String, String> scheduleArgs = new HashMap<>();
        scheduleArgs.put("command", "TUI command to run");
        scheduleArgs.put("minutes_from_now", "Time delay");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemScheduleTaskTool(
            "system.schedule_task",
            "Schedule a TUI command to run after a delay.",
            scheduleArgs,
            ToolRiskClass.STATE_CHANGING));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemListTasksTool(
            "system.list_tasks",
            "List all currently scheduled tasks.",
            Collections.emptyMap(),
            ToolRiskClass.READ_ONLY));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemCancelTaskTool(
            "system.cancel_task",
            "Cancel a scheduled task by ID.",
            Collections.singletonMap("id", "The numeric ID of the task to cancel."),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemWebSearchQueryTool(
            "system.web_search_query",
            "Perform a web search and get a summary of results.",
            Collections.singletonMap("query", "The search query"),
            ToolRiskClass.READ_ONLY));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemWebFetchTool(
            "system.web_fetch",
            "Fetch the full content of a specific URL.",
            Collections.singletonMap("url", "The URL to fetch"),
            ToolRiskClass.READ_ONLY));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchWebTool(
            "system.search_web",
            "Open a web search in the browser.",
            Collections.singletonMap("query", "The search query"),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemMemoryStoreTool(
            "system.memory_store",
            "Store information in long-term memory.",
            Collections.singletonMap("fact", "The information to remember"),
            ToolRiskClass.STATE_CHANGING));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemMemoryRetrieveTool(
            "system.memory_retrieve",
            "Retrieve information from long-term memory.",
            Collections.singletonMap("query", "Search term for memory"),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSetVolumeTool(
            "system.set_volume",
            "Set system volume percentage (0-100)",
            Collections.singletonMap("percentage", "integer from 0 to 100"),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemUninstallAppTool(
            "system.uninstall_app",
            "Uninstall an application.",
            Collections.singletonMap("package_name", "The unique package name of the app."),
            ToolRiskClass.STATE_CHANGING));
    }

    public void cancel() { 
        String rid = lastRequestId.get();
        if (rid != null) requestManager.cancel(rid);
        handleTerminalState(rid, AIRequestState.CANCELLED);
    }

    public synchronized void hardKill() {
        String rid = lastRequestId.get();
        if (rid != null) {
            requestManager.cancel(rid);
        }
        
        conversationManager.clear();
        clearPendingConfirmationState();
        cancelPendingUserInteraction();
        
        if (rid != null) {
            handleTerminalState(rid, AIRequestState.CANCELLED);
        }
        
        notifyListeners(false);
        lastRequestId.set(null);
    }

    public void dispose() {
        cancel();
        if (instance == this) instance = null;
    }
    public bhupendra.ai.launcher.ai.platform.LauncherIndex getLauncherIndex() { return launcherIndex; }
    public LongTermMemory getLongTermMemory() { return longTermMemory; }
    public void setInstance() { instance = this; }
    public ToolRegistry getToolRegistry() { return toolRegistry; }
    public ConversationManager getConversationManager() { return conversationManager; }
    public MainPack getMainPack() { return mainPack; }
    public void setMainPack(MainPack mainPack) { this.mainPack = mainPack; }
    public bhupendra.ai.launcher.ai.platform.JourneyManager getJourneyManager() { return journeyManager; }
    public boolean isAvailable() { return provider != null; }
    public boolean isInFlight() { return requestManager.isInFlight(); }
    public boolean isAwaitingConfirmation() { return awaitingConfirmation; }

    public synchronized void refresh() {
        if (appContext == null) return;
        
        String providerName = XMLPrefsManager.get(Ai.provider);
        
        android.util.Log.d("AI_REFRESH", "Refreshing AI subsystem: provider=" + providerName);
        this.provider = buildProvider(providerName);
        requestManager.setProvider(this.provider);
        automationRequestManager.setProvider(this.provider);
    }

    public void confirmCurrentTool() {
        Runnable confirmAction = null;
        synchronized (this) {
            if (awaitingConfirmation && pendingConfirmAction != null) {
                confirmAction = pendingConfirmAction;
                clearPendingConfirmationState();
            }
        }
        if (confirmAction != null) {
            confirmAction.run();
        }
    }

    public void declineCurrentTool() {
        Runnable declineAction = null;
        synchronized (this) {
            if (awaitingConfirmation && pendingDeclineAction != null) {
                declineAction = pendingDeclineAction;
                clearPendingConfirmationState();
            }
        }
        if (declineAction != null) {
            declineAction.run();
        }
    }

    private synchronized void clearPendingConfirmationState() {
        awaitingConfirmation = false;
        pendingConfirmAction = null;
        pendingDeclineAction = null;
    }

    private void handleTerminalState(String requestId, AIRequestState state) {
        if (requestId == null) return;
        clearPendingConfirmationState();
        cancelPendingUserInteraction();
        notifyListeners(false);
        if (state == AIRequestState.COMPLETED
                || state == AIRequestState.FAILED
                || state == AIRequestState.CANCELLED) {
            logAITurnFinished();
        }
        lastRequestId.compareAndSet(requestId, null);
    }

    private void logAITurnFinished() {
        if (appContext == null) return;
        try {
            android.util.Log.i("AI_OUTPUT", "AI_TURN_FINISHED");
        } catch (RuntimeException ignored) {
            // Local JVM tests do not mock android.util.Log.
        }
    }

    private void handleAIResponse(String requestId, AIResponse response, AICallback callback) {
        if (response.type == AIResponse.Type.TOOL_CALLS) {
            conversationManager.append(ConversationTurn.assistantCalls(response.toolCalls));
            beginToolExecution(requestId, response.toolCalls, callback);
        } else if (response.type == AIResponse.Type.TEXT) {
            if (response.text != null && !response.text.isEmpty()) {
                conversationManager.append(ConversationTurn.assistant(response.text));
            }
            callback.onResponse(response);
            requestManager.finishToolExecution(requestId, true);
        } else if (response.type == AIResponse.Type.ERROR) {
            callback.onResponse(response);
            requestManager.finishToolExecution(requestId, false);
        }
    }

    private void performFollowUp(String requestId, AICallback callback) {
        requestManager.transition(requestId, AIRequestState.THINKING, callback);
        AIRequest request = new AIRequest.Builder()
            .requestId(requestId)
            .history(conversationManager.getHistory())
            .tools(toolRegistry.getTools())
            .systemPrompt(getSystemPrompt())
            .build();
        provider.complete(request, requestId, new AICallback() {
            @Override public void onToken(String rid, String token) { callback.onToken(rid, token); }
            @Override public void onResponse(AIResponse response) { handleAIResponse(requestId, response, callback); }
            @Override public void onStateChange(String rid, AIRequestState s) { callback.onStateChange(rid, s); }
        });
    }

    public String submitAutomation(String userMessage, AICallback callback) {
        String requestId = UUID.randomUUID().toString();
        automationConversationManager.append(ConversationTurn.user(userMessage));
        AIRequest request = new AIRequest.Builder()
            .requestId(requestId)
            .userMessage(userMessage)
            .history(automationConversationManager.getHistory())
            .tools(toolRegistry.getTools())
            .systemPrompt(getSystemPrompt())
            .maxTokens(2048)
            .build();
        automationRequestManager.submit(request, new AICallback() {
            @Override public void onToken(String rid, String token) { callback.onToken(rid, token); }
            @Override public void onResponse(AIResponse response) { handleAutomationResponse(requestId, response, callback); }
            @Override public void onStateChange(String rid, AIRequestState s) { callback.onStateChange(rid, s); }
        });
        return requestId;
    }

    private void handleAutomationResponse(String requestId, AIResponse response, AICallback callback) {
        if (response.type == AIResponse.Type.TOOL_CALLS) {
            automationConversationManager.append(ConversationTurn.assistantCalls(response.toolCalls));
            beginAutomationToolExecution(requestId, response.toolCalls, callback);
        } else if (response.type == AIResponse.Type.TEXT) {
            if (response.text != null && !response.text.isEmpty()) {
                automationConversationManager.append(ConversationTurn.assistant(response.text));
            }
            callback.onResponse(response);
            automationRequestManager.finishToolExecution(requestId, true);
        } else if (response.type == AIResponse.Type.ERROR) {
            callback.onResponse(response);
            automationRequestManager.finishToolExecution(requestId, false);
        }
    }

    private void beginAutomationToolExecution(final String requestId, List<ToolCall> toolCalls, final AICallback callback) {
        executeAutomationToolAtIndex(requestId, toolCalls, 0, callback);
    }

    private void executeAutomationToolAtIndex(final String requestId, final List<ToolCall> toolCalls, final int index, final AICallback callback) {
        if (index >= toolCalls.size()) {
            performAutomationFollowUp(requestId, callback);
            return;
        }
        ToolCall toolCall = toolCalls.get(index);
        Tool tool = toolRegistry.lookup(toolCall.toolName);
        if (tool == null) {
            automationConversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "Tool not available"));
            executeAutomationToolAtIndex(requestId, toolCalls, index + 1, callback);
            return;
        }
        Runnable runTool = () -> {
            try {
                automationRequestManager.transition(requestId, AIRequestState.THINKING, callback);
                if (appContext == null && toolExecutor instanceof AndroidToolExecutor) {
                    callback.onResponse(AIResponse.error(requestId, "Tool execution requires app context"));
                    automationRequestManager.finishToolExecution(requestId, false);
                    return;
                }
                
                String output = toolExecutor.execute(appContext, tool, toolCall.argumentsJson);
                automationConversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, output != null ? output : "[done]"));
                if (output != null && !output.isEmpty()) callback.onResponse(AIResponse.toolOutput(requestId, output, toolCall));
                executeAutomationToolAtIndex(requestId, toolCalls, index + 1, callback);
            } catch (Exception e) {
                automationConversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "[error: " + e.getMessage() + "]"));
                executeAutomationToolAtIndex(requestId, toolCalls, index + 1, callback);
            }
        };

        // For automation, we never prompt the user to confirm. We either run it if it's safe or reject it if it requires confirmation.
        boolean requireConfirmation = true;
        if (appContext != null) {
            requireConfirmation = XMLPrefsManager.getBoolean(Ai.confirm_state_changing);
        }
        
        if (tool.riskClass == ToolRiskClass.READ_ONLY || 
            "system.execute_command".equals(tool.name) ||
            !requireConfirmation) {
            runTool.run();
            return;
        }

        // Auto-decline if confirmation needed for automation (or we could just run it, but automation shouldn't bypass safety by default)
        automationConversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "[error: tool requires interactive confirmation which is unavailable in background automation]"));
        executeAutomationToolAtIndex(requestId, toolCalls, index + 1, callback);
    }

    private void performAutomationFollowUp(String requestId, AICallback callback) {
        automationRequestManager.transition(requestId, AIRequestState.THINKING, callback);
        AIRequest request = new AIRequest.Builder()
            .requestId(requestId)
            .history(automationConversationManager.getHistory())
            .tools(toolRegistry.getTools())
            .systemPrompt(getSystemPrompt())
            .build();
        provider.complete(request, requestId, new AICallback() {
            @Override public void onToken(String rid, String token) { callback.onToken(rid, token); }
            @Override public void onResponse(AIResponse response) { handleAutomationResponse(requestId, response, callback); }
            @Override public void onStateChange(String rid, AIRequestState s) { callback.onStateChange(rid, s); }
        });
    }

    public String submit(String userMessage, AICallback callback) {
        String requestId = UUID.randomUUID().toString();
        lastRequestId.set(requestId);
        conversationManager.append(ConversationTurn.user(userMessage));
        notifyListeners(true);
        AIRequest request = new AIRequest.Builder()
            .requestId(requestId)
            .userMessage(userMessage)
            .history(conversationManager.getHistory())
            .tools(toolRegistry.getTools())
            .systemPrompt(getSystemPrompt())
            .maxTokens(2048)
            .build();
        requestManager.submit(request, new AICallback() {
            @Override public void onToken(String rid, String token) { callback.onToken(rid, token); }
            @Override public void onResponse(AIResponse response) { handleAIResponse(requestId, response, callback); }
            @Override public void onStateChange(String rid, AIRequestState s) {
                callback.onStateChange(rid, s);
                if (s == AIRequestState.COMPLETED
                        || s == AIRequestState.CANCELLED
                        || s == AIRequestState.TIMED_OUT_CONNECT
                        || s == AIRequestState.TIMED_OUT_INACTIVITY
                        || s == AIRequestState.FAILED) {
                    handleTerminalState(rid, s);
                }
            }
        });
        return requestId;
    }

    private String getSystemPrompt() {
        String bundledPrompt = "";
        String localPrompt = "";
        java.io.File promptFile = new java.io.File(FileSystemManager.getFolder(), "ai_system_prompt.md");
        try {
            if (appContext != null) {
                java.io.InputStream in = appContext.getAssets().open("ai_system_prompt.md");
                bundledPrompt = FileSystemManager.inputStreamToString(in);
            }
        } catch (Exception e) {}
        try {
            if (promptFile.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(promptFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                reader.close();
                localPrompt = sb.toString();
            }
        } catch (Exception e) {}

        String basePrompt = bundledPrompt;
        if (basePrompt.isEmpty()) {
            basePrompt = localPrompt;
        } else if (!localPrompt.isEmpty()) {
            basePrompt = basePrompt + "\nLOCAL PROMPT OVERRIDES:\n" + localPrompt;
        }

        if (basePrompt.isEmpty() && appContext != null) {
            try {
                java.io.InputStream in = appContext.getAssets().open("ai_system_prompt.md");
                basePrompt = FileSystemManager.inputStreamToString(in);
            } catch (Exception ignored) {}
        }
        if (basePrompt.isEmpty()) basePrompt = "You are an AI assistant.";

        StringBuilder cmds = new StringBuilder();
        if (mainPack != null && mainPack.commandGroup != null) {
            for (String n : mainPack.commandGroup.getCommandNames()) cmds.append(n).append(", ");
        }
        basePrompt = basePrompt.replace("{{AVAILABLE_COMMANDS}}", cmds.toString());

        String pulseContent = "";
        if (appContext != null) {
            StringBuilder pulse = new StringBuilder();
            pulse.append("Time: ").append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())).append("\n");
            try {
                int battery = DeviceStateManager.getBatteryLevel(appContext);
                pulse.append("Battery: ").append(battery).append("%\n");
                
                ActivityManager am = (ActivityManager) appContext.getSystemService(Context.ACTIVITY_SERVICE);
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                pulse.append("Memory: ").append(mi.availMem / (1024*1024)).append("MB free\n");
            } catch (Exception ignored) {}
            pulseContent = pulse.toString();
        }
        return basePrompt.replace("{{SYSTEM_PULSE}}", pulseContent) + PROMPT_APPENDIX;
    }

    private void beginToolExecution(final String requestId, List<ToolCall> toolCalls, final AICallback callback) {
        executeToolAtIndex(requestId, toolCalls, 0, callback);
    }

    private void executeToolAtIndex(final String requestId, final List<ToolCall> toolCalls, final int index, final AICallback callback) {
        if (index >= toolCalls.size()) {
            performFollowUp(requestId, callback);
            return;
        }
        ToolCall toolCall = toolCalls.get(index);
        Tool tool = toolRegistry.lookup(toolCall.toolName);
        if (tool == null) {
            conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "Tool not available"));
            executeToolAtIndex(requestId, toolCalls, index + 1, callback);
            return;
        }
        Runnable runTool = () -> {
            try {
                requestManager.transition(requestId, AIRequestState.THINKING, callback);
                
                // Technical visibility: Inform the user which tool is running
                if (appContext != null) {
                    Tuils.sendOutput(Color.GRAY, appContext, "[executing] " + tool.name, TerminalManager.CATEGORY_OUTPUT);
                }

                if (appContext == null && toolExecutor instanceof AndroidToolExecutor) {
                    callback.onResponse(AIResponse.error(requestId, "Tool execution requires app context"));
                    requestManager.finishToolExecution(requestId, false);
                    return;
                }
                
                String output = toolExecutor.execute(appContext, tool, toolCall.argumentsJson);
                conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, output != null ? output : "[done]"));
                if (output != null && !output.isEmpty()) callback.onResponse(AIResponse.toolOutput(requestId, output, toolCall));
                executeToolAtIndex(requestId, toolCalls, index + 1, callback);
            } catch (Exception e) {
                conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "[error: " + e.getMessage() + "]"));
                executeToolAtIndex(requestId, toolCalls, index + 1, callback);
            }
        };

        boolean requireConfirmation = true;
        if (appContext != null) {
            requireConfirmation = XMLPrefsManager.getBoolean(Ai.confirm_state_changing);
        }
        
        if (tool.riskClass == ToolRiskClass.READ_ONLY || 
            "system.execute_command".equals(tool.name) ||
            !requireConfirmation) {
            runTool.run();
            return;
        }

        awaitingConfirmation = true;
        pendingConfirmAction = runTool;
        pendingDeclineAction = () -> {
            clearPendingConfirmationState();
            conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "[user declined]"));
            executeToolAtIndex(requestId, toolCalls, index + 1, callback);
        };
        String label = tool.description != null && tool.description.length() > 0 ? tool.description : tool.name;
        callback.onResponse(AIResponse.text(requestId, "[AI wants to] " + label + "\nRun? (Enter to confirm)"));
    }
}
