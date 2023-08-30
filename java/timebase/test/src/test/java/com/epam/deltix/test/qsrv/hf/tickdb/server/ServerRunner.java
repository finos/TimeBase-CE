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
package com.epam.deltix.test.qsrv.hf.tickdb.server;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DataCacheOptions;

import java.io.File;

public class ServerRunner extends TDBRunner {

    public ServerRunner() {
        this(true, true, getTemporaryLocation());
    }

    public ServerRunner(boolean isRemote, boolean doFormat) {
        this(isRemote, doFormat, getTemporaryLocation());
    }

    public ServerRunner(boolean doFormat, String location) {
        this(true, doFormat, location);
    }

    public ServerRunner(boolean isRemote, boolean doFormat, String location) {
        super(isRemote, doFormat, location, new TestServer(new File(location)));
    }

    public ServerRunner(boolean isRemote, boolean doFormat, DataCacheOptions options) {
        super(isRemote, doFormat, getTemporaryLocation(), new TestServer(options, new File(getTemporaryLocation())));
    }

    public static ServerRunner create(boolean isRemote, boolean doFormat, long cacheSize) {
        DataCacheOptions dataCacheOptions = new DataCacheOptions();
        dataCacheOptions.cacheSize = cacheSize;
        return new ServerRunner(isRemote, doFormat, dataCacheOptions);
    }
}