/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.replication;

import com.epam.deltix.timebase.messages.IdentityKey;

/**
 *
 */
public class CommonOptions {

    /**
     * Entities for the replication/backup
     */
    public IdentityKey[] entities; // selected entities

    /**
     * Message types for the replication/backup
     */
    public String[]             types; // selected entities

    /**
     * Time range for the replication/backup
     */
    public long[]               range;

    /**
     * Reload mode for the replication/backup. 'allow' by default
     */
    public ReloadMode           mode = ReloadMode.allow;

    /**
     * Number of reconnect attempts. 0 by default.
     */
    public int                  retries = 0;

    /**
     * Period of time in milliseconds between reconnect attempts. 5000 ms by default.
     */
    public long                 retryTimeout = 5000;

    /**
     * Run replication process asynchronously in separate thread
     */
    public boolean              async = false;

    /**
     * Low latency mode. if true, enables ChannelPerformance.LOW_LATENCY for SelectionOptions.channelPerformance;
     */

    public boolean              lowLatency = false;

    /**
     * Replication space
     */

    public String               space;
}