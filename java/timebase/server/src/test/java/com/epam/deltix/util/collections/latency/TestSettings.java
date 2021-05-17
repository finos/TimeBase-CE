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
package com.epam.deltix.util.collections.latency;

import com.epam.deltix.util.vsocket.TransportType;

/**
 *
 */
public interface TestSettings {
    static boolean USE_VSOCKET = true;
    static TransportType TRANSPORT_TYPE = TransportType.OFFHEAP_IPC;
    static boolean AERON_UDP = false;

    static boolean LAUNCH_SERVER_THREAD = true;

    static String AERON_UDP_URL = "udp://localhost:50123";

    static String CLIENT_FILE = "client.tmp";
    static String SERVER_FILE = "server.tmp";

    static int THROUGHPUT = Integer.getInteger("throughput", 10000);
    static int REPS = Integer.getInteger("reps", 100000);
    static int WARMUP = Integer.getInteger("warmup", 100000);

    static int PORT = Integer.getInteger("port", 7788);
}
