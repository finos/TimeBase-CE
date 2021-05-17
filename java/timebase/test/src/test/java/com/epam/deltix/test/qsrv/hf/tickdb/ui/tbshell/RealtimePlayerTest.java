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
package com.epam.deltix.test.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerCommandProcessor;
import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author Alexei Osipov
 */
public class RealtimePlayerTest {

    @Test
    public void test() throws Exception {
        String stream1Src = "ticks-src";
        String stream1Dst = "ticks-dst";

        TickDBShellTestAccessor tickDBShell = new TickDBShellTestAccessor();
        String tempDbLocation = TDBRunner.getTemporaryLocation();
        tickDBShell.doCommand("set", "db " + tempDbLocation);
        tickDBShell.dbmgr.doCommand("open", "");

        // Src stream
        DXTickStream srcStream = createTmpStream(stream1Src, tickDBShell);
        int messagesInSourceStream = 10;
        populateSrcStream(srcStream, messagesInSourceStream);

        // Dest stream
        DXTickStream dstStream = createTmpStream(stream1Dst, tickDBShell);

        PlayerCommandProcessor player = new PlayerCommandProcessor(tickDBShell);

        assertTrue(player.doSet("player", "realtime"));
        assertTrue(player.doCommand("playback", "add " + stream1Src + " " + stream1Dst));
        assertTrue(player.doSet("cyclic", "true"));
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
        int count = 0;
        while (count <= messagesInSourceStream && dstReader.next()) {
            count += 1;
        }
        assertTrue(messagesInSourceStream < count);
        dstReader.close();
    }

    private void populateSrcStream(DXTickStream srcStream, int messageCount) {
        TickLoader stubLoader = srcStream.createLoader(new LoadingOptions(true));
        InstrumentMessage rawMessage = createRawMessage("TST");
        for (int i = 0; i < messageCount; i++) {
            rawMessage.setTimeStampMs(System.currentTimeMillis());
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