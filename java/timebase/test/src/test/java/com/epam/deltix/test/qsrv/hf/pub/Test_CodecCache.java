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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.util.ZIPUtil;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * Test for CodecCache
 */
@Category (JUnitCategories.TickDBFast.class)
public class Test_CodecCache {


    @Test
    public void testMultiThreadingCodecs() throws Exception {
        File folder = new File(TDBRunner.getTemporaryLocation());

        IOUtil.removeRecursive(folder);

        try (InputStream is = IOUtil.openResourceAsStream("com/epam/deltix/testticks.zip")) {
            ZIPUtil.extractZipStream(is, folder);
        }

        TDBRunner runner = new ServerRunner(true, false, folder.getAbsolutePath());
        runner.startup();

        String URL = "dxtick://localhost:" + runner.getPort();

        final AtomicInteger exceptionCount = new AtomicInteger(0);

        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread th, Throwable ex) {
                ex.printStackTrace(System.out);
                //System.out.println("Uncaught exception: " + ex);
                exceptionCount.incrementAndGet();
            }
        };

        final Thread[] threads = new Thread[50];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run () {
                    DXTickDB db = TickDBFactory.createFromUrl(URL);
                    db.open(false);
                    TickStream stream = db.getStream("test_1");
                    try (TickCursor cursor = db.select(Long.MIN_VALUE, new SelectionOptions(), stream)) {
                        int count = 0;
                        while (cursor.next())
                            count++;
                        assertEquals(12000, count);    // 70 messages in this stream
//                            System.out.println(count++);
                    } catch (RuntimeException e) {
                        throw e;
                    } finally {
                        db.close();
                    }
                }
            });
            threads[i].setUncaughtExceptionHandler(handler);
            threads[i].start();
        }

        for (Thread t : threads)
            t.join();

        assertEquals(0, exceptionCount.get());  // 0 exceptions when read stream in (threads.length) cursors

        runner.shutdown();
    }
}
