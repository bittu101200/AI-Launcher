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
                Log.d(TAG, "onToken: " + token);
                tokenBuffer.append(token);
                // If it looks like it's starting a tool call, buffer it
                if (tokenBuffer.toString().trim().startsWith("tool_")) {
                    return;
                }
                Tuils.sendOutput(Color.WHITE, pack.context, token, TerminalManager.CATEGORY_AI);
            }

            @Override public void onResponse(AIResponse r) {
                Log.d(TAG, "onResponse: type=" + r.type + " text=" + r.text + " err=" + r.errorMessage);
                if (r.type == AIResponse.Type.TEXT && r.text != null) {
                    String currentText = r.text.trim();
                    // If it looks exactly like a raw tool call that escaped parsing, hide it
                    if (TOOL_CALL_PATTERN.matcher(currentText).matches()) {
                        Log.d(TAG, "onResponse: Hiding escaped tool call: " + currentText);
                        return;
                    }
                    Tuils.sendOutput(Color.WHITE, pack.context, r.text, TerminalManager.CATEGORY_AI);
                } else if (r.type == AIResponse.Type.ERROR) {
                    Tuils.sendOutput(Color.RED, pack.context, "[AI error: " + r.errorMessage + "]", TerminalManager.CATEGORY_ERROR);
                }
            }

            @Override public void onStateChange(String rid, AIRequestState s) {
                Log.d(TAG, "onStateChange: " + s);
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