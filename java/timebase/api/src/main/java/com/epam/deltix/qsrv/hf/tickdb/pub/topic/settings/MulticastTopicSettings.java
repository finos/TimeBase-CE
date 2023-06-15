/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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