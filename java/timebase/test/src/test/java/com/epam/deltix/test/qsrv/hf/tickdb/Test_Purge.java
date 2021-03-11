package com.epam.deltix.test.qsrv.hf.tickdb;

/*  ##TICKDB.SLOW## */

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.progress.ExecutionStatus;

import java.io.File;
import java.util.GregorianCalendar;
import java.util.Random;
import java.util.Calendar;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.epam.deltix.qsrv.hf.tickdb.TDBRunner.getTemporaryLocation;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Date: Feb 12, 2010
 */
@Category(Long.class)
public class Test_Purge {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        String location = getTemporaryLocation();
        DataCacheOptions options = new DataCacheOptions(Integer.MAX_VALUE, 500 * 1024 * 1024);
        options.fs.withMaxFolderSize(20).withMaxFileSize(1024 * 1024);

        runner = new TDBRunner(true, true, location, new TestServer(options, new File(location)));
        runner.startup();
    }

    @AfterClass
    public static void      stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    private static void                    executePurge(DXTickStream stream, long time) throws InterruptedException {
        stream.purge(time);

        waitForExecution(stream);
    }

    static BackgroundProcessInfo   waitForExecution(DXTickStream stream) throws InterruptedException {

        if (stream.getFormatVersion() >= 5)
            return null;

        boolean complete = false;
        while (!complete) {
            BackgroundProcessInfo process = stream.getBackgroundProcess();
            complete = process != null && process.isFinished();
            Thread.sleep(100);
        }

        return stream.getBackgroundProcess();
    }

    @Test
    public void purgeTest2() throws InterruptedException {
        String name = "purgeTest2";

        DXTickStream stream = TickDBCreator.createBarsStream(getServerDb(), name);

        long[] range = stream.getTimeRange();

        stream.truncate((range[0] + range[1])/2);
        purgeTest(stream);
    }

    @Test
    public void purgeTest41() throws InterruptedException {
        String name = "purgeTest41";

        DXTickStream stream = getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));

        loadData(stream, 15_000_000, "ES");

        long[] range = stream.getTimeRange();

        purgeTest(stream, range[0] + BarMessage.BAR_MINUTE * 2);
    }

    @Test
    public void purgeTest42() throws InterruptedException {
        String name = "purgeTest42";

        DXTickStream stream = getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));

        loadData(stream, 10_000_000, "ES");

        loadData(stream, 1_000_000, "SS");

        long[] range = stream.getTimeRange();

        purgeTest(stream, range[1] - BarMessage.BAR_MINUTE * 2);
    }

    @Test
    public void purgeTest43() throws InterruptedException {
        String name = "purgeTest43";

        DXTickStream stream = getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));

        for (int i = 0; i < 5; i++) {
            loadData(stream, 10_000_000, "ES");
            long[] range = stream.getTimeRange();
            purgeTest(stream, range[1] - BarMessage.BAR_MINUTE * 2);
        }
    }

    @Test
    public void loadTest43() throws InterruptedException {
        String name = "loadTest43";

        DXTickStream stream = getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));

        for (int i = 0; i < 5; i++) {
            loadData(stream, 10_000_000, "ES" + i);
            long[] range = stream.getTimeRange();
        }
    }


    private void    loadData(DXTickStream stream, int count, String id) {
        GregorianCalendar calendar = new GregorianCalendar(2018, 1, 1);

        for (int c = 0; c < 1; c++) {
            //calendar.add(Calendar.MONTH, 1);

            TickLoader      loader = stream.createLoader ();

            try {
                //Random rnd = new Random(2018);
                BarMessage message = new BarMessage();
                message.setCurrencyCode((short)840);

                for (int i = 0; i < count; i++) {
                    message.setSymbol(id + (i % 100));
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
    }

    public void purgeTest5() throws InterruptedException {
        String name = "purgeTest5";

        DXTickStream stream = getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));


        GregorianCalendar calendar = new GregorianCalendar(2013, 1, 1);

        for (int c = 0; c < 1; c++) {
            calendar.add(Calendar.MONTH, 1);

            TickLoader      loader = stream.createLoader ();

            try {
                Random rnd = new Random(2013);
                BarMessage message = new BarMessage();
                message.setCurrencyCode((short)840);

                for (int i = 0; i < 10_000_000; i++) {
                    message.setSymbol("ES" + (i % 500));
                    message.setTimeStampMs(calendar.getTimeInMillis());

                    message.setHigh(rnd.nextDouble()*100);
                    message.setOpen(message.getHigh() - rnd.nextDouble()*10);
                    message.setClose(message.getHigh() - rnd.nextDouble()*10);
                    message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
                    message.setVolume(i);

                    loader.send(message);

                    calendar.add(Calendar.MINUTE, 1);
                }
            } finally {
                Util.close (loader);
            }
        }

        long[] range = stream.getTimeRange();

        long time = (range[0] + range[1])/2 + BarMessage.BAR_HOUR;

        //purgeTest(stream, time);
        //stream.purge(time);

        getServerDb().close();
        getServerDb().open(false);

        stream = getServerDb().getStream(name);

        TickCursor tickCursor = null;
        try {
            tickCursor = stream.select(range[0] + BarMessage.BAR_HOUR, new SelectionOptions(true, false));
            if (tickCursor.next())
                time = tickCursor.getMessage().getTimeStampMs();
        } finally {
            Util.close(tickCursor);
        }

        range = stream.getTimeRange();
        assertEquals(time, range[0]);
    }

    public static DXTickDB  getTickDb() {
        return runner.getTickDb();
    }

    public static DXTickDB  getServerDb() {
        return runner.getServerDb();
    }


    public static void purgeTest(DXTickStream stream) throws InterruptedException {
        long[] timeRange = stream.getTimeRange();

        assertTrue(timeRange != null);

        long range = timeRange[1] - timeRange[0] - stream.getPeriodicity().getInterval().toMilliseconds();
        long time = timeRange[0] + (long)(Math.min(1, (Math.random() * 2)) * range);

        purgeTest(stream, time);
    }

    public static void purgeTest(DXTickStream stream, long time) throws InterruptedException {
        TickCursor tickCursor = null;
        try {
            tickCursor = stream.select(time, new SelectionOptions(true, false));
            if (tickCursor.next())
                time = tickCursor.getMessage().getTimeStampMs();
        } finally {
            Util.close(tickCursor);
        }

        long[] range = stream.getTimeRange();

        System.out.println("Time range = (" + GMT.formatDateTimeMillis(range[0]) + " : " + GMT.formatDateTimeMillis(range[1]) + "]");
        System.out.println("Purging(" + GMT.formatDateTimeMillis(time) + ") " + time);

        executePurge(stream, time);

        range = stream.getTimeRange();

        long lastTime = range != null ? range[0] : Long.MIN_VALUE;
        TickCursor cursor = null;
        try {
            cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false));
            while (cursor.next()) {
                lastTime = cursor.getMessage().getTimeStampMs();
            }
        } finally {
            Util.close(cursor);
        }

        if (lastTime == Long.MIN_VALUE) {
            assertTrue(range != null);
        } else {
            assertTrue("Purge(" + GMT.formatDateTimeMillis(time) + ") but stream start time = " +
                    GMT.formatDateTimeMillis(range[0]), range[0] == time);

            assertTrue("Cursor last time = (" + GMT.formatDateTimeMillis(lastTime) + ") but stream end time = " +
                    GMT.formatDateTimeMillis(range[1]), range[1] == lastTime);
        }
    }

    @Test
    public void onlineTest() throws Throwable {
        String name = "onlineTest";

        DXTickStream stream = getServerDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();

        TickLoader      loader = stream.createLoader (options);

        final int[] errors = new int[] {0};
        loader.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
            }
        });

        final int total = 50000;
        final TickCursor live = stream.select(Long.MIN_VALUE, new SelectionOptions(false, true));

        GregorianCalendar calendar = new GregorianCalendar(2008, 1, 1);
        final long time = new GregorianCalendar(2008, 1, 1, 2, 0, 0).getTimeInMillis();
        final long lastTime = calendar.getTimeInMillis() + BarMessage.BAR_MINUTE * total;

        Thread consumer = new Thread("Consumer") {
            @Override
            public void run() {
                long time = 0;
                while (lastTime < time && live.next()) {
                    time = live.getMessage().getTimeStampMs();
                }
            }
        };

        consumer.start();

        try {
            Random rnd = new Random();
            int count = 0;

            while (count < total) {
                BarMessage message = new BarMessage();
                message.setSymbol("ES1");

                calendar.add(Calendar.MINUTE, 1);
                message.setTimeStampMs(calendar.getTimeInMillis());

                message.setHigh(rnd.nextDouble()*100);
                message.setOpen(message.getHigh() - rnd.nextDouble()*10);
                message.setClose(message.getHigh() - rnd.nextDouble()*10);
                message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
                message.setVolume(rnd.nextInt(10000));
                message.setCurrencyCode((short)840);
                loader.send(message);

                if (count == 1000) {
                    stream.purge(time);
                }
                count++;
            }
        } finally {
            Util.close (loader);
        }

        BackgroundProcessInfo process = waitForExecution(stream);
        if (process != null && process.status == ExecutionStatus.Failed)
            throw process.error;

        consumer.join();

        live.close();

        long[] range = stream.getTimeRange();

        assertEquals("Finished with loading errors!", 0, errors[0]);
        assertEquals(range[1], calendar.getTimeInMillis());

        assertTrue("Purge(" + GMT.formatDateTimeMillis(time) + ") but stream start time = " +
                GMT.formatDateTimeMillis(range[0]), range[0] == time);
    }

    @Test
    public void purgeTest3() throws InterruptedException {
        String name = "purgeTest3";

        DXTickStream stream = TickDBCreator.createBarsStream(getServerDb(), name);

        while (true) {
            long[] before = stream.getTimeRange();
            purgeTest(stream);
            long[] after = stream.getTimeRange();
            if (before[0] == after[0] && before[1] == after[1])
                break;
        }

        Thread.sleep(100);
    }

    @Test
    public void purgeTest1() throws InterruptedException {
        String name = "purgeTest1";

        DXTickStream stream = TickDBCreator.createBarsStream(getServerDb(), name);

        long[] range = stream.getTimeRange();
        long time = (range[0] + range[1])/2;

        TickCursor tickCursor = null;
        try {
            tickCursor = stream.select(time, null);
            if (tickCursor.next())
                time = tickCursor.getMessage().getTimeStampMs();

            executePurge(stream, time - BarMessage.BAR_DAY);

            assertTrue(tickCursor.next());
            assertTrue(tickCursor.getMessage().getTimeStampMs() == time);
        } finally {
            Util.close(tickCursor);
        }
    }
}