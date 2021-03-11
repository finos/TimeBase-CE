package com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings;

/**
 * Warning: This API is a "work in progress" (WIP) and is likely to change.
 *
 * @author Alexei Osipov
 */
public class MulticastTopicSettings {
    // (optional) Multicast group IP address or a hostname that resolves to a multicast IP.
    private String endpointHost;

    // (optional) Port for multicast
    private Integer endpointPort;

    // (optional) Network interface for sending messages (from publisher) and receiving messages (by consumer)/
    private String networkInterface;

    // (optional) TTL for multicast packets.
    private Integer ttl;


    public MulticastTopicSettings() {
    }

    public String getEndpointHost() {
        return endpointHost;
    }

    public void setEndpointHost(String endpointHost) {
        this.endpointHost = endpointHost;
    }

    public Integer getEndpointPort() {
        return endpointPort;
    }

    public void setEndpointPort(Integer endpointPort) {
        this.endpointPort = endpointPort;
    }

    public String getNetworkInterface() {
        return networkInterface;
    }

    public void setNetworkInterface(String networkInterface) {
        this.networkInterface = networkInterface;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(Integer ttl) {
        this.ttl = ttl;
    }
}
