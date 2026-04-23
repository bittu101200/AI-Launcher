package bhupendra.ai.launcher.commands.main.raw;

import android.graphics.Color;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AICallback;
import bhupendra.ai.launcher.ai.AIResponse;
import bhupendra.ai.launcher.ai.AIRequestState;
import bhupendra.ai.launcher.tuils.Tuils;
import android.util.Log;

import bhupendra.ai.launcher.managers.TerminalManager;
import org.json.JSONObject;

public class ai implements CommandAbstraction {

    private static final String TAG = "AI_CMD";

    @Override
    public String exec(ExecutePack pack) throws Exception {
        String query = pack.getString();
        Log.d(TAG, "exec: query=" + query);
        if (query == null || query.trim().isEmpty()) {
            return pack.context.getString(R.string.help_ai);
        }
        MainPack mp = (MainPack) pack;
        AISubsystem aiSubsystem = mp.aiSubsystem;
        Log.d(TAG, "exec: aiSubsystem=" + aiSubsystem + " available=" + (aiSubsystem != null && aiSubsystem.isAvailable()));
        if (aiSubsystem == null || !aiSubsystem.isAvailable()) {
            return "[AI subsystem not available — check ai.xml]";
        }

        Tuils.sendOutput(Color.GRAY, pack.context, "[thinking...]", TerminalManager.CATEGORY_OUTPUT);

        aiSubsystem.submit(query.trim(), new AICallback() {
            private StringBuilder tokenBuffer = new StringBuilder();
            private final java.util.regex.Pattern TOOL_CALL_PATTERN = java.util.regex.Pattern.compile("^tool_[A-Za-z0-9_]+\\(.*\\)$", java.util.regex.Pattern.DOTALL);

            @Override public void onToken(String rid, String token) {
                // HACK: Don't stream tokens because it breaks markdown parsing midway.
                // We'll wait for the full response in onResponse.
                // If we want streaming, TerminalManager needs complex partial markdown logic.
                tokenBuffer.append(token);
            }

            @Override public void onResponse(AIResponse r) {
                Log.d(TAG, "onResponse: type=" + r.type + " text=" + (r.text != null ? r.text.substring(0, Math.min(20, r.text.length())) : "null") + " err=" + r.errorMessage + " isTool=" + r.isToolOutput);
                if (r.type == AIResponse.Type.TEXT && r.text != null) {
                    if (r.isToolOutput && r.toolCall != null) {
                        // Minimal notification for web tools
                        String notify = null;
                        try {
                            JSONObject args = new JSONObject(r.toolCall.argumentsJson);
                            if ("system.web_search_query".equals(r.toolCall.toolName)) {
                                notify = "[searching: " + args.optString("query", "...") + "]";
                            } else if ("system.web_fetch".equals(r.toolCall.toolName)) {
                                String url = args.optString("url", "...");
                                if (url.length() > 40) url = url.substring(0, 37) + "...";
                                notify = "[fetching: " + url + "]";
                            } else {
                                notify = "[executed: " + r.toolCall.toolName + "]";
                            }
                        } catch (Exception e) {
                            notify = "[executed: " + r.toolCall.toolName + "]";
                        }
                        
                        if (notify != null) {
                            Tuils.sendOutput(Color.GRAY, pack.context, notify, TerminalManager.CATEGORY_OUTPUT);
                        }
                        return;
                    }

                    String currentText = r.text.trim();
                    if (TOOL_CALL_PATTERN.matcher(currentText).matches()) {
                        return;
                    }
                    // Send FULL text here so it can be parsed as markdown correctly
                    Tuils.sendOutput(Color.WHITE, pack.context, r.text, TerminalManager.CATEGORY_AI);
                } else if (r.type == AIResponse.Type.ERROR) {
                    Tuils.sendOutput(Color.RED, pack.context, "[AI error: " + r.errorMessage + "]", TerminalManager.CATEGORY_ERROR);
                }
            }

            @Override public void onStateChange(String rid, AIRequestState s) {
                if (s == AIRequestState.EXECUTING_TOOLS) {
                    Tuils.sendOutput(Color.GRAY, pack.context, "[executing tools...]", TerminalManager.CATEGORY_OUTPUT);
                }
            }
        });
        return null;
    }

    @Override public int[] argType() { return new int[]{CommandAbstraction.PLAIN_TEXT}; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_ai; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return pack.context.getString(R.string.help_ai); }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return pack.context.getString(R.string.help_ai); }
}
