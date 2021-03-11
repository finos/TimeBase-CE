package com.epam.deltix.qsrv.hf.pub;

/**
 *
 */
public enum ChannelQualityOfService {
    /**
     *  Generate compiled code to maximize the throughput of the channel.
     */
    MAX_THROUGHPUT,

    /**
     *  Minimize initialization time. This setting will minimize or eliminate
     *  the generation of compiled code, somewhat limiting the throughput of
     *  a channel, but significantly speeding up the initialization time.
     */
    MIN_INIT_TIME
}
