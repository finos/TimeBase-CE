package com.epam.deltix.qsrv.hf.pub.md;

/**
 * Maintains stream-to-channel mapping
 */
public interface Channel2StreamMapper {
    void define(String channelKey, boolean isInput, String... streamKeys);

    String [] getChannelStream(String channelKey, boolean isInput);

    String getChannelQQL(String channelKey);

    boolean isEmpty();

    String[] defineMissingMapping(String channelKey, boolean isInput);
}
