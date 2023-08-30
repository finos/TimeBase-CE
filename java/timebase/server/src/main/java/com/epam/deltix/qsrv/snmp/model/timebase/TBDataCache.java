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
package com.epam.deltix.qsrv.snmp.model.timebase;

import com.epam.deltix.snmp.pub.*;

/**
 *
 */
public interface TBDataCache {
    @Id(1)
    @Description ("Total (maximum) data cache size, in MB")
    public int      getCacheSize();

    @Id(2)
    @Description ("Used data cache size, in MB")
    public int      getUsedCacheSize();

    @Id(3)
    @Description ("Number of allocated pages")
    public int      getNumPages();

    @Id(4)
    @Description ("Number of opened files")
    public int      getNumOpenFiles();

    @Id(5)
    @Description ("Number of IO write bytes")
    public int      getNumWriteBytes();

    @Id(6)
    @Description ("Number of IO read bytes")
    public int      getNumReadBytes();

    @Id(7)
    @Description ("Write queue length")
    public int      getWriteQueueLength();

    @Id(8)
    @Description ("Writer Thread state")
    public String   getWriterState();

    @Id(9)
    @Description ("Number of IO failures")
    public Table<Failure> getIOFailures();
}