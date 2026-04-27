package bhupendra.ai.launcher.commands;

import android.content.Context;

import java.util.ArrayList;

import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.xml.classes.XMLPrefsSave;

@SuppressWarnings("deprecation")
public abstract class ExecutePack implements CommandContext {

    public Object[] args;
    public Context context;
    public CommandGroup commandGroup;

    public int currentIndex = 0;

    public ExecutePack(CommandGroup group) {
        this.commandGroup = group;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> c) {
        return (T) get();
    }

    public <T> T get(Class<T> c, int index) {
        if(index < args.length) return (T) args[index];
        return null;
    }

    public Object get() {
        if(currentIndex < args.length) return args[currentIndex++];
        return null;
    }

    public String getString() {
        return (String) get();
    }

    public int getInt() {
        return (int) get();
    }

    public boolean getBoolean() {
        return (boolean) get();
    }

    public ArrayList getList() {
        return (ArrayList) get();
    }

    public XMLPrefsSave getPrefsSave() {
        return (XMLPrefsSave) get();
    }

    public AppsManager.LaunchInfo getLaunchInfo() {
        return (AppsManager.LaunchInfo) get();
    }

    public void set(Object[] args) {
        this.args = args;
    }

    public void clear() {
        args = null;
        currentIndex = 0;
    }

    // CommandContext dummy implementations for subclasses (like TuixtPack) that don't override them
    @Override public android.content.res.Resources getResources() { return null; }
    @Override public java.io.File getCurrentDirectory() { return null; }
    @Override public void setCurrentDirectory(java.io.File directory) {}
    @Override public android.net.wifi.WifiManager getWifiManager() { return null; }
    @Override public bhupendra.ai.launcher.managers.ContactManager getContactManager() { return null; }
    @Override public bhupendra.ai.launcher.managers.music.MusicManager2 getMusicManager() { return null; }
    @Override public bhupendra.ai.launcher.managers.AliasManager getAliasManager() { return null; }
    @Override public bhupendra.ai.launcher.managers.AppsManager getAppsManager() { return null; }
    @Override public bhupendra.ai.launcher.commands.CommandsPreferences getCmdPrefs() { return null; }
    @Override public String getLastCommand() { return null; }
    @Override public void setLastCommand(String command) {}
    @Override public bhupendra.ai.launcher.tuils.interfaces.Redirectator getRedirectator() { return null; }
    @Override public bhupendra.ai.launcher.tuils.libsuperuser.ShellHolder getShellHolder() { return null; }
    @Override public bhupendra.ai.launcher.managers.RssManager getRssManager() { return null; }
    @Override public okhttp3.OkHttpClient getHttpClient() { return null; }
    @Override public bhupendra.ai.launcher.ai.AISubsystem getAiSubsystem() { return null; }
    @Override public int getCommandColor() { return 0; }
    @Override public void setCommandColor(int color) {}
    @Override public void dispose() {}
    @Override public void destroy() {}
    @Override public Context getContext() { return context; }

}
