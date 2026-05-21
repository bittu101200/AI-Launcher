package bhupendra.ai.launcher.managers;

import bhupendra.ai.launcher.managers.TextProcessor;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.IBinder;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.LongClickMovementMethod;
import bhupendra.ai.launcher.tuils.LongClickableSpan;
import bhupendra.ai.launcher.tuils.PrivateIOReceiver;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.CommandExecuter;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

public class TerminalManager {

    private final int SCROLL_DELAY = 200;
    private final int CMD_LIST_SIZE = 40;

    public static final int CATEGORY_INPUT = 10, CATEGORY_OUTPUT = 11, CATEGORY_AI = 12, CATEGORY_ERROR = 13, CATEGORY_NO_COLOR = 20;

    public static int NO_COLOR = Integer.MAX_VALUE;

    private long lastEnter;

    private String prefix;
    private String suPrefix;

    private ScrollView mScrollView;
    private TextView mTerminalView;
    private EditText mInputView;

    private TextView mPrefix;
    private boolean suMode;

    private List<String> cmdList = new ArrayList<>(CMD_LIST_SIZE);
    private int howBack = -1;

    private Runnable mScrollRunnable = new Runnable() {
        @Override
        public void run() {
            mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            mInputView.requestFocus();
        }
    };

    private MainPack mainPack;

    private boolean defaultHint = true;
    private Runnable suggestionsRefreshCallback;

    private int clearCmdsCount= 0;

    private int clearAfterCmds, clearAfterMs, maxLines;
    private Runnable clearRunnable = new Runnable() {

        @Override
        public void run() {
            clear();
            mTerminalView.postDelayed(this, clearAfterMs);
        }
    };

    private String inputFormat, outputFormat;
    private int inputColor, outputColor;

    private boolean clickCommands, longClickCommands;

    public Context mContext;

    private CommandExecuter executer;
    private ImageButton deleteView;
    private int toolbarColor;
    private boolean aiRunning = false;

    // Streaming fields
    private int streamStartOffset = -1;
    private final Object streamLock = new Object();
    private CharSequence pendingStreamContent = "";
    private boolean streamUpdatePending = false;
    private final Handler streamHandler = new Handler(Looper.getMainLooper());
    private static final long STREAM_THROTTLE_MS = 40; // ~25 FPS
    private final Runnable streamUpdater = new Runnable() {
        @Override
        public void run() {
            CharSequence content;
            synchronized (streamLock) {
                content = pendingStreamContent;
                streamUpdatePending = false;
            }
            if (streamStartOffset != -1 && mTerminalView != null) {
                CharSequence currentText = mTerminalView.getText();
                if (currentText instanceof Editable) {
                    Editable editable = (Editable) currentText;
                    if (streamStartOffset <= editable.length()) {
                        editable.replace(streamStartOffset, editable.length(), content);
                        scrollToEnd();
                    }
                }
            }
        }
    };

    public Context getContext() {
        return mContext;
    }

    public TerminalManager(final TextView terminalView, EditText inputView, TextView prefixView, ImageView submitView, final ImageView backView, ImageButton nextView, ImageButton deleteView,
                           ImageButton pasteView, final Context context, MainPack mainPack, CommandExecuter executer) {
        if (terminalView == null || inputView == null || prefixView == null)
            throw new UnsupportedOperationException();

        this.mContext = context;
        this.executer = executer;

        this.mainPack = mainPack;

        this.clickCommands = XMLPrefsManager.getBoolean(Behavior.click_commands);
        this.longClickCommands = XMLPrefsManager.getBoolean(Behavior.long_click_commands);

        this.clearAfterMs = XMLPrefsManager.getInt(Behavior.clear_after_seconds) * 1000;
        this.clearAfterCmds = XMLPrefsManager.getInt(Behavior.clear_after_cmds);
        this.maxLines = XMLPrefsManager.getInt(Behavior.max_lines);

        inputFormat = XMLPrefsManager.get(Behavior.input_format);
        outputFormat = XMLPrefsManager.get(Behavior.output_format);

        inputColor = XMLPrefsManager.getColor(Theme.input_color);
        outputColor = XMLPrefsManager.getColor(Theme.output_color);

        prefix = XMLPrefsManager.get(Ui.input_prefix);
        suPrefix = XMLPrefsManager.get(Ui.input_root_prefix);

        int ioSize = XMLPrefsManager.getInt(Ui.input_output_size);

        prefixView.setTypeface(Tuils.getTypeface(context));
        prefixView.setTextColor(XMLPrefsManager.getColor(Theme.input_color));
        prefixView.setTextSize(ioSize);
        prefixView.setText(prefix.endsWith(Tuils.SPACE) ? prefix : prefix + Tuils.SPACE);
        this.mPrefix = prefixView;

        this.toolbarColor = XMLPrefsManager.getColor(Theme.toolbar_color);
        int enterColor = XMLPrefsManager.getColor(Theme.enter_color);

        if (submitView != null) {
            submitView.setColorFilter(enterColor, android.graphics.PorterDuff.Mode.SRC_IN);
            submitView.setOnClickListener(v -> onNewInput());
        }

        if (backView != null) {
            backView.setColorFilter(toolbarColor, android.graphics.PorterDuff.Mode.SRC_IN);
            backView.setBackgroundColor(0);
            backView.setOnClickListener(v -> onBackPressed());
        }

        if (nextView != null) {
            nextView.setColorFilter(toolbarColor, android.graphics.PorterDuff.Mode.SRC_IN);
            nextView.setBackgroundColor(0);
            nextView.setOnClickListener(v -> onNextPressed());
        }

        if (pasteView != null) {
            pasteView.setColorFilter(toolbarColor, android.graphics.PorterDuff.Mode.SRC_IN);
            pasteView.setBackgroundColor(0);
            pasteView.setOnClickListener(v -> {
                String text = Tuils.getTextFromClipboard(context);
                if(text != null && text.length() > 0) {
                    setInput(getInput() + text);
                }
            });
        }

        this.deleteView = deleteView;
        if (this.deleteView != null) {
            this.deleteView.setColorFilter(toolbarColor, android.graphics.PorterDuff.Mode.SRC_IN);
            this.deleteView.setBackgroundColor(0);
            this.deleteView.setOnClickListener(v -> {
                if (aiRunning) {
                    bhupendra.ai.launcher.ai.AISubsystem ai = bhupendra.ai.launcher.ai.AISubsystem.getInstance();
                    if (ai != null) {
                        ai.cancel();
                    }
                } else {
                    setInput(Tuils.EMPTYSTRING);
                }
            });
        }

        this.mTerminalView = terminalView;
        this.mTerminalView.setText("", TextView.BufferType.EDITABLE);
        this.mTerminalView.setTypeface(Tuils.getTypeface(context));
        this.mTerminalView.setTextSize(ioSize);
        this.mTerminalView.setFocusable(false);
        this.mTerminalView.setClickable(true);
        this.mTerminalView.setTextIsSelectable(false);
        this.mTerminalView.setMovementMethod(LongClickMovementMethod.getInstance(XMLPrefsManager.getInt(Behavior.long_click_duration)));

        int hintColor = XMLPrefsManager.getColor(Theme.session_info_color);

        ColorStateList list = mTerminalView.getHintTextColors();
        try {
            Field colors = list.getClass().getDeclaredField("mColors");
            Field dColor = list.getClass().getDeclaredField("mDefaultColor");

            colors.setAccessible(true);
            dColor.setAccessible(true);

            int[] a = (int[]) colors.get(list);
            for(int c = 0; c < a.length; c++) {
                a[c] = hintColor;
            }

            colors.set(list, a);
            dColor.set(list, hintColor);
        } catch (Exception e) {
            Tuils.log(e);
        }

        if(clearAfterMs > 0) this.mTerminalView.postDelayed(clearRunnable, clearAfterMs);
        if(maxLines > 0) {
            this.mTerminalView.getViewTreeObserver().addOnPreDrawListener(() -> {
                if(TerminalManager.this.mTerminalView == null) return true;

                Layout l = terminalView.getLayout();
                if(l == null) return true;

                int count = l.getLineCount() - 1;

                if(count > maxLines) {
                    int excessive = count - maxLines;

                    CharSequence text = terminalView.getText();
                    while (excessive >= 0) {
                        int index = TextUtils.indexOf(text, Tuils.NEWLINE);
                        if(index == -1) break;
                        text = text.subSequence(index + 1, text.length());
                        excessive--;
                    }

                    terminalView.setText(text, TextView.BufferType.EDITABLE);
                }

                return true;
            });
        }

        View v = mTerminalView;
        do {
            v = (View) v.getParent();
        } while (!(v instanceof ScrollView));
        this.mScrollView = (ScrollView) v;

        this.mInputView = inputView;
        this.mInputView.setFocusable(true);
        this.mInputView.setFocusableInTouchMode(true);
        this.mInputView.setTextSize(ioSize);
        this.mInputView.setTextColor(XMLPrefsManager.getColor(Theme.input_color));
        this.mInputView.setTypeface(Tuils.getTypeface(context));
        this.mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        this.mInputView.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        this.mInputView.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        this.mInputView.setAutofillHints("");
        Tuils.setCursorDrawableColor(this.mInputView, XMLPrefsManager.getColor(Theme.cursor_color));
        this.mInputView.setHighlightColor(Color.TRANSPARENT);
        this.mInputView.setOnEditorActionListener((v1, actionId, event) -> {
            if(!mInputView.hasFocus()) mInputView.requestFocus();

//                physical enter
            if(actionId == KeyEvent.ACTION_DOWN) {
                if(lastEnter == 0) {
                    lastEnter = System.currentTimeMillis();
                } else {
                    long difference = System.currentTimeMillis() - lastEnter;
                    lastEnter = System.currentTimeMillis();
                    if(difference < 350) {
                        return true;
                    }
                }
            }

            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || actionId == KeyEvent.ACTION_DOWN) {
                onNewInput();
            }

//                if (event == null && actionId == EditorInfo.IME_NULL) onNewInput();
//                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) onNewInput();

            return true;
        });
//        if(autoLowerFirstChar) {
//            this.mInputView.setFilters(new InputFilter[] {new InputFilter() {
//                @Override
//                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
//                    if (dstart == 0 && dend == 0 && start == 0 && end == 1 && source.length() > 0) {
//                        return TextUtils.concat(source.toString().toLowerCase().charAt(0) + Tuils.EMPTYSTRING, source.subSequence(1,source.length()));
//                    }
//
//                    return source;
//                }
//            }});
//        }
    }

    private void setupNewInput() {
        mInputView.setText(Tuils.EMPTYSTRING);

        if(defaultHint) {
            mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        }

        requestInputFocus();

        if (suggestionsRefreshCallback != null) {
            mInputView.post(suggestionsRefreshCallback);
        }
    }

    private boolean onNewInput() {
        if (mInputView == null) {
            return false;
        }

        CharSequence input = mInputView.getText();

        String cmd = input.toString().trim();

        Object obj = null;
        try {
            obj = ((Spannable) input).getSpans(0, input.length(), AppsManager.LaunchInfo.class)[0];
        } catch (Exception e) {
//            an error will probably be thrown everytime, but we don't need to track it
        }

        if(input.length() > 0) {
            clearCmdsCount++;
            if(clearCmdsCount != 0 && clearAfterCmds > 0 && clearCmdsCount % clearAfterCmds == 0) clear();

            writeToView(input, CATEGORY_INPUT);

            if(cmdList.size() == CMD_LIST_SIZE) {
                cmdList.remove(0);
            }
            cmdList.add(cmdList.size(), cmd);
            howBack = -1;
        }

//        DO NOT USE THE INTENT APPROACH
//        apps are not launching properly, when one has been launched, an other attempt will show always the same
//
//        Intent intent = new Intent(MainManager.ACTION_EXEC);
//        intent.putExtra(MainManager.CMD, cmd);
//        intent.putExtra(MainManager.CMD_COUNT, MainManager.commandCount);
//
//        Parcelable p = null;
//        if(obj instanceof Parcelable) p = (Parcelable) obj;
//        intent.putExtra(MainManager.PARCELABLE, p);
//
//        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(intent);

        bhupendra.ai.launcher.ai.AISubsystem ai = bhupendra.ai.launcher.ai.AISubsystem.getInstance();
        if (mainPack.getRedirectator().getRedirect() != null) {
            executer.execute(cmd, obj);
        } else if (ai != null && ai.hasPendingUserInteraction()) {
            ai.resolvePendingUserInteraction(cmd);
        } else {
            executer.execute(cmd, obj);
        }

//        because it will clear suggestions without refilling them, because "aftertextchanged" wont be called
//        if(cmd.length() > 0) LocalBroadcastManager.getInstance(mContext.getApplicationContext()).sendBroadcast(new Intent(UIManager.ACTION_CLEAR_SUGGESTIONS));

        setupNewInput();

        return true;
    }

    public void setOutput(CharSequence output, int type) {
        if (shouldSuppressOutput(output)) return;
        writeToView(output, type);
    }

    public void setOutput(int color, CharSequence output) {
        if(shouldSuppressOutput(output)) return;

        // Use standard category output but override the color if specified
        CharSequence finalOut = getFinalText(output, CATEGORY_OUTPUT);
        if (shouldSuppressOutput(finalOut)) return;
        if (color != TerminalManager.NO_COLOR && finalOut instanceof Spannable) {
            ((Spannable) finalOut).setSpan(new ForegroundColorSpan(color), 0, finalOut.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        writeToView(TextUtils.concat(Tuils.NEWLINE, finalOut));
    }

    public String getTerminalText() {
        return mTerminalView.getText().toString();
    }

    public void onBackPressed() {
        if(cmdList.size() > 0) {

            if(howBack == -1) {
                howBack = cmdList.size();
            } else if(howBack == 0) {
                return;
            }
            howBack--;

            setInput(cmdList.get(howBack));
        }
    }

    public void onNextPressed() {
        if(howBack != -1 && howBack < cmdList.size()) {
            howBack++;

            String input;
            if(howBack == cmdList.size()) {
                input = Tuils.EMPTYSTRING;
            } else {
                input = cmdList.get(howBack);
            }

            setInput(input);
        }
    }

    public static final String FORMAT_INPUT = "%i";
    public static final String FORMAT_OUTPUT = "%o";
    public static final String FORMAT_PREFIX = "%p";
    public static final String FORMAT_NEWLINE = "%n";

    private void writeToView(CharSequence text, int type) {
        text = getFinalText(text, type);
        if (shouldSuppressOutput(text)) return;
        text = TextUtils.concat(Tuils.NEWLINE, text);
        writeToView(text);
    }

    private void writeToView(final CharSequence text) {
        mTerminalView.post(() -> {
            mTerminalView.append(text);
            scrollToEnd();
        });
    }

    private CharSequence getFinalText(CharSequence t, int type) {
        if (shouldSuppressOutput(t)) return Tuils.EMPTYSTRING;
        try {
            SpannableStringBuilder finalSsb = new SpannableStringBuilder();
            switch (type) {
                case CATEGORY_INPUT:
                    boolean su = t.toString().startsWith("su ") || suMode;
                    SpannableString si = TextProcessor.span(inputFormat, inputColor);
                    if(clickCommands || longClickCommands) si.setSpan(new LongClickableSpan(clickCommands ? t.toString() : null, longClickCommands ? t.toString() : null, PrivateIOReceiver.ACTION_INPUT), 0,
                            si.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    CharSequence processedInput = TimeManager.instance.replace(si);
                    processedInput = TextUtils.replace(processedInput,
                            new String[] {FORMAT_INPUT, FORMAT_PREFIX, FORMAT_NEWLINE, FORMAT_INPUT.toUpperCase(), FORMAT_PREFIX.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                            new CharSequence[] {t, su ? suPrefix : prefix, Tuils.NEWLINE, t, su ? suPrefix : prefix, Tuils.NEWLINE});
                    finalSsb.append(processedInput);
                    break;
                case CATEGORY_OUTPUT:
                    t = TextProcessor.parseMarkdown(t);
                    String outputTemplate = XMLPrefsManager.get(Behavior.output_format);
                    int outCol = XMLPrefsManager.getColor(Theme.output_color);
                    
                    if (outputTemplate != null) {
                        int oIndex = outputTemplate.indexOf(FORMAT_OUTPUT);
                        if (oIndex != -1) {
                            finalSsb.append(outputTemplate.substring(0, oIndex));
                            finalSsb.append(t);
                            finalSsb.append(outputTemplate.substring(oIndex + FORMAT_OUTPUT.length()));
                        } else {
                            finalSsb.append(t);
                        }
                    } else {
                        finalSsb.append(t);
                    }
                    finalSsb.setSpan(new ForegroundColorSpan(outCol), 0, finalSsb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case CATEGORY_AI:
                    t = TextProcessor.parseMarkdown(t);
                    String aiTemplate = XMLPrefsManager.get(Behavior.output_format);
                    int aiCol = XMLPrefsManager.getColor(Theme.output_color);

                    if (aiTemplate != null) {
                        int aiIndex = aiTemplate.indexOf(FORMAT_OUTPUT);
                        if (aiIndex != -1) {
                            finalSsb.append(aiTemplate.substring(0, aiIndex));
                            finalSsb.append(t);
                            finalSsb.append(aiTemplate.substring(aiIndex + FORMAT_OUTPUT.length()));
                        } else {
                            finalSsb.append(t);
                        }
                    } else {
                        finalSsb.append(t);
                    }
                    finalSsb.setSpan(new ForegroundColorSpan(aiCol), 0, finalSsb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case CATEGORY_ERROR:
                    SpannableString ser = TextProcessor.span(outputFormat, Color.RED);
                    CharSequence err = TextUtils.replace(ser,
                            new String[] {FORMAT_OUTPUT, FORMAT_NEWLINE, FORMAT_OUTPUT.toUpperCase(), FORMAT_NEWLINE.toUpperCase()},
                            new CharSequence[] {t, Tuils.NEWLINE, t, Tuils.NEWLINE});
                    finalSsb.append(err);
                    break;
                case CATEGORY_NO_COLOR:
                    finalSsb.append(t);
                    break;
                default:
                    return null;
            }

            return finalSsb;
        } catch (Exception e) {
            android.util.Log.e("TerminalManager", "getFinalText error: " + e.getMessage(), e);
            return t; // Fallback to raw text
        }
    }

    private boolean shouldSuppressOutput(CharSequence output) {
        if (output == null) return true;
        String text = output.toString().trim();
        return text.length() == 0 || "null".equalsIgnoreCase(text);
    }

    public void simulateEnter() {
        onNewInput();
    }

    public void setSuggestionsRefreshCallback(Runnable suggestionsRefreshCallback) {
        this.suggestionsRefreshCallback = suggestionsRefreshCallback;
    }

    public String getInput() {
        return mInputView.getText().toString();
    }

    public void setInput(String input, Object obj) {
        SpannableString spannable = new SpannableString(input);
        if(obj != null) {
            spannable.setSpan(obj, 0, input.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        mInputView.setText(spannable);
        focusInputEnd();
    }

    public void setInput(String input) {
        setInput(input, null);
    }

    public void setHint(String hint) {
        defaultHint = false;

        if(mInputView != null) {
            mInputView.setHint(hint);
        }
    }

    public void setDefaultHint() {
        defaultHint = true;

        if(mInputView != null) {
            mInputView.setHint(Tuils.getHint(mainPack.currentDirectory.getAbsolutePath()));
        }
    }

    public void focusInputEnd() {
        mInputView.setSelection(getInput().length());
    }

    public void scrollToEnd() {
        mScrollView.postDelayed(mScrollRunnable, SCROLL_DELAY);
    }

    public void requestInputFocus() {
        mInputView.requestFocus();
    }

    public IBinder getInputWindowToken() {
        return mInputView.getWindowToken();
    }

    public View getInputView() {
        return mInputView;
    }

    public void onAIStateChanged(boolean running) {
        this.aiRunning = running;
        if (deleteView != null) {
            int color = running ? android.graphics.Color.RED : toolbarColor;
            deleteView.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
        }
    }

    public void clear() {
        mTerminalView.post(() -> mTerminalView.setText(Tuils.EMPTYSTRING, TextView.BufferType.EDITABLE));
        cmdList.clear();
        clearCmdsCount = 0;
    }

    public void startStreaming() {
        mTerminalView.post(() -> {
            mTerminalView.append(Tuils.NEWLINE);
            streamStartOffset = mTerminalView.getText().length();
            synchronized (streamLock) {
                pendingStreamContent = "";
            }
            streamUpdatePending = false;
        });
    }

    public void updateStream(final String text, final int category) {
        final int aiCol = XMLPrefsManager.getColor(Theme.output_color);
        final SpannableString spannable = new SpannableString(text);
        spannable.setSpan(new ForegroundColorSpan(aiCol), 0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        synchronized (streamLock) {
            pendingStreamContent = spannable;
            if (!streamUpdatePending) {
                streamUpdatePending = true;
                streamHandler.postDelayed(streamUpdater, STREAM_THROTTLE_MS);
            }
        }
    }

    public void finishStreaming(final String finalText) {
        synchronized (streamLock) {
            streamHandler.removeCallbacks(streamUpdater);
            streamUpdatePending = false;
        }
        mTerminalView.post(() -> {
            if (streamStartOffset != -1 && mTerminalView != null) {
                CharSequence currentText = mTerminalView.getText();
                if (currentText instanceof Editable) {
                    Editable editable = (Editable) currentText;
                    if (streamStartOffset <= editable.length()) {
                        CharSequence formattedText = getFinalText(finalText, CATEGORY_AI);
                        editable.replace(streamStartOffset, editable.length(), formattedText);
                    }
                }
                streamStartOffset = -1;
                scrollToEnd();
            }
        });
    }

    public void onRoot() {
        ((Activity) mContext).runOnUiThread(() -> {
            suMode = true;
            mPrefix.setText(suPrefix.endsWith(Tuils.SPACE) ? suPrefix : suPrefix + Tuils.SPACE);
        });
    }

    public void onStandard() {
        ((Activity) mContext).runOnUiThread(() -> {
            suMode = false;
            mPrefix.setText(prefix.endsWith(Tuils.SPACE) ? prefix : prefix + Tuils.SPACE);
        });
    }
}
