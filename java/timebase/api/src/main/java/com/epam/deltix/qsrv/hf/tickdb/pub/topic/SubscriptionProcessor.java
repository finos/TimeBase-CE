package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.util.lang.Disposable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Executes message processing in current thread.
 *
 * Note: thread will be blocked till processing is explicitly stopped by other thread.
 *
 * Only one "process*" method can be used during processors lifecycle.
 *
 * @author Alexei Osipov
 */
public interface SubscriptionProcessor extends Disposable {
    /**
     * Executes message processing (blocks on this method) till processing is stopped from another thread (via call to close()).
     * Closes processor upon completion.
     */
    void processMessagesUntilStopped();

    /**
     * Executes message processing (blocks on this method) while {@code condition} is {@code true}
     * and till processing is stopped from another thread (via call to close()).
     *
     * Closes processor upon completion.
     *
     * @param condition processing will be stopped if this condition returns false. Note: it's not guarantied that condition is checked after each message.
     */
    void processMessagesWhileTrue(BooleanSupplier condition);
}
