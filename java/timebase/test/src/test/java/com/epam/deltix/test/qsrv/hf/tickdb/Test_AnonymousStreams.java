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
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.util.lang.Util;
import org.junit.*;

import java.util.Arrays;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 * @author Andy
 *         Date: Oct 25, 2010 4:40:56 PM
 */
@Category(TickDBFast.class)
@Ignore("AnonymousStreams was deprecated")
public class Test_AnonymousStreams extends TDBTestBase {

    private DXTickStream stream;
    private TickLoader loader;
    private TickCursor cursor;

    public Test_AnonymousStreams() {
        super(true);
    }

    @Before
    public void prepare () {
        DXTickDB db = getTickDb();

        StreamOptions options = new StreamOptions ();
        options.name = "test";
        options.scope = StreamScope.RUNTIME;
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = 1*1024*1024;
        options.bufferOptions.maxBufferSize = 4*1024*1024;
        options.bufferOptions.lossless = true;
        options.setFixedType(makeTradeMessageDescriptor()); // TODO: Poly
        stream = db.createAnonymousStream(options);

        LoadingOptions lo = new LoadingOptions(false);
        loader = stream.createLoader (lo);

    }

    @After
    public void finish () {
        Util.close(loader);
        Util.close(cursor);
    }
    
    @Test
    public void testSendReceive() throws InterruptedException {
        SelectionOptions so = new SelectionOptions(false, true);
        ConstantIdentityKey key = new ConstantIdentityKey("MSFT");

        cursor = stream.select(System.currentTimeMillis(), so,
                null, new IdentityKey[] {key} );

        TradeMessage msg = createTradeMessage("MSFT");
        //Assert.assertTrue (cursor.getFilter().accept(msg));
        loader.send (msg);

        Assert.assertTrue (cursor.next()); // live cursor will wait for msg
        TradeMessage received = (TradeMessage) cursor.getMessage();
        Assert.assertEquals(msg.getTimeStampMs(), received.getTimeStampMs());
        Assert.assertEquals(msg.getSize(), received.getSize(), 0);
    }

    @Test
    public void testLatency() throws InterruptedException {
        SelectionOptions so = new SelectionOptions(false, true);
        cursor = stream.createCursor (so); //System.nanoTime(), filter, so);
        cursor.reset(Long.MIN_VALUE);
        cursor.addEntity(new ConstantIdentityKey("MSFT"));

        final TradeMessage msg = createTradeMessage("MSFT");
        //Assert.assertTrue (cursor.getFilter().accept(msg));
        final int WARMUP = 20000;
        final int TEST = 50000;
        final long [] latencies = new long [TEST];
        assert TEST >= WARMUP;


        Thread producer = new Thread ("Producer") {
            @Override
            public void run() {
                for (int i=0; i < WARMUP; i++) {
                    msg.setOriginalTimestamp(System.nanoTime());
                    loader.send (msg);
                    //Test_AnonymousStreams.sleep (2);
                }

                for (int i=0; i < TEST; i++) {
                    msg.setOriginalTimestamp(System.nanoTime());
                    loader.send (msg);
                    if (i % 10 == 0)
                        Test_AnonymousStreams.sleep (2);
                }

            }
        };

        Thread consumer = new Thread ("Consumer") {
            @Override
            public void run() {

                for (int i=0; i < WARMUP; i++) {
                    Assert.assertTrue ("next", cursor.next()); // live cursor will wait for msg
                    MarketMessage received = (MarketMessage) cursor.getMessage();
                    latencies[i] = System.nanoTime() - received.getOriginalTimestamp();
                }

                for (int i=0; i < TEST; i++) {
                    Assert.assertTrue ("next", cursor.next()); // live cursor will wait for msg
                    MarketMessage received = (MarketMessage) cursor.getMessage();
                    latencies[i] = System.nanoTime() - received.getOriginalTimestamp();
                }

            }
        };


        consumer.start();
        producer.start();

        producer.join();
        consumer.join();   // lossless stream

        long min = Long.MAX_VALUE, max = 0, sum = 0;
        for (int i=0; i < TEST; i++) {
            long latency = latencies[i];
            if (min > latency)
                min = latency;
            if (max < latency)
                max = latency;
            sum += latency;
        }

        System.out.printf ("Latency (ns): Avg: %,d; Min: %,d; Max: %,d \n", sum/TEST, min, max);
        //System.out.println("Latency (ns): Avg: " + (sum/TEST) + " Min:" + min + " Max:" + max);
    }

    public void testRawThroughput () throws InterruptedException {
        StreamOptions options = new StreamOptions();
        options.name = "test";
        options.scope = StreamScope.RUNTIME;
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = 1*1024*1024;
        options.bufferOptions.maxBufferSize = 1*1024*1024;
        options.bufferOptions.lossless = true;
        options.setFixedType(StreamConfigurationHelper.BINARY_MESSAGE_DESCRIPTOR);
        DXTickStream stream = getTickDb().createAnonymousStream(options);


        LoadingOptions lo = new LoadingOptions(true);
        final TickLoader loader = stream.createLoader (lo);

        SelectionOptions so = new SelectionOptions(true, true);
        final TickCursor cursor = stream.createCursor (so);
        cursor.addEntity(new ConstantIdentityKey("MSFT"));

        Runnable producer = new Runnable() {
            @Override
            public void run() {
                final InstrumentMessage msg = createRawMessage("MSFT", 100);
                while(true) {
                    loader.send (msg);
                }
            }
        };

        class MessageConsumer implements Runnable {
            volatile long messageCounter;

            @Override
            public void run() {
                while (true) {
                    cursor.next();
                    RawMessage received = (RawMessage) cursor.getMessage();
                    messageCounter++;
                }
            }
        };

        MessageConsumer consumer = new MessageConsumer();

        new Thread(consumer).start();
        new Thread(producer).start();

//        executor.execute(consumer);
//        executor.execute(producer);


        long lastSeenMessageCount = 0;
        long lastTime = System.currentTimeMillis();
        int index = 0;
        while (index++ < 5) {
            Thread.sleep(15000);

            final long messageCount = consumer.messageCounter;
            final long now = System.currentTimeMillis();
            long throughput = (messageCount - lastSeenMessageCount) / ((now - lastTime)/1000);
            System.out.println("Test AnonymousStream throughput processed " + throughput + " messages/sec");
            lastSeenMessageCount = messageCount;
            lastTime = now;
        }
    }

    private static InstrumentMessage createRawMessage(String symbol, int size) {
        RawMessage msg = new RawMessage(StreamConfigurationHelper.BINARY_MESSAGE_DESCRIPTOR);
        msg.setSymbol(symbol);
        msg.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        msg.data = new byte[128];
        msg.offset = 0;
        msg.length = msg.data.length;
        Arrays.fill(msg.data, (byte) 1);
        return msg;
    }

    @Test
    public void testThoughput () throws InterruptedException {
        
        SelectionOptions so = new SelectionOptions(false, true);
        cursor = stream.createCursor (so);
        cursor.addEntity(new ConstantIdentityKey(   "MSFT"));

        final TradeMessage msg = createTradeMessage("MSFT");
        //Assert.assertTrue (cursor.getFilter().accept(msg));
        final int WARMUP = 20000;
        final int TEST   = 1000000;
        assert TEST >= WARMUP;

        final long [] startStop = new long [2];

        Thread producer = new Thread ("Producer") {
            @Override
            public void run() {

                for (int i=0; i < WARMUP; i++) {
                    loader.send (msg);
                }
                startStop [0] = System.nanoTime();
                for (int i=0; i < TEST; i++) {
                    loader.send (msg);
                }
            }
        };

        Thread consumer = new Thread ("Consumer") {
            @Override
            public void run() {

                for (int i=0; i < WARMUP; i++) {
                    cursor.next();
                    //Assert.assertTrue ("next", cursor.next()); // live cursor will wait for msg
                    MarketMessage received = (MarketMessage) cursor.getMessage();
                }

                for (int i=0; i < TEST; i++) {
                    cursor.next();
                    //Assert.assertTrue ("next", cursor.next()); // live cursor will wait for msg
                    MarketMessage received = (MarketMessage) cursor.getMessage();
                }
                startStop [1] = System.nanoTime();
            }
        };


        consumer.start();
        producer.start();

        producer.join();
        consumer.join();   // lossless stream

        System.out.printf("Throughput: %,.0f msg/s \n", 1E9 * (long) TEST / (startStop[1] - startStop[0]));
    }


    private static TradeMessage createTradeMessage(String symbol) {
        TradeMessage msg = new TradeMessage();
        msg.setSymbol(symbol);
        msg.setOriginalTimestamp(System.currentTimeMillis());
        msg.setTimeStampMs(msg.getOriginalTimestamp());
        msg.setPrice(25);
        msg.setSize(10);
        return msg;
    }


    private static RecordClassDescriptor makeTradeMessageDescriptor ()
    {
        RecordClassDescriptor marketMsgDescriptor = mkMarketMessageDescriptor (840);


        return (StreamConfigurationHelper.mkTradeMessageDescriptor (marketMsgDescriptor, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO));
    }

    public static RecordClassDescriptor     mkMarketMessageDescriptor (
        Integer                 staticCurrencyCode
    )
    {
        final String            name = MarketMessage.class.getName ();
        final DataField []      fields = {
            new NonStaticDataField(
                "originalTimestamp", "Original Time",
                new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
            StreamConfigurationHelper.mkField (
                "currencyCode", "Currency Code",
                new IntegerDataType (IntegerDataType.ENCODING_INT16, true), null,
                staticCurrencyCode
            )
        };

        return (new RecordClassDescriptor (name, name, true, null, fields));
    }

    private static void sleep (long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}