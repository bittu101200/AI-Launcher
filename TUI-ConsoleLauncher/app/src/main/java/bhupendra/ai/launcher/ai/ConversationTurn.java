package bhupendra.ai.launcher.ai;

import java.util.List;

public class ConversationTurn {
    public enum Role { USER, ASSISTANT, TOOL }
    
    public final Role role;
    public final String content;
    public boolean pinned;
    
    // For Assistant tool calls
    public final List<ToolCall> toolCalls;
    
    // For Tool results
    public final String toolCallId;
    public final String toolName;

    public ConversationTurn(Role role, String content) {
        this(role, content, null, null, null);
    }

    public ConversationTurn(Role role, String content, List<ToolCall> toolCalls, String toolCallId, String toolName) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
        this.toolName = toolName;
    }
    
    public static ConversationTurn user(String content) {
        return new ConversationTurn(Role.USER, content, null, null, null);
    }
    
    public static ConversationTurn assistant(String content) {
        return new ConversationTurn(Role.ASSISTANT, content, null, null, null);
    }
    
    public static ConversationTurn assistantCalls(List<ToolCall> calls) {
        return new ConversationTurn(Role.ASSISTANT, null, calls, null, null);
    }
    
    public static ConversationTurn tool(String toolCallId, String toolName, String result) {
        return new ConversationTurn(Role.TOOL, result, null, toolCallId, toolName);
    }
}
