package com.epam.deltix.qsrv.util.time;

import com.epam.deltix.qsrv.hf.pub.TimeSource;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.TimerRunner;

/**
 * @author Andy
 *         Date: Jul 14, 2010 1:10:14 PM
 *
 * see http://www.ibm.com/developerworks/library/i-seconds/
 * see http://msdn.microsoft.com/en-us/magazine/cc163996.aspx
 */
public final class PreciseTimeSource extends TimerRunner implements TimeSource {

    private static final long NANOS_IN_MILLISECOND = java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(1);

    private static final long DEFAULT_TIMER_RESOLUTION = Util.getLongSystemProperty("UHF.preciseTime.defaultResolution", 10, 0, 10000);
    private static final long SYNC_INTERVAL = Util.getLongSystemProperty("UHF.preciseTime.syncInterval", 60000, 0, Long.MAX_VALUE);

    private final long baseTimeMilliseconds;
    private volatile long baseTimeNanos;

    public PreciseTimeSource () {
        baseTimeMilliseconds = defineBaseTime(true);

        // We need to resync with PC clock periodically to compensate for nanoTime drift
        if (SYNC_INTERVAL > 0)
            GlobalTimer.INSTANCE.schedule(this, SYNC_INTERVAL, SYNC_INTERVAL);
    }


    @Override
    protected void runInternal() throws Exception {
        defineBaseTime(false);
    }

    private long defineBaseTime(boolean initialCall) {


        while (true) {
            long nanos0 = System.nanoTime();
            long millis0 = System.currentTimeMillis();

            long nanosSpendInsideInnerLoop = 0;

            while (true) {
                long millis1 = System.currentTimeMillis();
                long nanos1 = System.nanoTime();

                final long deltaMillis = millis1 - millis0;
                if (deltaMillis == 0) {
                    nanosSpendInsideInnerLoop = nanos1 - nanos0;
                    nanos0 = nanos1;
                    continue; // inner loop
                }
                if (deltaMillis > DEFAULT_TIMER_RESOLUTION)
                    break;

                long accuracy = (nanos1 - nanos0 + nanosSpendInsideInnerLoop);
                if (accuracy > 10000000)
                    break;


                if (initialCall) {
                    //baseTimeMilliseconds = millis1;
                    baseTimeNanos = nanos1 - accuracy / 2;
                } else {
                    //millis1 = (baseTimeMilliseconds + (nanos1 - baseTimeNanos + driftNanos) / NANOS_IN_MILLISECOND);
                    baseTimeNanos+= (millis1 - baseTimeMilliseconds)*NANOS_IN_MILLISECOND  + baseTimeNanos - nanos1;
                }
                return millis1;
            }
        }
    }


    @Override
    public long currentTimeMillis() {
        return baseTimeMilliseconds + (System.nanoTime() - baseTimeNanos) / NANOS_IN_MILLISECOND;
    }

    public String toString() {
        return "baseTimeMilliseconds: " + baseTimeMilliseconds + " baseTimeNanos: " + (System.nanoTime() - baseTimeNanos)/ NANOS_IN_MILLISECOND ;
    }
}
