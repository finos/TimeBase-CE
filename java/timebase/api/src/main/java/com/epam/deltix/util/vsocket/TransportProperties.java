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


/**
 *
 */
public class TransportProperties {
    //public static final String      TRANSPORT_DIR_DEF = Home.getPath("temp/dxipc");

    public final TransportType      transportType;
    public final String             transportDir;

//    public TransportProperties() {
//        this(TransportType.SOCKET_TCP);
//    }

//    public TransportProperties(TransportType transportType) {
//        this(transportType, TRANSPORT_DIR_DEF);
//    }

    public TransportProperties(TransportType transportType, String transportDir) {
        this.transportType = transportType;
        this.transportDir = transportDir;
    }
}