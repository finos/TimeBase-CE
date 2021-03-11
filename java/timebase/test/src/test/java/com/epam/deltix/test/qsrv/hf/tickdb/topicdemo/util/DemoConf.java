package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util;

import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.util.time.TimeKeeper;
import org.agrona.concurrent.BusySpinIdleStrategy;
import org.agrona.concurrent.IdleStrategy;

/**
 * @author Alexei Osipov
 */
public class DemoConf {
    public static final String DEMO_MAIN_TOPIC = "demoTopic";
    public static final String DEMO_ECHO_TOPIC = "demoTopicEcho";

    public static final String DEMO_MAIN_STREAM = "demoStream";
    public static final String DEMO_ECHO_STREAM = "demoStreamEcho";

    public static final int FRACTION_OF_MARKED = Integer.getInteger("markedFraction", 1);

    public static final TimeSourceType LATENCY_CLOCK_TYPE = TimeSourceType.SYSTEM_NANO_TIME;

    public static final int TARGET_MESSAGE_SIZE = Integer.getInteger("messageSize", 100);

    public static final StreamScope STREAM_SCOPE = StreamScope.TRANSIENT;

    static {
        if (LATENCY_CLOCK_TYPE == TimeSourceType.TIME_KEEPER_HIGH_PRECISION) {
            TimeKeeper.setMode(TimeKeeper.Mode.HIGH_RESOLUTION_SYNC_BACK);
        }
    }

    public static IdleStrategy getReaderIdleStrategy() {
        // This test just spins where there is no data.
        return new BusySpinIdleStrategy();
    }

}
