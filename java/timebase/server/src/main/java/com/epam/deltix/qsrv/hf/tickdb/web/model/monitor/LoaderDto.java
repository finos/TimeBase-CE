package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class LoaderDto {

    private long id;
    private String application;
    private String user;
    private String stream;
    private LoaderOptionsDto options;
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

    public String getStream() {
        return stream;
    }

    public void setStream(String stream) {
        this.stream = stream;
    }

    public LoaderOptionsDto getOptions() {
        return options;
    }

    public void setOptions(LoaderOptionsDto options) {
        this.options = options;
    }

    public ChannelStatsDto getStats() {
        return stats;
    }

    public void setStats(ChannelStatsDto stats) {
        this.stats = stats;
    }
}
