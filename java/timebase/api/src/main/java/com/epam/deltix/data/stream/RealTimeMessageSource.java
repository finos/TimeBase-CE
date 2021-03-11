package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;

/**
 * Message source that has indication of switching in real-time mode from historical.
 * 
 * Message source will emit RealTimeStartMessage when when realTimeAvailable() is true and
 * no ACTUAL data available at this moment:
 * for DURABLE streams - data files does not contain unread data,
 * for TRANSIENT streams - message buffer is exhausted.
 *
 * After invoking "reset()"  RealTimeMessageSource will produce at most one RealTimeStartMessage.
 */

public interface RealTimeMessageSource<T> extends MessageSource<T> {

    /**
     * @return true if this source already switched from historical to real-time data portion
     */
    boolean     isRealTime();

    /**
     *  @return true if source can be switched in real-time.
     *
     *  When realtime mode is available client can use method {@link #isRealTime()} ()}
     *  to detect switch from historical to real-time portion of data.
     *  Also in this mode client will receive special message {@link RealTimeStartMessage} .
     */
    boolean     realTimeAvailable();
}
