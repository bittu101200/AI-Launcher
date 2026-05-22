package bhupendra.ai.launcher.di;

import dagger.hilt.EntryPoint;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import bhupendra.ai.launcher.managers.AppsManager;
import bhupendra.ai.launcher.managers.ContactManager;
import bhupendra.ai.launcher.managers.AliasManager;
import bhupendra.ai.launcher.managers.RssManager;
import bhupendra.ai.launcher.managers.HTMLExtractManager;
import bhupendra.ai.launcher.managers.music.MusicManager2;
import okhttp3.OkHttpClient;
import androidx.annotation.Nullable;

@EntryPoint
@InstallIn(SingletonComponent.class)
public interface ManagerEntryPoint {
    AppsManager appsManager();
    @Nullable ContactManager contactManager();
    AliasManager aliasManager();
    RssManager rssManager();
    HTMLExtractManager htmlExtractManager();
    @Nullable MusicManager2 musicManager2();
    OkHttpClient okHttpClient();
}
