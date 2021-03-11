package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer.VirtualTimePeriodicTaskExecutor;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

/**
 * @author Alexei Osipov
 */
public class VirtualTimePeriodicTaskExecutorTest {


    @Test
    public void testTaskExecutionSpeed() throws Exception {
        int speed1 = 5;
        AtomicInteger counter = new AtomicInteger(0);
        VirtualTimePeriodicTaskExecutor virtualClock = new VirtualTimePeriodicTaskExecutor(speed1, 1000, virtualCLockTime -> counter.incrementAndGet(), null, null);
        virtualClock.startFromTimestamp(0);

        Thread.sleep(2000);

        virtualClock.stop();
        int counterValue = counter.get();
        System.out.println("counterValue: " + counterValue);
        assertTrue("Expected 10, got " + counterValue, 9 <= counterValue && counterValue <= 11);
    }

}