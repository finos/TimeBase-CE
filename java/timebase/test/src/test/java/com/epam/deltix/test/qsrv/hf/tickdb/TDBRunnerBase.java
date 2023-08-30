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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.File;

public abstract class TDBRunnerBase {

    protected static TDBRunner runner;

    @BeforeClass
    public static void      start() throws Throwable {
        //long time = System.currentTimeMillis();

        runner = new TDBRunner(true, true, TDBRunner.getTemporaryLocation(), new TomcatServer());
        runner.startup();
        
        //System.out.println("TDBRunner start-up time: " + (System.currentTimeMillis() - time)/1000.0 + " sec");
    }

    @AfterClass
    public static void      stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    public static DXTickDB  getTickDb() {
        return runner.getTickDb();
    }

    public static DXTickDB  getServerDb() {
        return runner.getServerDb();
    }

    public DXTickStream     createStream(DXTickDB db, String key, StreamOptions so) {
        DXTickStream stream = db.getStream(key);
        if (stream != null)
            stream.delete();

        return db.createStream(key, so);
    }
}