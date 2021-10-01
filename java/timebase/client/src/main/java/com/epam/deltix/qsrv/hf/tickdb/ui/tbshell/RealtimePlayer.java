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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.data.stream.*;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.schema.*;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.cmdline.ShellCommandException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeKeeper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *  Plays data in real-time.
 */
@ParametersAreNonnullByDefault
public class RealtimePlayer implements PlayerInterface {

    private final ShellPlayerThread playerThread;
    private boolean playing = false;

    public RealtimePlayer(ShellPlayerThread playerThread) {
        this.playerThread = playerThread;
    }

    private static class ShellPlayerThread extends RealtimePlayerThread {
        private final TickDBShell shell;

        private volatile PlayerCommandProcessor.LogMode logMode = PlayerCommandProcessor.LogMode.time;

        private long nextRepTime = 0;
        private long lastReportedCount;

        public ShellPlayerThread(MessageSource <InstrumentMessage> src, MessageChannel<InstrumentMessage> dest, SchemaConverter converter, TickDBShell shell, @Nullable Runnable streamRestarter, double speed) {
            super(src, dest, converter, streamRestarter, speed);
            this.shell = shell;
        }

        @Override
        public void             run () {
            lastReportedCount = 0;
            shell.confirm("Playback started. 'pause', 'next' and 'stop' control playback.");
            super.run();
            shell.confirm("Playback finished");

            printFinalLog(TimeKeeper.currentTimeNanos);
        }

        @Override
        protected void onMessageConversionError (RawMessage msg) {
            shell.error("Cannot convert message:" + msg, 0);
        }

        @Override
        protected void log(long mt, long now, RawMessage outMsg) {
            switch (logMode) {
                case off:
                    break;

                case data:
                    System.out.println (count + ": " + outMsg);
                    break;

                case time:
                    if (now > nextRepTime) {
                        printProgress(mt);
                        nextRepTime = now + 2000 * TimeStamp.NANOS_PER_MS;
                    }
                    break;

                default:
                    throw new UnsupportedOperationException ();
            }
        }

        private void printProgress(long timeNanos) {
            lastReportedCount = count;
            System.out.printf(
                    "%,d msgs; t=%s\n",
                    count,
                    GMT.formatNanos(timeNanos)
            );
        }

        protected void printFinalLog(long now) {
            if (logMode == PlayerCommandProcessor.LogMode.time) {
                if (count > lastReportedCount) {
                    printProgress(now);
                }
            }
        }
    }

    
    private void assertPlaying() {
        if (!playing) {
            throw new ShellCommandException("Playback is not in progress");
        }
    }

    @Override
    public void                stop () {
        assertPlaying();

        playerThread.setMode (RealtimePlayerThread.PlayMode.STOP);
        try {
            playerThread.join ();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interruption flag.
            throw new RuntimeException(e);
        }
    }

    @Override
    public void                pause () {
        assertPlaying();

        playerThread.setMode (RealtimePlayerThread.PlayMode.PAUSED);
    }

    @Override
    public void                resume () {
        assertPlaying();

        playerThread.setMode (RealtimePlayerThread.PlayMode.PLAY);
    }

    @Override
    public void                next () {
        assertPlaying();

        playerThread.setMode (RealtimePlayerThread.PlayMode.SKIP);
    }


    @Override
    public void play() {
        if (playing) {
            throw new ShellCommandException("Playback is already in progress");
        } else {
            playing = true;
            playerThread.start();
        }
    }

    @Override
    public void setSpeed(double speed) {
        // Not supported
    }

    @Override
    public void setTimeSliceDuration(int virtualTimeSliceDurationMs) {
        // Not supported
    }

    @Override
    public void setStopAtTimestamp(Long stopAtTimestamp) {
        setEndTimeNanos(playerThread, stopAtTimestamp);
    }

    private static void setEndTimeNanos(RealtimePlayerThread playerThread, @Nullable Long stopAtTimestamp) {
        playerThread.setEndTimeNano(stopAtTimestamp != null ? TimeUnit.MILLISECONDS.toNanos(stopAtTimestamp) : Long.MAX_VALUE);
    }

    @Override
    public void setLogMode(PlayerCommandProcessor.LogMode logMode) {
        playerThread.logMode = logMode;
    }

    public static PlayerInterface create(PlayerCommandProcessor.Config config, TickDBShell shell) {
        Collection<String> destinations = new HashSet<>(config.streamMapping.values());
        if (destinations.size() != 1) {
            throw new ShellCommandException("Exactly one destination stream is expected", 2);
        }
        String destinationStreamName = destinations.iterator().next();

        DXChannel<InstrumentMessage> dest = shell.dbmgr.getChannel(destinationStreamName);
        if (dest == null) {
            throw new ShellCommandException("Target '" + destinationStreamName + "' does not exist.", 2);
        }

        List<DXTickStream> sources = new ArrayList<>();
        for (String sourceStreamName : config.streamMapping.keySet()) {
            DXTickStream src = shell.dbmgr.getStream(sourceStreamName);
            if (src == null) {
                throw new ShellCommandException("Stream '" + sourceStreamName + "' does not exist.", 2);
            }
            sources.add(src);
        }

        DXTickStream[] sourceArray = sources.toArray(new DXTickStream[0]);
        SchemaConverter   converter = TickDBShell.createConverter(dest, sourceArray);

        if (converter == null) {
            throw new ShellCommandException("Source and destination streams in not compatible.");
        }

        MessageSource <InstrumentMessage>   cur = null;
        MessageChannel <InstrumentMessage>  out = null;

        ShellPlayerThread playerThread;
        try {
            long startTime = config.time;

            final InstrumentMessageSource finalCur = shell.selector.select (startTime, new SelectionOptions(true, false), sourceArray);
            cur = finalCur;
            out = shell.createPublisher (dest, true, sourceArray);

            if (out instanceof TickLoader) {
                ((TickLoader) out).addEventListener(new LoadingErrorListener() {
                    @Override
                    public void onError(LoadingError e) {
                        e.printStackTrace(System.out);
                    }
                });
            }

            Runnable streamRestarter = config.cyclic ? () -> finalCur.reset(startTime) : null;
            playerThread = new ShellPlayerThread(finalCur, out, converter, shell, streamRestarter, config.speed);

            cur = null; // let them escape
            out = null;
        } finally {
            Util.close (cur);
            Util.close (out);
        }

        playerThread.logMode = config.logMode;
        setEndTimeNanos(playerThread, config.stopAtTimestamp);


        return new RealtimePlayer(playerThread);
    }
}