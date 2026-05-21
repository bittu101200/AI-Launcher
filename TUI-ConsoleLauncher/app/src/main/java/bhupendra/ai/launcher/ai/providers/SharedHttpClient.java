package bhupendra.ai.launcher.ai.providers;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

public class SharedHttpClient {

    private static final OkHttpClient SHARED_CLIENT = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();

    public static OkHttpClient get() {
        return SHARED_CLIENT;
    }
}
