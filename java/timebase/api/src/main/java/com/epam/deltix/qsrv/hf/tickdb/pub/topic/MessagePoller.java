package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.util.lang.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Executes processing of messages from topic.
 *
 * Please note that {@link #close()} method must be called from the same treas as {@link #processMessages}
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public interface MessagePoller extends Disposable {
    /**
     *
     * Executes non-blocking message processing.
     * If there are no messages available then this call returns 0 (without blocking).
     * <p>
     * IMPORTANT: Don't create a new processor instance for each call. Create it once outside the polling loop.
     *
     * @param messageCountLimit maximum number of messages to process during this method call
     * @param messageProcessor processor for messages.
     * @return number of messages to process.
     */
    int processMessages(int messageCountLimit, @Nonnull MessageProcessor messageProcessor);

    /**
     * Provides estimate of amount of data in the incoming message buffer.
     * <p>
     * Note: value 0 does not necessary mean 0 incoming messages. Don't use this method to check if there messages to process.
     * <p>
     * WARNING: This operation is relatively slow. Do not use it for each message. It's a good practice to call this method only once per 100 or 1000 messages.
     *
     * @return number in range from 0 to 100. 0 means that buffer is close to empty and 100 mean that buffer is full.
     */
    byte getBufferFillPercentage();
}
