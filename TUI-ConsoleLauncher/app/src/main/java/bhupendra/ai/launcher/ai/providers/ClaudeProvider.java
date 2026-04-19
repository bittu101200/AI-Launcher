package bhupendra.ai.launcher.ai.providers;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;
import bhupendra.ai.launcher.ai.*;

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
        callback.onStateChange(requestId, AIRequestState.THINKING);
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
                    try {
                        if (!response.isSuccessful()) {
                            callback.onResponse(AIResponse.error(requestId, "HTTP " + response.code()));
                            return;
                        }
                        JSONObject json = new JSONObject(response.body().string());
                        JSONArray contentArray = json.getJSONArray("content");
                        if (contentArray.length() > 0) {
                            JSONObject block = contentArray.getJSONObject(0);
                            if ("text".equals(block.getString("type"))) {
                                callback.onResponse(AIResponse.text(requestId, block.getString("text")));
                            } else {
                                callback.onResponse(AIResponse.error(requestId, "unexpected block type"));
                            }
                        } else {
                            callback.onResponse(AIResponse.error(requestId, "empty content"));
                        }
                    } catch (Exception e) {
                        callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                    } finally {
                        response.close();
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