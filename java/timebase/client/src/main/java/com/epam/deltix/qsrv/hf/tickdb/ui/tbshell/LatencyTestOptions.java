package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;

/**
 *
 */
public class LatencyTestOptions {
    public DXTickStream stream;

    public int messageDataSize = Integer.getInteger("LatencyTestOptions.messageDataSize", 20);
    public int throughput = Integer.getInteger("LatencyTestOptions.throughput", 20_000);
    public int numConsumers = Integer.getInteger("LatencyTestOptions.numConsumers", 1);
    public int warmupSize = Integer.getInteger("LatencyTestOptions.warmupSize", 200_000);
    public int messagesPerLaunch = Integer.getInteger("LatencyTestOptions.messagesPerLaunch", 100_000);
    public int launches = Integer.getInteger("LatencyTestOptions.launches", 10);
}