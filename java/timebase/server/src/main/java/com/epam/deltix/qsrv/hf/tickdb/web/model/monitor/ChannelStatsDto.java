package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class ChannelStatsDto {

    private long totalNumMessages;
    private long lastMessageTimestamp;
    private long lastMessageSysTimestamp;

    public long getTotalNumMessages() {
        return totalNumMessages;
    }

    public void setTotalNumMessages(long totalNumMessages) {
        this.totalNumMessages = totalNumMessages;
    }

    public long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public long getLastMessageSysTimestamp() {
        return lastMessageSysTimestamp;
    }

    public void setLastMessageSysTimestamp(long lastMessageSysTimestamp) {
        this.lastMessageSysTimestamp = lastMessageSysTimestamp;
    }
}
