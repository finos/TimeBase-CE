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

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_QuickExecutor extends TDBRunnerBase {

    static {
        System.setProperty("QuickExecutor.threads", "50");
    }

    @Test
    public void             complexOpenCloseCycle1 () throws InterruptedException {

        final DXTickDB tickDb = runner.getTickDb();

        final int numThreads = 100;
        final int numCursors = 100;

        final CountDownLatch done = new CountDownLatch(numThreads);

        Runnable runnable1 = new Runnable() {
            @Override
            public void run() {

                for (int ii = 0; ii < numCursors; ii++) {
                    TickCursor cur = tickDb.createCursor(new SelectionOptions(true, false, false));
                    cur.close ();
                }

                done.countDown();
            }
        };

        for (int i = 0; i < numThreads; i++) {
            new Thread(runnable1).start();
        }

        done.await();
    }
}