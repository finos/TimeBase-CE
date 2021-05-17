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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

/**
 * @author Alexei Osipov
 */
public enum BenchmarkChannelType {
    DURABLE_STREAM("dstream", "Durable stream"),
    TRANSIENT_STREAM("tstream", "Transient stream"),
    TOPIC("ipctopic", "Topic IPC"),
    UDP_SINGLE_PRODUCER_TOPIC("udpsingletopic", "Topic UDP Single Producer");

    private final String key;
    private final String name;

    BenchmarkChannelType(String key, String name) {
        this.key = key;
        this.name = name;
    }

    public static BenchmarkChannelType getByKey(String key) {
        for (BenchmarkChannelType value : values()) {
            if (value.getKey().equalsIgnoreCase(key)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid type key: " + key);
    }

    public String getKey() {
        return key;
    }

    public String getName() {
        return name;
    }
}
