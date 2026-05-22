package bhupendra.ai.launcher.di;

import android.app.Activity;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.components.ActivityComponent;
import dagger.hilt.android.scopes.ActivityScoped;
import androidx.annotation.Nullable;

import bhupendra.ai.launcher.LauncherActivity;
import bhupendra.ai.launcher.MainManager;
import bhupendra.ai.launcher.tuils.interfaces.Reloadable;
import bhupendra.ai.launcher.managers.ThemeManager;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.HTMLExtractManager;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import okhttp3.OkHttpClient;

@Module
@InstallIn(ActivityComponent.class)
public class ActivityModule {

    @Provides
    @ActivityScoped
    public LauncherActivity provideLauncherActivity(Activity activity) {
        return (LauncherActivity) activity;
    }

    @Provides
    @ActivityScoped
    public Reloadable provideReloadable(Activity activity) {
        return (Reloadable) activity;
    }

    @Provides
    @ActivityScoped
    public ThemeManager provideThemeManager(OkHttpClient client, Activity activity, Reloadable reloadable) {
        return new ThemeManager(client, activity, reloadable);
    }

    @Provides
    @ActivityScoped
    public MainManager provideMainManager(
            LauncherActivity activity,
            @Nullable ContactManager contactManager,
            AppsManager appsManager,
            AliasManager aliasManager,
            RssManager rssManager,
            ThemeManager themeManager,
            @Nullable MusicManager2 musicManager2,
            HTMLExtractManager htmlExtractManager,
            OkHttpClient client
    ) {
        return new MainManager(
                activity,
                contactManager,
                appsManager,
                aliasManager,
                rssManager,
                themeManager,
                musicManager2,
                htmlExtractManager,
                client
        );
    }
}
