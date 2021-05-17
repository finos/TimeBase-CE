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
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import com.epam.deltix.util.lang.Util;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;

/**
 * @author Alexei Osipov
 */
@Category(TickDBFast.class)
public class Test_MulticastCursor extends TDBTestBase {

    private DXTickStream stream;
    private TickLoader loader;
    private MessageSource<InstrumentMessage> cursor1;
    private MessageSource<InstrumentMessage> cursor2;

    public Test_MulticastCursor() {
        super(true);
    }

    @Before
    public void prepare () {
        //System.setProperty("deltix.tickdb.useDisruptorQueue", "true");
        DXTickDB db = getTickDb();

        StreamOptions options = new StreamOptions ();
        options.name = "test";
        options.scope = StreamScope.RUNTIME;
        options.bufferOptions = new BufferOptions();
        options.bufferOptions.initialBufferSize = 1*1024*1024;
        options.bufferOptions.maxBufferSize = 4*1024*1024;
        options.bufferOptions.lossless = true;
        options.setFixedType(makeTradeMessageDescriptor()); // TODO: Poly
        stream = db.createStream("Test_MulticastCursor_stream", options);

        LoadingOptions lo = new LoadingOptions(false);
        loader = stream.createLoader (lo);

    }

    @After
    public void finish () {
        Util.close(loader);
        Util.close(cursor1);
        Util.close(cursor2);
        //System.setProperty("deltix.tickdb.useDisruptorQueue", "false");
    }
    
    @Test
    public void testSendReceive() throws InterruptedException {
        /*
        SelectionOptions so = new SelectionOptions(false, true);
        ConstantIdentityKey key = new ConstantIdentityKey(InstrumentType.EQUITY, "MSFT");

        DXTickDB db = getTickDb();
        */
        TradeMessage msg = createTradeMessage("MSFT");

        cursor1 = stream.selectMulticast(false);
        long ts1 = System.currentTimeMillis();
        msg.setTimeStampMs(ts1);
        loader.send(msg);
        Assert.assertTrue(cursor1.next()); // live cursor will wait for msg

        cursor2 = stream.selectMulticast(false);
        long ts2 = System.currentTimeMillis();
        msg.setTimeStampMs(ts2);
        loader.send(msg);
        Assert.assertTrue(cursor2.next()); // live cursor will wait for msg


        TradeMessage received1 = (TradeMessage) cursor1.getMessage();
        TradeMessage received2 = (TradeMessage) cursor2.getMessage();
        Assert.assertEquals(ts1, received1.getTimeStampMs());
        Assert.assertEquals(ts2, received2.getTimeStampMs());
        Assert.assertEquals(msg.getSize(), received1.getSize(), 0);
        Assert.assertEquals(msg.getSize(), received2.getSize(), 0);
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
    public void testThroughput() throws InterruptedException {
        cursor1 = stream.selectMulticast (false);
        cursor2 = stream.selectMulticast (false);

        final TradeMessage msg = createTradeMessage("MSFT");
        //Assert.assertTrue (cursor.getFilter().accept(msg));
        final int WARMUP = 5_000_000;
        final int TEST   = 10_000_000;
        assert TEST >= WARMUP;

        final long [] startStop = new long [1];

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

        ThroughputConsumer consumer1 = new ThroughputConsumer(WARMUP, TEST, cursor1, "Consumer1");
        ThroughputConsumer consumer2 = new ThroughputConsumer(WARMUP, TEST, cursor2, "Consumer2");


        consumer1.start();
        consumer2.start();
        producer.start();

        producer.join();
        consumer1.join();   // lossless stream
        consumer2.join();   // lossless stream

        System.out.printf("Throughput: %,.0f msg/s \n", 1E9 * (long) TEST / (consumer1.getStopTime() - startStop[0]));
        System.out.printf("Throughput: %,.0f msg/s \n", 1E9 * (long) TEST / (consumer2.getStopTime() - startStop[0]));
    }

    private static class ThroughputConsumer extends Thread {
        private final int WARMUP;
        private final int TEST;
        private long stopTime;
        private final MessageSource<InstrumentMessage> cursor;

        public ThroughputConsumer(int WARMUP, int TEST, MessageSource<InstrumentMessage> cursor, String name) {
            super(name);
            this.WARMUP = WARMUP;
            this.TEST = TEST;
            this.cursor = cursor;
        }

        @Override
        public void run() {

            for (int i = 0; i < WARMUP; i++) {
                cursor.next();
                //Assert.assertTrue ("next", cursor.next()); // live cursor will wait for msg
                MarketMessage received = (MarketMessage) cursor.getMessage();
            }

            for (int i = 0; i < TEST; i++) {
                cursor.next();
                //Assert.assertTrue ("next", cursor.next()); // live cursor will wait for msg
                MarketMessage received = (MarketMessage) cursor.getMessage();
            }
            stopTime = System.nanoTime();
        }

        public long getStopTime() {
            return stopTime;
        }
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
}
