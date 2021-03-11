package com.epam.deltix.qsrv.hf.pub.monitor;

import com.epam.deltix.util.lang.Disposable;

public interface LatencyMonitorService extends Disposable {

    void measure(LatencyMetric metric, long signalId, long measureTimeNanos, long latencyNanos, String exchange, String moduleKey);
}
