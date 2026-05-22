package bhupendra.ai.launcher;

import bhupendra.ai.launcher.managers.DeviceStateManager;


import bhupendra.ai.launcher.managers.TextProcessor;


import bhupendra.ai.launcher.managers.FileSystemManager;


import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Handler;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.view.GestureDetectorCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import bhupendra.ai.launcher.commands.main.MainPack;
import bhupendra.ai.launcher.commands.main.specific.RedirectCommand;
import bhupendra.ai.launcher.managers.NotesManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.suggestions.SuggestionTextWatcher;
import bhupendra.ai.launcher.managers.suggestions.SuggestionsManager;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.managers.xml.options.Suggestions;
import bhupendra.ai.launcher.managers.xml.options.Theme;
import bhupendra.ai.launcher.managers.xml.options.Toolbar;
import bhupendra.ai.launcher.managers.xml.options.Ui;
import bhupendra.ai.launcher.tuils.AllowEqualsSequence;
import bhupendra.ai.launcher.ui.views.OutlineEditText;
import bhupendra.ai.launcher.ui.views.OutlineTextView;
import bhupendra.ai.launcher.tuils.Tuils;
import bhupendra.ai.launcher.tuils.interfaces.CommandExecuter;
import bhupendra.ai.launcher.tuils.interfaces.OnRedirectionListener;
import bhupendra.ai.launcher.tuils.interfaces.OnTextChanged;
import bhupendra.ai.launcher.tuils.stuff.PolicyReceiver;
import bhupendra.ai.launcher.ui.status.StatusUIController;
import bhupendra.ai.launcher.ui.status.WeatherUIController;

public class UIManager implements OnTouchListener, LabelUpdater {

    public static String ACTION_UPDATE_SUGGESTIONS = BuildConfig.APPLICATION_ID + ".ui_update_suggestions";
    public static String ACTION_UPDATE_HINT = BuildConfig.APPLICATION_ID + ".ui_update_hint";
    public static String ACTION_SHOW_CHOICE_SUGGESTIONS = BuildConfig.APPLICATION_ID + ".ui_show_choice_suggestions";
    public static String ACTION_SHOW_PARAMETER_SUGGESTIONS = BuildConfig.APPLICATION_ID + ".ui_show_parameter_suggestions";
    public static String ACTION_RESET_SUGGESTIONS_MODE = BuildConfig.APPLICATION_ID + ".ui_reset_suggestions_mode";
    public static String EXTRA_SUGGESTION_PROMPT = "suggestion_prompt";
    public static String EXTRA_SUGGESTION_OPTIONS = "suggestion_options";
    public static String EXTRA_SUGGESTION_FIELD = "suggestion_field";
    public static String EXTRA_SUGGESTION_PREFILL = "suggestion_prefill";
    public static String ACTION_ROOT = BuildConfig.APPLICATION_ID + ".ui_root";
    public static String ACTION_NOROOT = BuildConfig.APPLICATION_ID + ".ui_noroot";
    public static String ACTION_LOGTOFILE = BuildConfig.APPLICATION_ID + ".ui_log";
    public static String ACTION_CLEAR = BuildConfig.APPLICATION_ID + "ui_clear";
    public static String ACTION_WEATHER = BuildConfig.APPLICATION_ID + "ui_weather";
    public static String ACTION_WEATHER_GOT_LOCATION = BuildConfig.APPLICATION_ID + "ui_weather_location";
    public static String ACTION_WEATHER_DELAY = BuildConfig.APPLICATION_ID + "ui_weather_delay";
    public static String ACTION_WEATHER_MANUAL_UPDATE = BuildConfig.APPLICATION_ID + "ui_weather_update";

    public static String FILE_NAME = "fileName";
    public static String PREFS_NAME = "ui";

    public static final String UNLOCK_KEY = "unlockTimes";
    public static final String NEXT_UNLOCK_CYCLE_RESTART = "nextUnlockRestart";

    public enum Label {
        ram,
        device,
        time,
        battery,
        storage,
        network,
        notes,
        weather,
        unlock
    }

    private StatusUIController statusUIController;
    private WeatherUIController weatherUIController;

    protected Context mContext;

    private Handler handler;

    private DevicePolicyManager policy;
    private ComponentName component;
    private GestureDetectorCompat gestureDetector;

    SharedPreferences preferences;

    private InputMethodManager imm;
    private TerminalManager mTerminalAdapter;

    boolean hideToolbarNoInput;
    View toolbarView;

    //    never access this directly, use getLabelView
    private TextView[] labelViews = new TextView[Label.values().length];

    private float[] labelIndexes = new float[labelViews.length];
    private int[] labelSizes = new int[labelViews.length];
    private CharSequence[] labelTexts = new CharSequence[labelViews.length];

    private TextView getLabelView(Label l) {
        return labelViews[(int) labelIndexes[l.ordinal()]];
    }

    private TextView getLabelViewSafe(Label l) {
        int index = (int) labelIndexes[l.ordinal()];
        if (index < 0 || index >= labelViews.length) return null;
        return labelViews[index];
    }
//    you need to use labelIndexes[i]
    @Override
    public void updateText(Label l, CharSequence s) {
        labelTexts[l.ordinal()] = s;

        int base = (int) labelIndexes[l.ordinal()];

        List<Float> indexs = new ArrayList<>();
        for(int count = 0; count < Label.values().length; count++) {
            if((int) labelIndexes[count] == base && labelTexts[count] != null) indexs.add(labelIndexes[count]);
        }
//        now I'm sorting the labels on the same line for decimals (2.1, 2.0, ...)
        Collections.sort(indexs);

        CharSequence sequence = Tuils.EMPTYSTRING;

        for(int c = 0; c < indexs.size(); c++) {
            float i = indexs.get(c);

            for(int a = 0; a < Label.values().length; a++) {
                if(i == labelIndexes[a] && labelTexts[a] != null) {
                    if(sequence.length() > 0) sequence = TextUtils.concat(sequence, Tuils.SPACE);
                    sequence = TextUtils.concat(sequence, labelTexts[a]);
                }
            }
        }

        if(sequence.length() == 0) labelViews[base].setVisibility(View.GONE);
        else {
            labelViews[base].setVisibility(View.VISIBLE);
            labelViews[base].setText(sequence);
        }
    }

    @Override
    public int getLabelSize(Label l) {
        return labelSizes[l.ordinal()];
    }

    private SuggestionsManager suggestionsManager;

    private TextView terminalView;

    private String doubleTapCmd;
    private boolean lockOnDbTap;

    private BroadcastReceiver receiver;

    public MainPack pack;

    private boolean clearOnLock;
    private boolean isPaused = false;
    private final ViewGroup rootView;
    private final boolean canApplyTheme;

    public void reapplyBackground() {
        if (!XMLPrefsManager.getBoolean(Ui.system_wallpaper) || !canApplyTheme) {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.bg_color));
        } else {
            rootView.setBackgroundColor(XMLPrefsManager.getColor(Theme.overlay_color));
        }
    }

    protected UIManager(final Context context, final ViewGroup rootView, MainPack mainPack, boolean canApplyTheme, CommandExecuter executer) {
        this.rootView = rootView;
        this.canApplyTheme = canApplyTheme;

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_UPDATE_SUGGESTIONS);
        filter.addAction(ACTION_UPDATE_HINT);
        filter.addAction(ACTION_SHOW_CHOICE_SUGGESTIONS);
        filter.addAction(ACTION_SHOW_PARAMETER_SUGGESTIONS);
        filter.addAction(ACTION_RESET_SUGGESTIONS_MODE);
        filter.addAction(ACTION_ROOT);
        filter.addAction(ACTION_NOROOT);
//        filter.addAction(ACTION_CLEAR_SUGGESTIONS);
        filter.addAction(ACTION_LOGTOFILE);
        filter.addAction(ACTION_CLEAR);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(action.equals(ACTION_UPDATE_SUGGESTIONS)) {
                    if(suggestionsManager != null) suggestionsManager.requestSuggestion(Tuils.EMPTYSTRING);
                } else if(action.equals(ACTION_UPDATE_HINT)) {
                    mTerminalAdapter.setDefaultHint();
                } else if(action.equals(ACTION_SHOW_CHOICE_SUGGESTIONS)) {
                    if (suggestionsManager != null) {
                        String prompt = intent.getStringExtra(EXTRA_SUGGESTION_PROMPT);
                        java.util.ArrayList<String> options = intent.getStringArrayListExtra(EXTRA_SUGGESTION_OPTIONS);
                        suggestionsManager.showChoiceMode(prompt, options);
                        if (prompt != null && prompt.length() > 0) {
                            mTerminalAdapter.setHint(prompt);
                        }
                        suggestionsManager.requestSuggestion(mTerminalAdapter.getInput());
                        mTerminalAdapter.requestInputFocus();
                    }
                } else if(action.equals(ACTION_SHOW_PARAMETER_SUGGESTIONS)) {
                    if (suggestionsManager != null) {
                        String field = intent.getStringExtra(EXTRA_SUGGESTION_FIELD);
                        String prompt = intent.getStringExtra(EXTRA_SUGGESTION_PROMPT);
                        String prefill = intent.getStringExtra(EXTRA_SUGGESTION_PREFILL);
                        suggestionsManager.showParameterMode(field, prompt);
                        if (prompt != null && prompt.length() > 0) {
                            mTerminalAdapter.setHint(prompt);
                        }
                        mTerminalAdapter.setInput(prefill != null ? prefill : Tuils.EMPTYSTRING);
                        suggestionsManager.requestSuggestion(mTerminalAdapter.getInput());
                        mTerminalAdapter.requestInputFocus();
                    }
                } else if(action.equals(ACTION_RESET_SUGGESTIONS_MODE)) {
                    if (suggestionsManager != null) {
                        suggestionsManager.showCommandMode();
                        suggestionsManager.requestSuggestion(mTerminalAdapter.getInput());
                    }
                    mTerminalAdapter.setDefaultHint();
                } else if(action.equals(ACTION_ROOT)) {
                    mTerminalAdapter.onRoot();
                } else if(action.equals(ACTION_NOROOT)) {
                    mTerminalAdapter.onStandard();
//                } else if(action.equals(ACTION_CLEAR_SUGGESTIONS)) {
//                    if(suggestionsManager != null) suggestionsManager.clear();
                } else if(action.equals(ACTION_LOGTOFILE)) {
                    String fileName = intent.getStringExtra(FILE_NAME);
                    if(fileName == null || fileName.contains(File.separator)) return;

                    File file = new File(FileSystemManager.getFolder(), fileName);
                    if(file.exists()) file.delete();

                    try {
                        file.createNewFile();

                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(mTerminalAdapter.getTerminalText().getBytes());

                        Tuils.sendOutput(context, "Logged to " + file.getAbsolutePath());
                    } catch (Exception e) {
                        Tuils.sendOutput(Color.RED, context, e.toString());
                    }
                } else if(action.equals(ACTION_CLEAR)) {
                    mTerminalAdapter.clear();
                    if (suggestionsManager != null)
                        suggestionsManager.requestSuggestion(Tuils.EMPTYSTRING);
                }
            }
        };

        LocalBroadcastManager.getInstance(context.getApplicationContext()).registerReceiver(receiver, filter);

        policy = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        component = new ComponentName(context, PolicyReceiver.class);

        mContext = context;

        preferences = mContext.getSharedPreferences(PREFS_NAME, 0);

        handler = new Handler();

        imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);

        reapplyBackground();

//        scrolllllll
        if(XMLPrefsManager.getBoolean(Behavior.auto_scroll)) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                int heightDiff = rootView.getRootView().getHeight() - rootView.getHeight();
                if (heightDiff > Tuils.dpToPx(context, 200)) { // if more than 200 dp, it's probably a keyboard...
                    if(mTerminalAdapter != null) mTerminalAdapter.scrollToEnd();
                }
            });
        }

        clearOnLock = XMLPrefsManager.getBoolean(Behavior.clear_on_lock);

        lockOnDbTap = XMLPrefsManager.getBoolean(Behavior.double_tap_lock);
        doubleTapCmd = XMLPrefsManager.get(Behavior.double_tap_cmd);
        if(!lockOnDbTap && doubleTapCmd == null) {
            policy = null;
            component = null;
            gestureDetector = null;
        } else {
            gestureDetector = new GestureDetectorCompat(mContext, new GestureDetector.OnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return false;
                }

                @Override
                public void onShowPress(MotionEvent e) {}

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent e) {}

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return false;
                }
            });

            gestureDetector.setOnDoubleTapListener(new OnDoubleTapListener() {

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    return false;
                }

                @Override
                public boolean onDoubleTapEvent(MotionEvent e) {
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {

                    if(doubleTapCmd != null && doubleTapCmd.length() > 0) {
                        String input = mTerminalAdapter.getInput();
                        mTerminalAdapter.setInput(doubleTapCmd);
                        mTerminalAdapter.simulateEnter();
                        mTerminalAdapter.setInput(input);
                    }

                    if(lockOnDbTap) {
                        boolean admin = policy.isAdminActive(component);

                        if (!admin) {
                            Intent i = DeviceStateManager.requestAdmin(component, mContext.getString(R.string.admin_permission));
                            mContext.startActivity(i);
                        } else {
                            policy.lockNow();
                        }
                    }

                    return true;
                }
            });
        }

        int[] displayMargins = getListOfIntValues(XMLPrefsManager.get(Ui.display_margin_mm), 4, 0);
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        rootView.setPadding(Tuils.mmToPx(metrics, displayMargins[0]), Tuils.mmToPx(metrics, displayMargins[1]), Tuils.mmToPx(metrics, displayMargins[2]), Tuils.mmToPx(metrics, displayMargins[3]));

        labelSizes[Label.time.ordinal()] = XMLPrefsManager.getInt(Ui.time_size);
        labelSizes[Label.ram.ordinal()] = XMLPrefsManager.getInt(Ui.ram_size);
        labelSizes[Label.battery.ordinal()] = XMLPrefsManager.getInt(Ui.battery_size);
        labelSizes[Label.storage.ordinal()] = XMLPrefsManager.getInt(Ui.storage_size);
        labelSizes[Label.network.ordinal()] = XMLPrefsManager.getInt(Ui.network_size);
        labelSizes[Label.notes.ordinal()] = XMLPrefsManager.getInt(Ui.notes_size);
        labelSizes[Label.device.ordinal()] = XMLPrefsManager.getInt(Ui.device_size);
        labelSizes[Label.weather.ordinal()] = XMLPrefsManager.getInt(Ui.weather_size);
        labelSizes[Label.unlock.ordinal()] = XMLPrefsManager.getInt(Ui.unlock_size);

        labelViews = new TextView[] {
                (TextView) rootView.findViewById(R.id.tv0),
                (TextView) rootView.findViewById(R.id.tv1),
                (TextView) rootView.findViewById(R.id.tv2),
                (TextView) rootView.findViewById(R.id.tv3),
                (TextView) rootView.findViewById(R.id.tv4),
                (TextView) rootView.findViewById(R.id.tv5),
                (TextView) rootView.findViewById(R.id.tv6),
                (TextView) rootView.findViewById(R.id.tv7),
                (TextView) rootView.findViewById(R.id.tv8),
        };

        boolean[] show = new boolean[Label.values().length];
        show[Label.notes.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_notes);
        show[Label.ram.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_ram);
        show[Label.device.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_device_name);
        show[Label.time.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_time);
        show[Label.battery.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_battery);
        show[Label.network.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_network_info);
        show[Label.storage.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_storage_info);
        show[Label.weather.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_weather);
        show[Label.unlock.ordinal()] = XMLPrefsManager.getBoolean(Ui.show_unlock_counter);

        float[] indexes = new float[Label.values().length];
        indexes[Label.notes.ordinal()] = show[Label.notes.ordinal()] ? XMLPrefsManager.getFloat(Ui.notes_index) : Integer.MAX_VALUE;
        indexes[Label.ram.ordinal()] = show[Label.ram.ordinal()] ? XMLPrefsManager.getFloat(Ui.ram_index) : Integer.MAX_VALUE;
        indexes[Label.device.ordinal()] = show[Label.device.ordinal()] ? XMLPrefsManager.getFloat(Ui.device_index) : Integer.MAX_VALUE;
        indexes[Label.time.ordinal()] = show[Label.time.ordinal()] ? XMLPrefsManager.getFloat(Ui.time_index) : Integer.MAX_VALUE;
        indexes[Label.battery.ordinal()] = show[Label.battery.ordinal()] ? XMLPrefsManager.getFloat(Ui.battery_index) : Integer.MAX_VALUE;
        indexes[Label.network.ordinal()] = show[Label.network.ordinal()] ? XMLPrefsManager.getFloat(Ui.network_index) : Integer.MAX_VALUE;
        indexes[Label.storage.ordinal()] = show[Label.storage.ordinal()] ? XMLPrefsManager.getFloat(Ui.storage_index) : Integer.MAX_VALUE;
        indexes[Label.weather.ordinal()] = show[Label.weather.ordinal()] ? XMLPrefsManager.getFloat(Ui.weather_index) : Integer.MAX_VALUE;
        indexes[Label.unlock.ordinal()] = show[Label.unlock.ordinal()] ? XMLPrefsManager.getFloat(Ui.unlock_index) : Integer.MAX_VALUE;

        int[] statusLineAlignments = getListOfIntValues(XMLPrefsManager.get(Ui.status_lines_alignment), 9, -1);

        String[] statusLinesBgRectColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_bgrectcolor), 9, "#ff000000");
        String[] otherBgRectColors = {
                XMLPrefsManager.get(Theme.input_bgrectcolor),
                XMLPrefsManager.get(Theme.output_bgrectcolor),
                XMLPrefsManager.get(Theme.suggestions_bgrectcolor),
                XMLPrefsManager.get(Theme.toolbar_bgrectcolor)
        };
        String[] bgRectColors = new String[statusLinesBgRectColors.length + otherBgRectColors.length];
        System.arraycopy(statusLinesBgRectColors, 0, bgRectColors, 0, statusLinesBgRectColors.length);
        System.arraycopy(otherBgRectColors, 0, bgRectColors, statusLinesBgRectColors.length, otherBgRectColors.length);

        String[] statusLineBgColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_bg), 9, "#00000000");
        String[] otherBgColors = {
                XMLPrefsManager.get(Theme.input_bg),
                XMLPrefsManager.get(Theme.output_bg),
                XMLPrefsManager.get(Theme.suggestions_bg),
                XMLPrefsManager.get(Theme.toolbar_bg)
        };
        String[] bgColors = new String[statusLineBgColors.length + otherBgColors.length];
        System.arraycopy(statusLineBgColors, 0, bgColors, 0, statusLineBgColors.length);
        System.arraycopy(otherBgColors, 0, bgColors, statusLineBgColors.length, otherBgColors.length);

        String[] statusLineOutlineColors = getListOfStringValues(XMLPrefsManager.get(Theme.status_lines_shadow_color), 9, "#00000000");
        String[] otherOutlineColors = {
                XMLPrefsManager.get(Theme.input_shadow_color),
                XMLPrefsManager.get(Theme.output_shadow_color),
        };
        String[] outlineColors = new String[statusLineOutlineColors.length + otherOutlineColors.length];
        System.arraycopy(statusLineOutlineColors, 0, outlineColors, 0, statusLineOutlineColors.length);
        System.arraycopy(otherOutlineColors, 0, outlineColors, 9, otherOutlineColors.length);

        int shadowXOffset, shadowYOffset;
        float shadowRadius;
        String[] shadowParams = getListOfStringValues(XMLPrefsManager.get(Ui.shadow_params), 3, "0");
        shadowXOffset = Integer.parseInt(shadowParams[0]);
        shadowYOffset = Integer.parseInt(shadowParams[1]);
        shadowRadius = Float.parseFloat(shadowParams[2]);

        final int INPUT_BGCOLOR_INDEX = 9;
        final int OUTPUT_BGCOLOR_INDEX = 10;
        final int SUGGESTIONS_BGCOLOR_INDEX = 11;
        final int TOOLBAR_BGCOLOR_INDEX = 12;

        int strokeWidth, cornerRadius;
        String[] rectParams = getListOfStringValues(XMLPrefsManager.get(Ui.bgrect_params), 2, "0");
        strokeWidth = Integer.parseInt(rectParams[0]);
        cornerRadius = Integer.parseInt(rectParams[1]);

        final int OUTPUT_MARGINS_INDEX = 1;
        final int INPUTAREA_MARGINS_INDEX = 2;
        final int INPUTFIELD_MARGINS_INDEX = 3;
        final int TOOLBAR_MARGINS_INDEX = 4;
        final int SUGGESTIONS_MARGINS_INDEX = 5;

        final int[][] margins = new int[6][4];
        margins[0] = getListOfIntValues(XMLPrefsManager.get(Ui.status_lines_margins), 4, 0);
        margins[1] = getListOfIntValues(XMLPrefsManager.get(Ui.output_field_margins), 4, 0);
        margins[2] = getListOfIntValues(XMLPrefsManager.get(Ui.input_area_margins), 4, 0);
        margins[3] = getListOfIntValues(XMLPrefsManager.get(Ui.input_field_margins), 4, 0);
        margins[4] = getListOfIntValues(XMLPrefsManager.get(Ui.toolbar_margins), 4, 0);
        margins[5] = getListOfIntValues(XMLPrefsManager.get(Ui.suggestions_area_margin), 4, 0);

        AllowEqualsSequence sequence = new AllowEqualsSequence(indexes, Label.values());

        LinearLayout lViewsParent = (LinearLayout) labelViews[0].getParent();

        int effectiveCount = 0;
        for(int count = 0; count < labelViews.length; count++) {
            labelViews[count].setOnTouchListener(this);

            Object[] os = sequence.get(count);

//            views on the same line
            for(int j = 0; j < os.length; j++) {
//                i is the object gave to the constructor
                int i = ((Label) os[j]).ordinal();
//                v is the adjusted index (2.0, 2.1, 2.2, ...)
                float v = (float) count + ((float) j * 0.1f);

                labelIndexes[i] = v;
            }

            if(count >= sequence.getMinKey() && count <= sequence.getMaxKey() && os.length > 0) {
                labelViews[count].setTypeface(Tuils.getTypeface(context));

                int ec = effectiveCount++;

//                -1 = left     0 = center     1 = right
                int p = statusLineAlignments[ec];
                if(p >= 0) labelViews[count].setGravity(p == 0 ? Gravity.CENTER_HORIZONTAL : Gravity.RIGHT);

                if(count != labelIndexes[Label.notes.ordinal()]) {
                    labelViews[count].setVerticalScrollBarEnabled(false);
                }

                applyBgRect(labelViews[count], bgRectColors[count], bgColors[count], margins[0], strokeWidth, cornerRadius);
                applyShadow(labelViews[count], outlineColors[count], shadowXOffset, shadowYOffset, shadowRadius);
            } else {
                lViewsParent.removeView(labelViews[count]);
                labelViews[count] = null;
            }
        }

        final TextView notesView = getLabelViewSafe(Label.notes);
        statusUIController = new StatusUIController(context, handler, preferences, this, notesView, () -> {
            if (clearOnLock) {
                mTerminalAdapter.clear();
            }
        });
        statusUIController.init(show);

        weatherUIController = new WeatherUIController(context, handler, this);
        weatherUIController.init();

        final boolean inputBottom = XMLPrefsManager.getBoolean(Ui.input_bottom);
        int layoutId = inputBottom ? R.layout.input_down_layout : R.layout.input_up_layout;

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View inputOutputView = inflater.inflate(layoutId, null);
        rootView.addView(inputOutputView);

        terminalView = (TextView) inputOutputView.findViewById(R.id.terminal_view);
        terminalView.setOnTouchListener(this);
        ((View) terminalView.getParent().getParent()).setOnTouchListener(this);

        applyBgRect(terminalView, bgRectColors[OUTPUT_BGCOLOR_INDEX], bgColors[OUTPUT_BGCOLOR_INDEX], margins[OUTPUT_MARGINS_INDEX], strokeWidth, cornerRadius);
        applyShadow(terminalView, outlineColors[OUTPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);

        final EditText inputView = (EditText) inputOutputView.findViewById(R.id.input_view);
        TextView prefixView = (TextView) inputOutputView.findViewById(R.id.prefix_view);

        applyBgRect(inputOutputView.findViewById(R.id.input_group), bgRectColors[INPUT_BGCOLOR_INDEX], bgColors[INPUT_BGCOLOR_INDEX], margins[INPUTAREA_MARGINS_INDEX], strokeWidth, cornerRadius);
        applyShadow(inputView, outlineColors[INPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);
        applyShadow(prefixView, outlineColors[INPUT_BGCOLOR_INDEX], shadowXOffset, shadowYOffset, shadowRadius);

        applyMargins(inputView, margins[INPUTFIELD_MARGINS_INDEX]);
        applyMargins(prefixView, margins[INPUTFIELD_MARGINS_INDEX]);

        ImageView submitView = (ImageView) inputOutputView.findViewById(R.id.submit_tv);
        boolean showSubmit = XMLPrefsManager.getBoolean(Ui.show_enter_button);
        if (!showSubmit) {
            submitView.setVisibility(View.GONE);
            submitView = null;
        }

//        final ImageButton finalSubmitView = submitView;
//        inputView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                Tuils.scaleImage(finalSubmitView, 20, 20);
//
//                inputView.getViewTreeObserver().removeOnPreDrawListener(this);
//
//                return false;
//            }
//        });

//        toolbar
        boolean showToolbar = XMLPrefsManager.getBoolean(Toolbar.show_toolbar);
        ImageButton backView = null;
        ImageButton nextView = null;
        ImageButton deleteView = null;
        ImageButton pasteView = null;

        if(!showToolbar) {
            inputOutputView.findViewById(R.id.tools_view).setVisibility(View.GONE);
            toolbarView = null;
        } else {
            backView = (ImageButton) inputOutputView.findViewById(R.id.back_view);
            nextView = (ImageButton) inputOutputView.findViewById(R.id.next_view);
            deleteView = (ImageButton) inputOutputView.findViewById(R.id.delete_view);
            pasteView = (ImageButton) inputOutputView.findViewById(R.id.paste_view);

            toolbarView = inputOutputView.findViewById(R.id.tools_view);
            hideToolbarNoInput = XMLPrefsManager.getBoolean(Toolbar.hide_toolbar_no_input);

            applyBgRect(toolbarView, bgRectColors[TOOLBAR_BGCOLOR_INDEX], bgColors[TOOLBAR_BGCOLOR_INDEX], margins[TOOLBAR_MARGINS_INDEX], strokeWidth, cornerRadius);
        }

        mTerminalAdapter = new TerminalManager(terminalView, inputView, prefixView, submitView, backView, nextView, deleteView, pasteView, context, mainPack, executer);

        if (XMLPrefsManager.getBoolean(Suggestions.show_suggestions)) {
            HorizontalScrollView sv = (HorizontalScrollView) rootView.findViewById(R.id.suggestions_container);
            sv.setFocusable(false);
            sv.setOnFocusChangeListener((v, hasFocus) -> {
                if(hasFocus) {
                    v.clearFocus();
                }
            });
            applyBgRect(sv, bgRectColors[SUGGESTIONS_BGCOLOR_INDEX], bgColors[SUGGESTIONS_BGCOLOR_INDEX], margins[SUGGESTIONS_MARGINS_INDEX], strokeWidth, cornerRadius);

            LinearLayout suggestionsView = (LinearLayout) rootView.findViewById(R.id.suggestions_group);

            suggestionsManager = new SuggestionsManager(suggestionsView, mainPack, mTerminalAdapter);
            mTerminalAdapter.setSuggestionsRefreshCallback(() -> {
                if (suggestionsManager != null) {
                    suggestionsManager.requestSuggestion(mTerminalAdapter.getInput());
                }
            });

            inputView.addTextChangedListener(new SuggestionTextWatcher(suggestionsManager, (currentText, before) -> {
                if(!hideToolbarNoInput) return;

                if(currentText.length() == 0) toolbarView.setVisibility(View.GONE);
                else if(before == 0) toolbarView.setVisibility(View.VISIBLE);
            }));

            if (hideToolbarNoInput && toolbarView != null) {
                toolbarView.setVisibility(inputView.getText().length() == 0 ? View.GONE : View.VISIBLE);
            }

            suggestionsManager.requestSuggestion(inputView.getText().toString());
        } else {
            rootView.findViewById(R.id.suggestions_group).setVisibility(View.GONE);
        }

        int drawTimes = XMLPrefsManager.getInt(Ui.text_redraw_times);
        if(drawTimes <= 0) drawTimes = 1;
        OutlineTextView.redrawTimes = drawTimes;
    }

    public static int[] getListOfIntValues(String values, int length, int defaultValue) {
        int[] is = new int[length];
        values = removeSquareBrackets(values);
        String[] split = values.split(",");
        int c = 0;
        for(; c < split.length; c++) {
            try {
                is[c] = Integer.parseInt(split[c]);
            } catch (Exception e) {
                is[c] = defaultValue;
            }
        }
        while(c < split.length) is[c] = defaultValue;

        return is;
    }

    public static String[] getListOfStringValues(String values, int length, String defaultValue) {
        String[] is = new String[length];
        String[] split = values.split(",");

        int len = Math.min(split.length, is.length);
        System.arraycopy(split, 0, is, 0, len);

        while(len < is.length) is[len++] = defaultValue;

        return is;
    }

    private static Pattern sbPattern = Pattern.compile("[\\[\\]\\s]");
    private static String removeSquareBrackets(String s) {
        return sbPattern.matcher(s).replaceAll(Tuils.EMPTYSTRING);
    }

//    0 = ext hor
//    1 = ext ver
//    2 = int hor
//    3 = int ver
    private static void applyBgRect(View v, String strokeColor, String bgColor, int[] spaces, int strokeWidth, int cornerRadius) {
        try {
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.RECTANGLE);
            d.setCornerRadius(cornerRadius);

            if(!(strokeColor.startsWith("#00") && strokeColor.length() == 9)) {
                d.setStroke(strokeWidth, Color.parseColor(strokeColor));
            }

            applyMargins(v, spaces);

            d.setColor(Color.parseColor(bgColor));
            v.setBackgroundDrawable(d);
        } catch (Exception e) {
            FileSystemManager.toFile(e);
            Tuils.log(e);
        }
    }

    private static void applyMargins(View v, int[] margins) {
        v.setPadding(margins[2], margins[3], margins[2], margins[3]);

        ViewGroup.LayoutParams params = v.getLayoutParams();
        if(params instanceof RelativeLayout.LayoutParams) {
            ((RelativeLayout.LayoutParams) params).setMargins(margins[0], margins[1], margins[0], margins[1]);
        } else if(params instanceof LinearLayout.LayoutParams) {
            ((LinearLayout.LayoutParams) params).setMargins(margins[0], margins[1], margins[0], margins[1]);
        }
    }

    private static void applyShadow(TextView v, String color, int x, int y, float radius) {
        try {
            if(color != null && !(color.startsWith("#00") && color.length() == 9)) {
                v.setShadowLayer(radius, x, y, Color.parseColor(color));
                v.setTag(OutlineTextView.SHADOW_TAG);
            }
        } catch (Exception e) {
            Tuils.log(e);
        }
    }

    public void dispose() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }

        if (suggestionsManager != null) suggestionsManager.dispose();
        if (statusUIController != null) {
            statusUIController.dispose();
        }
        if (weatherUIController != null) {
            weatherUIController.dispose();
        }
        LocalBroadcastManager.getInstance(mContext.getApplicationContext()).unregisterReceiver(receiver);

        Tuils.cancelFont();
    }

    public void openKeyboard() {
        mTerminalAdapter.requestInputFocus();
        imm.showSoftInput(mTerminalAdapter.getInputView(), InputMethodManager.SHOW_IMPLICIT);
//        mTerminalAdapter.scrollToEnd();
    }

    public void closeKeyboard() {
        imm.hideSoftInputFromWindow(mTerminalAdapter.getInputWindowToken(), 0);
    }

    public void onStart(boolean openKeyboardOnStart) {
        if (suggestionsManager != null) {
            suggestionsManager.requestSuggestion(mTerminalAdapter.getInput());
        }
        if(openKeyboardOnStart) openKeyboard();
    }

    public void setInput(String s) {
        if (s == null)
            return;

        mTerminalAdapter.setInput(s);
        mTerminalAdapter.focusInputEnd();
    }

    public void setHint(String hint) {
        mTerminalAdapter.setHint(hint);
    }

    public void resetHint() {
        mTerminalAdapter.setDefaultHint();
    }

    public void setOutput(CharSequence s, int category) {
        mTerminalAdapter.setOutput(s, category);
    }

    public void setOutput(int color, CharSequence output) {
        mTerminalAdapter.setOutput(color, output);
    }

    public void startStreaming() {
        if (mTerminalAdapter != null) mTerminalAdapter.startStreaming();
    }

    public void updateStream(String thinkingText, String text, int category) {
        if (mTerminalAdapter != null) mTerminalAdapter.updateStream(thinkingText, text, category);
    }

    public void finishStreaming(String thinkingText, String finalText) {
        if (mTerminalAdapter != null) mTerminalAdapter.finishStreaming(thinkingText, finalText);
    }

    public void disableSuggestions() {
        if(suggestionsManager != null) suggestionsManager.disable();
    }

    public void enableSuggestions() {
        if(suggestionsManager != null) suggestionsManager.enable();
    }

    public void onBackPressed() {
        mTerminalAdapter.onBackPressed();
    }

    public void focusTerminal() {
        mTerminalAdapter.requestInputFocus();
    }

    public void pause() {
        closeKeyboard();
        isPaused = true;
        if (statusUIController != null) {
            statusUIController.pause();
        }
        if (weatherUIController != null) {
            weatherUIController.pause();
        }
    }

    public void resume() {
        if (!isPaused) return;
        isPaused = false;
        if (statusUIController != null) {
            statusUIController.resume();
        }
        if (weatherUIController != null) {
            weatherUIController.resume();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return v.onTouchEvent(event);
    }

    public OnRedirectionListener buildRedirectionListener() {
        return new OnRedirectionListener() {
            @Override
            public void onRedirectionRequest(final RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(() -> {
                    mTerminalAdapter.setHint(mContext.getString(cmd.getHint()));
                    disableSuggestions();
                });
            }

            @Override
            public void onRedirectionEnd(RedirectCommand cmd) {
                ((Activity) mContext).runOnUiThread(() -> {
                    mTerminalAdapter.setDefaultHint();
                    enableSuggestions();
                });
            }
        };
    }

    public void setAIState(boolean running) {
        if (mTerminalAdapter != null) {
            mTerminalAdapter.onAIStateChanged(running);
        }
    }

    public boolean applyLiveConfigChange(String key) {
        if (key == null) return false;

        switch (key) {
            case "show_notes":
                return statusUIController != null && statusUIController.applyNotesVisibility(XMLPrefsManager.getBoolean(Ui.show_notes));
            case "show_ram":
                return statusUIController != null && statusUIController.applySimpleLabelVisibility(Label.ram, XMLPrefsManager.getBoolean(Ui.show_ram));
            case "show_time":
                return statusUIController != null && statusUIController.applySimpleLabelVisibility(Label.time, XMLPrefsManager.getBoolean(Ui.show_time));
            case "show_storage_info":
                return statusUIController != null && statusUIController.applySimpleLabelVisibility(Label.storage, XMLPrefsManager.getBoolean(Ui.show_storage_info));
            case "show_network_info":
                return statusUIController != null && statusUIController.applySimpleLabelVisibility(Label.network, XMLPrefsManager.getBoolean(Ui.show_network_info));
            case "show_weather":
                return weatherUIController != null && weatherUIController.applyWeatherVisibility(XMLPrefsManager.getBoolean(Ui.show_weather));
            case "show_unlock_counter":
                return statusUIController != null && statusUIController.applySimpleLabelVisibility(Label.unlock, XMLPrefsManager.getBoolean(Ui.show_unlock_counter));
            case "show_device_name":
                return statusUIController != null && statusUIController.applyDeviceVisibility(XMLPrefsManager.getBoolean(Ui.show_device_name));
            case "show_battery":
                return statusUIController != null && statusUIController.applyBatteryVisibility(XMLPrefsManager.getBoolean(Ui.show_battery));
            case "notification_whitelist":
            case "notification_blacklist":
                if (bhupendra.ai.launcher.managers.notifications.NotificationService.instance != null) {
                    bhupendra.ai.launcher.managers.notifications.NotificationService.instance.updateFiltering();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }
}
