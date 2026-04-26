package bhupendra.ai.launcher.ai.providers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import bhupendra.ai.launcher.ai.AICallback;
import bhupendra.ai.launcher.ai.AIProvider;
import bhupendra.ai.launcher.ai.AIRequest;
import bhupendra.ai.launcher.ai.AIRequestState;
import bhupendra.ai.launcher.ai.AIResponse;
import bhupendra.ai.launcher.ai.ConversationTurn;
import bhupendra.ai.launcher.ai.OpenAIToolSupport;
import bhupendra.ai.launcher.ai.ToolCall;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenCodeZenProvider implements AIProvider {

    private static final String TAG = "OpenCodeZenProvider";
    private static final String API_URL = "https://opencode.ai/zen/v1/chat/completions";
    private static final String DEFAULT_MODEL = "minimax-m2.5-free";
    private static final MediaType JSON_TYPE = MediaType.get("application/json");

    private final String apiKey;
    private final String model;
    private final OkHttpClient client;

    public OpenCodeZenProvider(String apiKey, String model) {
        this.apiKey = apiKey;
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

                                JSONObject function = new JSONObject();
                                function.put("name", OpenAIToolSupport.encodeToolName(tc.toolName));
                                function.put("arguments", tc.argumentsJson);
                                call.put("function", function);
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

            Request.Builder reqBuilder = new Request.Builder()
                .url(API_URL)
                .addHeader("content-type", "application/json")
                .post(RequestBody.create(body.toString(), JSON_TYPE));
            if (apiKey != null && !apiKey.isEmpty()) {
                reqBuilder.addHeader("x-api-key", apiKey);
            }

            Log.d(TAG, "Request: endpoint=" + API_URL + " model=" + model);

            client.newCall(reqBuilder.build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "onFailure: " + e.getMessage(), e);
                    if (!call.isCanceled()) {
                        callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String respBody = response.body() != null ? response.body().string() : "";
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

    @Override
    public boolean supportsToolUse() {
        return true;
    }

    @Override
    public boolean supportsStreaming() {
        return false;
    }

    @Override
    public String providerId() {
        return "opencode_zen";
    }
}
