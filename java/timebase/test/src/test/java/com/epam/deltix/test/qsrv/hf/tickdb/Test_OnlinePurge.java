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

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBStress;

@Category(TickDBStress.class)
public class Test_OnlinePurge extends TDBRunnerBase {

    @Test
    public void purgeOnlineTest() throws Throwable {
        String name = "purgeOnlineTest";

        DXTickDB db = getServerDb();

        DXTickStream stream = db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        String name1 = "purgeOnlineTest1";
        DXTickStream stream1 = db.createStream(name1,
                StreamOptions.fixedType(StreamScope.DURABLE, name1, name1, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();

        TickLoader      loader1 = stream.createLoader (options);

        final int[] errors = new int[] {0};
        loader1.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
                //e.printStackTrace(System.out);
            }
        });

        TickLoader      loader2 = stream1.createLoader (options);
        loader2.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
                //e.printStackTrace(System.out);
            }
        });

        final int total = 10000000;
        final TickCursor live = db.select(Long.MIN_VALUE,
                new SelectionOptions(false, true), stream, stream1);

        GregorianCalendar calendar = new GregorianCalendar(2008, 1, 1);
        final long time = new GregorianCalendar(2008, 1, 1, 12, 0, 0).getTimeInMillis();
        final long lastTime = calendar.getTimeInMillis() + BarMessage.BAR_MINUTE * (total - 10);

        //System.out.println(GMT.formatDateTimeMillis(lastTime));

        Thread consumer = new Thread("Consumer") {

            @Override
            public void run() {
                try {
                    runInternal();
                }
                catch (Throwable ex) {
                    ex.printStackTrace(System.out);
                }
            }

            public void runInternal() {
                int count = 0;
                long time = 0;
                while (time < lastTime && live.next()) {
                    time = live.getMessage().getTimeStampMs();

                    count++;
//                    if (count % (total / 10) == 0)
//                        System.out.println(count + ":" + live.getMessage());

//                   if (count++ == 30000)
//                       live.reset(Long.MIN_VALUE);
                }
            }
        };

        consumer.start();

        try {
            Random rnd = new Random();
            int count = 0;

            while (count < total) {
                BarMessage message = new BarMessage();
                message.setSymbol("ES" + (count % 50));

                calendar.add(Calendar.MINUTE, 1);
                message.setTimeStampMs(calendar.getTimeInMillis());

                message.setHigh(rnd.nextDouble()*100);
                message.setOpen(message.getHigh() - rnd.nextDouble()*10);
                message.setClose(message.getHigh() - rnd.nextDouble()*10);
                message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
                message.setVolume(rnd.nextInt(10000));
                message.setCurrencyCode((short)840);
                loader1.send(message);

                message.setSymbol("CS" + (count % 3));
                loader2.send(message);

//                if (count == 10000) {
//                    stream.truncate(time);
//                }

                if (count == 700000) {
                    stream.purge(time);
                    testStream(stream);
                }

                if (count == 200000) {
                    stream1.purge(time);
                    testStream(stream1);
                }

                if (count % (total / 10) == 0) {
                    System.out.println("Send " + count + " messages");
                }

                if (count % 1000 == 0)
                    Thread.sleep(1);

                count++;
            }
        } finally {
            Util.close(loader1);
            Util.close (loader2);
        }

//        BackgroundProcessInfo process = waitForExecution(stream);
//        if (process != null && process.status == ExecutionStatus.Failed)
//            throw process.error;
//
//        process = waitForExecution(stream1);
//        if (process != null && process.status == ExecutionStatus.Failed)
//            throw process.error;

        consumer.join();
        live.close();

        Test_Purge.waitForExecution(stream);

        long[] range = stream.getTimeRange();

        assertEquals("Finished with loading errors!", 0, errors[0]);

        assertTrue("Expected end time (" + GMT.formatDateTimeMillis(calendar.getTimeInMillis()) + ") but stream end time = " + GMT.formatDateTimeMillis(range[1]),
                range[1] == calendar.getTimeInMillis());

        assertTrue("Purge(" + GMT.formatDateTimeMillis(time) + ") but stream start time = " +
                GMT.formatDateTimeMillis(range[0]), range[0] == time);

        range = stream1.getTimeRange();
        assertTrue("Purge(" + GMT.formatDateTimeMillis(time) + ") but stream start time = " +
                GMT.formatDateTimeMillis(range[0]), range[0] == time);

        //stream.delete();
        //stream1.delete();
    }

    @Test
    public void purgeOnlineTest3() throws Throwable {
        String name = "purgeOnlineTest3";

        DXTickDB db = getServerDb();

        DXTickStream stream = db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();

        TickLoader      loader = stream.createLoader(options);

        final int[] errors = new int[] {0};
        loader.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
                //e.printStackTrace(System.out);
            }
        });

        final TickCursor live = db.select(Long.MIN_VALUE, new SelectionOptions(false, true), stream);

        GregorianCalendar calendar = new GregorianCalendar(2014, 1, 1);
        int messages = 100;

        final long lastTime = calendar.getTimeInMillis() + messages * BarMessage.BAR_MINUTE;

        Thread consumer = new Thread("Consumer") {
            @Override
            public void run() {
                int count = 0;
                long time = 0;
                while (time < lastTime && live.next()) {
                    time = live.getMessage().getTimeStampMs();

                    //System.out.println(live.getMessage());
                    if (count++ > 100)
                        break;

//                  if (count++ == 30000)
//                      live.reset(Long.MIN_VALUE);
                }
            }
        };

        consumer.start();

        Random rnd = new Random();
        for (int i = 0; i < 100; i++) {
            BarMessage message = new BarMessage();
            message.setSymbol("ES" + (i % 5));

            calendar.add(Calendar.MINUTE, 1);
            message.setTimeStampMs(calendar.getTimeInMillis());

            message.setHigh(rnd.nextDouble()*100);
            message.setOpen(message.getHigh() - rnd.nextDouble()*10);
            message.setClose(message.getHigh() - rnd.nextDouble()*10);
            message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
            message.setVolume(rnd.nextInt(10000));
            message.setCurrencyCode((short)840);
            loader.send(message);
        }

        loader.close();

        executePurge(stream, calendar.getTimeInMillis() - 5 * BarMessage.BAR_MINUTE);

        loader = stream.createLoader (options);

        for (int i = 0; i < 10; i++) {
            BarMessage message = new BarMessage();
            message.setSymbol("ES" + (i % 5));

            calendar.add(Calendar.MINUTE, 1);
            message.setTimeStampMs(calendar.getTimeInMillis());

            message.setHigh(rnd.nextDouble()*100);
            message.setOpen(message.getHigh() - rnd.nextDouble()*10);
            message.setClose(message.getHigh() - rnd.nextDouble()*10);
            message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
            message.setVolume(rnd.nextInt(10000));
            message.setCurrencyCode((short)840);
            loader.send(message);
        }

        loader.close();

        consumer.join();
        live.close();

        //stream.delete();
    }

    @Test
    public void purgeOnlineTest2() throws Throwable {
        String name = "purgeOnlineTest2";

        DXTickDB db = getServerDb();

        DXTickStream stream = db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();

        TickLoader      loader1 = stream.createLoader (options);

        final int[] errors = new int[] {0};
        loader1.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
                //e.printStackTrace(System.out);
            }
        });

        final int total = 100000;
        final TickCursor live = db.select(Long.MIN_VALUE, new SelectionOptions(false, true), stream);

        GregorianCalendar calendar = new GregorianCalendar(2008, 1, 1);
        final long startTime = calendar.getTimeInMillis();
        final long lastTime = startTime + BarMessage.BAR_MINUTE * (total - 1);

        Thread consumer = new Thread("Consumer") {
            @Override
            public void run() {
                int count = 0;
                long time = 0;
                while (time < lastTime && live.next()) {
                    time = live.getMessage().getTimeStampMs();
                    //System.out.println(live.getMessage());
//                   if (count++ == 30000)
//                       live.reset(Long.MIN_VALUE);
                }
            }
        };

        consumer.start();

        long purgeTime = 0;

        try {
            Random rnd = new Random();
            int count = 0;

            BarMessage message = new BarMessage();

            message.setTimeStampMs(calendar.getTimeInMillis());

            while (count < total) {

                calendar.add(Calendar.MINUTE, 1);
                message.setSymbol("ES" + (count % 100));
                message.setTimeStampMs(calendar.getTimeInMillis());
                message.setHigh(rnd.nextDouble()*100);
                message.setOpen(message.getHigh() - rnd.nextDouble()*10);
                message.setClose(message.getHigh() - rnd.nextDouble()*10);
                message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
                message.setVolume(count);
                loader1.send(message);

                message.setSymbol("CS" + (count % 3));

                if (count == 40000) {
                    purgeTime = startTime + (calendar.getTimeInMillis() - startTime) / 2;
                    // round to minutes
                    purgeTime -= purgeTime % BarMessage.BAR_MINUTE;

                    stream.truncate(calendar.getTimeInMillis());
                    //assertEquals(time, stream.getTimeRange()[1]);
                }

                if (count == 70000)
                    executePurge(stream, purgeTime);

                count++;
            }
        } finally {
            Util.close (loader1);
        }

        consumer.join();
        live.close();

        Test_Purge.waitForExecution(stream);

        long[] range = stream.getTimeRange();

        assertEquals("Finished with loading errors!", 0, errors[0]);

        assertTrue("Expected end time (" + GMT.formatDateTimeMillis(calendar.getTimeInMillis()) + ") but stream end time = " + GMT.formatDateTimeMillis(range[1]),
                range[1] == calendar.getTimeInMillis());

        assertTrue("Purge(" + GMT.formatDateTimeMillis(purgeTime) + ") but stream start time = " +
                GMT.formatDateTimeMillis(range[0]), range[0] == purgeTime);

        //stream.delete();
    }

    static void            testStream(DXTickStream stream) {
        long lastTime = 0;

        TickCursor cursor = null;
        try {
            SelectionOptions options = new SelectionOptions(true, false);

            cursor = stream.select(Long.MIN_VALUE, options);
            while (cursor.next()) {
                lastTime = cursor.getMessage().getTimeStampMs();
            }

            cursor.close();

            options.reversed = true;
            cursor = stream.select(Long.MAX_VALUE - 1, options);
            if (cursor.next())
                assertEquals(lastTime, cursor.getMessage().getTimeStampMs());

            int count = 0;
            while (cursor.next()) {
                count++;
            }

            System.out.println(count);

        } finally {
            Util.close(cursor);
        }
    }

    private void                    executePurge(DXTickStream stream, long time) throws InterruptedException {
        stream.purge(time);
        Test_Purge.waitForExecution(stream);

        SelectionOptions options = new SelectionOptions(true, false);

        // check stream time range

        long[] range = stream.getTimeRange();

        long lastTime = range != null ? range[0] : Long.MIN_VALUE;
        TickCursor cursor = null;
        try {

            cursor = stream.select(Long.MIN_VALUE, options);
            while (cursor.next()) {
                lastTime = cursor.getMessage().getTimeStampMs();
            }

            cursor.close();

            options.reversed = true;
            cursor = stream.select(Long.MAX_VALUE - 1, options);
            if (cursor.next())
                assertEquals(lastTime, cursor.getMessage().getTimeStampMs());

            int count = 0;
            while (cursor.next()) {
                count++;
            }

            System.out.println(count);

        } finally {
            Util.close(cursor);
        }

        if (lastTime == Long.MIN_VALUE) {
            assertTrue(range != null);
        } else {
            assertTrue("Purge(" + GMT.formatDateTimeMillis(time) + ") but stream start time = " +
                    GMT.formatDateTimeMillis(range[0]), range[0] == time);
        }
    }
}
