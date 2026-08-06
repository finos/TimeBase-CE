package com.epam.deltix.util.vsocket;

/*
 * Copyright 2026 EPAM Systems, Inc
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
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Experimental
public class VSClientOptions {
    // Init fields with default values

    private int socketSendBufferSize = VSocketImpl.SOCKET_SEND_BUFFER_SIZE;
    private int socketReceiveBufferSize = VSocketImpl.SOCKET_RECEIVE_BUFFER_SIZE;
    private boolean sslTermination = VSClient.SSL_TERMINATION;
    private int transportReconnectAttemptInterval = Integer.getInteger("TimeBase.network.VSClient.transportReconnectAttemptInterval", 1000);

    // Controls socket timeout ("soTimeout") during the initial handshake. After handshake is complete, soTimeout is always "0" (infinite).
    private int handshakeSocketTimeout = Integer.getInteger("TimeBase.network.VSClient.soTimeout", 5000);
    private int socketConnectTimeout = Integer.getInteger("TimeBase.network.VSClient.timeout", 5000);


    public int getSocketReceiveBufferSize() {
        return socketReceiveBufferSize;
    }

    public void setSocketReceiveBufferSize(int socketReceiveBufferSize) {
        this.socketReceiveBufferSize = socketReceiveBufferSize;
    }

    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
    }

    public void setSocketSendBufferSize(int socketSendBufferSize) {
        this.socketSendBufferSize = socketSendBufferSize;
    }

    public int getHandshakeSocketTimeout() {
        return handshakeSocketTimeout;
    }

    public void setHandshakeSocketTimeout(int handshakeSocketTimeout) {
        this.handshakeSocketTimeout = handshakeSocketTimeout;
    }

    public boolean isSslTermination() {
        return sslTermination;
    }

    public void setSslTermination(boolean sslTermination) {
        this.sslTermination = sslTermination;
    }

    public int getSocketConnectTimeout() {
        return socketConnectTimeout;
    }

    public void setSocketConnectTimeout(int socketConnectTimeout) {
        this.socketConnectTimeout = socketConnectTimeout;
    }

    public int getTransportReconnectAttemptInterval() {
        return transportReconnectAttemptInterval;
    }

    public void setTransportReconnectAttemptInterval(int transportReconnectAttemptInterval) {
        this.transportReconnectAttemptInterval = transportReconnectAttemptInterval;
    }
}
