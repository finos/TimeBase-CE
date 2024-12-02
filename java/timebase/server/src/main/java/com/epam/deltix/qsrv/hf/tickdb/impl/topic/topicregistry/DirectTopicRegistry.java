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

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.google.common.collect.ImmutableList;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.TopicChannelFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.DuplicateTopicException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.RemoteAccessToLocalTopic;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.google.common.collect.ImmutableMap;
import io.aeron.driver.Configuration;
import net.jcip.annotations.GuardedBy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DirectTopicRegistry {
    private static final Log LOGGER = LogFactory.getLog(DirectTopicRegistry.class);

    private static final int BUFFER_COUNT = 3; // Aeron uses 3 term buffers of same size

    private final Map<String, DirectTopic> topicMap = new HashMap<>();
    private final Map<String, DirectTopic> copyToStreamTargets = new HashMap<>(); //
    private final TopicRegistryEventListener eventListener;
    private final boolean bypassRemoteCheckForIpcTopics;
    private final int defaultTopicTermBufferLength;
    private final long topicTotalTermBufferLimit;

    @GuardedBy("topicMap")
    private long totalTermBufferLength = 0;

    public DirectTopicRegistry() {
        this(null, false, Configuration.TERM_BUFFER_LENGTH_DEFAULT, Long.MAX_VALUE);
    }

    public DirectTopicRegistry(@Nullable TopicRegistryEventListener eventListener, boolean bypassRemoteCheckForIpcTopics, int defaultTopicTermBufferLength, long topicTotalTermBufferLimit) {
        this.eventListener = eventListener;
        this.bypassRemoteCheckForIpcTopics = bypassRemoteCheckForIpcTopics;
        this.defaultTopicTermBufferLength = defaultTopicTermBufferLength;
        this.topicTotalTermBufferLimit = topicTotalTermBufferLimit;
    }

    /**
     * Creates topic.
     *
     * @return optional additional data if {@code copyToStream} is set.
     */
    @Nonnull
    public CreateTopicResult createDirectTopic(String topicKey, List<RecordClassDescriptor> types, @Nullable String channel,
                                               IdGenerator idGenerator,
                                               TopicType topicType, @Nullable Map<TopicChannelOption, String> channelOptions,
                                               @Nullable String copyToStream, @Nullable String copyToSpace
    ) throws DuplicateTopicException {
        synchronized (topicMap) {
            DirectTopic directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                long newTotalTermBufferLength = totalTermBufferLength + getUsedMemory(channelOptions);
                if (newTotalTermBufferLength > topicTotalTermBufferLimit) {
                    throw new IllegalStateException("No more new topics can't be created: total term buffer length limit was exceeded. " +
                            "Delete existing topic or increase the limit. Current limit=" + topicTotalTermBufferLimit + " bytes. " +
                            "To increase configure Aeron driver container and set new value for " + DXServerAeronContext.SYS_PROP_TOPIC_TERM_BUFFER_LIMIT);
                }
                int dataStreamId = idGenerator.nextId();
                directTopic = new DirectTopic(channel, dataStreamId, types, topicType, channelOptions, copyToStream, copyToSpace);
                topicMap.put(topicKey, directTopic);
                totalTermBufferLength = newTotalTermBufferLength;
                if (eventListener != null) {
                    eventListener.topicCreated(topicKey, channel, directTopic.getTypes(), topicType, directTopic.getChannelOptions(), copyToStream);
                }
                LOGGER.info("Topic \"%s\" was created: dataStreamId=%s copyToStream=%s").with(topicKey).with(dataStreamId).with(copyToStream);
                return new CreateTopicResult(directTopic.getTopicDeletedSignal(), directTopic.getCopyProcessStoppedFuture());
            } else {
                throw new DuplicateTopicException("Topic '" + topicKey + "' already exists.");
            }
        }
    }

    private int getUsedMemory(@Nullable Map<TopicChannelOption, String> channelOptions) {
        Map<TopicChannelOption, String> options = channelOptions != null ? channelOptions : ImmutableMap.of();
        int singleTermBufferLength = TopicChannelFactory.getTermBufferLength(options, defaultTopicTermBufferLength);
        return singleTermBufferLength * BUFFER_COUNT;
    }

    public void deleteDirectTopic(String topicKey) throws TopicNotFoundException {
        DirectTopic directTopic;
        synchronized (topicMap) {
            directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            } else {
                topicMap.remove(topicKey);
                totalTermBufferLength -= getUsedMemory(directTopic.getChannelOptions());
                if (eventListener != null) {
                    eventListener.topicDeleted(topicKey);
                }
            }
        }

        // Tell Copy thread that it should stop
        AtomicBoolean stopSignal = directTopic.getTopicDeletedSignal();
        if (stopSignal != null) {
            stopSignal.set(true);
        }

        CompletableFuture<Void> copyProcessStoppedFuture = directTopic.getCopyProcessStoppedFuture();
        if (copyProcessStoppedFuture != null) {
            // Wait for "copyTo" thread to stop. Usually this should last more than few ms.
            long startTime = System.currentTimeMillis();
            try {
                copyProcessStoppedFuture.join();
            } catch (Exception e) {
                LOGGER.warn("Error while waiting for topic \"%s\" process copy stream process to be deleted").with(topicKey);
            }
            long stopTime = System.currentTimeMillis();
            LOGGER.info("Waited for topic \"%s\" process copy stream process to be deleted for %s ms")
                    .with(topicKey).with(stopTime - startTime);
        }
        LOGGER.info("Topic \"%s\" was deleted").with(topicKey);
    }

    public ImmutableList<RecordClassDescriptor> getTopicTypes(String topicKey) throws TopicNotFoundException {
        synchronized (topicMap) {
            DirectTopic directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            } else {
                return directTopic.getTypes();
            }
        }
    }

    public List<String> listDirectTopics() {
        synchronized (topicMap) {
            // TODO: Keys are not ordered. Show we return them as set? Should we order them?
            return new ArrayList<>(topicMap.keySet());
        }
    }

    /**
     * @param serverAddress server address that can be used for UDP-based topics
     */
    public LoaderSubscriptionResult addLoader(String topicKey, boolean isLocal, @Nullable String serverAddress) throws TopicNotFoundException {
        DirectTopic directTopic;

        synchronized (topicMap) {
            directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            }
            assertNoRemoteAccessToIpc(isLocal, directTopic);
        }

        if (directTopic.getTopicType() == TopicType.UDP_SINGLE_PUBLISHER && serverAddress == null) {
            throw new IllegalArgumentException("Server address is not set. Configure TimeBase.host property");
        }

        String publisherChannel = TopicChannelFactory.createPublisherChannel(directTopic.getTopicType(), directTopic.getChannel(), directTopic.getChannelOptions(), defaultTopicTermBufferLength);


        long loaderId = directTopic.getNextLoaderId();

        synchronized (directTopic) {
            // TODO: Loader count is always 0 for now. We don't count loaders yet (in current version).
            if (directTopic.getTopicType() == TopicType.UDP_SINGLE_PUBLISHER && directTopic.getLoaderCount() > 0) {
                throw new IllegalStateException("UDP publisher is already present.");
            }
        }

        return new LoaderSubscriptionResult(directTopic.getTopicType(), directTopic.getTypes(), publisherChannel, directTopic.getDataStreamId());
    }

    public ReaderSubscriptionResult addReader(String topicKey, boolean isLocal, @Nullable String serverAddress, @Nullable String remoteClientAddress) throws TopicNotFoundException {
        synchronized (topicMap) {
            DirectTopic directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            }
            assertNoRemoteAccessToIpc(isLocal, directTopic);

            if (directTopic.getTopicType() == TopicType.UDP_SINGLE_PUBLISHER && serverAddress == null) {
                throw new IllegalArgumentException("Server address is not set. Configure TimeBase.host property");
            }

            String subscriberHost;
            if (remoteClientAddress != null) {
                subscriberHost = remoteClientAddress;
            } else {
                if (isLocal) {
                    subscriberHost = serverAddress;
                } else {
                    throw new IllegalArgumentException("Remote client address is not set.");
                }
            }

            String subscriberChannel = TopicChannelFactory.createSubscriberChannel(directTopic.getTopicType(), directTopic.getChannel(), directTopic.getChannelOptions(), subscriberHost, defaultTopicTermBufferLength);

            synchronized (directTopic) {
                return new ReaderSubscriptionResult(directTopic.getTopicType(), directTopic.getTypes(), subscriberChannel, directTopic.getDataStreamId());
            }
        }
    }

    public void iterateCopyToStreamTopics(CopyToStreamTopicVisitor visitor) {
        synchronized (topicMap) {
            for (Map.Entry<String, DirectTopic> entry : topicMap.entrySet()) {
                String topicKey = entry.getKey();
                DirectTopic topic = topicMap.get(topicKey);
                String copyToStream = topic.getCopyToStream();
                if (copyToStream != null) {
                    AtomicBoolean copyToThreadShouldStopSignal = topic.getTopicDeletedSignal();
                    CompletableFuture<Void> copyProcessStoppedFuture = topic.getCopyProcessStoppedFuture();
                    assert copyToThreadShouldStopSignal != null;
                    assert copyProcessStoppedFuture != null;
                    visitor.visit(topicKey, topic.getTypes(), copyToStream, topic.getCopyToSpace(), copyToThreadShouldStopSignal, copyProcessStoppedFuture);
                }
            }
        }
    }

    public interface CopyToStreamTopicVisitor {
        void visit(String topicKey, ImmutableList<RecordClassDescriptor> types, String copyToStream, @Nullable String copyToSpace, AtomicBoolean copyToThreadShouldStopSignal, CompletableFuture<Void> copyProcessStoppedFuture);
    }

    /**
     * IPC is not supported for remote (non-local clients).
     * Ensure that client not tries to do that.
     */
    private void assertNoRemoteAccessToIpc(boolean isLocal, DirectTopic directTopic) {
        if (bypassRemoteCheckForIpcTopics) {
            return;
        }
        if (!isLocal && !directTopic.supportsRemote()) {
            throw new RemoteAccessToLocalTopic();
        }
    }
}