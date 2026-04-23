package bhupendra.ai.launcher.commands.main;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.io.File;
import java.lang.reflect.Method;

import bhupendra.ai.launcher.commands.CommandGroup;
import bhupendra.ai.launcher.commands.CommandsPreferences;
import bhupendra.ai.launcher.commands.ExecutePack;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.TerminalManager;
import bhupendra.ai.launcher.managers.flashlight.TorchManager;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;
import bhupendra.ai.launcher.tuils.interfaces.Redirectator;
import bhupendra.ai.launcher.tuils.libsuperuser.ShellHolder;
import okhttp3.OkHttpClient;

/**
 * Created by francescoandreuzzi on 24/01/2017.
 */

public class MainPack extends ExecutePack {

    //	current directory
    public File currentDirectory;

    //	resources references
    public Resources res;

    //	internet
    public WifiManager wifi;

    //	3g/data
    public Method setMobileDataEnabledMethod;
    public ConnectivityManager connectivityMgr;
    public Object connectMgr;

    //	contacts
    public ContactManager contacts;

    //	music
    public MusicManager2 player;

    //	apps & assocs
    public AliasManager aliasManager;
    public AppsManager appsManager;

    public CommandsPreferences cmdPrefs;

    public String lastCommand;

    public Redirectator redirectator;

    public ShellHolder shellHolder;

    public RssManager rssManager;

    public OkHttpClient client;

    public int commandColor = TerminalManager.NO_COLOR;

    public bhupendra.ai.launcher.ai.AISubsystem aiSubsystem;

    public MainPack(Context context, CommandGroup commandGroup, AliasManager alMgr, AppsManager appmgr, MusicManager2 p,
                    ContactManager c, Redirectator redirectator, RssManager rssManager, OkHttpClient client) {
        super(commandGroup);

        this.currentDirectory = XMLPrefsManager.get(File.class, Behavior.home_path);

        this.rssManager = rssManager;

        this.client = client;

        this.res = context.getResources();

        this.context = context;

        this.aliasManager = alMgr;
        this.appsManager = appmgr;

        this.cmdPrefs = new CommandsPreferences();

        this.player = p;
        this.contacts = c;

        this.redirectator = redirectator;
    }

    public void dispose() {
        TorchManager mgr = TorchManager.getInstance();
        if(mgr.isOn()) mgr.turnOff();
    }

    public void destroy() {
        if(player != null) player.destroy();
        appsManager.onDestroy();
        if(rssManager != null) rssManager.dispose();
        contacts.destroy(context);
    }

    @Override
    public void clear() {
        super.clear();

        commandColor = TerminalManager.NO_COLOR;
    }

    @Override public Context getContext() { return context; }
    @Override public Resources getResources() { return res; }
    @Override public File getCurrentDirectory() { return currentDirectory; }
    @Override public void setCurrentDirectory(File directory) { this.currentDirectory = directory; }
    @Override public WifiManager getWifiManager() { return wifi; }
    @Override public ConnectivityManager getConnectivityManager() { return connectivityMgr; }
    @Override public Object getConnectMgr() { return connectMgr; }
    @Override public ContactManager getContactManager() { return contacts; }
    @Override public MusicManager2 getMusicManager() { return player; }
    @Override public AliasManager getAliasManager() { return aliasManager; }
    @Override public AppsManager getAppsManager() { return appsManager; }
    @Override public bhupendra.ai.launcher.commands.CommandsPreferences getCmdPrefs() { return cmdPrefs; }
    @Override public String getLastCommand() { return lastCommand; }
    @Override public void setLastCommand(String command) { this.lastCommand = command; }
    @Override public Redirectator getRedirectator() { return redirectator; }
    @Override public ShellHolder getShellHolder() { return shellHolder; }
    @Override public RssManager getRssManager() { return rssManager; }
    @Override public OkHttpClient getHttpClient() { return client; }
    @Override public bhupendra.ai.launcher.ai.AISubsystem getAiSubsystem() { return aiSubsystem; }
    @Override public int getCommandColor() { return commandColor; }
    @Override public void setCommandColor(int color) { this.commandColor = color; }

}
