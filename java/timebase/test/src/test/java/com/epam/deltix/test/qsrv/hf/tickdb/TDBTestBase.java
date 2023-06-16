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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.test.EmbeddedServer;
import org.junit.After;
import org.junit.Before;


public abstract class TDBTestBase extends TDBRunner {

    public TDBTestBase(boolean isRemote) {
        this(isRemote, true);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat) {
        this(isRemote, doFormat, getTemporaryLocation());
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, String location) {
        this(isRemote, doFormat, location, null, false);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, EmbeddedServer server) {
        this(isRemote, doFormat, getTemporaryLocation(), server);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, String location, EmbeddedServer server) {
        this(isRemote, doFormat, location, server, false);
    }

    public TDBTestBase(boolean isRemote, boolean doFormat, String location, EmbeddedServer server, boolean enableLocalTopics) {
        super(isRemote, doFormat, location, server == null ? new TomcatServer() : server, enableLocalTopics);
    }

    @Before
    public void startup() throws Exception {
        super.startup();
    }

    @After
    public void shutdown() throws Exception {
        super.shutdown();
    }
}