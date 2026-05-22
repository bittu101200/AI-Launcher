package bhupendra.ai.launcher;

import android.app.Application;
import dagger.hilt.android.HiltAndroidApp;
import bhupendra.ai.launcher.managers.xml.XMLPrefsManager;

@HiltAndroidApp
public class TUIApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        XMLPrefsManager.loadCommons(this);
    }
}
