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
package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.TimeMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBTestBase;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerCommandProcessor;
import com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell.TickDBShellTestAccessor;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexei Osipov
 */
@Category(Long.class)
public class VirtualPlayerRemoteTest extends TDBTestBase {
    public VirtualPlayerRemoteTest() {
        super(true);
    }

    /**
     * Test plan:
     * 0) Populate source stream with messages that come with 20ms interval
     * 1) Launch VirtualPlayer with 250ms slice interval
     * 2) Attach timer cursor consumer but not read messages
     * 3) Wait 5 seconds
     * 4) Stop player
     * 5) Assert that there are only messages for first 500 ms in timer stream (not for 5 seconds).
     * Expected result:
     * Messages for first 500ms are copied + there is 1 timer message in stream buffer and about 3 messages in other buffers (each represents 250ms slice).
     * (500ms (time before pause) + 4x 250ms ) / 20ms (data message interval) = 1000 / 20 ~= 75 (messages with data).
     */
    @Test
    public void testTimebufferOption() throws Exception {
        String timeMessageStream = "timeMessageStream";
        String stream1Src = "ticks-src";
        String stream1Dst = "ticks-dst";

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor(); //Mockito.mock(TickDBShell.class);
        tickDBShell.dbmgr.setDb(getTickDb());

        // Src stream
        DXTickStream srcStream = createTmpStream(stream1Src, tickDBShell);
        long virtualStartTs = 10_000_000;
        populateSrcStream(srcStream, virtualStartTs, 20, 10000);

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
        assertTrue(player.doSet("vtime", tickDBShell.formatTime(virtualStartTs)));
        assertTrue(player.doCommand("play", ""));
        SelectionOptions timerCursorOptions = new SelectionOptions(false, true);
        timerCursorOptions.channelBufferSize = TimeMessage.getTimeMessageSizeInTransientStream();

        final TickCursor timerCursor = tickDBShell.dbmgr.getDB().getStream(timeMessageStream).select(
                0,
                timerCursorOptions,
                new String[]{TimeMessage.class.getName()}
        );
        //timerCursor.setAvailabilityListener(() -> {}); // Empty availability listener to fail fast when we reach end

        Assert.assertTrue(timerCursor.next()); // Skip first timer message
        long firstTimeMessageTimestamp = timerCursor.getMessage().getTimeStampMs();
        assertEquals(virtualStartTs + interval, firstTimeMessageTimestamp);

        int timeBeforePause = 5_000;
        int timeMessagesToSkip = 6;
        Thread timerCursorThread = new Thread("Timer thread") {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeBeforePause + 500);
                } catch (InterruptedException ignored) {
                }
                System.out.println("Reading messages from timer stream");
                synchronized (timerCursor) {
                    for (int i = 0; i < timeMessagesToSkip; i++) {
                        Assert.assertTrue(timerCursor.next());
                    }
                }
            }
        };
        timerCursorThread.start();
        System.out.println("Going to sleep");
        Thread.sleep(timeBeforePause);
        System.out.println("Pausing");
        // Note: we can't "stop" here because attempt to stop will block because there some data to be sent
        assertTrue(player.doCommand("pause", ""));
        timerCursorThread.join();

        long lastTimeMessageTimestamp;
        synchronized (timerCursor) {
            lastTimeMessageTimestamp = timerCursor.getMessage().getTimeStampMs();
            Assert.assertTrue(lastTimeMessageTimestamp - firstTimeMessageTimestamp == interval * timeMessagesToSkip);
        }


        TickCursor dstReader = dstStream.select(0, new SelectionOptions(true, false));
        assertTrue(dstReader.next());
        long maxAcceptableDataMessageTimestamp = lastTimeMessageTimestamp + interval * 3; // We might get messages for next interval + buffer issues
        int copiedMessageCount = 0;
        do {
            long msgTs = dstReader.getMessage().getTimeStampMs();
            System.out.println("MsgTs: " + msgTs + " lastTimeMessageTimestamp: " + lastTimeMessageTimestamp + " diff:" + (lastTimeMessageTimestamp - msgTs));
            assertTrue(msgTs <= maxAcceptableDataMessageTimestamp);
            copiedMessageCount++;
        } while (dstReader.next());
        int maxExpectedDataMessages = 100;
        assertTrue(copiedMessageCount <= maxExpectedDataMessages);
        dstReader.close();
        timerCursor.close();
        assertTrue(player.doCommand("stop", ""));
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