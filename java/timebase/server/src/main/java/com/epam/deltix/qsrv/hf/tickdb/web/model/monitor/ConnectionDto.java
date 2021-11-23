package com.epam.deltix.qsrv.hf.tickdb.web.model.monitor;

public class ConnectionDto {

    private String clientId;
    private String applicationId;
    private long creationDate;
    private int numTransportChannels;
    private long throughput;
    private double averageThroughput;
    private String remoteAddress;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public int getNumTransportChannels() {
        return numTransportChannels;
    }

    public void setNumTransportChannels(int numTransportChannels) {
        this.numTransportChannels = numTransportChannels;
    }

    public long getThroughput() {
        return throughput;
    }

    public void setThroughput(long throughput) {
        this.throughput = throughput;
    }

    public double getAverageThroughput() {
        return averageThroughput;
    }

    public void setAverageThroughput(double averageThroughput) {
        this.averageThroughput = averageThroughput;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }
}
