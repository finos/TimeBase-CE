/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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