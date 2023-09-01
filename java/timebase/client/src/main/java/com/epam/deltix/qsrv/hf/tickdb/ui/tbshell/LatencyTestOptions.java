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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;

/**
 *
 */
public class LatencyTestOptions {
    public DXTickStream stream;

    public int messageDataSize = Integer.getInteger("LatencyTestOptions.messageDataSize", 20);
    public int throughput = Integer.getInteger("LatencyTestOptions.throughput", 20_000);
    public int numConsumers = Integer.getInteger("LatencyTestOptions.numConsumers", 1);
    public int warmupSize = Integer.getInteger("LatencyTestOptions.warmupSize", 200_000);
    public int messagesPerLaunch = Integer.getInteger("LatencyTestOptions.messagesPerLaunch", 100_000);
    public int launches = Integer.getInteger("LatencyTestOptions.launches", 10);
}