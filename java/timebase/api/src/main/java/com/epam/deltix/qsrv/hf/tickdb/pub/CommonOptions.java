package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.qsrv.hf.pub.*;

/**
 * Provides fields common for SelectionOptions and LoadingOptions 
 */
public abstract class CommonOptions extends ChannelPreferences {

    /**
     * Channel quality of service setting.
     */
    public ChannelQualityOfService  channelQOS = ChannelQualityOfService.MAX_THROUGHPUT;

    public ChannelCompression       compression = ChannelCompression.AUTO;

    public int                      channelBufferSize = 0; // 0 means default value

    protected void copy(CommonOptions template) {
        this.typeLoader = template.typeLoader;
        this.channelQOS = template.channelQOS;
        this.channelPerformance = template.channelPerformance;
        this.channelBufferSize = template.channelBufferSize;
    }
}
