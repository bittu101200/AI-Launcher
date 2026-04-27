package bhupendra.ai.launcher.tuils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LauncherExecutors {
    // For command execution logic
    public static final ExecutorService commandExecutor = new ThreadPoolExecutor(0, 4, 30L, TimeUnit.SECONDS, new SynchronousQueue<>());

    // For fast AI and tool execution
    public static final ExecutorService aiExecutor = Executors.newFixedThreadPool(4);

    // For IO heavy background tasks (RSS, Themes, Apps scanning, Regex)
    public static final ExecutorService bgExecutor = Executors.newFixedThreadPool(4);
    
    // For notifications (needs to be responsive)
    public static final ExecutorService notificationExecutor = Executors.newFixedThreadPool(2);
}
