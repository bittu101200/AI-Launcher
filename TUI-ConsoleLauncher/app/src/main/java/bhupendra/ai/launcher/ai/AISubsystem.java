package bhupendra.ai.launcher.ai;

import bhupendra.ai.launcher.managers.FileSystemManager;
import android.content.Context;
import android.app.ActivityManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Ai;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.TermuxManager;
import bhupendra.ai.launcher.ai.tools.*;
import bhupendra.ai.launcher.managers.DeviceStateManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

public class AISubsystem {

    private static volatile AISubsystem instance;

    private final AIProvider provider;
    private final Context appContext;
    private final ToolExecutor toolExecutor;
    private final ToolRegistry toolRegistry;
    private final ConversationManager conversationManager;
    private final RequestManager requestManager;
    private final LongTermMemory longTermMemory;
    private final bhupendra.ai.launcher.ai.platform.LauncherIndex launcherIndex = new bhupendra.ai.launcher.ai.platform.LauncherIndex();
    private final AtomicReference<String> lastRequestId = new AtomicReference<>();
    private final bhupendra.ai.launcher.ai.platform.JourneyManager journeyManager = new bhupendra.ai.launcher.ai.platform.JourneyManager();

    private MainPack mainPack;
    private volatile boolean awaitingConfirmation;
    private volatile Runnable pendingConfirmAction;
    private volatile Runnable pendingDeclineAction;

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
        // Feature disabled for now to maintain polish
    }

    public interface ChoiceCallback {
        void onSelection(String choice);
    }

    public static AISubsystem getInstance() { return instance; }

    public AISubsystem(AIProvider provider) {
        this(provider, null, null);
    }

    public AISubsystem(AIProvider provider, Context context, ToolExecutor toolExecutor) {
        this.provider = provider;
        this.appContext = context != null ? context.getApplicationContext() : null;
        this.toolExecutor = toolExecutor;
        this.toolRegistry = new ToolRegistry();
        this.conversationManager = new ConversationManager(ConversationManager.Mode.SESSION, 4000);
        this.requestManager = new RequestManager(provider, 10_000, 30_000);
        this.longTermMemory = this.appContext != null ? new LongTermMemory(this.appContext) : null;
        
        if (this.appContext != null) {
            registerSystemTools();
        }

        instance = this;
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
            "Read or update application settings.",
            configArgs,
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchConfigTool(
            "system.search_config",
            "Search for available configuration keys and their current values.",
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
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRequestUserChoiceTool());

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
    }
    public void dispose() { cancel(); }
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
            notifyListeners(false);
            android.util.Log.i("AI_OUTPUT", "AI_TURN_FINISHED");
        } else if (response.type == AIResponse.Type.ERROR) {
            callback.onResponse(response);
            requestManager.finishToolExecution(requestId, false);
            notifyListeners(false);
            android.util.Log.i("AI_OUTPUT", "AI_TURN_FINISHED");
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
            @Override public void onStateChange(String rid, AIRequestState s) { callback.onStateChange(rid, s); }
        });
        return requestId;
    }

    private String getSystemPrompt() {
        String basePrompt = "";
        java.io.File promptFile = new java.io.File(FileSystemManager.getFolder(), "ai_system_prompt.md");
        try {
            if (promptFile.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(promptFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line).append("\n");
                reader.close();
                basePrompt = sb.toString();
            }
        } catch (Exception e) {}
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
        return basePrompt.replace("{{SYSTEM_PULSE}}", pulseContent);
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
                String output = toolExecutor.execute(appContext, tool, toolCall.argumentsJson);
                conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, output != null ? output : "[done]"));
                if (output != null && !output.isEmpty()) callback.onResponse(AIResponse.toolOutput(requestId, output, toolCall));
                executeToolAtIndex(requestId, toolCalls, index + 1, callback);
            } catch (Exception e) {
                conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "[error: " + e.getMessage() + "]"));
                executeToolAtIndex(requestId, toolCalls, index + 1, callback);
            }
        };

        // PERMISSION FLOW REPAIR: 
        // 1. system.execute_command should NOT ask for permission (it's the core engine)
        // 2. READ_ONLY and LAUNCH_ONLY should NOT ask for permission
        if (tool.riskClass == ToolRiskClass.READ_ONLY || 
            tool.riskClass == ToolRiskClass.LAUNCH_ONLY || 
            "system.execute_command".equals(tool.name)) {
            runTool.run();
            return;
        }

        awaitingConfirmation = true;
        pendingConfirmAction = () -> { awaitingConfirmation = false; runTool.run(); };
        pendingDeclineAction = () -> {
            awaitingConfirmation = false;
            conversationManager.append(ConversationTurn.tool(toolCall.callId, toolCall.toolName, "[user declined]"));
            executeToolAtIndex(requestId, toolCalls, index + 1, callback);
        };
        callback.onResponse(AIResponse.text(requestId, "[AI wants to] " + tool.name + "\nRun? (Enter to confirm)"));
    }
}
