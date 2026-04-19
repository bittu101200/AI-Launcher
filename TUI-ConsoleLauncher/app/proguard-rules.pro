-keep public class bhupendra.ai.launcher.commands.main.raw.** { *; }
-keep public class bhupendra.ai.launcher.commands.main.specific.** { *; }
-keep public class bhupendra.ai.launcher.commands.tuixt.raw.** { *; }
-keep public class bhupendra.ai.launcher.tuils.GenericFileProvider { *; }
-keep public class bhupendra.ai.launcher.tuils.PrivateIOReceiver { *; }
-keep public class bhupendra.ai.launcher.tuils.PublicIOReceiver { *; }
-keep class bhupendra.ai.launcher.managers.** { *; }
-keep class bhupendra.ai.launcher.tuils.libsuperuser.**
-keep class bhupendra.ai.launcher.managers.suggestions.HideSuggestionViewValues
-keep public class it.andreuzzi.comparestring2.**

-dontwarn bhupendra.ai.launcher.commands.main.raw.**

-dontwarn javax.annotation.**
-dontwarn javax.inject.**
-dontwarn sun.misc.Unsafe

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn org.htmlcleaner.**
-dontwarn com.jayway.jsonpath.**
-dontwarn org.slf4j.**

-dontwarn org.jdom2.**

-keep class bhupendra.ai.launcher.ai.** { *; }