package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

import com.epam.deltix.util.vsocket.VSChannelState;

public class VirtualChannelDto {

    private int localId;
    private int remoteId;
    private VSChannelState state;
    private boolean isAutoFlush;

    public int getLocalId() {
        return localId;
    }

    public void setLocalId(int localId) {
        this.localId = localId;
    }

    public int getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(int remoteId) {
        this.remoteId = remoteId;
    }

    public VSChannelState getState() {
        return state;
    }

    public void setState(VSChannelState state) {
        this.state = state;
    }

    public boolean isAutoFlush() {
        return isAutoFlush;
    }

    public void setAutoFlush(boolean autoFlush) {
        isAutoFlush = autoFlush;
    }
}
