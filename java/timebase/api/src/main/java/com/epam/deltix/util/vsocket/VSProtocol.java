/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.util.vsocket;

import java.util.logging.Logger;

/**
 *
 */
public class VSProtocol {
    public static final Logger      LOGGER = Logger.getLogger ("deltix.vsocket");

    public static final int         VERSION = 1015;
    public static final int         KEEP_ALIVE_INTERVAL = 1000;

    public static final int         HEADER = 0xD1;
    public static final int         SSL_HEADER = 0xD2;

    public static final int         CONN_RESP_OK = 0;
    public static final int         CONN_RESP_INCOMPATIBLE_CLIENT = 1;
    public static final int         CONN_RESP_SSL_NOT_SUPPORTED = 2;
    public static final int         CONN_RESP_CONNECTION_REJECTED = 3;

    static final int                LISTENER_ID = 0xFFFF;
    static final int                KEEP_ALIVE = 0xFFFE;
    static final int                PING = 0xFFFD;

    static final int                MAXSIZE = 0xFF00; // max packet size to send
    static final int                MINSIZE = 7; // min packet size to send    

    static final int                CONNECT_ACK = MAXSIZE + 1;
    static final int                CLOSING = MAXSIZE + 2;
    static final int                CLOSED = MAXSIZE + 3;
    static final int                BYTES_AVAILABLE_REPORT = MAXSIZE + 4;
    static final int                DISPATCHER_CLOSE = MAXSIZE + 5;
    static final int                BYTES_RECIEVED = MAXSIZE + 6;
    //static final int                BYTES_READ = MAXSIZE + 7;

    static final long               SHUTDOWN_TIMEOUT = 5000;
    static final long               RECONNECT_TIMEOUT = 500;
    public static final int         LINGER_INTERVAL = 10000;
    public static final int         IDLE_TIME = 20000;

    public static final int         CHANNEL_BUFFER_SIZE = 1 << 17; // optimized for local connections
    public static final int         CHANNEL_MAX_BUFFER_SIZE = 1 << 19; // optimized for remote connections

    public static int               getIdleTime() {

        String delay = System.getProperty("VSProtocol.idleTime");
        try {
            return delay != null ? Integer.parseInt(delay) : IDLE_TIME;
        } catch (NumberFormatException e) {
            return IDLE_TIME;
        }
    }

    public static int               getHeader(boolean ssl) {
        return ssl ? SSL_HEADER : HEADER;
    }
}