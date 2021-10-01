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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.util.lang.Util;

/**
 * Timebase RAM Disk options.
 */
public class DataCacheOptions {

    public static final long  DEFAULT_CACHE_SIZE = 100*1024*1024;

    public int          maxNumOpenFiles = Util.getIntSystemProperty("TimeBase.dataCache.maxOpenFiles", Integer.MAX_VALUE, 100, Integer.MAX_VALUE);
    public long         cacheSize = Util.getLongSystemProperty("TimeBase.dataCache.size", DEFAULT_CACHE_SIZE);
    public double       preallocateRatio = 0;
    public long         shutdownTimeout = Long.MAX_VALUE;
    public FSOptions    fs = new FSOptions(); // File-System related options

    public DataCacheOptions() {
    }

    public DataCacheOptions(int maxNumOpenFiles, long cacheSize) {
        this(maxNumOpenFiles, cacheSize, 0);
    }

    public DataCacheOptions(int maxNumOpenedFiles, long cacheSize, double preallocateRatio) {
        this.maxNumOpenFiles = maxNumOpenedFiles;
        this.cacheSize = cacheSize;
        this.preallocateRatio = preallocateRatio;
    }

    public long         getInitialCacheSize() {
        return (long) (cacheSize * preallocateRatio);
    }
}
