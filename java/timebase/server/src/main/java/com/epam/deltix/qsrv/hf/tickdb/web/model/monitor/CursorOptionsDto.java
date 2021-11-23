package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;

public class CursorOptionsDto {

    private boolean raw;
    private boolean live;
    private boolean reversed;
    private boolean unordered;
    private boolean realTime;
    private ChannelPerformance channelPerformance;

    public boolean isRaw() {
        return raw;
    }

    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    public boolean isLive() {
        return live;
    }

    public void setLive(boolean live) {
        this.live = live;
    }

    public boolean isReversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public boolean isUnordered() {
        return unordered;
    }

    public void setUnordered(boolean unordered) {
        this.unordered = unordered;
    }

    public boolean isRealTime() {
        return realTime;
    }

    public void setRealTime(boolean realTime) {
        this.realTime = realTime;
    }

    public ChannelPerformance getChannelPerformance() {
        return channelPerformance;
    }

    public void setChannelPerformance(ChannelPerformance channelPerformance) {
        this.channelPerformance = channelPerformance;
    }
}
