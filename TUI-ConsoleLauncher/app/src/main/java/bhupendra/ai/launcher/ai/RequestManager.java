package bhupendra.ai.launcher.ai;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicReference;

public class RequestManager {

    private volatile AIProvider provider;
    private final AtomicReference<String> activeId = new AtomicReference<>();
    private final AtomicReference<AIRequestState> state = new AtomicReference<>(AIRequestState.IDLE);
    private volatile AICallback activeCallback;
    private volatile boolean cancelRequested;

    private Handler handler;
    private volatile long connectTimeoutMs;
    private volatile long inactivityTimeoutMs;
    private Runnable timeoutRunnable;

    public void setTimeouts(long connectTimeoutMs, long inactivityTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.inactivityTimeoutMs = inactivityTimeoutMs;
    }

    public RequestManager(AIProvider provider, long connectTimeoutMs, long inactivityTimeoutMs) {
        this.provider = provider;
        this.connectTimeoutMs = connectTimeoutMs;
        this.inactivityTimeoutMs = inactivityTimeoutMs;
        try {
            this.handler = new Handler(Looper.getMainLooper());
        } catch (RuntimeException e) {
            this.handler = null;
        }
    }

    public void setProvider(AIProvider provider) {
        this.provider = provider;
    }

    private void resetTimer(final String requestId, long timeout, final AIRequestState timeoutState) {
        cancelTimer();
        if (handler == null) return;
        timeoutRunnable = () -> {
            if (requestId.equals(activeId.get())) {
                cancelRequested = true;
                transition(requestId, timeoutState, activeCallback);
            }
        };
        handler.postDelayed(timeoutRunnable, timeout);
    }

    private void cancelTimer() {
        if (timeoutRunnable != null && handler != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    public void submit(AIRequest request, AICallback callback) {
        activeId.set(request.requestId);
        activeCallback = callback;
        cancelRequested = false;
        transition(request.requestId, AIRequestState.THINKING, callback);
        resetTimer(request.requestId, connectTimeoutMs, AIRequestState.TIMED_OUT_CONNECT);

        provider.complete(request, request.requestId, new AICallback() {
            @Override
            public void onToken(String rid, String token) {
                if (!rid.equals(activeId.get()) || cancelRequested) return;
                cancelTimer();
                transition(rid, AIRequestState.STREAMING, activeCallback);
                resetTimer(rid, inactivityTimeoutMs, AIRequestState.TIMED_OUT_INACTIVITY);
                activeCallback.onToken(rid, token);
            }

            @Override
            public void onResponse(AIResponse response) {
                if (!response.requestId.equals(activeId.get()) || cancelRequested) return;
                
                // Any response from the provider means we've connected.
                cancelTimer();
                resetTimer(response.requestId, inactivityTimeoutMs, AIRequestState.TIMED_OUT_INACTIVITY);
                
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

    public boolean isActive(String requestId) {
        return requestId != null && requestId.equals(activeId.get()) && !cancelRequested && isInFlight();
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
            cancelTimer();
            activeId.compareAndSet(rid, null);
            activeCallback = null;
            cancelRequested = false;
        } else if (newState == AIRequestState.THINKING) {
            resetTimer(rid, connectTimeoutMs, AIRequestState.TIMED_OUT_CONNECT);
        } else if (newState == AIRequestState.FOLLOWUP) {
            // Once we are in followup, we are connected. Use inactivity timeout.
            resetTimer(rid, inactivityTimeoutMs, AIRequestState.TIMED_OUT_INACTIVITY);
        } else if (newState == AIRequestState.EXECUTING_TOOLS) {
            // Tools might take a while, give them a longer inactivity timeout or don't timeout.
            // For now, let's keep the inactivity timeout or disable it during tool execution.
            cancelTimer();
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
