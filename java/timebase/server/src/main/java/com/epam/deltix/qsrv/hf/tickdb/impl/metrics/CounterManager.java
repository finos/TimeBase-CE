package com.epam.deltix.qsrv.hf.tickdb.impl.metrics;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;

/**
 * Counter Manager
 */
public interface CounterManager extends AutoCloseable {
    // Generic counter with number of metrics in snapshot that is always sent last in snapshot
    String SNAPSHOT_COUNTER = "snapshot_metrics_total";

    // starts writing registered metrics to internal metrics# stream every snapshot interval ms
    // each counter will be written in a separate message with counter Id as symbol and
    // the last message in snapshot marked with the boolean isLast attribute
    void open(DXTickDB timebase);

    void close();

    Counter registerCursorCounter(long cursorId, String streamKey, String applicationName);

    Counter registerLoaderCounter(long loaderId, String streamKey, String applicationName);

    Counter registerGenericCounter(String counterId);

    void unregisterCounter(Counter counter);
}
