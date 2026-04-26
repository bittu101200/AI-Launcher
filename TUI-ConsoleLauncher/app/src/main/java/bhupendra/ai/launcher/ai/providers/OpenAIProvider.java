package bhupendra.ai.launcher.ai.providers;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;
import bhupendra.ai.launcher.ai.*;
import android.util.Log;

public class OpenAIProvider implements AIProvider {

    private static final String TAG = "OpenAIProvider";
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
        this.client = SharedHttpClient.get();
    }

    @Override
    public void complete(AIRequest request, String requestId, AICallback callback) {
        callback.onStateChange(requestId, AIRequestState.THINKING);
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
                JSONObject msg = new JSONObject();
                switch (t.role) {
                    case USER:
                        msg.put("role", "user");
                        msg.put("content", t.content);
                        messages.put(msg);
                        break;
                    case ASSISTANT:
                        msg.put("role", "assistant");
                        if (t.content != null) {
                            msg.put("content", t.content);
                        }
                        if (t.toolCalls != null && !t.toolCalls.isEmpty()) {
                            JSONArray calls = new JSONArray();
                            for (ToolCall tc : t.toolCalls) {
                                JSONObject call = new JSONObject();
                                call.put("id", tc.callId);
                                call.put("type", "function");
                                JSONObject fn = new JSONObject();
                                fn.put("name", OpenAIToolSupport.encodeToolName(tc.toolName));
                                fn.put("arguments", tc.argumentsJson);
                                call.put("function", fn);
                                
                                if (tc.thoughtSignature != null) {
                                    JSONObject extra = new JSONObject();
                                    JSONObject google = new JSONObject();
                                    google.put("thought_signature", tc.thoughtSignature);
                                    extra.put("google", google);
                                    call.put("extra_content", extra);
                                }
                                
                                calls.put(call);
                            }
                            msg.put("tool_calls", calls);
                        }
                        messages.put(msg);
                        break;
                    case TOOL:
                        msg.put("role", "tool");
                        msg.put("tool_call_id", t.toolCallId);
                        msg.put("name", OpenAIToolSupport.encodeToolName(t.toolName));
                        msg.put("content", t.content);
                        messages.put(msg);
                        break;
                }
            }
            
            body.put("messages", messages);
            if (request.tools != null && !request.tools.isEmpty()) {
                body.put("tools", OpenAIToolSupport.buildToolsArray(request.tools));
                body.put("tool_choice", "auto");
            }

            String endpoint = baseUrl.replaceAll("/$", "") + "/v1/chat/completions";
            Log.d(TAG, "Request: endpoint=" + endpoint + " model=" + model);
            
            Request.Builder reqBuilder = new Request.Builder()
                .url(endpoint)
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_TYPE));
            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.addHeader("Authorization", "Bearer " + apiKey);
            }

            client.newCall(reqBuilder.build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "onFailure: " + e.getMessage(), e);
                    if (!call.isCanceled()) callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String respBody = response.body().string();
                        Log.d(TAG, "onResponse: code=" + response.code());
                        if (!response.isSuccessful()) {
                            callback.onResponse(AIResponse.error(requestId, "HTTP " + response.code() + ": " + respBody));
                            return;
                        }
                        JSONObject json = new JSONObject(respBody);
                        AIResponse aiResponse = OpenAIToolSupport.parseResponse(requestId, json);
                        callback.onResponse(aiResponse);
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse parse error: " + e.getMessage(), e);
                        callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "complete error: " + e.getMessage(), e);
            callback.onResponse(AIResponse.error(requestId, e.getMessage()));
        }
    }

    @Override public boolean supportsToolUse() { return true; }
    @Override public boolean supportsStreaming() { return false; }
    @Override public String providerId() { return "openai"; }
}