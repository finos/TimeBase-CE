package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.util.LiveCursorWatcher;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.timebase.messages.TimeStamp;
import org.junit.Test;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBStress;

@Category(TickDBStress.class)
public class Test_TBMemory {

    final int NUMBER_OF_CURSORS = 10_000;

    @Test
    public void testConnectionAllocations() throws Throwable {

        Runtime     rt = Runtime.getRuntime ();
        long        usedBefore = (rt.totalMemory () - rt.freeMemory ());

        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor bar = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
                null, 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

//        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
//                true, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
//        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
//                "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
//
//        RecordClassDescriptor l2 = StreamConfigurationHelper.mkLevel2MessageDescriptor(marketMsgDescriptor,
//                "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        TDBRunner runner = new com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner(true, true);
        runner.startup();

        runner.createStream(runner.getServerDb(), "loading", StreamOptions.polymorphic(StreamScope.DURABLE, "loading", null, 400, bar));

        BarMessage message = new BarMessage();
        message.setSymbol("ES1");

        message.setTimeStampMs(TimeStamp.TIMESTAMP_UNKNOWN);
        message.setCurrencyCode((short)840);

        DXTickDB db = new TickDBClient("localhost", runner.getPort());

        for (int i = 0; i < 1000; i++) {

            db.open(false);

            message.setVolume(i);

            try (TickLoader loader = db.getStream("loading").createLoader() ) {
                for (int j = 0; j < 100; j++) {
                    message.setSymbol("ES" + j);
                    message.setClose(j);
                    loader.send(message);
                }
            }

            SelectionOptions options = new SelectionOptions(false, false);
            options.channelQOS = ChannelQualityOfService.MAX_THROUGHPUT; // compiled codecs

            try (TickCursor cursor = db.getStream("loading").select(0, options) ) {
                for (int j = 0; j < 40; j++) {
                    cursor.next();
                }
            }

            options.channelQOS = ChannelQualityOfService.MIN_INIT_TIME; // interpreted codecs

            try (TickCursor cursor = db.getStream("loading").select(0, options) ) {
                for (int j = 0; j < 40; j++) {
                    cursor.next();
                }
            }

            db.close();
        }

        runner.shutdown();

        long        usedAfter = (rt.totalMemory () - rt.freeMemory ());
        System.out.println("Memory delta: " + (usedAfter - usedBefore) / (1 << 20) + " MB");
    }

    @Test
    public void testCursors() throws Throwable {
        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor bar = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
            null, 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

//        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
//                true, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
//        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
//                "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);
//
//        RecordClassDescriptor l2 = StreamConfigurationHelper.mkLevel2MessageDescriptor(marketMsgDescriptor,
//                "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        TDBRunner runner = new com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner(true, true);
        runner.startup();

        runner.createStream(runner.getServerDb(), "selection", StreamOptions.polymorphic(StreamScope.DURABLE, "selection", null, 400, bar));

        BarMessage message = new BarMessage();
        message.setSymbol("ES1");

        message.setTimeStampMs(TimeStamp.TIMESTAMP_UNKNOWN);
        message.setCurrencyCode((short)840);

        SelectionOptions right = new SelectionOptions(true, false, false);

        DXTickStream stream = runner.getTickDb().getStream("selection");

        try (TickLoader loader = stream.createLoader() ) {
            for (int j = 0; j < 100; j++) {
                message.setSymbol("ES" + j);
                message.setClose(j);
                loader.send(message);
            }
        }

        for (int i = 0; i < NUMBER_OF_CURSORS; i++) {
            try (TickCursor cursor = stream.select(0, right) ) {
                for (int j = 0; j < 10; j++) {
                    cursor.next();
                }
            }

            if (i > 0 && i % (NUMBER_OF_CURSORS / 10) == 0)
                System.out.println("Processed " + i * 100 / NUMBER_OF_CURSORS + "% cursors.");
        }

        System.out.println();

        SelectionOptions reversed = new SelectionOptions(true, false, true);

        for (int i = 0; i < NUMBER_OF_CURSORS; i++) {
            try (TickCursor cursor = stream.select(Long.MAX_VALUE, reversed) ) {
                for (int j = 0; j < 100; j++) {
                    cursor.next();
                }
            }

            if (i > 0 && i % (NUMBER_OF_CURSORS / 10) == 0)
                System.out.println("Processed " + i * 100 / NUMBER_OF_CURSORS + "% of reverse cursors.");
        }

        runner.shutdown();
    }

    @Test
    public void testTransientCursors() throws Throwable {

        TDBRunner runner = new ServerRunner(true, true);
        runner.startup();

        RecordClassDescriptor marketMsgDescriptor =
            StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor bar = StreamConfigurationHelper.mkBarMessageDescriptor(marketMsgDescriptor,
            null, 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        StreamOptions options = StreamOptions.polymorphic(StreamScope.TRANSIENT, "transient.lossless", null, 400, bar);
        (options.bufferOptions = new BufferOptions()).lossless = true;
        DXTickStream stream = runner.createStream(runner.getTickDb(), "transient.lossless", options);

        testCursors(stream);

        options = StreamOptions.polymorphic(StreamScope.TRANSIENT, "transient.lossy", null, 400, bar);
        (options.bufferOptions = new BufferOptions()).lossless = false;
        stream = runner.createStream(runner.getTickDb(), "transient.lossy", options);

        testCursors(stream);

        runner.shutdown();
    }

    public void testCursors(DXTickStream stream) throws InterruptedException {
        SelectionOptions right = new SelectionOptions(true, true, false);

        LiveCursorWatcher.MessageListener listener = new LiveCursorWatcher.MessageListener() {
            @Override
            public void onMessage(InstrumentMessage m) {
            }
        };

        for (int i = 0; i < NUMBER_OF_CURSORS; i++) {

            try (TickCursor cursor = stream.select(Long.MIN_VALUE, right)) {
                LiveCursorWatcher watcher = new LiveCursorWatcher(cursor, listener);
                watcher.close();
            }

            if (i > 0 && i % (NUMBER_OF_CURSORS / 10) == 0)
                System.out.println("Processed " + i * 100 / NUMBER_OF_CURSORS + "% cursors.");
        }
    }
}
