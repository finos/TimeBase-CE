package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TimeMessage;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerCommandProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.TickDBShellTestAccessor;
import org.junit.Assert;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexei Osipov
 */
public class VirtualPlayerTest {
    private static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Test
    public void test() throws Exception {
        String timeMessageStream = "timeMessageStream";
        String stream1Src = "ticks-src srcpPart2";
        String stream1Dst = "ticks-dst";

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor(); //Mockito.mock(TickDBShell.class);
        String tempDbLocation = TDBRunner.getTemporaryLocation();
        tickDBShell.doCommand("set", "db " + tempDbLocation);
        tickDBShell.dbmgr.doCommand("open", "");

        // TimeMessage stream
        StreamOptions timeStreamOpts = new StreamOptions(StreamScope.DURABLE, null, null, 1);
        timeStreamOpts.setFixedType(TimeMessage.DESCRIPTOR);
        //tickDBShell.dbmgr.getDB().createStream(timeMessageStream, timeStreamOpts);

        // Src stream
        DXTickStream srcStream = createTmpStream(stream1Src, tickDBShell);
        long startTs = System.currentTimeMillis();
        populateSrcStream(srcStream);

        // Dest stream
        DXTickStream dstStream = createTmpStream(stream1Dst, tickDBShell);

        PlayerCommandProcessor player = new PlayerCommandProcessor(tickDBShell);

        assertTrue(player.doSet("player", "virtual"));
        assertTrue(player.doSet("timer", timeMessageStream));
        assertTrue(tickDBShell.doSet("space", "off"));
        assertTrue(player.doCommand("playback", "add " + stream1Src + ", " + stream1Dst));
        assertTrue(tickDBShell.doSet("space", "on"));
        assertTrue(player.doSet("speed", "2"));
        assertTrue(player.doSet("frequency", "250"));
        assertTrue(player.doSet("vtime", df.format(startTs)));
        assertTrue(player.doCommand("play", ""));

        Thread.sleep(2_000);

        assertTrue(player.doCommand("pause", ""));
        Thread.sleep(500);
        assertTrue(player.doCommand("resume", ""));
        Thread.sleep(500);
        assertTrue(player.doCommand("stop", ""));
        assertTrue(player.doCommand("play", ""));
        Thread.sleep(500);
        assertTrue(player.doCommand("stop", ""));

        TickCursor dstReader = dstStream.select(0, new SelectionOptions(true, false));
        assertTrue(dstReader.next());
        dstReader.close();
        tickDBShell.dbmgr.getDB().close();
    }

    @Test
    public void testDestvtime() throws Exception {
        String timeMessageStream = "timeMessageStream";
        String stream1Src = "ticks-src";
        String stream1Dst = "ticks-dst";

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor(); //Mockito.mock(TickDBShell.class);
        String tempDbLocation = TDBRunner.getTemporaryLocation();
        tickDBShell.doCommand("set", "db " + tempDbLocation);
        tickDBShell.dbmgr.doCommand("open", "");

        // TimeMessage stream
        StreamOptions timeStreamOpts = new StreamOptions(StreamScope.DURABLE, null, null, 1);
        timeStreamOpts.setFixedType(TimeMessage.DESCRIPTOR);
        //tickDBShell.dbmgr.getDB().createStream(timeMessageStream, timeStreamOpts);

        // Src stream
        DXTickStream srcStream = createTmpStream(stream1Src, tickDBShell);
        long startTs = System.currentTimeMillis();

        long srcStartTime = startTs - TimeUnit.HOURS.toMillis(1);

        int stepMs = 100;
        double speed = 10;
        int messageCount = 100;
        populateSrcStream(srcStream, srcStartTime, stepMs, messageCount);
        TickCursor srcReader = srcStream.select(srcStartTime, new SelectionOptions(true, false));
        verifySrc(srcStartTime, stepMs, srcReader, messageCount);

        // Dest stream
        DXTickStream dstStream = createTmpStream(stream1Dst, tickDBShell);

        PlayerCommandProcessor player = new PlayerCommandProcessor(tickDBShell);

        //tickDBShell.doSet();
        assertTrue(tickDBShell.doSet("tz", df.getTimeZone().getID()));
        assertTrue(player.doSet("player", "virtual"));
        assertTrue(player.doSet("timer", timeMessageStream));
        assertTrue(tickDBShell.doSet("space", "off"));
        assertTrue(player.doCommand("playback", "add " + stream1Src + ", " + stream1Dst));
        assertTrue(tickDBShell.doSet("space", "on"));

        assertTrue(player.doSet("speed", speed+""));
        assertTrue(player.doSet("frequency", "10"));
        assertTrue(tickDBShell.doSet("time", df.format(srcStartTime)));
        assertTrue(player.doSet("destvtime", df.format(startTs)));
        assertTrue(player.doCommand("play", ""));

        long timeToWait = (long) (messageCount * stepMs / speed + 100);
        Thread.sleep(timeToWait);


        TickCursor dstReader = dstStream.select(0, new SelectionOptions(true, true));
        int count = 0;
        while (dstReader.next()) {
            long timeStampMs = dstReader.getMessage().getTimeStampMs();
            long expectedTime = startTs + (long) (stepMs * count / speed);
            assertEquals(expectedTime, timeStampMs);
            count++;
            if (count == messageCount) {
                break;
            }
        }
        assertEquals(messageCount, count);
        dstReader.close();
        tickDBShell.dbmgr.getDB().close();
    }

    private void verifySrc(long startTs, int stepMs, TickCursor srcReader, int messageCount) {
        int count = 0;
        while (srcReader.next()) {
            long timeStampMs = srcReader.getMessage().getTimeStampMs();
            long expectedTime = startTs + (long) (stepMs * count);
            assertEquals(expectedTime, timeStampMs);
            count++;
        }
        Assert.assertEquals(messageCount, count);
    }

    @Test
    public void testEndtime() throws Exception {
        String timeMessageStream = "timeMessageStream";
        String stream1Src = "ticks-src srcpPart2";
        String stream1Dst = "ticks-dst";

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor(); //Mockito.mock(TickDBShell.class);
        String tempDbLocation = TDBRunner.getTemporaryLocation();
        tickDBShell.doCommand("set", "db " + tempDbLocation);
        tickDBShell.dbmgr.doCommand("open", "");

        // TimeMessage stream
        StreamOptions timeStreamOpts = new StreamOptions(StreamScope.DURABLE, null, null, 1);
        timeStreamOpts.setFixedType(TimeMessage.DESCRIPTOR);
        //tickDBShell.dbmgr.getDB().createStream(timeMessageStream, timeStreamOpts);

        // Src stream
        DXTickStream srcStream = createTmpStream(stream1Src, tickDBShell);
        long startTs = System.currentTimeMillis();
        populateSrcStream(srcStream);

        // Dest stream
        DXTickStream dstStream = createTmpStream(stream1Dst, tickDBShell);

        PlayerCommandProcessor player = new PlayerCommandProcessor(tickDBShell);

        assertTrue(player.doSet("player", "virtual"));
        assertTrue(player.doSet("timer", timeMessageStream));
        assertTrue(tickDBShell.doSet("space", "off"));
        assertTrue(player.doCommand("playback", "add " + stream1Src + ", " + stream1Dst));
        assertTrue(tickDBShell.doSet("space", "on"));
        assertTrue(player.doSet("speed", "2"));
        assertTrue(player.doSet("frequency", "250"));
        assertTrue(player.doSet("vtime", df.format(startTs)));
        assertTrue(tickDBShell.doSet("endtime", df.format(startTs + 1000)));
        assertTrue(player.doCommand("play", ""));

        Thread.sleep(2_000);

        assertTrue(player.doCommand("stop", ""));

        TickCursor dstReader = dstStream.select(0, new SelectionOptions(true, false));
        assertTrue(dstReader.next());
        dstReader.close();
        tickDBShell.dbmgr.getDB().close();
    }

    @Test
    public void testTimebufferOption() throws Exception {
        String timeMessageStream = "timeMessageStream";
        String stream1Src = "ticks-src";
        String stream1Dst = "ticks-dst";

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor(); //Mockito.mock(TickDBShell.class);
        String tempDbLocation = TDBRunner.getTemporaryLocation();
        tickDBShell.doCommand("set", "db " + tempDbLocation);
        tickDBShell.dbmgr.doCommand("open", "");

        // Src stream
        DXTickStream srcStream = createTmpStream(stream1Src, tickDBShell);
        long virtualStartTs = 10_000_000;
        populateSrcStream(srcStream, virtualStartTs, 100, 100);

        // Dest stream
        DXTickStream dstStream = createTmpStream(stream1Dst, tickDBShell);

        PlayerCommandProcessor player = new PlayerCommandProcessor(tickDBShell);

        assertTrue(player.doSet("player", "virtual"));
        assertTrue(player.doSet("timer", timeMessageStream));
        assertTrue(player.doCommand("playback", "add " + stream1Src + " " + stream1Dst));
        assertTrue(player.doSet("speed", "2"));
        int interval = 250;
        assertTrue(player.doSet("frequency", Integer.toString(interval)));
        assertTrue(player.doSet("timebuffer", "1"));
        assertTrue(player.doSet("vtime", df.format(virtualStartTs)));
        assertTrue(player.doCommand("play", ""));
        final TickCursor timerCursor = tickDBShell.dbmgr.getDB().getStream(timeMessageStream).select(0, new SelectionOptions(true, true));
        timerCursor.next();
        long timeStamp1 = timerCursor.getMessage().getTimeStampMs();

        int timeBeforeStop = 5_000;
        int timeMessagesToSkip = 2;
        Thread timerCursorThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeBeforeStop + 500);
                } catch (InterruptedException ignored) {
                }
                synchronized (timerCursor) {
                    for (int i = 0; i < timeMessagesToSkip; i++) {
                        timerCursor.next();
                    }
                }
            }
        };
        timerCursorThread.start();
        Thread.sleep(timeBeforeStop);
        assertTrue(player.doCommand("stop", ""));
        timerCursorThread.join();

        long timeStamp2;
        synchronized (timerCursor) {
            timeStamp2 = timerCursor.getMessage().getTimeStampMs();
            Assert.assertTrue(timeStamp2 - timeStamp1 == interval * timeMessagesToSkip);
        }

        TickCursor dstReader = dstStream.select(0, new SelectionOptions(true, false));
        assertTrue(dstReader.next());
        long timeStamp3 = timeStamp2 + interval; // We might get messages for next interval
        int copiedMessageCount = 0;
        do {
            long msgTs = dstReader.getMessage().getTimeStampMs();
            System.out.println("MsgTs: " + msgTs + " timeStamp2: " + timeStamp2);
            assertTrue(msgTs <= timeStamp3);
            copiedMessageCount++;
        } while (dstReader.next());
        assertEquals(100, copiedMessageCount);
        dstReader.close();
        timerCursor.close();
        tickDBShell.dbmgr.getDB().close();
    }

    private void populateSrcStream(DXTickStream srcStream) {
        populateSrcStream(srcStream, System.currentTimeMillis(), 100, 100);
    }

    private void populateSrcStream(DXTickStream srcStream, long startTime, int stepMs, int count) {
        TickLoader stubLoader = srcStream.createLoader(new LoadingOptions(true));
        InstrumentMessage rawMessage = createRawMessage("TST");
        for (int i = 0; i < count; i++) {
            rawMessage.setTimeStampMs(startTime + i * stepMs);
            stubLoader.send(rawMessage);
        }
    }

    private DXTickStream createTmpStream(String streamName, TickDBShellTestAccessor tickDBShell) {
        StreamOptions options = new StreamOptions(StreamScope.DURABLE, null, null, 1);
        options.setFixedType(Messages.BINARY_MESSAGE_DESCRIPTOR);
        return tickDBShell.dbmgr.getDB().createStream(streamName, options);
    }


    private static final String MESSAGE = "8=FIX.4.4\u00019=83\u000135=5\u000134=1\u000149=XXXXXXXXXX\u000150=XXXXXXXXXX\u000152=20131013-21:10:01.513\u000156=CITIFX\u000157=CITIFX\u000110=056\u0001";
    private static final byte[] MESSAGE_BYTES = getBytes(MESSAGE);

    private static byte[] getBytes(String asciiText) {
        try {
            return asciiText.getBytes("US-ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Expecting ASCII string", e);
        }
    }
    // TODO: do not repeat same function
    private static InstrumentMessage createRawMessage(String symbol) {
        RawMessage msg = new RawMessage(Messages.BINARY_MESSAGE_DESCRIPTOR);
        msg.setSymbol(symbol);

        msg.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN);
        msg.data = MESSAGE_BYTES;
        msg.offset = 0;
        msg.length = msg.data.length;
        Arrays.fill(msg.data, (byte) 1);
        return msg;
    }
}