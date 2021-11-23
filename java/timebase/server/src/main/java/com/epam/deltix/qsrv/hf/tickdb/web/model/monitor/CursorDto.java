package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class CursorDto {

    private long id;
    private String application;
    private String user;
    private String[] streams;
    private long lastResetTimestamp;
    private CursorOptionsDto options;
    private CursorSubscriptionDto subscription;
    private ChannelStatsDto stats;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String[] getStreams() {
        return streams;
    }

    public void setStreams(String[] streams) {
        this.streams = streams;
    }

    public long getLastResetTimestamp() {
        return lastResetTimestamp;
    }

    public void setLastResetTimestamp(long lastResetTimestamp) {
        this.lastResetTimestamp = lastResetTimestamp;
    }

    public CursorOptionsDto getOptions() {
        return options;
    }

    public void setOptions(CursorOptionsDto options) {
        this.options = options;
    }

    public CursorSubscriptionDto getSubscription() {
        return subscription;
    }

    public void setSubscription(CursorSubscriptionDto subscription) {
        this.subscription = subscription;
    }

    public ChannelStatsDto getStats() {
        return stats;
    }

    public void setStats(ChannelStatsDto stats) {
        this.stats = stats;
    }
}
