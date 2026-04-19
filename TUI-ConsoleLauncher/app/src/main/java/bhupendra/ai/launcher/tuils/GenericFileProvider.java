package bhupendra.ai.launcher.tuils;

import androidx.core.content.FileProvider;

import bhupendra.ai.launcher.BuildConfig;

public class GenericFileProvider extends FileProvider {
    public static final String PROVIDER_NAME = BuildConfig.APPLICATION_ID + ".FILE_PROVIDER";
}
