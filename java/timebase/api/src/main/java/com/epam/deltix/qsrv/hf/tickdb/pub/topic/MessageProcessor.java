package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.timebase.messages.InstrumentMessage;

/**
 * Interface for code that processes single {@link InstrumentMessage}.
 *
 * @author Alexei Osipov
 */
@FunctionalInterface
public interface MessageProcessor {
    /**
     * Process single message.
     * Method implementations should not block.
     *
     * Method should not throw any exceptions unless it want to abort any further processing.
     *
     * @param message message to process
     */
    void process(InstrumentMessage message);
}
