package bhupendra.ai.launcher.commands;

import android.content.Context;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;

import java.io.File;

import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.tuils.interfaces.Redirectator;
import bhupendra.ai.launcher.tuils.libsuperuser.ShellHolder;
import okhttp3.OkHttpClient;

public interface CommandContext {
    Context getContext();
    Resources getResources();
    File getCurrentDirectory();
    void setCurrentDirectory(File directory);
    
    WifiManager getWifiManager();
    ConnectivityManager getConnectivityManager();
    Object getConnectMgr();
    
    ContactManager getContactManager();
    MusicManager2 getMusicManager();
    AliasManager getAliasManager();
    AppsManager getAppsManager();
    CommandsPreferences getCmdPrefs();
    
    String getLastCommand();
    void setLastCommand(String command);
    
    Redirectator getRedirectator();
    ShellHolder getShellHolder();
    RssManager getRssManager();
    OkHttpClient getHttpClient();
    
    AISubsystem getAiSubsystem();
    
    int getCommandColor();
    void setCommandColor(int color);
    
    void dispose();
    void destroy();
}
