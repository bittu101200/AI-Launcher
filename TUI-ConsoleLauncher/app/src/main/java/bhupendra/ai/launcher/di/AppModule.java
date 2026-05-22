package bhupendra.ai.launcher.di;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

import javax.inject.Singleton;

import bhupendra.ai.launcher.managers.FileSystemManager;
import bhupendra.ai.launcher.managers.DeviceStateManager;
import bhupendra.ai.launcher.managers.TextProcessor;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.HTMLExtractManager;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import bhupendra.ai.launcher.ai.ToolRegistry;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

import androidx.annotation.Nullable;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;
import bhupendra.ai.launcher.managers.xml.options.Behavior;

@Module
@InstallIn(SingletonComponent.class)
public class AppModule {

    @Provides
    @Singleton
    public FileSystemManager provideFileSystemManager() {
        return new FileSystemManager();
    }

    @Provides
    @Singleton
    public DeviceStateManager provideDeviceStateManager() {
        return new DeviceStateManager();
    }

    @Provides
    @Singleton
    public TextProcessor provideTextProcessor() {
        return new TextProcessor();
    }

    @Provides
    @Singleton
    public ToolRegistry provideToolRegistry() {
        return new ToolRegistry();
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(@ApplicationContext Context context) {
        return new OkHttpClient.Builder()
                .cache(new Cache(context.getCacheDir(), 10 * 1024 * 1024))
                .build();
    }

    @Provides
    @Singleton
    public AppsManager provideAppsManager(@ApplicationContext Context context) {
        return new AppsManager(context);
    }

    @Provides
    @Singleton
    @Nullable
    public ContactManager provideContactManager(@ApplicationContext Context context) {
        try {
            return new ContactManager(context);
        } catch (NullPointerException e) {
            return null;
        }
    }

    @Provides
    @Singleton
    public AliasManager provideAliasManager(@ApplicationContext Context context) {
        return new AliasManager(context);
    }

    @Provides
    @Singleton
    public RssManager provideRssManager(@ApplicationContext Context context, OkHttpClient client) {
        return new RssManager(context, client);
    }

    @Provides
    @Singleton
    public HTMLExtractManager provideHTMLExtractManager(@ApplicationContext Context context, OkHttpClient client) {
        return new HTMLExtractManager(context, client);
    }

    @Provides
    @Singleton
    @Nullable
    public MusicManager2 provideMusicManager2(@ApplicationContext Context context) {
        if (XMLPrefsManager.getBoolean(Behavior.enable_music)) {
            return new MusicManager2(context);
        }
        return null;
    }
}
