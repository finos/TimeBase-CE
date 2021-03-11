package com.epam.deltix.test.qsrv.hf.tickdb.perfomancedrop;

/**
 * Config for test DB that contains data for the problem.
 * @author Alexei Osipov
 */
class LocalTestBaseConfig {
    static final String STREAM_KEY = "ticks"; // Name of main stream.
    static final String TRANSIENT_KEY = "live_trans42"; // Name of transient stream with live messages.
    static final String HOME = "C:\\dev\\Russell3000_SSD\\timebase"; // TimBase location. Must contain stream "ticks".
}
