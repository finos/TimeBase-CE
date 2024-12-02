/*
 * Copyright 2024 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DirectTopic {

    private final ImmutableList<RecordClassDescriptor> types;
    private final String channel;
    private final TopicType topicType;
    private final Map<TopicChannelOption, String> channelOptions;

    private final int dataStreamId; // This stream is for data from Publishers to Subscribers
    //private final boolean isMulticast;
    private final String copyToStream; // Key of stream to copy messages to.
    private final String copyToSpace;

    private final Object loaderLock = new Object();

    private final AtomicBoolean topicDeletedSignal; // If switched to true then the topic being deleted and copy thread should stop
    private final CompletableFuture<Void> copyProcessStoppedFuture; // That future will be resolved as soon as copy operation stops for any reason.

    private byte loaderCount = 0; // TODO: Add loader counting

    private final AtomicLong nextLoaderId;

    DirectTopic(@Nullable String channel, int dataStreamId, List<RecordClassDescriptor> types,
                TopicType topicType, @Nullable Map<TopicChannelOption, String> channelOptions,
                @Nullable String copyToStream, @Nullable String copyToSpace
    ) {
        if (copyToSpace != null && copyToStream == null) {
            throw new IllegalArgumentException("copyToSpace is set but copyToStream is not set");
        }
        this.channel = channel;
        this.types = ImmutableList.copyOf(types);
        this.dataStreamId = dataStreamId;
        this.topicType = topicType;
        this.copyToSpace = copyToSpace;
        if (channelOptions == null) {
            this.channelOptions = ImmutableMap.of();
        } else {
            this.channelOptions = ImmutableMap.copyOf(channelOptions);
        }
        this.copyToStream = copyToStream;
        if (copyToStream != null) {
            topicDeletedSignal = new AtomicBoolean(false);
            copyProcessStoppedFuture = new CompletableFuture<>();
        } else {
            topicDeletedSignal = null;
            copyProcessStoppedFuture = null;
        }
        this.nextLoaderId = new AtomicLong(System.currentTimeMillis());
    }

    public long getNextLoaderId() {
        return nextLoaderId.getAndIncrement();
    }


    public byte getLoaderCount() {
        return loaderCount;
    }

    public int getDataStreamId() {
        return dataStreamId;
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
    public String getCopyToSpace() {
        return copyToSpace;
    }

    @Nullable
    AtomicBoolean getTopicDeletedSignal() {
        return topicDeletedSignal;
    }

    @Nullable
    CompletableFuture<Void> getCopyProcessStoppedFuture() {
        return copyProcessStoppedFuture;
    }
}