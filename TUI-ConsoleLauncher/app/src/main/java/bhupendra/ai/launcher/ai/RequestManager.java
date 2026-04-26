package bhupendra.ai.launcher.ai;

import java.util.concurrent.atomic.AtomicReference;

public class RequestManager {

    private volatile AIProvider provider;
    private final AtomicReference<String> activeId = new AtomicReference<>();
    private final AtomicReference<AIRequestState> state = new AtomicReference<>(AIRequestState.IDLE);
    private volatile AICallback activeCallback;
    private volatile boolean cancelRequested;

    public RequestManager(AIProvider provider, long connectTimeoutMs, long inactivityTimeoutMs) {
        this.provider = provider;
    }

    public void setProvider(AIProvider provider) {
        this.provider = provider;
    }

    public void submit(AIRequest request, AICallback callback) {
        activeId.set(request.requestId);
        activeCallback = callback;
        cancelRequested = false;
        transition(request.requestId, AIRequestState.THINKING, callback);

        provider.complete(request, request.requestId, new AICallback() {
            @Override
            public void onToken(String rid, String token) {
                if (!rid.equals(activeId.get()) || cancelRequested) return;
                transition(rid, AIRequestState.STREAMING, activeCallback);
                activeCallback.onToken(rid, token);
            }

            @Override
            public void onResponse(AIResponse response) {
                if (!response.requestId.equals(activeId.get()) || cancelRequested) return;
                
                if (response.type == AIResponse.Type.TOOL_CALLS) {
                    transition(response.requestId, AIRequestState.EXECUTING_TOOLS, activeCallback);
                }
                
                activeCallback.onResponse(response);
                
                if (response.type == AIResponse.Type.ERROR) {
                    transition(response.requestId, AIRequestState.FAILED, activeCallback);
                } else if (response.type == AIResponse.Type.TEXT) {
                    transition(response.requestId, AIRequestState.COMPLETED, activeCallback);
                }
            }

            @Override
            public void onStateChange(String rid, AIRequestState s) {
                if (!rid.equals(activeId.get()) || cancelRequested) return;
                transition(rid, s, activeCallback);
            }
        });
    }

    public void cancel(String requestId) {
        if (requestId.equals(activeId.get())) {
            cancelRequested = true;
            transition(requestId, AIRequestState.CANCELLED, activeCallback);
        }
    }

    public boolean isCancelRequested() {
        return cancelRequested;
    }

    public boolean isInFlight() {
        AIRequestState s = state.get();
        return s == AIRequestState.THINKING || s == AIRequestState.STREAMING
            || s == AIRequestState.EXECUTING_TOOLS || s == AIRequestState.FOLLOWUP;
    }

    public AIRequestState getState() { return state.get(); }

    public void finishToolExecution(String requestId, boolean success) {
        if (!requestId.equals(activeId.get()) || cancelRequested) return;
        transition(requestId, success ? AIRequestState.COMPLETED : AIRequestState.FAILED, activeCallback);
    }

    public void transition(String rid, AIRequestState newState, AICallback cb) {
        state.set(newState);
        if (cb != null) cb.onStateChange(rid, newState);
        if (isTerminalState(newState)) {
            activeId.compareAndSet(rid, null);
            activeCallback = null;
            cancelRequested = false;
        }
    }

    private boolean isTerminalState(AIRequestState state) {
        return state == AIRequestState.COMPLETED
            || state == AIRequestState.CANCELLED
            || state == AIRequestState.TIMED_OUT_CONNECT
            || state == AIRequestState.TIMED_OUT_INACTIVITY
            || state == AIRequestState.FAILED;
    }
}
