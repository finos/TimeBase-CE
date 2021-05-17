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
package com.epam.deltix.test.qsrv.hf.tickdb.tb5;


import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Created by Alex Karpovich on 10/16/2018.
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_Reading {

    private static TDBRunner runner;
    private static final int TOTAL = 100_000;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, true);
        runner.startup();

        String name = "bars";

        DXTickStream stream = runner.getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));

        loadData(stream, TOTAL, "T_");
        Thread.sleep(5000); // wait to free files
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    private static  void    loadData(DXTickStream stream, int count, String prefix) {
        GregorianCalendar calendar = new GregorianCalendar(2018, 1, 1);

        TickLoader loader = stream.createLoader ();

        try {
            //Random rnd = new Random(2018);
            BarMessage message = new BarMessage();
            message.setCurrencyCode((short)840);

            for (int i = 0; i < count; i++) {
                message.setSymbol(prefix + (i % 100));
                message.setTimeStampMs(calendar.getTimeInMillis());

                message.setHigh(i * 100.0);
                message.setLow(i * 99.0);
                message.setOpen(i * 98);
                message.setClose(i * 99.5);
                message.setVolume(i);

                loader.send(message);

                calendar.add(Calendar.SECOND, 1);
            }
        } finally {
            Util.close (loader);
        }

    }

    @Test
    public void testReading() throws ParseException, InterruptedException {
//        DXTickDB tickDB = TickDBFactory.createFromUrl("dxtick://localhost:8011");
//        tickDB.open(true);

        DXTickDB tickDB = runner.getServerDb();

        final DXTickStream stream = tickDB.getStream("bars");

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                int count = 0;

                try (TickCursor cursor = stream.select(0, new SelectionOptions(true, false))) {

                    while (cursor.next()) {
                        if (count == 0)
                            System.out.println(Thread.currentThread() + ": got first message: " + GMT.formatDateTimeMillis(cursor.getMessage().getTimeStampMs()));

                        count++;
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace(System.out);
                } finally {
                    System.out.println(Thread.currentThread() + ": got messages: " + count);
                }

                Assert.assertEquals(TOTAL, count);
            }
        };

        final ArrayList<Throwable> errors = new ArrayList<>();

        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                synchronized (errors) {
                    errors.add(e);
                }
            }
        };

        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(runnable);
            threads[i].setUncaughtExceptionHandler(handler);
        }

        for (int i = 0; i < threads.length; i++)
            threads[i].start();

        for (int i = 0; i < threads.length; i++)
            threads[i].join();

        Assert.assertEquals(Arrays.toString(errors.toArray()), 0, errors.size());
    }

}
