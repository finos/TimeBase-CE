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

import com.epam.deltix.data.stream.MessageSourceMultiplexer;
import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedBoundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.RealTimeStartMessage;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.Test;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_RealTimeMode extends TDBTestBase {

//    static  {
//        MessageSourceMultiplexer.LOGGER.setLevel(Level.FINE);
//    }
    
    public Test_RealTimeMode() {
        super(true, true);
    }

    @Test(timeout = 20000)
    public void testLive() throws InterruptedException {

        final DXTickStream historicStream = createBarsStream("bars", 0);

        populateStream(historicStream, new TDBRunner.BarsGenerator(new GregorianCalendar(2010, 1, 1), (int) BarMessage.BAR_MINUTE, 100, "MSFT", "AAPL"));

        final DXTickStream liveStream = createBarsStream("live", 1);

        TickCursor historicCursor = selectLive(historicStream, Long.MIN_VALUE);
        TickCursor liveCursor = selectLive(liveStream, Long.MIN_VALUE);

        MessageSourceMultiplexer<InstrumentMessage> mx = new MessageSourceMultiplexer<InstrumentMessage>(true, true);
        mx.add(new DelayedSource(historicCursor, true));
        mx.add(liveCursor);

        Thread liveStreamProducer = new Thread() {
            @Override
            public void run() {
                populateStream(liveStream, new TDBRunner.BarsGenerator(null, (int) BarMessage.BAR_MINUTE, 100, "Q", "W") {
                    int counter = 0;
                    @Override
                    public boolean next() {
                        counter++;
                        boolean hasNext = super.next();
                        getMessage().setTimeStampMs(System.currentTimeMillis() + counter);

                        return hasNext;
                    }
                });
            }
        };
        liveStreamProducer.start();

        // Delayed listener throws away each 10 message
        // And we will get additional RealTimeStartMessage
        int total = 191;

        int count = 0;
        while (count < total && mx.next()) {
            count++;
            InstrumentMessage message = mx.getMessage();
            //System.out.println(count + " - " + message);

            if (count < 91)
                assert "MSFT".equals(message.getSymbol().toString()) ||
                        "AAPL".equals(message.getSymbol().toString()) : count + ":" + message;

            if (count == 91)
                assert message instanceof RealTimeStartMessage;

            if (count >= 92) {
                if (count % 2 == 0)
                    assertEquals("Q", message.getSymbol().toString());
                else
                    assertEquals("W", message.getSymbol().toString());
            }
        }
        liveStreamProducer.join();

        mx.close();

        historicCursor.close();
        liveCursor.close();
    }

    @Test(timeout = 60000)
    public void testLive1() throws InterruptedException {

        DXTickDB tickDb = getTickDb();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "bars", null, 0);
        options.location = "abc"; //TODO
        options.setFixedType (StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream bars = tickDb.createStream("bars", options);

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2010, 1, 1), (int) BarMessage.BAR_MINUTE, 100, "MSFT", "AAPL");

        populateStream(bars, gn);

        options = new StreamOptions (StreamScope.DURABLE, "live", null, 0);
        options.location = "abc"; //TODO
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream live = tickDb.createStream("live", options);

        TickCursor cursor1 = selectLive(bars, Long.MIN_VALUE);
        TickCursor cursor2 = selectLive(live, Long.MAX_VALUE);

        MessageSourceMultiplexer<InstrumentMessage> mx = new MessageSourceMultiplexer<InstrumentMessage>(true, true);
        mx.add(new DelayedSource(cursor1, true));
        mx.add(cursor2);

        cursor2.reset(TimeKeeper.currentTime);
        
        Thread producer = new Thread() {
            @Override
            public void run() {
                TDBRunner.BarsGenerator gn =
                    new TDBRunner.BarsGenerator(new GregorianCalendar(), (int) BarMessage.BAR_MINUTE, 100, "QQQQ", "WWWW");

                populateStream(live, gn);
            }
        };
        producer.start();

        // Delayed listener throws away each 10 message
        // And we will get additional RealTimeStartMessage
        int total = 193;

        int count = 0;
        while (mx.next()) {
            count++;
            InstrumentMessage message = mx.getMessage();
            //System.out.println(count + " - " + message);

             if (count < 91)
                assert "MSFT".equals(message.getSymbol().toString()) ||
                        "AAPL".equals(message.getSymbol().toString());

            if (count == 91)
                assert message instanceof RealTimeStartMessage;
            
//            if (count == 92)
//                assert message instanceof RealTimeStartMessage;

            if (message instanceof BarMessage && ((BarMessage) message).getVolume() == 1 && count > 92)
                break;

//            if (count > 91)
//                assert "QQQQ".equals(message.getSymbol().toString()) ||
//                        "WWWW".equals(message.getSymbol().toString());
        }
        producer.join();

        mx.close();

        cursor1.close();
        cursor2.close();
    }

    @Test(timeout = 60000)
    public void testLive2() throws InterruptedException {

        DXTickDB tickDb = getServerDb();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "bars", null, 0);
        options.location = "abc"; //TODO
        options.setFixedType (StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream bars = tickDb.createStream("bars", options);

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2010, 1, 1), (int) BarMessage.BAR_MINUTE, 100, "MSFT", "AAPL");

        populateStream(bars, gn);

        options = new StreamOptions (StreamScope.DURABLE, "live", null, 0);
        options.location = "abc"; //TODO
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream live = tickDb.createStream("live", options);

        final TickLoader liveLoader = live.createLoader();

        Thread producer = new Thread() {
            @Override
            public void run() {
                TDBRunner.BarsGenerator gn =
                    new TDBRunner.BarsGenerator(null, (int) BarMessage.BAR_MINUTE, -1,
                            "QQQQ", "WWWW", "DDDD");

                while (gn.next()) {
                    if (isInterrupted())
                        break;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        break;
                    }

                    liveLoader.send(gn.getMessage());
                }

            }
        };
        producer.start();

        Thread.sleep(2000);

        TickCursor cursor1 = selectLive(bars, Long.MIN_VALUE);
        TickCursor cursor2 = selectLive(live, Long.MAX_VALUE);

        MessageSourceMultiplexer<InstrumentMessage> mx = new MessageSourceMultiplexer<InstrumentMessage>(true, true);

        mx.add(new DelayedSource(cursor1, true));
        mx.add(cursor2);

        cursor2.reset(TimeKeeper.currentTime - 1000 * 60);

        int count = 0;
        while (mx.next()) {
            count++;
            InstrumentMessage message = mx.getMessage();
            //System.out.println(count + " - " + message);

            if (message instanceof RealTimeStartMessage) {
                producer.interrupt();
                break;
            }

            if (count < 91)
                assert "MSFT".equals(message.getSymbol().toString()) ||
                        "AAPL".equals(message.getSymbol().toString());
        }
        producer.join();
        liveLoader.close();

        mx.close();

        cursor1.close();
        cursor2.close();
    }

    public void testCursor() throws InterruptedException {
        DXTickDB tickdb = getServerDb();
        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "data", null, 0);
        options.location = "abc"; //TODO
        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor (9);

        RecordClassDescriptor rd1 = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
                "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor rd2 = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
                "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        options.setPolymorphic(rd1, rd2);

        final DXTickStream data = tickdb.createStream("data", options);

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2010, 1, 1), (int) BarMessage.BAR_MINUTE, 10, "MSFT", "AAPL");

        populateStream(data, gn);

        MessageSourceMultiplexer<InstrumentMessage> msm = new
                MessageSourceMultiplexer<InstrumentMessage>();


        try (TickCursor cursor = selectLive(data, System.currentTimeMillis())) {
            msm.add(cursor);

//        int count = 0;
//        assertTrue(cursor.next());
//        do {
//            count++;
//        } while (cursor.next() && count < 10);

            assertTrue(msm.next());

            assertEquals(0, cursor.getCurrentTypeIndex());
            assertEquals(StreamConfigurationHelper.REAL_TIME_START_MESSAGE_DESCRIPTOR, cursor.getCurrentType());
            cursor.reset(System.currentTimeMillis());
        }
    }

    @Test(timeout = 20000)
    public void testLiveHist() throws InterruptedException {

        DXTickDB tickdb = getTickDb();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "bars", null, 0);
        options.location = "abc"; //TODO
        options.setFixedType (StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream bars = tickdb.createStream("bars", options);

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2010, 1, 1), (int) BarMessage.BAR_MINUTE, 100, "MSFT", "AAPL");

        populateStream(bars, gn);

        options = new StreamOptions (StreamScope.DURABLE, "live", null, 1);
        options.location = "abc"; //TODO
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream live = tickdb.createStream("live", options);

        SelectionOptions options1 = new SelectionOptions(false, true);
        TickCursor cursor1 = bars.select(Long.MIN_VALUE, options1);

        TickCursor cursor2 = selectLive(live, Long.MIN_VALUE);

        Thread producer = new Thread() {
            @Override
            public void run() {
                TDBRunner.BarsGenerator gn =
                    new TDBRunner.BarsGenerator(null, (int) BarMessage.BAR_MINUTE, 100, "QQQQ", "WWWW");

                populateStream(live, gn);
            }
        };
        producer.start();

        MessageSourceMultiplexer<InstrumentMessage> mx = new MessageSourceMultiplexer<InstrumentMessage>(true, true);
        //mx.setLive(false);

        mx.add(new DelayedSource(cursor1, false));
        mx.add(cursor2);

        // Delayed listener throws away each 10 message
        // And we won't get RealTimeStartMessage since as cursor1 will never return RTSM
        int total = 191;

        int count = 0;
        while (count < total && mx.next()) {
            count++;
            InstrumentMessage message = mx.getMessage();
            //System.out.println(count + " - " + message);

            if (count == 1)
                assert message instanceof RealTimeStartMessage;

        }
        producer.join();

        mx.close();
        cursor1.close();
        cursor2.close();
    }

    public class DelayedSource extends InstrumentMessageSourceAdapter
            implements RealTimeMessageSource<InstrumentMessage> {

        QuickExecutor.QuickTask notifier =
            new QuickExecutor.QuickTask () {
                @Override
                public void run () {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        
                    }
                    if (listener != null)
                        listener.run();
                }
            };

        int count = 0;

        private volatile Runnable   listener;
        private boolean             inRealtime;
        private boolean             realtime;

        public DelayedSource(TickCursor delegate, boolean realtime) {
            super(delegate);
            this.realtime = realtime;
        }

        @Override
        public boolean next() {
            
            boolean hasNext = super.next();
            if (hasNext)
                count++;

            if (hasNext && count % 10 == 0) {
                notifier.submit();
                throw UnavailableResourceException.INSTANCE;
            }
            
            if (!inRealtime)
                inRealtime = getMessage() instanceof RealTimeStartMessage;

            return hasNext;
        }

        @Override
        public void setAvailabilityListener(Runnable lnr) {
            this.listener = lnr;
            super.setAvailabilityListener(lnr);
        }

        @Override
        public boolean isRealTime() {
            return inRealtime;
        }

        @Override
        public boolean realTimeAvailable() {
            return realtime;
        }
    }

    @Test(timeout = 20000)
    public void testLiveTruncate() throws InterruptedException {

        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
                true, "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
                "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        DXTickDB db = getTickDb();

        DXTickStream st = db.getStream("zzz");
        if (st != null)
            st.delete();

        StreamOptions so = StreamOptions.polymorphic(StreamScope.DURABLE, "zzz", null, 0, bbo, trade);
        so.location = "abc"; //TODO
        DXTickStream stream = db.createStream("zzz", so);

        GregorianCalendar calendar = new GregorianCalendar(2010, 0, 1);
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        DataGenerator generator = new DataGenerator(calendar, 60 * 1000, "ORCL", "IBM");

        TickLoader loader = stream.createLoader(new LoadingOptions());
        long count = 0;

        try {

            while (generator.next() && count < 100) {
                loader.send(generator.getMessage());
                count++;
            }
        } finally {
            Util.close(loader);
        }

        SelectionOptions options = new SelectionOptions(false, true);
        options.realTimeNotification = true;
        options.allowLateOutOfOrder = true;

        TickCursor cursor = stream.select(Long.MIN_VALUE, options);

        count = 1;
        while (cursor.next() && count < 100) {
            count++;
        }

        calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(stream.getTimeRange()[1] - 60 * 1000); // -one minute

        generator = new DataGenerator(calendar, 60 * 1000, "ORCL", "IBM");
        loader = stream.createLoader(new LoadingOptions());

        stream.truncate(calendar.getTime().getTime());

        long lastTime = Long.MIN_VALUE;
        try {
            count = 0;
            while (generator.next() && count < 10) {
                InstrumentMessage message = generator.getMessage();
                loader.send(message);
                lastTime = message.getTimeStampMs();
                count++;
            }
        } finally {
            Util.close(loader);
        }

        count = 0;
        try {
            while (count++ < 9 && cursor.next()) {
                InstrumentMessage message = cursor.getMessage();
                assertTrue(message.getTimeStampMs() <= lastTime);
                //System.out.println(count +  " : " + message);
            }
        } finally {
            Util.close(cursor);
        }

        assertEquals(10, count);
    }

    private DXTickStream        createTestStream(DXTickDB db, int count, String name) {

        RecordClassDescriptor classDescriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null, "DECIMAL(4)", "DECIMAL(0)"
        );

        StreamOptions options = StreamOptions.fixedType(StreamScope.DURABLE, name, null, 0, classDescriptor);
        options.location = "abc"; //TODO
        DXTickStream stream = db.createStream(name, options);

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2012, 1, 1), (int) BarMessage.BAR_MINUTE, count,
                "MSFT", "AAPL", "ORCL", "IBM");

        populateStream(stream, gn);

        return stream;
    }

    @Test
    public void testQQL() {
        DXTickDB tickDb = getServerDb();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "qql_bars", null, 0);
        //options.location = "abc"; //TODO
        options.setFixedType (StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream bars = tickDb.createStream(options.name, options);

        SelectionOptions so = new SelectionOptions(false, true);
        so.realTimeNotification = true;
        InstrumentMessageSource source = tickDb.executeQuery(
                "select * from hybrid (qql_bars) where (this is " + RealTimeStartMessage.CLASS_NAME + ") or (" + BarMessage.CLASS_NAME + ":volume >= 1449.50)", so);

        assertTrue(source.next());
        //System.out.println(source.getMessage());

        source.close();
    }

    public void testTruncate() {
        RecordClassDescriptor classDescriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null, "DECIMAL(4)", "DECIMAL(0)"
        );

        StreamOptions so = StreamOptions.fixedType(StreamScope.DURABLE, "truncate", null, 0, classDescriptor);
        so.location = "abc"; //TODO
        DXTickStream stream = getTickDb().createStream("truncate", so);

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2012, 0, 1), (int) BarMessage.BAR_DAY, 100, "IBM");

        populateStream(stream, gn);

        long time = stream.getTimeRange()[1];
        time -= BarMessage.BAR_DAY;

        SelectionOptions options = new SelectionOptions(false, true);
        options.realTimeNotification = true;
        TickCursor cursor = stream.select(0, options);

        int count = 0;
        while (cursor.next()) {
            count ++;
            //cursor.getMessage();
            if (count == 100) {
                stream.truncate(time);

                TickLoader loader = stream.createLoader();
                loader.send(gn.getMessage());
                loader.close();
            }

            //if (count >= 100)
            //    System.out.println(count + ": " + cursor.getMessage());
        }
    }

    @Test(timeout = 60000)
    public void testTransient() throws InterruptedException {
        RecordClassDescriptor classDescriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null, "DECIMAL(4)", "DECIMAL(0)"
        );

        StreamOptions options = StreamOptions.fixedType(StreamScope.TRANSIENT, "transient", null, 0, classDescriptor);
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.lossless = false;
        options.bufferOptions.maxBufferSize = 1 << 20;

        DXTickDB db = getTickDb();
        final DXTickStream stream = db.createStream("transient", options);
        
        final boolean[] stopped = new boolean[] {false};

        Thread consumer = new Thread() {
            @Override
            public void run() {
                TickCursor cursor = stream.select(0, new SelectionOptions(false, true));
                while (!stopped[0] && cursor.next()) {
                }

                cursor.close();
            }
        };
        consumer.start();

        Thread producer = new Thread() {
            @Override
            public void run() {

                try(TickLoader loader = stream.createLoader(new LoadingOptions(true))) {

                    final RawMessage message = new RawMessage(stream.getFixedType());
                    message.setSymbol("DLTX");

                    BarMessage bar = new BarMessage();

                    bar.setOpen(9.52);
                    bar.setClose(bar.getClose());
                    bar.setHigh(bar.getClose());
                    bar.setLow(bar.getClose());

                    FixedBoundEncoder encoder = CodecFactory.INTERPRETED.createFixedBoundEncoder(TypeLoaderImpl.DEFAULT_INSTANCE, message.type);
                    MemoryDataOutput out = new MemoryDataOutput();
                    encoder.encode(bar, out);
                    message.setBytes(out, 0);

                    while (!stopped[0])
                        loader.send(message);

                }
            }
        };
        producer.start();

        Thread.sleep(1000); // wait for the threads to start

        SelectionOptions so = new SelectionOptions(false, true);
        so.realTimeNotification = true;

        IdentityKey[] ids = new IdentityKey[100];
        for (int i = 0; i < ids.length; i++)
            ids[i] = new ConstantIdentityKey("MSFT" + i);

        TickCursor cursor = stream.select(0, so, null, ids);
        
        assert cursor.next();
        assert cursor.getMessage() instanceof RealTimeStartMessage;

        cursor.close();

        stopped[0] = true;

        consumer.join();
        producer.join();

        stream.delete();
    }

    public void testPerformance() {
        final int total = 10000000;
        DXTickStream stream = createTestStream(getServerDb(), total, "perf");

        try (TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, true))) {

            long t0 = System.currentTimeMillis();
            for (int i = 0; i < total; i++)
                cursor.next();

            long t1 = System.currentTimeMillis();
            cursor.close();

            double s = (t1 - t0) * 0.001;
            System.out.printf("Normal  : %,d messages in %,.3fs; speed: %,.0f msg/s\n", total, s, total / s);
        }

        SelectionOptions options = new SelectionOptions(true, true);
        options.realTimeNotification = true;

        try (TickCursor cursor = stream.select(Long.MIN_VALUE, options)) {
            long t0 = System.currentTimeMillis();
            for (int i = 0; i < total; i++)
                cursor.next();

            long  t1 = System.currentTimeMillis();

            cursor.close();
            double s = (t1 - t0) * 0.001;
            System.out.printf("Realtime: %,d messages in %,.3fs; speed: %,.0f msg/s\n", total, s, total / s);
        }
    }

    private static void populateStream(DXTickStream data, TDBRunner.BarsGenerator gn) {
        try (TickLoader loader = data.createLoader()) {
            while (gn.next())
                loader.send(gn.getMessage());
        }
    }

    private static TickCursor selectLive(DXTickStream liveStream, long timestamp) {
        SelectionOptions options2 = new SelectionOptions(false, true);
        options2.realTimeNotification = true;
        return liveStream.select(timestamp, options2);
    }


    private DXTickStream createBarsStream (String streamKey, int distributionFactor) {
        DXTickDB tickDb = getTickDb();

        StreamOptions options = new StreamOptions (StreamScope.DURABLE, "dummy name for " + streamKey, null, distributionFactor);
        options.location = "abc"; //TODO
        options.setFixedType (StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        return tickDb.createStream(streamKey, options);
    }
}