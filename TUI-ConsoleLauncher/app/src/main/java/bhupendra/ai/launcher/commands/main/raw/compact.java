package bhupendra.ai.launcher.commands.main.raw;

import android.graphics.Color;

import java.util.List;

import bhupendra.ai.launcher.R;
import bhupendra.ai.launcher.commands.CommandAbstraction;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.AICallback;
import bhupendra.ai.launcher.ai.AIResponse;
import bhupendra.ai.launcher.ai.AIRequestState;
import bhupendra.ai.launcher.ai.CompactionEngine;
import bhupendra.ai.launcher.ai.ConversationTurn;
import bhupendra.ai.launcher.tuils.Tuils;

public class compact implements CommandAbstraction {

    @Override
    public String exec(ExecutePack pack) throws Exception {
        MainPack mp = (MainPack) pack;
        AISubsystem ai = mp.getAiSubsystem();
        if (ai == null || !ai.isAvailable()) return "[AI subsystem not available]";

        List<ConversationTurn> history = ai.getConversationManager().getHistory();
        if (history.isEmpty()) return "[No conversation history to compact]";

        Tuils.sendOutput(Color.GRAY, pack.getContext(), "[compacting conversation history...]");
        CompactionEngine engine = new CompactionEngine();
        String prompt = engine.buildCompactionPrompt(history);

        ai.submit(prompt, new AICallback() {
            @Override public void onToken(String rid, String t) {}
            @Override public void onResponse(AIResponse r) {
                if (r.type == AIResponse.Type.TEXT && r.text != null) {
                    ConversationTurn summary = new ConversationTurn(
                        ConversationTurn.Role.ASSISTANT, "[Compacted] " + r.text);
                    engine.applyCompaction(ai.getConversationManager(), summary);
                    Tuils.sendOutput(Color.GREEN, pack.getContext(),
                        "[compacted — " + ai.getConversationManager().getHistory().size() + " turn(s) remain]");
                } else {
                    Tuils.sendOutput(Color.RED, pack.getContext(), "[compaction failed]");
                }
            }
            @Override public void onStateChange(String rid, AIRequestState s) {}
        });
        return null;
    }

    @Override public int[] argType() { return new int[0]; }
    @Override public int priority() { return 0; }
    @Override public int helpRes() { return R.string.help_compact; }
    @Override public String onArgNotFound(ExecutePack pack, int indexNotFound) { return null; }
    @Override public String onNotArgEnough(ExecutePack pack, int nArgs) { return null; }
}