package bhupendra.ai.launcher.terminal;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TerminalEventBus {
    private static final TerminalEventBus INSTANCE = new TerminalEventBus();

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();

    private TerminalEventBus() {}

    public static TerminalEventBus get() {
        return INSTANCE;
    }

    public void register(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregister(Listener listener) {
        listeners.remove(listener);
    }

    public boolean post(TerminalEvent event) {
        if (listeners.isEmpty()) return false;
        for (Listener listener : listeners) {
            listener.onTerminalEvent(event);
        }
        return true;
    }

    public interface Listener {
        void onTerminalEvent(TerminalEvent event);
    }

    public abstract static class TerminalEvent {
        public final String requestId;

        TerminalEvent(String requestId) {
            this.requestId = requestId;
        }
    }

    public static final class InputEvent extends TerminalEvent {
        public final String text;

        public InputEvent(String text, String requestId) {
            super(requestId);
            this.text = text;
        }
    }

    public static final class OutputEvent extends TerminalEvent {
        public final CharSequence text;
        public final int color;
        public final int type;
        public final Object action;
        public final Object longAction;

        public OutputEvent(CharSequence text, int color, int type, Object action, Object longAction, String requestId) {
            super(requestId);
            this.text = text;
            this.color = color;
            this.type = type;
            this.action = action;
            this.longAction = longAction;
        }
    }
}
