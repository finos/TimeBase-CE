package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.util.memory.MemoryDataInput;

/**
 *  Processes a message.
 */
public interface TSMessageConsumer {
    /**
     *  Process next message.
     * 
     * @param entity
     * @param timestampNanos    Message timestamp. All times are in nanoseconds.
     * @param type
     * @param bodyLength
     * @param mdi 
     */
    void     process (
        int                 entity,
        long                timestampNanos, 
        int                 type,
        int                 bodyLength,
        MemoryDataInput     mdi
    );

    /**
     * Notifies consumer about the end of historical data portion
     *
     * @return true if consumer supports real time mode notification and will emit RealTimeStartMessage as the next message from this source.
     */
    boolean     processRealTime(long timestampNanos);

    /**
     * @return true if this source already switched from historical to real-time data portion (Only when {@link #realTimeAvailable()}).
     */
    boolean     isRealTime();

   /**
     *  @return true if source can be switched in real-time.
     *
     *  When realtime mode is available client can use method {@link #isRealTime()} ()}
     *  to detect switch from historical to real-time portion of data.
    *  Also in this mode client will receive special message {@link RealTimeStartMessage} .
     */
    boolean realTimeAvailable();
}
