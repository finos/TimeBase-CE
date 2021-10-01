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
package com.epam.deltix.qsrv.hf.pub;


import com.epam.deltix.qsrv.hf.tickdb.pub.InsufficientCpuResourcesException;

public enum ChannelPerformance {

    /**
     * Prefer to minimize CPU usage. Don't do anything extra to minimize latency.
     */
    MIN_CPU_USAGE,

    /**
     * Prefer to minimize latency at the expense of higher CPU usage (one CPU core per process)
     */
    LOW_LATENCY,

    /**
     * Focus on minimizing latency even if this means <b>heavy</b> load on CPU.
     * <p>
     * Note: you <b>must</b> have free CPU cores both on client and server for that to be useful.
     * Without that latency may be higher instead.
     * Use {@link #LOW_LATENCY} if not sure.
     *
     * <ul>
     * <li>Each cursor and loader in this mode will fully consume (fully load) one CPU core on client and one CPU core on TimeBase server.</li>
     * <li>TimeBase server will limit number of clients that use LATENCY_CRITICAL mode.
     * So TimeBase server may reject request for cursor (by throwing {@link InsufficientCpuResourcesException} )
     * if there are already too many LATENCY_CRITICAL cursors or loaders.</li>
     * </ul>
     */
    LATENCY_CRITICAL,

    /**
     * Prefer to maximize messages throughput. For loopback connections IPC communication will be used.
     */
    HIGH_THROUGHPUT;

    public boolean isLowLatency() {
        return this.equals(LOW_LATENCY) || this.equals(LATENCY_CRITICAL);
    }

    public void toDetailedString() {

    }
}