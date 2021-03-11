package com.epam.deltix.qsrv.hf.tickdb.comm;

/**
 * Topic-specific channel options.
 *
 * @author Alexei Osipov
 */
public enum TopicChannelOption {
    // Note: names of entries used in serialization. So they should not be altered.

    //MEDIA_TYPE,

    // Single publisher mode
    PUBLISHER_HOST,
    PUBLISHER_PORT,
    SUBSCRIBER_PORT,

    // Multicast
    MULTICAST_ENDPOINT_HOST,
    MULTICAST_ENDPOINT_PORT,
    MULTICAST_TTL,
    MULTICAST_NETWORK_INTERFACE
}
