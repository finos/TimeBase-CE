package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util;

import com.epam.deltix.util.time.TimeKeeper;

/**
 * @author Alexei Osipov
 */
public class NanoTimeSource {

    public static long getNanos() {
        switch (DemoConf.LATENCY_CLOCK_TYPE) {
            case SYSTEM_NANO_TIME:
                return System.nanoTime();
            case TIME_KEEPER_HIGH_PRECISION:
                return TimeKeeper.currentTimeNanos;
            default:
                throw new IllegalStateException();
        }
    }

}
