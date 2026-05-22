package bhupendra.ai.launcher;

public interface LabelUpdater {
    void updateText(UIManager.Label l, CharSequence s);
    int getLabelSize(UIManager.Label l);
}
