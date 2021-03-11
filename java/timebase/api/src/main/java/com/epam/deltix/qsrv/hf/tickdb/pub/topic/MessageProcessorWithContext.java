package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 * Interface for code that processes single {@link InstrumentMessage}.
 *
 * @author Alexei Osipov
 */
@FunctionalInterface
public interface MessageProcessorWithContext<T> {
    /**
     * Process single message.
     * Method implementations should not block.
     *
     * @param message message to process
     * @param context additional context
     */
    void process(InstrumentMessage message, T context);
}
