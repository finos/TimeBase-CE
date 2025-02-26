package com.epam.deltix.qsrv.hf.tickdb.impl.metrics;

/**
 * Metrics Counter
 */
public interface Counter {
    CharSequence getId();

    long increment();

    long getValue();

    // removes counter from counter manager
    void close();
}
