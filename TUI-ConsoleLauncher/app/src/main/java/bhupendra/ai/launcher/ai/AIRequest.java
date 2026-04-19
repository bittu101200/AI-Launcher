package bhupendra.ai.launcher.ai;

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