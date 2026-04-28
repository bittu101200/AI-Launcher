package bhupendra.ai.launcher.managers;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InternetSpeedTestManagerTest {

    @Test
    public void formatThroughputUsesMbpsAndMBps() {
        assertEquals("16.00 Mbps (2.00 MB/s)", InternetSpeedTestManager.formatThroughput(2_000_000, 1000));
    }

    @Test
    public void formatThroughputReturnsZeroForInvalidDuration() {
        assertEquals("0 Mbps", InternetSpeedTestManager.formatThroughput(10_000, 0));
    }
}
