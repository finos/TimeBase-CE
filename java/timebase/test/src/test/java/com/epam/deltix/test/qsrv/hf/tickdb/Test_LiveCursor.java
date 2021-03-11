package com.epam.deltix.test.qsrv.hf.tickdb;


import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class Test_LiveCursor {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, true);
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void testLive() throws InterruptedException {
        String name = "live";

        DXTickDB db = runner.getServerDb();

        final DXTickStream stream = db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();

        final int total = 1000000;
        GregorianCalendar calendar = new GregorianCalendar(2008, 1, 1);

        //final long time = new GregorianCalendar(2008, 1, 1, 12, 0, 0).getTimeInMillis();
        final long lastTime = calendar.getTimeInMillis() + BarMessage.BAR_MINUTE * total;

        final Thread consumer = new Thread("Consumer") {
            @Override
            public void run() {
                final TickCursor live = stream.select(Long.MIN_VALUE, new SelectionOptions(false, true));

                int count = 0;
                long time = 0;
                System.out.println("Last time: " + GMT.formatDateTimeMillis(lastTime));

                while (time < lastTime && live.next()) {
                    time = live.getMessage().getTimeStampMs();
                    //System.out.println(live.getMessage());
                    count++;
                    if (count % 10000 == 0) {
                        System.out.println("read " + count + " message:" + live.getMessage());
                        System.out.println(GMT.formatDateTimeMillis(stream.getTimeRange(live.getMessage())[1]));
                    }

//                    if (count++ == 30000)
//                       live.reset(Long.MIN_VALUE);
                }

                live.close();

                System.out.println("Consumer finished having recieved " + count + " messages.");
            }
        };

        consumer.start();

        TickLoader loader = stream.createLoader (options);

        final int[] errors = new int[] {0};
        loader.addEventListener(new LoadingErrorListener() {

            public void onError(LoadingError e) {
                errors[0]++;
                //e.printStackTrace(System.out);
            }
        });

        try {
            Random rnd = new Random();
            int count = 0;

            while (count < total) {
                BarMessage message = new BarMessage();
                message.setSymbol("ES" + (count % 100));
                calendar.add(Calendar.MINUTE, 1);
                message.setTimeStampMs(calendar.getTimeInMillis());

                message.setHigh(rnd.nextDouble()*100);
                message.setOpen(message.getHigh() - rnd.nextDouble()*10);
                message.setClose(message.getHigh() - rnd.nextDouble()*10);
                message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble()*10);
                message.setVolume(rnd.nextInt(10000));
                message.setCurrencyCode((short)840);
                loader.send(message);

                message.setSymbol("CS" + (count % 3));
                loader.send(message);

//                if (count == 10000) {
//                    stream.truncate(time);
//                }


                count++;
            }

            System.out.println("Send " + count + " messages.");
        } finally {
            Util.close(loader);
        }

        //consumer.start();
        consumer.join();
        //live.close();
    }


    @Test
    public void testSplit() {

        String name = "split.test";

        int TOTAL = 1_000_000;

        final DXTickStream stream = runner.getTickDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();
        options.writeMode = LoadingOptions.WriteMode.INSERT;

        //options.channelPerformance = ChannelPerformance.MIN_LATENCY;
        TickLoader loader = stream.createLoader(options);

        for (int i = 0; i < 3; i++) {

            long count = 0;
            GregorianCalendar calendar = new GregorianCalendar(2010, 0, 1);
            calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

            TDBRunner.BarsGenerator generator = new TDBRunner.BarsGenerator(calendar, 1, TOTAL, "MSFT", "ORCL", "DLTX");

            while (generator.next()) {
                loader.send(generator.getMessage());
                count++;
                if (count % (TOTAL / 10) == 0) {
                    System.out.printf("Send  %,3d messages\n", count);
                }

//            if (count % (TOTAL / 100) == 0)
//                Thread.sleep(10);
            }
        }

        loader.close();

    }


    @Test
    public void testSplit1() {

        String name = "split.test1";

        int TOTAL = 10_000_000;

        final DXTickStream stream = runner.getTickDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));

        LoadingOptions options = new LoadingOptions();
        options.writeMode = LoadingOptions.WriteMode.INSERT;

        //options.channelPerformance = ChannelPerformance.MIN_LATENCY;

        String[] tickers = new String[1000];
        for (int i = 0; i < tickers.length; i++)
            tickers[i] = "A" + i;

        for (int i = 0; i < 3; i++) {

            TickLoader loader = stream.createLoader(options);
            long count = 0;
            GregorianCalendar calendar = new GregorianCalendar(2010, 3-i, 1);
            calendar.setTimeZone(TimeZone.getTimeZone("GMT"));

            TDBRunner.BarsGenerator generator = new TDBRunner.BarsGenerator(calendar, 1, TOTAL, tickers);

            while (generator.next()) {
                loader.send(generator.getMessage());
                count++;
                if (count % (TOTAL / 10) == 0) {
                    System.out.printf("Send  %,3d messages\n", count);
                }

//            if (count % (TOTAL / 100) == 0)
//                Thread.sleep(10);
            }

            loader.close();
        }



    }
}
