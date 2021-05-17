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
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.TopicChannelFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.DuplicateTopicException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.RemoteAccessToLocalTopic;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.concurrent.QuickExecutor;
import io.aeron.Aeron;
import io.aeron.ExclusivePublication;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopic.NO_FREE_SLOTS;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class DirectTopicRegistry {
    private final Map<String, DirectTopic> topicMap = new HashMap<>();
    private final TopicRegistryEventListener eventListener;

    public DirectTopicRegistry() {
        this(null);
    }

    public DirectTopicRegistry(@Nullable TopicRegistryEventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Creates topic.
     *
     * @return optional additional data if {@code copyToStream} is set.
     */
    @Nonnull
    public CreateTopicResult createDirectTopic(String topicKey, List<RecordClassDescriptor> types, @Nullable String channel,
                                               IdGenerator idGenerator, @Nullable Collection<? extends IdentityKey> initialEntitySet,
                                               TopicType topicType, @Nullable Map<TopicChannelOption, String> channelOptions,
                                               @Nullable String copyToStream) throws DuplicateTopicException {
        synchronized (topicMap) {
            DirectTopic directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                int dataStreamId = idGenerator.nextId();
                int serverMetadataStreamId = idGenerator.nextId();
                directTopic = new DirectTopic(channel, dataStreamId, serverMetadataStreamId, types, initialEntitySet != null ? initialEntitySet : Collections.emptySet(), topicType, channelOptions, copyToStream);
                topicMap.put(topicKey, directTopic);
                if (eventListener != null) {
                    eventListener.topicCreated(topicKey, channel, directTopic.getTypes(), directTopic.getEntities(), topicType, directTopic.getChannelOptions(), copyToStream);
                }
                return new CreateTopicResult(directTopic.getTopicDeletedSignal(), directTopic.getCopyToThreadStopLatch());
            } else {
                throw new DuplicateTopicException("Topic '" + topicKey + "' already exists.");
            }
        }
    }

    public void deleteDirectTopic(String topicKey) throws TopicNotFoundException {
        DirectTopic directTopic;
        synchronized (topicMap) {
            directTopic = topicMap.get(topicKey);
            if (directTopic == null) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            } else {
                topicMap.remove(topicKey);
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

        CountDownLatch copyToThreadStopLatch = directTopic.getCopyToThreadStopLatch();
        if (copyToThreadStopLatch != null) {
            try {
                // Wait for "copyTo" thread to stop. Usually this should last more than few ms.
                copyToThreadStopLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for thread to stop", e);
            }
        }
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

    public ConstantIdentityKey[] getTopicMappingSnapshot(String topicKey) throws TopicNotFoundException {
        return getTopicMappingSnapshot(topicKey, false, Integer.MAX_VALUE);
    }

    /**
     * Same as {@link #getTopicMappingSnapshot(String)} but also ensures that dataStreamId matches the data on topic.
     * This is done to avoid a case when topic gets deleted and then immediately re-created.
     * In that case we want to ensure that mapping requests that were done for old topic will not get mapping for the new topic.
     * Because those mappings are not compatible.
     */
    public ConstantIdentityKey[] getTopicMappingSnapshot(String topicKey, int dataStreamId) throws TopicNotFoundException {
        return getTopicMappingSnapshot(topicKey, true, dataStreamId);
    }

    private ConstantIdentityKey[] getTopicMappingSnapshot(String topicKey, boolean validateDataStreamId, int dataStreamId) throws TopicNotFoundException {
        DirectTopic directTopic;
        synchronized (topicMap) {
            directTopic = topicMap.get(topicKey);
            // If dataStreamId does not match than this is a different topic
            if (directTopic == null || validateDataStreamId && directTopic.getDataStreamId() != dataStreamId) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            }
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (directTopic) {
            return directTopic.getMappingSnapshot();
        }
    }

    /**
     * Returns temporary mapping for ConstantIdentityKey indexes.
     * Temporary mapping is a mapping individually generated by each loader and used only by specific loader till it gets a permanent mapping from TB server.
     *
     * @param topicKey topic key
     * @param dataStreamId data stream id for the topic so we can ensure that topics was not recreated between requests
     * @param requestedTempEntityIndex index of temporary entry that client wants to lookup
     */
    public IntegerToObjectHashMap<ConstantIdentityKey> getTopicTemporaryMappingSnapshot(String topicKey, int dataStreamId, int requestedTempEntityIndex) throws TopicNotFoundException {
        return getTopicTemporaryMappingSnapshot(topicKey, true, dataStreamId, requestedTempEntityIndex);
    }
    public IntegerToObjectHashMap<ConstantIdentityKey> getTopicTemporaryMappingSnapshot(String topicKey, int requestedTempEntityIndex) throws TopicNotFoundException {
        return getTopicTemporaryMappingSnapshot(topicKey, false, Integer.MAX_VALUE, requestedTempEntityIndex);
    }


    private IntegerToObjectHashMap<ConstantIdentityKey> getTopicTemporaryMappingSnapshot(String topicKey, boolean validateDataStreamId, int dataStreamId, int requestedTempEntityIndex) throws TopicNotFoundException {
        DirectTopic directTopic;
        synchronized (topicMap) {
            directTopic = topicMap.get(topicKey);
            // If dataStreamId does not match than this is a different topic
            if (directTopic == null || validateDataStreamId && directTopic.getDataStreamId() != dataStreamId) {
                throw new TopicNotFoundException("Topic '" + topicKey + "' is not found.");
            }
        }

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (directTopic) {
            DirectTopicHandler activeHandler = directTopic.getActiveHandler();
            if (activeHandler == null) {
                // No active handler means that we don't have any loaders for that topics
                // No loaders means no temporary mappings
                return new IntegerToObjectHashMap<>(0);
            }

            return activeHandler.getTemporaryMappingSnapshot(requestedTempEntityIndex);
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
    public LoaderSubscriptionResult addLoader(String topicKey, InputStream loaderInputStream, List<? extends IdentityKey> keys, QuickExecutor executor, Aeron aeron, boolean isLocal, @Nullable String serverAddress) throws TopicNotFoundException {
        DirectTopicHandler handler;
        byte loaderNumber;
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

        String publisherChannel = TopicChannelFactory.createPublisherChannel(directTopic.getTopicType(), directTopic.getChannel(), directTopic.getChannelOptions());
        String metadataSubscriberChannel = TopicChannelFactory.createMetadataSubscriberChannel(directTopic.getTopicType(), directTopic.getChannel(), directTopic.getChannelOptions(), serverAddress);
        String metadataPublisherChannel = TopicChannelFactory.createMetadataPublisherChannel(directTopic.getTopicType(), directTopic.getChannel(), directTopic.getChannelOptions(), serverAddress);

        synchronized (directTopic) {
            if (directTopic.getTopicType() == TopicType.UDP_SINGLE_PUBLISHER && directTopic.getLoaderCount() > 0) {
                throw new IllegalStateException("UDP publisher is already present.");
            }

            loaderNumber = directTopic.getLoaderNumber();
            if (loaderNumber == NO_FREE_SLOTS) {
                throw new IllegalStateException("No free loader slots");
            }

            handler = directTopic.getActiveHandler();
            if (handler == null) {
                ExclusivePublication serverMetadataStream = aeron.addExclusivePublication(metadataPublisherChannel, directTopic.getServerMetadataStreamId());
                handler = new DirectTopicHandler(serverMetadataStream, executor, directTopic.getEntities());
                directTopic.setActiveHandler(handler);
            }
        }

        handler.addLoader(keys, loaderInputStream, () -> directTopic.releaseLoaderNumber(loaderNumber));
        int minTempEntityIndex = DirectProtocol.getMinTempEntryIndex(loaderNumber);
        int maxTempEntityIndex = DirectProtocol.getMaxTempEntryIndex(loaderNumber);
        Runnable dataAvailabilityCallback = handler.getLoaderDataAvailabilityCallback();
        ConstantIdentityKey[] mapping = directTopic.getMappingSnapshot();
        return new LoaderSubscriptionResult(directTopic.getTopicType(), mapping, directTopic.getTypes(), publisherChannel, metadataSubscriberChannel, directTopic.getDataStreamId(), directTopic.getServerMetadataStreamId(), loaderNumber, minTempEntityIndex, maxTempEntityIndex, dataAvailabilityCallback);
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

            String clientAddress = isLocal ? serverAddress : remoteClientAddress;

            String subscriberChannel = TopicChannelFactory.createSubscriberChannel(directTopic.getTopicType(), directTopic.getChannel(), directTopic.getChannelOptions(), clientAddress);

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (directTopic) {
                ConstantIdentityKey[] mapping = directTopic.getMappingSnapshot();
                return new ReaderSubscriptionResult(directTopic.getTopicType(), mapping, directTopic.getTypes(), subscriberChannel, directTopic.getDataStreamId());
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
                    CountDownLatch copyToThreadStopLatch = topic.getCopyToThreadStopLatch();
                    assert copyToThreadShouldStopSignal != null;
                    assert copyToThreadStopLatch != null;
                    visitor.visit(topicKey, topic.getTypes(), copyToStream, copyToThreadShouldStopSignal, copyToThreadStopLatch);
                }
            }
        }
    }

    /**
     * Creates a mapping provider for specific topic.
     */
    public MappingProvider getMappingProvider(String topicKey) {
        return new MappingProvider() {
            @Override
            public ConstantIdentityKey[] getMappingSnapshot() {
                return getTopicMappingSnapshot(topicKey);
            }

            @Override
            public IntegerToObjectHashMap<ConstantIdentityKey> getTempMappingSnapshot(int neededTempEntityIndex) {
                return getTopicTemporaryMappingSnapshot(topicKey, neededTempEntityIndex);
            }
        };
    }

    public interface CopyToStreamTopicVisitor {
        void visit(String topicKey, ImmutableList<RecordClassDescriptor> types, String copyToStream, AtomicBoolean copyToThreadShouldStopSignal, CountDownLatch copyToThreadStopLatch);
    }

    /**
     * IPC is not supported for remote (non-local clients).
     * Ensure that client not tries to do that.
     */
    private void assertNoRemoteAccessToIpc(boolean isLocal, DirectTopic directTopic) {
        if (!isLocal && !directTopic.supportsRemote()) {
            throw new RemoteAccessToLocalTopic();
        }
    }
}
