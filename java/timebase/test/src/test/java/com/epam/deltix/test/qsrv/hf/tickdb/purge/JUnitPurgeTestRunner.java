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
package com.epam.deltix.test.qsrv.hf.tickdb.purge;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.tests.procs.TestLoader;
import com.epam.deltix.qsrv.hf.tickdb.tests.procs.TestPurge;
import com.epam.deltix.qsrv.hf.tickdb.tests.procs.TestReader;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Ignore
public class JUnitPurgeTestRunner {

    private static TDBRunner runner;

    @BeforeClass
    public static void beforeClass() throws Exception {
        runner = ServerRunner.create(true, false, 1 << 24);
        runner.startup();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void test() throws InterruptedException {
        runTest("testStream", 1, 10, 30 * 1000, 60 * 1000);
    }

    public String getServerUrl() {
        return String.format("dxtick://localhost:%d", runner.getPort());
    }

    public DXTickDB getTickDb() {
        return runner.getTickDb();
    }


    public void runTest(String stream, int loaders, int readers, long purgePeriod, long purgeInterval) throws InterruptedException {
        TestReader testReader = TestReader.create(getServerUrl(), stream, readers, false);
        TestLoader testLoader = TestLoader.create(getServerUrl(), stream, loaders);
        TestPurge testPurge = TestPurge.create(getServerUrl(), stream, purgePeriod, purgeInterval);
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        executorService.execute(testPurge);
        Thread.sleep(5000);
        executorService.execute(testLoader);
        executorService.execute(testReader);
        executorService.awaitTermination(10, TimeUnit.MINUTES);
    }

}