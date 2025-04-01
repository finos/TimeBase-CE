package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerCommandProcessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerInterface;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.RealtimePlayer;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.util.JUnitCategories.TickDBFast;
import com.epam.deltix.util.lang.Util;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.GregorianCalendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


@Category(TickDBFast.class)
public class Test_RealtimePlayerTest {

    public static final String SOURCE_STREAM_NAME = "simple";
    public static final String DEST_1 = "dest";
    public static final String DEST_2 = "dest2";
    public static final String SYMBOL = "AAPL";

    private static TDBRunner runner;
    private static TickDBShell shell;
    private static DXTickDB tickDb;
    private static long startTime;
    private static long endTime;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new TDBRunner(true, true, new TomcatServer());
        runner.startup();
        tickDb = runner.getTickDb();
        shell = new TickDBShell();
        shell.dbmgr.setDb(tickDb);
        DXTickStream sourceStream = createSourceStream(1000, 50);
        startTime = sourceStream.getTimeRange()[0];
        endTime = sourceStream.getTimeRange()[1];
    }
    @After
    public void deleteStreams() {
        for (DXTickStream stream : tickDb.listStreams()) {
            if (stream.getKey().equals(DEST_1) || stream.getKey().endsWith(DEST_2))
                stream.delete();
        }
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void skipTimeGapTest() throws InterruptedException {
        String srcStream = "gapBar";
        createStreamWithTimeGap(srcStream);
        PlayerInterface realtimePlayer = createRealtimePlayer(srcStream, DEST_1, startTime, 1);
        realtimePlayer.play();
        Thread.sleep(6000);
        realtimePlayer.next();
        Thread.sleep(4500);
        realtimePlayer.stop();
        Thread.sleep(1000);
        assertEquals(11, getCount(DEST_1));
    }

    @Test
    public void cycleOnEndSrcTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, Long.MAX_VALUE, 100, true);
        realtimePlayer.play();
        Thread.sleep(1100);
        realtimePlayer.stop();
        Thread.sleep(1000);

        int count = getCount(DEST_1);
        assertTrue(count > 100 && count < 150);
    }

    @Test
    public void cycleOnEndTimeTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, endTime, 100, true);
        realtimePlayer.play();
        Thread.sleep(1100);
        realtimePlayer.stop();
        Thread.sleep(1100);

        int count = getCount(DEST_1);
        assertTrue(count > 100 && count < 150);
    }

    @Test
    public void parallelPlaybackTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, 2.5);
        PlayerInterface realtimePlayer2 = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_2, startTime, 2.5);
        realtimePlayer.play();
        realtimePlayer2.play();
        Thread.sleep(500);
        for (int i = 1; i < 6; i++) {
            realtimePlayer.setSpeed(i);
            realtimePlayer2.setSpeed(6 - i);
            Thread.sleep(500);
        }
        realtimePlayer.stop();
        realtimePlayer2.stop();
        Thread.sleep(1000);

        assertEquals(getCount(DEST_1), getCount(DEST_2));
    }

    @Test
    public void changeSpeedTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, 1);
        realtimePlayer.play();    //1
        Thread.sleep(1000);  //2
        realtimePlayer.setSpeed(2);
        Thread.sleep(1000);  //4
        realtimePlayer.setSpeed(4);
        Thread.sleep(1000);  //8
        realtimePlayer.setSpeed(0.5);
        Thread.sleep(2000);  //9
        realtimePlayer.setSpeed(1);
        Thread.sleep(1500);  //10
        realtimePlayer.stop();    //11
        Thread.sleep(1000);

        assertEquals(11, getCount(DEST_1));
    }

    @Test
    public void changeModTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, 1);

        realtimePlayer.play();   // 1
        Thread.sleep(500);  // 50/100
        realtimePlayer.pause();
        Thread.sleep(500);
        realtimePlayer.next();  // 2
        Thread.sleep(500); // 50/100
        realtimePlayer.pause();
        Thread.sleep(500);
        realtimePlayer.setSpeed(2);
        Thread.sleep(500);
        realtimePlayer.resume();
        Thread.sleep(500); // 3
        realtimePlayer.pause();
        realtimePlayer.stop();  // 4
        Thread.sleep(1000);

        assertEquals(4, getCount(DEST_1));
    }

    @Test
    public void changeSpeedAndRecalculateWaitTimeTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, 1);

        realtimePlayer.play();  // 1
        Thread.sleep(250); // 250/1000
        realtimePlayer.setSpeed(2);
        Thread.sleep(250); // 750/1000
        realtimePlayer.setSpeed(1);
        Thread.sleep(250); // 2
        realtimePlayer.setSpeed(0.5);
        Thread.sleep(1000); // 500/1000
        realtimePlayer.setSpeed(1);
        Thread.sleep(250);  // 750/1000
        realtimePlayer.setSpeed(5);
        Thread.sleep(100);  // 3
        realtimePlayer.stop();   // 4
        Thread.sleep(1000);

        assertEquals(4, getCount(DEST_1));

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSpeedTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, 1);
        realtimePlayer.play();
        Thread.sleep(500);
        realtimePlayer.setSpeed(-1);
        Thread.sleep(500);
    }

    @Test
    public void resumeWaitingAfterPauseTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, 1);
        realtimePlayer.play(); // 1
        for (int i = 0; i < 9; i++) { // 1 + (250 * 9 / 1000) = 3
            Thread.sleep(250);
            realtimePlayer.pause();
            Thread.sleep(250);
            realtimePlayer.resume();
        }
        realtimePlayer.stop(); // 4
        Thread.sleep(1000);

        assertEquals(4, getCount(DEST_1));
    }

    @Test(timeout = 10000)
    public void highlyLoadedSkipTest() throws InterruptedException, Introspector.IntrospectionException {
        PrintStream originalErr = System.err;
        PrintStream noopPrintStream = new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        });
        System.setErr(noopPrintStream);

        Introspector emptyMessageIntrospector = Introspector.createEmptyMessageIntrospector();
        RecordClassDescriptor descriptor = emptyMessageIntrospector.introspectRecordClass(BestBidOfferMessage.class);
        RecordClassDescriptor barDescriptor = emptyMessageIntrospector.introspectRecordClass(BarMessage.class);

        DXTickStream source = createStream("newSource", descriptor, barDescriptor);
        loadBarStream(1000, 50, source);
        createStream(DEST_1, descriptor);
        PlayerInterface realtimePlayer = createRealtimePlayer("newSource", DEST_1, startTime, Long.MAX_VALUE, Long.MAX_VALUE, true);
        realtimePlayer.play();
        Thread.sleep(100);
        realtimePlayer.stop();

        System.setErr(originalErr);

        assertEquals(0,getCount(DEST_1));
    }

    @Test(timeout = 10000)
    public void highlyLoadedCommandTest() throws InterruptedException {
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, Long.MAX_VALUE, Long.MAX_VALUE, true);
        realtimePlayer.play();
        Thread.sleep(1000);
        realtimePlayer.next();
        realtimePlayer.pause();
        realtimePlayer.resume();
        realtimePlayer.setSpeed(1);
        realtimePlayer.pause();
        realtimePlayer.setSpeed(Long.MAX_VALUE);
        realtimePlayer.pause();
        realtimePlayer.next();
        realtimePlayer.stop();
        assertTrue(getCount(DEST_1) > 0);
    }

    @Test(timeout = 10000)
    public void invalidStartTime() throws InterruptedException {
        long startTime = Long.MAX_VALUE;
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, Long.MAX_VALUE, Long.MAX_VALUE, false);
        realtimePlayer.play();
        Thread.sleep(1000);
        realtimePlayer.pause();
        assertEquals(0, getCount(DEST_1));
    }

    @Test(timeout = 10000)
    public void incorrectStartTime() throws InterruptedException {
        long startTime = endTime + 1;
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, Long.MAX_VALUE, Long.MAX_VALUE, false);
        realtimePlayer.play();
        Thread.sleep(1000);
        realtimePlayer.pause();
        realtimePlayer.next();
        assertEquals(0, getCount(DEST_1));
    }

    @Test(timeout = 10000)
    public void startFromMinTimestamp() throws InterruptedException {

        long startTime = Long.MIN_VALUE;
        PlayerInterface realtimePlayer = createRealtimePlayer(SOURCE_STREAM_NAME, DEST_1, startTime, Long.MAX_VALUE, Long.MAX_VALUE, false);
        realtimePlayer.play();
        Thread.sleep(2000);
        assertEquals(50, getCount(DEST_1));
    }


    @Test(timeout = 10000)
    public void emptyStreamPlayback() throws InterruptedException {
        String streamName = "emptyStream";
        DXTickStream stream = createEmptyStream(streamName);
        long startTime = Long.MIN_VALUE;
        PlayerInterface realtimePlayer = createRealtimePlayer(streamName, DEST_1, startTime, Long.MAX_VALUE, Long.MAX_VALUE, false);
        realtimePlayer.play();
        Thread.sleep(1000);
        assertEquals(0, getCount(DEST_1));
    }

    private PlayerInterface createRealtimePlayer(String source, String dest, long from, double speed) {
        return createRealtimePlayer(source, dest, from, Long.MAX_VALUE, speed, false);
    }
    private PlayerInterface createRealtimePlayer(String source, String dest, long from, long to, double speed, boolean cyclic) {
        PlayerCommandProcessor.Config config = new PlayerCommandProcessor.Config();
        createStreamIfPossible(dest, source);
        config.streamMapping.put(source, dest);
        config.time = from;
        config.speed = speed;
        config.cyclic = cyclic;
        config.stopAtTimestamp = to;
        return RealtimePlayer.create(config, shell);
    }

    private static DXTickStream createSourceStream(int interval, int count){
        DXTickStream stream = createEmptyStream(SOURCE_STREAM_NAME);
        return loadBarStream(interval, count, stream);
    }

    private static DXTickStream createEmptyStream(String streamName) {
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null,
                "DECIMAL(4)",
                "DECIMAL(0)"
        );
        StreamOptions so = StreamOptions.fixedType(StreamScope.DURABLE, streamName,  null, 0, descriptor);
        return tickDb.createStream(streamName, so);
    }

    @NotNull
    private static DXTickStream loadBarStream(int interval, int count, DXTickStream stream) {
        TDBRunner.BarsGenerator g = new TDBRunner.BarsGenerator(new GregorianCalendar(2017, 0, 1), interval, count, SYMBOL);
        try (TickLoader loader = stream.createLoader()) {
            while (g.next())
                loader.send(g.getMessage());
        }
        return stream;
    }

    private static DXTickStream createStream(String key, RecordClassDescriptor... descriptor){
        DXTickStream stream = tickDb.getStream(key);
        if (stream == null) {
            StreamOptions so = StreamOptions.polymorphic(StreamScope.DURABLE, key,  null, 0, descriptor);
            stream = tickDb.createStream(key, so);
            return stream;
        }
        return stream;
    }

    private static DXTickStream createStreamWithTimeGap(String streamKey){
        RecordClassDescriptor descriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null,
                "DECIMAL(4)",
                "DECIMAL(0)"
        );
        StreamOptions so = StreamOptions.fixedType(StreamScope.DURABLE, streamKey,  null, 0, descriptor);
        TDBRunner.BarsGenerator g = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2017, 0, 1), 1000, 55, SYMBOL) {
            private int processedMessage = 0;
            @Override
            public boolean next() {
                processedMessage++;
                boolean next = super.next();
                if (next && processedMessage > 5) {
                    current.setTimeStampMs(current.getTimeStampMs() + 10000);
                }
                return next;
            }
        };

        DXTickStream stream = tickDb.createStream(streamKey, so);

        try (TickLoader loader = stream.createLoader()) {
            while (g.next())
                loader.send(g.getMessage());
        }
        return stream;
    }

    private void createStreamIfPossible(String targetStreamName, String sourceStreamName) {
        DXTickStream stream = tickDb.getStream(targetStreamName);
        if (stream == null) {
            DXTickStream source = tickDb.getStream(sourceStreamName);
            if (source != null) {
                StreamOptions options = source.getStreamOptions();
                options.version = null;
                options.name = targetStreamName;
                tickDb.createStream(targetStreamName, options);
            }
        }
    }

    private int getCount(String streamName) {
        TickCursor cursor1 = TickCursorFactory.create(tickDb.getStream(streamName), Long.MIN_VALUE, SYMBOL);
        return countMessages(cursor1);
    }


    private int countMessages(TickCursor cursor) {
        try {
            int count = 0;
            while (cursor.next())
                count++;
            return (count);
        } finally {
            Util.close(cursor);
        }
    }

}
