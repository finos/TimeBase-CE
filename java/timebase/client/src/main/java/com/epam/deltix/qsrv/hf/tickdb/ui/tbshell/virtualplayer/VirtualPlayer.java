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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.virtualplayer;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TimeMessage;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaConverter;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.DBMgr;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerCommandProcessor;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.PlayerInterface;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.util.cmdline.ShellCommandException;

import java.util.*;

/**
 * Shell command for virtual player.
 * Plays data from specified time point with specified speed.
 *
 * @author Alexei Osipov
 */
public class VirtualPlayer implements PlayerInterface {
    private final VirtualPlayerPlayback playback;

    private VirtualPlayer(VirtualPlayerPlayback playback) {
        this.playback = playback;
    }

    public static PlayerInterface create(PlayerCommandProcessor.Config config, TickDBShell shell) {
        DBMgr dbmgr = shell.dbmgr;
        String timeStreamName = config.timerStreamName;
        if (timeStreamName == null) {
            throw new ShellCommandException("Timer stream name is not set");
        }

        long startFromVirtualTimestamp;
        if (config.startFromVirtualTimestamp == null) {
            if (config.time > Long.MIN_VALUE && config.time < Long.MAX_VALUE) {
                startFromVirtualTimestamp = config.time;
            } else {
                throw new ShellCommandException("\"vtime\" is not set and \"time\" is not specified too");
            }
        } else {
            startFromVirtualTimestamp = config.startFromVirtualTimestamp;
        }

        if ((int) (config.virtualTimeSliceDurationMs / config.speed) == 0) {
            throw new ShellCommandException("Real time period is less than 1ms. Not supported.");
        }

        int mappingCount = config.streamMapping.size();
        int uniqueDestinationStreamCount = new HashSet<>(config.streamMapping.values()).size();
        if (uniqueDestinationStreamCount < mappingCount) {
            throw new ShellCommandException("Multiple mappings to same destination are not supported fro Virtual player");
        }

        DXTickStream timerStream = dbmgr.getDB().getStream(timeStreamName);
        if (timerStream == null) {
            // Time message does not exist. Let's create it.
            StreamOptions timeStreamOpts = new StreamOptions(StreamScope.RUNTIME, null, null, 1);
            timeStreamOpts.bufferOptions = new BufferOptions();
            timeStreamOpts.bufferOptions.lossless = true;
            if (config.timeQueueSize != null) {
                int queueSize = getBufferSizeForTimeMessages(config.timeQueueSize);
                timeStreamOpts.bufferOptions.initialBufferSize = queueSize;
                timeStreamOpts.bufferOptions.maxBufferSize = queueSize;
            }
            timeStreamOpts.setPolymorphic(createTimeStreamDescriptors());
            timerStream = dbmgr.getDB().createStream(timeStreamName, timeStreamOpts);
        } else {
            if (config.timeQueueSize != null) {
                // Validate existing stream to match requirements
                int queueSize = getBufferSizeForTimeMessages(config.timeQueueSize);
                StreamOptions streamOptions = timerStream.getStreamOptions();
                BufferOptions bufferOptions = streamOptions.bufferOptions;

                if (streamOptions.scope != StreamScope.TRANSIENT && streamOptions.scope != StreamScope.RUNTIME) {
                    throw new ShellCommandException("Existing timer stream is not transient. \"timersize\" option is not applicable");
                }
                if (bufferOptions == null || !bufferOptions.lossless) {
                    throw new ShellCommandException("Existing timer stream is not lossless. \"timersize\" option is not applicable");
                }
                if (bufferOptions.initialBufferSize != queueSize) {
                    throw new ShellCommandException("Initial buffer size for existing timer stream does not match configured \"timersize\" option");
                }
                if (bufferOptions.maxBufferSize != queueSize) {
                    throw new ShellCommandException("Max buffer size for existing timer stream does not match configured \"timersize\" option");
                }
            }
        }


        LoadingOptions timerLoaderOptions = new LoadingOptions();
        timerLoaderOptions.channelBufferSize = TimeMessage.getTimeMessageSizeInTransientStream();
        MessageChannel<InstrumentMessage> timeMessageLoader = timerStream.createLoader(timerLoaderOptions);

        LoadingOptions rawLoaderOptions = new LoadingOptions(true);
        SelectionOptions selectionOptions = new SelectionOptions(true, false);
        List<CopyStreamEntry> mapping = new ArrayList<>();
        for (Map.Entry<String, String> entry : config.streamMapping.entries()) {
            String srcStreamName = entry.getKey();
            String dstStreamName = entry.getValue();
            DXTickStream srcStream = dbmgr.getStream(srcStreamName);
            DXTickStream dstStream = dbmgr.getStream(dstStreamName);
            InstrumentMessageSource srcCursor = shell.selector.select(config.time, selectionOptions, new TickStream[]{srcStream});

            TickLoader dstLoader = dstStream.createLoader(rawLoaderOptions);
            SchemaConverter schemaConverter = TickDBShell.createConverter(dstStream, srcStream);
            if (schemaConverter == null) {
                throw new ShellCommandException(String.format("Source and destination streams in not compatible: from %s to %s", srcStreamName, dstStreamName));
            }
            mapping.add(new CopyStreamEntry<>(srcCursor, dstLoader, schemaConverter));
        }

        VirtualPlayerPlayback playback = new VirtualPlayerPlayback(
                timeMessageLoader,
                mapping,
                config.speed,
                config.virtualTimeSliceDurationMs,
                startFromVirtualTimestamp,
                config.stopAtTimestamp,
                config.destinationVirtualTimestamp
        );
        return new VirtualPlayer(playback);
    }

    private static RecordClassDescriptor[] createTimeStreamDescriptors() {
        ArrayList<RecordClassDescriptor> descriptors = new ArrayList<>();
        descriptors.addAll(Arrays.asList(getDescriptorsForPlaybackEvents()));
        descriptors.add(TimeMessage.DESCRIPTOR);
        return descriptors.toArray(new RecordClassDescriptor[0]);
    }

    private static RecordClassDescriptor[] getDescriptorsForPlaybackEvents() {
        Class[] classes = {
                PlaybackEvent.class,
                PlaybackFrequencyChangedEvent.class,
                PlaybackSpeedChangedEvent.class
        };
        Introspector ix = Introspector.createEmptyMessageIntrospector();
        for (Class<?> clazz : classes) {
            try {
                ix.introspectRecordClass("Get RD for VirtualPlayer messages", clazz);
            } catch (Introspector.IntrospectionException e) {
                throw new RuntimeException(e);
            }
        }
        return ix.getRecordClasses(PlaybackEvent.class);
    }

    private static int getBufferSizeForTimeMessages(int timeQueueSize) {
        int extraBytesForMessageQueue = MessageSizeCodec.MAX_SIZE - 1; // MessageQueue always expects that message size is MessageSizeCodec.MAX_SIZE
        return TimeMessage.getTimeMessageSizeInTransientStream() * timeQueueSize + extraBytesForMessageQueue;
    }

    @Override
    public void setSpeed(double speed) {
        try {
            playback.setSpeed(speed);
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void setTimeSliceDuration(int virtualTimeSliceDurationMs) {
        try {
            playback.setTimeSliceDuration(virtualTimeSliceDurationMs);
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void setStopAtTimestamp(Long stopAtTimestamp) {
        try {
            playback.setStopAtVirtualTimestamp(stopAtTimestamp);
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void setLogMode(PlayerCommandProcessor.LogMode logMode) {
        // Not supported by this player
    }

    @Override
    public void stop() {
        try {
            playback.stop();
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void pause() {
        try {
            playback.pause();
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void resume() {
        try {
            playback.resume();
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void play() {
        try {
            playback.start();
        } catch (RuntimeException e) {
            handleException(e);
        }
    }

    @Override
    public void next() {
        throw new ShellCommandException("Command is not supported for virtual player");
    }


    private void handleException(RuntimeException e) {
        if (e instanceof IllegalStateException) {
            throw new ShellCommandException("Invalid state for that command");
        } else if (e instanceof IllegalArgumentException) {
            throw new ShellCommandException("Invalid argument for that option");
        } else {
            throw e;
        }
    }
}
