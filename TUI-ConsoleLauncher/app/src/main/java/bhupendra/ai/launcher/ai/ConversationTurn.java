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

    public ConversationTurn(Role role, String content) {
        this(role, content, null, null);
    }

    public ConversationTurn(Role role, String content, List<ToolCall> toolCalls, String toolCallId) {
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.toolCallId = toolCallId;
    }
    
    public static ConversationTurn user(String content) {
        return new ConversationTurn(Role.USER, content);
    }
    
    public static ConversationTurn assistant(String content) {
        return new ConversationTurn(Role.ASSISTANT, content);
    }
    
    public static ConversationTurn assistantCalls(List<ToolCall> calls) {
        return new ConversationTurn(Role.ASSISTANT, null, calls, null);
    }
    
    public static ConversationTurn tool(String toolCallId, String result) {
        return new ConversationTurn(Role.TOOL, result, null, toolCallId);
    }
}