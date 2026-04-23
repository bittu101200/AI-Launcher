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
import bhupendra.ai.launcher.ai.AISubsystem;
import bhupendra.ai.launcher.ai.ToolRegistry;

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
}
