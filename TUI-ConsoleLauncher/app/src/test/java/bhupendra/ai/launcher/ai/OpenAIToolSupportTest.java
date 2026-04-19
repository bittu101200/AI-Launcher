package bhupendra.ai.launcher.ai;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class OpenAIToolSupportTest {

    @Test
    public void buildToolsArray_encodesUnsafeToolNames() throws Exception {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("query", "Search text");

        Tool tool = new Tool("launch:com.apple.android.music", "Apple Music", params, ToolRiskClass.LAUNCH_ONLY);

        JSONArray tools = OpenAIToolSupport.buildToolsArray(Collections.singletonList(tool));
        JSONObject function = tools.getJSONObject(0).getJSONObject("function");

        assertEquals("function", tools.getJSONObject(0).getString("type"));
        assertNotEquals(tool.name, function.getString("name"));
        assertEquals(tool.name, OpenAIToolSupport.decodeToolName(function.getString("name")));
        assertEquals("object", function.getJSONObject("parameters").getString("type"));
        assertTrue(function.getJSONObject("parameters").getJSONObject("properties").has("query"));
    }

    @Test
    public void parseResponse_returnsToolCalls() throws Exception {
        String encodedName = OpenAIToolSupport.encodeToolName("launch:com.apple.android.music");
        JSONObject json = new JSONObject()
            .put("choices", new JSONArray().put(new JSONObject()
                .put("message", new JSONObject()
                    .put("tool_calls", new JSONArray().put(new JSONObject()
                        .put("id", "call_1")
                        .put("type", "function")
                        .put("function", new JSONObject()
                            .put("name", encodedName)
                            .put("arguments", "{}")))))));

        AIResponse response = OpenAIToolSupport.parseResponse("r1", json);

        assertEquals(AIResponse.Type.TOOL_CALLS, response.type);
        assertEquals(1, response.toolCalls.size());
        assertEquals("launch:com.apple.android.music", response.toolCalls.get(0).toolName);
        assertEquals("{}", response.toolCalls.get(0).argumentsJson);
    }

    @Test
    public void parseResponse_extractsFencedToolCallAsToolCall() throws Exception {
        JSONObject json = new JSONObject("{\"choices\":[{\"message\":{\"content\":\"```tool_launch_3a_com_2e_apple_2e_android_2e_music()```\"}}]}");

        AIResponse response = OpenAIToolSupport.parseResponse("r2", json);

        assertEquals(AIResponse.Type.TOOL_CALLS, response.type);
        assertEquals(1, response.toolCalls.size());
        assertEquals("launch:com.apple.android.music", response.toolCalls.get(0).toolName);
        assertEquals("{}", response.toolCalls.get(0).argumentsJson);
    }
}
