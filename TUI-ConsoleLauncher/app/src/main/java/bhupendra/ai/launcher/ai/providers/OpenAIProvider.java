package bhupendra.ai.launcher.ai.providers;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.*;
import bhupendra.ai.launcher.ai.*;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

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
            body.put("stream", true);

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
                    Log.d(TAG, "onResponse: code=" + response.code());
                    if (!response.isSuccessful()) {
                        String respBody = response.body() != null ? response.body().string() : "";
                        callback.onResponse(AIResponse.error(requestId, "HTTP " + response.code() + ": " + respBody));
                        response.close();
                        return;
                    }

                    bhupendra.ai.launcher.tuils.LauncherExecutors.aiExecutor.execute(() -> {
                        try (ResponseBody body = response.body()) {
                            if (body == null) {
                                callback.onResponse(AIResponse.error(requestId, "Empty response body"));
                                return;
                            }

                            okio.BufferedSource source = body.source();
                            StringBuilder accumulatedContent = new StringBuilder();
                            Map<Integer, StreamedToolCall> toolCallMap = new HashMap<>();

                            while (!source.exhausted()) {
                                String line = source.readUtf8Line();
                                if (line == null) break;
                                line = line.trim();
                                if (line.isEmpty()) continue;

                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6).trim();
                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }

                                    try {
                                        JSONObject chunk = new JSONObject(data);
                                        JSONArray choices = chunk.optJSONArray("choices");
                                        if (choices != null && choices.length() > 0) {
                                            JSONObject choice = choices.getJSONObject(0);
                                            JSONObject delta = choice.optJSONObject("delta");
                                            if (delta != null) {
                                                if (delta.has("reasoning_content") && !delta.isNull("reasoning_content")) {
                                                    String thinkingToken = delta.getString("reasoning_content");
                                                    callback.onThinkingToken(requestId, thinkingToken);
                                                }
                                                if (delta.has("content") && !delta.isNull("content")) {
                                                    String token = delta.getString("content");
                                                    accumulatedContent.append(token);
                                                    callback.onToken(requestId, token);
                                                }

                                                JSONArray toolCalls = delta.optJSONArray("tool_calls");
                                                if (toolCalls != null) {
                                                    for (int i = 0; i < toolCalls.length(); i++) {
                                                        JSONObject tc = toolCalls.getJSONObject(i);
                                                        int index = tc.optInt("index", 0);
                                                        StreamedToolCall stc = toolCallMap.get(index);
                                                        if (stc == null) {
                                                            stc = new StreamedToolCall();
                                                            stc.index = index;
                                                            toolCallMap.put(index, stc);
                                                        }
                                                        if (tc.has("id")) {
                                                            stc.id = tc.getString("id");
                                                        }
                                                        JSONObject function = tc.optJSONObject("function");
                                                        if (function != null) {
                                                            if (function.has("name")) {
                                                                stc.name = function.getString("name");
                                                            }
                                                            if (function.has("arguments")) {
                                                                stc.arguments.append(function.getString("arguments"));
                                                            }
                                                        }
                                                        JSONObject extra = tc.optJSONObject("extra_content");
                                                        if (extra != null) {
                                                            JSONObject google = extra.optJSONObject("google");
                                                            if (google != null && google.has("thought_signature")) {
                                                                stc.thoughtSignature = google.getString("thought_signature");
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        Log.w(TAG, "Failed to parse SSE chunk: " + line, e);
                                    }
                                }
                            }

                            JSONObject finalJson = new JSONObject();
                            JSONArray choicesArray = new JSONArray();
                            JSONObject choiceObj = new JSONObject();
                            JSONObject messageObj = new JSONObject();

                            messageObj.put("role", "assistant");
                            messageObj.put("content", accumulatedContent.toString());

                            if (!toolCallMap.isEmpty()) {
                                JSONArray finalToolCalls = new JSONArray();
                                List<Integer> keys = new ArrayList<>(toolCallMap.keySet());
                                Collections.sort(keys);
                                for (int key : keys) {
                                    StreamedToolCall stc = toolCallMap.get(key);
                                    JSONObject callObj = new JSONObject();
                                    callObj.put("id", stc.id);
                                    callObj.put("type", "function");

                                    JSONObject function = new JSONObject();
                                    function.put("name", stc.name);
                                    function.put("arguments", stc.arguments.toString());
                                    callObj.put("function", function);

                                    if (stc.thoughtSignature != null) {
                                        JSONObject extra = new JSONObject();
                                        JSONObject google = new JSONObject();
                                        google.put("thought_signature", stc.thoughtSignature);
                                        extra.put("google", google);
                                        callObj.put("extra_content", extra);
                                    }

                                    finalToolCalls.put(callObj);
                                }
                                messageObj.put("tool_calls", finalToolCalls);
                            }

                            choiceObj.put("message", messageObj);
                            choicesArray.put(choiceObj);
                            finalJson.put("choices", choicesArray);

                            AIResponse aiResponse = OpenAIToolSupport.parseResponse(requestId, finalJson);
                            callback.onResponse(aiResponse);

                        } catch (Exception e) {
                            Log.e(TAG, "Error during streaming: " + e.getMessage(), e);
                            callback.onResponse(AIResponse.error(requestId, e.getMessage()));
                        }
                    });
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "complete error: " + e.getMessage(), e);
            callback.onResponse(AIResponse.error(requestId, e.getMessage()));
        }
    }

    @Override public boolean supportsToolUse() { return true; }
    @Override public boolean supportsStreaming() { return true; }
    @Override public String providerId() { return "openai"; }

    private static class StreamedToolCall {
        int index;
        String id = "";
        String name = "";
        final StringBuilder arguments = new StringBuilder();
        String thoughtSignature = null;
    }
}