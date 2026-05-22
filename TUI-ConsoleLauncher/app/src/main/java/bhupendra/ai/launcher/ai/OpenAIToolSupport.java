package bhupendra.ai.launcher.ai;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class OpenAIToolSupport {

    private static final String NAME_PREFIX = "tool_";
    private static final Pattern FENCED_TOOL_CALL = Pattern.compile("^```(tool_[A-Za-z0-9_]+)\\((.*)\\)```$", Pattern.DOTALL);
    private static final Pattern RAW_TOOL_CALL = Pattern.compile("^(tool_[A-Za-z0-9_]+)\\((.*)\\)$", Pattern.DOTALL);

    private OpenAIToolSupport() {}

    public static String encodeToolName(String toolName) {
        StringBuilder encoded = new StringBuilder(NAME_PREFIX);
        for (int i = 0; i < toolName.length(); i++) {
            char c = toolName.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                encoded.append(c);
            } else {
                encoded.append('_');
                encoded.append(Integer.toHexString(c));
                encoded.append('_');
            }
        }
        return encoded.toString();
    }

    public static String decodeToolName(String functionName) {
        if (functionName == null || !functionName.startsWith(NAME_PREFIX)) return functionName;
        String encoded = functionName.substring(NAME_PREFIX.length());
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c != '_') {
                decoded.append(c);
                continue;
            }

            int end = encoded.indexOf('_', i + 1);
            if (end <= i) {
                decoded.append('_');
                continue;
            }

            try {
                decoded.append((char) Integer.parseInt(encoded.substring(i + 1, end), 16));
                i = end;
            } catch (NumberFormatException e) {
                decoded.append('_');
            }
        }
        return decoded.toString();
    }

    public static JSONArray buildToolsArray(List<Tool> tools) throws Exception {
        JSONArray result = new JSONArray();
        for (Tool tool : tools) {
            JSONObject fn = new JSONObject();
            fn.put("name", encodeToolName(tool.name));
            fn.put("description", tool.description);

            JSONObject parameters = new JSONObject();
            parameters.put("type", "object");

            JSONObject properties = new JSONObject();
            for (Map.Entry<String, String> entry : tool.parameters.entrySet()) {
                JSONObject property = new JSONObject();
                property.put("type", "string");
                property.put("description", entry.getValue());
                properties.put(entry.getKey(), property);
            }
            parameters.put("properties", properties);
            parameters.put("additionalProperties", false);
            fn.put("parameters", parameters);

            JSONObject wrapper = new JSONObject();
            wrapper.put("type", "function");
            wrapper.put("function", fn);
            result.put(wrapper);
        }
        return result;
    }

    public static AIResponse parseResponse(String requestId, JSONObject json) throws Exception {
        JSONObject message = json.getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message");

        JSONArray toolCalls = message.optJSONArray("tool_calls");
        if (toolCalls != null && toolCalls.length() > 0) {
            List<ToolCall> calls = new ArrayList<>();
            for (int i = 0; i < toolCalls.length(); i++) {
                JSONObject call = toolCalls.getJSONObject(i);
                JSONObject function = call.getJSONObject("function");
                
                String thoughtSignature = null;
                JSONObject extra = call.optJSONObject("extra_content");
                if (extra != null) {
                    JSONObject google = extra.optJSONObject("google");
                    if (google != null) {
                        thoughtSignature = google.optString("thought_signature");
                    }
                }
                
                calls.add(new ToolCall(
                    call.optString("id", "call_" + i),
                    decodeToolName(function.getString("name")),
                    function.optString("arguments", "{}"),
                    thoughtSignature));
            }
            return AIResponse.toolCalls(requestId, calls);
        }

        String content = message.optString("content", "");
        if (content == null) content = "";
        content = content.trim();

        ToolCall call = parseFencedToolCall(content);
        if (call == null) {
            call = parseRawToolCall(content);
        }

        if (call != null) {
            List<ToolCall> calls = new ArrayList<>();
            calls.add(call);
            return AIResponse.toolCalls(requestId, calls);
        }

        return AIResponse.text(requestId, content);
    }

    private static ToolCall parseFencedToolCall(String content) {
        if (content == null || content.isEmpty()) return null;
        Matcher matcher = FENCED_TOOL_CALL.matcher(content);
        if (!matcher.matches()) return null;

        String arguments = matcher.group(2) != null ? matcher.group(2).trim() : "";
        return new ToolCall(
            "call_fenced_0",
            decodeToolName(matcher.group(1)),
            arguments.isEmpty() ? "{}" : arguments);
    }

    private static ToolCall parseRawToolCall(String content) {
        if (content == null || content.isEmpty()) return null;
        Matcher matcher = RAW_TOOL_CALL.matcher(content);
        if (!matcher.matches()) return null;

        String arguments = matcher.group(2) != null ? matcher.group(2).trim() : "";
        return new ToolCall(
            "call_raw_0",
            decodeToolName(matcher.group(1)),
            arguments.isEmpty() ? "{}" : arguments);
    }
}