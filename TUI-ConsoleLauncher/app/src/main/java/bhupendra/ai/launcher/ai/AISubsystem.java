package bhupendra.ai.launcher.ai;

import bhupendra.ai.launcher.managers.FileSystemManager;


import android.content.Context;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Ai;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.tuils.TermuxManager;
import bhupendra.ai.launcher.ai.tools.*;


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
            java.util.Collections.singletonMap("percentage", "integer from 0 to 100"),
            ToolRiskClass.STATE_CHANGING));
            
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetBrightnessTool(
            "system.get_brightness",
            "Get current screen brightness percentage",
            java.util.Collections.emptyMap(),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemExecuteCommandTool(
            "system.execute_command",
            "Execute a raw TUI command (e.g., 'wifi -on', 'bluetooth -off', 'apps -l', 'status'). Use this for existing app features.",
            java.util.Collections.singletonMap("command", "The full command string to execute"),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetNotificationsTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemReplyNotificationTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemAddNotificationHookTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemListNotificationHooksTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRemoveNotificationHookTool());
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemGetNotificationUpdatesTool());

        java.util.Map<String, String> configArgs = new java.util.HashMap<>();
        configArgs.put("action", "get or set");
        configArgs.put("key", "The preference key (e.g., 'ui_input_output_color', 'behavior_auto_show_keyboard')");
        configArgs.put("value", "The value to set (optional for 'get')");
        
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemConfigTool(
            "system.config",
            "Get or set application configuration preferences.",
            configArgs,
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchConfigTool(
            "system.search_config",
            "Search for configuration keys by name or description. Use this to find the right key before setting it.",
            java.util.Collections.singletonMap("query", "The search term (e.g., 'color', 'size', 'toolbar')"),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchContactsTool(
            "system.search_contacts",
            "Search for contacts by name to find their exact name or phone number.",
            java.util.Collections.singletonMap("query", "The name or partial name of the contact"),
            ToolRiskClass.READ_ONLY));

        java.util.Map<String, String> volumeArgs = new java.util.HashMap<>();
        volumeArgs.put("stream", "one of: 'ring', 'media', 'alarm', 'notification', 'system', 'voice_call'");
        volumeArgs.put("percentage", "integer from 0 to 100");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSetVolumeTool(
            "system.set_volume",
            "Set volume for a specific stream.",
            volumeArgs,
            ToolRiskClass.STATE_CHANGING));

        // Persistent Memory Tools
        java.util.Map<String, String> storeArgs = new java.util.HashMap<>();
        storeArgs.put("key", "A short descriptive name for the memory (e.g., 'Mom's Birthday')");
        storeArgs.put("value", "The actual important information to store.");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemMemoryStoreTool(
            "system.memory_store",
            "Persistently store highly important information explicitly requested by the user.",
            storeArgs,
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemMemoryRetrieveTool(
            "system.memory_retrieve",
            "Retrieve stored memories by searching for a key or keyword.",
            java.util.Collections.singletonMap("query", "A keyword or key to search for in memories."),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemSearchWebTool(
            "system.search_web",
            "Open a Google search in Chrome.",
            java.util.Collections.singletonMap("query", "The search query."),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemWebFetchTool(
            "system.web_fetch",
            "Fetch the text content of a URL to answer questions accurately.",
            java.util.Collections.singletonMap("url", "The full URL starting with http/https."),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemWebSearchQueryTool(
            "system.web_search_query",
            "Perform a web search and return the results as text. Use this to find information BACKGROUND without opening Chrome.",
            java.util.Collections.singletonMap("query", "The search query."),
            ToolRiskClass.READ_ONLY));

        java.util.Map<String, String> scheduleArgs = new java.util.HashMap<>();
        scheduleArgs.put("command", "The full TUI command to execute (e.g., 'wifi -on', 'apps -l')");
        scheduleArgs.put("delay_minutes", "Minutes to wait before execution (integer)");
        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemScheduleTaskTool(
            "system.schedule_task",
            "Schedule a command to be executed after a certain delay in minutes.",
            scheduleArgs,
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemListTasksTool(
            "system.list_tasks",
            "List all currently scheduled tasks.",
            java.util.Collections.emptyMap(),
            ToolRiskClass.READ_ONLY));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemCancelTaskTool(
            "system.cancel_task",
            "Cancel a scheduled task by its ID.",
            java.util.Collections.singletonMap("id", "The task ID (e.g., 'task_12345678')"),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemUninstallAppTool(
            "system.uninstall_app",
            "Initiate uninstallation of an Android application.",
            java.util.Collections.singletonMap("packageName", "The full package name of the app to uninstall (e.g., com.example.app)."),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemAddContactTool(
            "system.add_contact",
            "Add a new contact to the device.",
            new java.util.HashMap<String, String>() {{
                put("name", "The full name of the contact.");
                put("phone", "The phone number of the contact.");
            }},
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new SystemRemoveContactTool(
            "system.remove_contact",
            "Remove an existing contact from the device.",
            java.util.Collections.singletonMap("name", "The full name of the contact to remove."),
            ToolRiskClass.STATE_CHANGING));

        toolRegistry.register(ToolRegistry.Tier.SYSTEM, new TermuxExecuteTool(
            "termux.execute",
            "Execute a Linux command in the Termux environment. Use this for complex tasks like git, python, node, or package management.",
            new java.util.HashMap<String, String>() {{
                put("command", "The base command (e.g., 'git', 'python', 'ls').");
                put("arguments", "The arguments for the command as a single string (optional).");
            }},
            ToolRiskClass.STATE_CHANGING));
    }

    public LongTermMemory getLongTermMemory() { return longTermMemory; }

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
            
            // Central completion signal for all AI turns (including tool follow-ups)
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
        java.io.File promptFile = new java.io.File(bhupendra.ai.launcher.managers.FileSystemManager.getFolder(), "ai_system_prompt.md");
        if (!promptFile.exists()) {
            try {
                java.io.InputStream is = appContext.getAssets().open("ai_system_prompt.md");
                java.io.FileOutputStream os = new java.io.FileOutputStream(promptFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }
                is.close();
                os.close();
            } catch (Exception e) {
                android.util.Log.e("AISubsystem", "Failed to copy default prompt", e);
            }
        }
        
        try {
            if (promptFile.exists()) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(promptFile));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("n");
                }
                reader.close();
                
                String prompt = sb.toString();
                
                // Append command names dynamically
                MainPack mp = getMainPack();
                StringBuilder cmds = new StringBuilder();
                if (mp != null && mp.commandGroup != null) {
                    String[] names = mp.commandGroup.getCommandNames();
                    for (int i = 0; i < names.length; i++) {
                        cmds.append(names[i]);
                        if (i < names.length - 1) cmds.append(", ");
                    }
                } else {
                    cmds.append("wifi, bluetooth, apps, status, call, flash, volume, etc.");
                }
                return prompt.replace("{{AVAILABLE_COMMANDS}}", cmds.toString());
            }
        } catch (Exception e) {
            android.util.Log.e("AISubsystem", "Error reading prompt file", e);
        }
        return "You are an AI assistant in a terminal launcher.";
    }

    public void cancel() {
        awaitingConfirmation = false;
        pendingConfirmAction = null;
        pendingDeclineAction = null;
        TermuxManager.cancelAll();
        String rid = lastRequestId.get();
        if (rid != null) {
            requestManager.cancel(rid);
        }
        notifyListeners(false);
    }

    public AIRequestState getState() { return requestManager.getState(); }

    public ToolRegistry getToolRegistry() { return toolRegistry; }

    public ConversationManager getConversationManager() { return conversationManager; }

    public RequestManager getRequestManager() { return requestManager; }

    public bhupendra.ai.launcher.ai.platform.JourneyManager getJourneyManager() { return journeyManager; }

    public bhupendra.ai.launcher.ai.platform.LauncherIndex getLauncherIndex() { return launcherIndex; }

    public void setMainPack(MainPack mainPack) {
        this.mainPack = mainPack;
        if (toolExecutor instanceof AndroidToolExecutor) {
            ((AndroidToolExecutor) toolExecutor).setMainPack(mainPack);
        }
    }

    public MainPack getMainPack() {
        return mainPack;
    }

    private void beginToolExecution(String requestId, List<ToolCall> toolCalls, AICallback callback) {
        executeToolAtIndex(requestId, toolCalls, 0, callback);
    }

    private void executeToolAtIndex(String requestId, List<ToolCall> toolCalls, int index, AICallback callback) {
        if (requestManager.isCancelRequested()) {
            android.util.Log.i("AI_OUTPUT", "Tool execution cancelled by user");
            return;
        }

        if (index >= toolCalls.size()) {
            performFollowUp(requestId, callback);
            return;
        }

        ToolCall toolCall = toolCalls.get(index);
        Tool tool = toolRegistry.lookup(toolCall.toolName);
        if (tool == null) {
            String error = "Tool not available: " + toolCall.toolName;
            conversationManager.append(ConversationTurn.tool(toolCall.callId, error));
            callback.onResponse(AIResponse.error(requestId, error));
            requestManager.finishToolExecution(requestId, false);
            return;
        }

        Runnable runTool = () -> {
            try {
                if (requestManager.isCancelRequested()) return;
                
                String output = toolExecutor.execute(appContext, tool, toolCall.argumentsJson);
                
                if (requestManager.isCancelRequested()) return;

                conversationManager.append(ConversationTurn.tool(toolCall.callId, output != null ? output : "[done]"));
                if (output != null && !output.isEmpty()) {
                    callback.onResponse(AIResponse.toolOutput(requestId, output, toolCall));
                }
                executeToolAtIndex(requestId, toolCalls, index + 1, callback);
            } catch (Exception e) {
                if (requestManager.isCancelRequested()) return;

                String error = e.getMessage() != null ? e.getMessage() : "Execution failed";
                conversationManager.append(ConversationTurn.tool(toolCall.callId, "[error: " + error + "]"));
                callback.onResponse(AIResponse.error(requestId, error));
                requestManager.finishToolExecution(requestId, false);
            }
        };

        boolean needsConfirmation = true;
        if (tool.riskClass == ToolRiskClass.READ_ONLY) {
            needsConfirmation = false;
        } else if (tool.riskClass == ToolRiskClass.STATE_CHANGING) {
            if (!XMLPrefsManager.getBoolean(Ai.confirm_state_changing)) {
                needsConfirmation = false;
            }
        }

        if (!needsConfirmation) {
            runTool.run();
            return;
        }

        awaitingConfirmation = true;
        pendingConfirmAction = () -> {
            awaitingConfirmation = false;
            runTool.run();
        };
        pendingDeclineAction = () -> {
            awaitingConfirmation = false;
            conversationManager.append(ConversationTurn.tool(toolCall.callId, "[user declined this action]"));
            callback.onResponse(AIResponse.text(requestId, "[skipped " + describeTool(tool) + "]"));
            executeToolAtIndex(requestId, toolCalls, index + 1, callback);
        };
        callback.onResponse(AIResponse.text(requestId,
            "[AI wants to] " + describeTool(tool) + "\nRun? (Enter to confirm / stop to cancel)"));
    }

    private String describeTool(Tool tool) {
        return tool.description;
    }
}