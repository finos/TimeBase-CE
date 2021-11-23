package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;

public class LoaderOptionsDto {

    private boolean raw;
    private boolean sorted;
    private ChannelPerformance channelPerformance;

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public boolean isSorted() {
        return sorted;
    }

    public void setSorted(boolean sorted) {
        this.sorted = sorted;
    }

    public ChannelPerformance getChannelPerformance() {
        return channelPerformance;
    }

    public void setChannelPerformance(ChannelPerformance channelPerformance) {
        this.channelPerformance = channelPerformance;
    }
}
