package bhupendra.ai.launcher.commands;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;

import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;
import bhupendra.ai.launcher.managers.xml.options.Cmd;

/**
 * Created by francescoandreuzzi on 06/01/2017.
 */

public class CommandsPreferences {

    public static final String PRIORITY_SUFFIX = "_priority";
    private static final String USAGE_PREFS = "command_usage";
    private static final String USAGE_SUFFIX = "_usage";
    private static final String PARAM_USAGE_SUFFIX = "_param_usage";

    private HashMap<String, String> preferenceHashMap;
    private final SharedPreferences usagePreferences;

    public CommandsPreferences(Context context) {
        preferenceHashMap = new HashMap<>();
        usagePreferences = context.getSharedPreferences(USAGE_PREFS, Context.MODE_PRIVATE);

        for(XMLPrefsSave save : Cmd.values()) {
            preferenceHashMap.put(save.label(), XMLPrefsManager.get(save));
        }
    }

    public String get(String s) {
        String v = preferenceHashMap.get(s);
        if(v == null) return XMLPrefsManager.get(XMLPrefsManager.XMLPrefsRoot.CMD, s);
        return v;
    }

    public String get(XMLPrefsSave save) {
        String v = get(save.label());
        if(v == null || v.length() == 0) v = save.defaultValue();
        return v;
    }

    public int userSetPriority(CommandAbstraction c) {
        try {
            String p = get(c.getClass().getSimpleName() + PRIORITY_SUFFIX);
            return Integer.parseInt(p);
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }

    public int getPriority(CommandAbstraction c) {
        int priority = userSetPriority(c);
        if(priority == Integer.MAX_VALUE) return c.priority();
        return priority;
    }

    public int getUsageCount(CommandAbstraction c) {
        if (c == null) return 0;
        return usagePreferences.getInt(c.getClass().getSimpleName() + USAGE_SUFFIX, 0);
    }

    public int getUsageScore(CommandAbstraction c) {
        int usage = getUsageCount(c);
        int priority = Math.max(-999, getPriority(c));
        return usage * 1000 + priority;
    }

    public void recordUsage(String commandName) {
        if (commandName == null || commandName.length() == 0) return;
        String key = commandName + USAGE_SUFFIX;
        int current = usagePreferences.getInt(key, 0);
        usagePreferences.edit().putInt(key, current + 1).apply();
    }

    public int getParamUsageCount(String commandName, String paramName) {
        if (commandName == null || commandName.length() == 0 || paramName == null || paramName.length() == 0) return 0;
        return usagePreferences.getInt(commandName + ":" + paramName + PARAM_USAGE_SUFFIX, 0);
    }

    public int getParamUsageScore(String commandName, String paramName) {
        int usage = getParamUsageCount(commandName, paramName);
        return usage * 1000;
    }

    public void recordParamUsage(String commandName, String paramName) {
        if (commandName == null || commandName.length() == 0 || paramName == null || paramName.length() == 0) return;
        String key = commandName + ":" + paramName + PARAM_USAGE_SUFFIX;
        int current = usagePreferences.getInt(key, 0);
        usagePreferences.edit().putInt(key, current + 1).apply();
    }
}
