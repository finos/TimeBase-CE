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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.qsrv.hf.blocks.InstrumentKeyToIntegerHashMap;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DirectTopic {
    static final int NO_FREE_SLOTS = -1;


    private final ImmutableList<RecordClassDescriptor> types;
    private final boolean[] usedLoaderNumbers = new boolean[DirectProtocol.MAX_PUBLISHER_NUMBER + 1]; // First position unused
    private final String channel;
    private final TopicType topicType;
    private final Map<TopicChannelOption, String> channelOptions;

    private final int dataStreamId; // This stream is for data from Publishers to Subscribers
    private final int serverMetadataStreamId; // This stream is for data from TimeBase server to Publishers
    //private final boolean isMulticast;
    private final String copyToStream; // Key of stream to copy messages to.

    private final Object loaderLock = new Object();

    private final AtomicBoolean topicDeletedSignal; // If switched to true then the topic being deleted and copy thread should stop
    private final CountDownLatch copyToThreadStopLatch; // If copy thread stops then it will cont this down

    private byte nextPos = 1;
    private byte loaderCount = 0;

    private DirectTopicHandler handler = null;
    private final InstrumentKeyToIntegerHashMap entities = new InstrumentKeyToIntegerHashMap();

    DirectTopic(@Nullable String channel, int dataStreamId, int serverMetadataStreamId, List<RecordClassDescriptor> types,
                Collection<? extends IdentityKey> initialEntitySet,
                TopicType topicType, @Nullable Map<TopicChannelOption, String> channelOptions, @Nullable String copyToStream) {
        this.channel = channel;
        this.types = ImmutableList.copyOf(types);
        this.dataStreamId = dataStreamId;
        this.serverMetadataStreamId = serverMetadataStreamId;
        this.topicType = topicType;
        if (channelOptions == null) {
            this.channelOptions = ImmutableMap.of();
        } else {
            this.channelOptions = ImmutableMap.copyOf(channelOptions);
        }
        this.copyToStream = copyToStream;
        for (IdentityKey key : initialEntitySet) {
            entities.put(ConstantIdentityKey.makeImmutable(key), entities.size());
        }
        if (copyToStream != null) {
            topicDeletedSignal = new AtomicBoolean(false);
            copyToThreadStopLatch = new CountDownLatch(1);
        } else {
            topicDeletedSignal = null;
            copyToThreadStopLatch = null;
        }
    }

    public byte getLoaderNumber() {
        synchronized (loaderLock) {
            if (loaderCount == DirectProtocol.MAX_PUBLISHER_NUMBER) {
                return NO_FREE_SLOTS;
            }

            byte initialPos = nextPos;
            byte currentPos = initialPos;
            while (true) {
                if (!usedLoaderNumbers[currentPos]) {
                    // Free
                    nextPos = next(currentPos);
                    usedLoaderNumbers[currentPos] = true;
                    loaderCount++;
                    return currentPos;
                }
                currentPos = next(currentPos);
                if (currentPos == initialPos) {
                    // We scanned all array and there no free positions
                    // Should not happen
                    throw new IllegalStateException();
                }
            }
        }
    }

    public void releaseLoaderNumber(byte number) {
        synchronized (loaderLock) {
            assert usedLoaderNumbers[number];
            usedLoaderNumbers[number] = false;
            loaderCount--;
        }
    }

    public byte getLoaderCount() {
        return loaderCount;
    }

    private byte next(byte pos) {
        if (pos == DirectProtocol.MAX_PUBLISHER_NUMBER) {
            return 1;
        } else {
            pos ++;
            return pos;
        }
    }

    public DirectTopicHandler getActiveHandler() {
        return handler;
    }

    public void setActiveHandler(DirectTopicHandler handler) {
        assert this.handler == null;
        this.handler = handler;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }

    public int getServerMetadataStreamId() {
        return serverMetadataStreamId;
    }

    public String getChannel() {
        return channel;
    }

    @Nonnull
    public TopicType getTopicType() {
        return topicType;
    }

    public Map<TopicChannelOption, String> getChannelOptions() {
        return channelOptions;
    }

    @Nonnull
    public ImmutableList<RecordClassDescriptor> getTypes() {
        return types;
    }

    public boolean supportsRemote() {
        return topicType != TopicType.IPC;
    }

    @Nullable
    public String getCopyToStream() {
        return copyToStream;
    }

    @Nullable
    AtomicBoolean getTopicDeletedSignal() {
        return topicDeletedSignal;
    }

    @Nullable
    CountDownLatch getCopyToThreadStopLatch() {
        return copyToThreadStopLatch;
    }

    /**
     * @return modifiable instance of entity mapping
     */
    InstrumentKeyToIntegerHashMap getEntities() {
        return entities;
    }

    @Nonnull
    public ConstantIdentityKey[] getMappingSnapshot() {
        synchronized (entities) {
            return entities.getInverseMapSnapshot();
        }
    }
}