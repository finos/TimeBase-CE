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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.CreateTopicResult;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.ReaderSubscriptionResult;
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import io.aeron.Aeron;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CopyTopicToStreamTaskManager {
    private final DXTickDB db;
    private final DXServerAeronContext aeronContext;
    private final AeronThreadTracker aeronThreadTracker;
    private final DirectTopicRegistry topicRegistry;


    private static final Log LOGGER = LogFactory.getLog(CopyTopicToStreamTaskManager.class);

    public CopyTopicToStreamTaskManager(DXTickDB db, DXServerAeronContext aeronContext, AeronThreadTracker aeronThreadTracker, DirectTopicRegistry topicRegistry) {
        this.db = db;
        this.aeronContext = aeronContext;
        this.aeronThreadTracker = aeronThreadTracker;
        this.topicRegistry = topicRegistry;
    }

    /**
     * Starts thread that copies data from topic to stream. If this operation fails then rollbacks topic creation (i.e) deletes topic.
     */
    public void subscribeToStreamCopyOrRollback(String topicKey, List<RecordClassDescriptor> types, String copyToStreamKey, CreateTopicResult createTopicResult, MappingProvider mappingProvider) {
        boolean success = false;
        try {
            subscribeToStreamCopy(topicKey, types, copyToStreamKey, createTopicResult.getTopicDeletedSignal(), createTopicResult.getCopyToThreadStopLatch(), mappingProvider);
            success = true;
        } finally {
            if (!success) {
                topicRegistry.deleteDirectTopic(topicKey);
            }
        }
    }

    private void subscribeToStreamCopy(String topicKey, List<RecordClassDescriptor> typeList, String targetStreamKey, AtomicBoolean topicDeletedSignal, CountDownLatch copyToThreadStopLatch, MappingProvider mappingProvider) {
        ReaderSubscriptionResult result = topicRegistry.addReader(topicKey, true, aeronContext.getPublicAddress(), null);

        DirectReaderFactory factory = new DirectReaderFactory();

        Aeron aeron = aeronContext.getAeron();

        // We create poller in the thread that just created this topic (not in the copy-to-stream thread).
        // This way we can be sure that when we return response with created topic the copy-to-thread will be
        // already subscribed so to it. So if client starts to send messages immediately then we still can be sure that
        // copy-to-stream not missed any messages.
        MessagePoller source = factory.createPoller(aeron, true, result.getSubscriberChannel(), result.getDataStreamId(), result.getTypes(), mappingProvider);

        DXTickStream stream = getOrCreateStreamForTopic(typeList, targetStreamKey);

        startTopicToStreamCopyThread(topicKey, targetStreamKey, source, stream, typeList, topicDeletedSignal, copyToThreadStopLatch);
    }

    public void startCopyToStreamThreadsForAllTopics() {
        topicRegistry.iterateCopyToStreamTopics((topicKey, types, copyToStream, topicDeletedSignal, copyToThreadStopLatch) -> {
            MappingProvider mappingProvider = topicRegistry.getMappingProvider(topicKey);
            try {
                subscribeToStreamCopy(topicKey, types, copyToStream, topicDeletedSignal, copyToThreadStopLatch, mappingProvider);
            } catch (Exception e) {
                LOGGER.error("Failed to start data copy from topic to a stream: %s").with(e);
            }
        });
    }

    private void startTopicToStreamCopyThread(String topicKey, String targetStreamKey, MessagePoller source, DXTickStream stream, List<RecordClassDescriptor> topicTypeList, AtomicBoolean topicDeletedSignal, CountDownLatch copyToThreadStopLatch) {
        LoadingOptions options = new LoadingOptions();
        options.raw = true;
        TickLoader loader = stream.createLoader(options);
        MessageProcessor messageProcessor = createTypeTransformingMessageProcessor(loader::send, topicTypeList, stream.getTypes());

        Thread thread = aeronThreadTracker.newCopyTopicToStreamThread(() -> {
            try {
                LOGGER.info("Started thread to copy data from topic \"%s\" to stream \"%s\"").with(topicKey).with(targetStreamKey);

                IdleStrategy idleStrategy = new SleepingIdleStrategy(TimeUnit.MICROSECONDS.toNanos(100));
                while (aeronContext.copyThreadsCanRun() && !Thread.currentThread().isInterrupted()) {
                    int workCount = source.processMessages(100, messageProcessor);
                    if (workCount == 0 && topicDeletedSignal.get()) {
                        // No data in the topic right now and we got stop signal => break
                        break;
                    }
                    idleStrategy.idle(workCount);
                }

                LOGGER.info("Stopped thread that copied data from topic \"%s\" to stream \"%s\"").with(topicKey).with(targetStreamKey);
            } catch (Exception e) {
                LOGGER.error("Exception in the thread that copied data from topic \"%s\" to stream \"%s\": %s").with(topicKey).with(targetStreamKey).with(e);
            } finally {
                try {
                    source.close();
                } catch (Exception e) {
                    LOGGER.log(LogLevel.WARN).append("Failed to close source: ").append(e).commit();
                }
                try {
                    loader.close();
                } catch (Exception e) {
                    LOGGER.log(LogLevel.WARN).append("Failed to close loader: ").append(e).commit();
                }
                copyToThreadStopLatch.countDown();
            }
        });
        thread.start();
    }

    // Transforms message type according to provided mapping.
    private MessageProcessor createTypeTransformingMessageProcessor(MessageProcessor baseMessageProcessor, List<RecordClassDescriptor> topicTypeList, RecordClassDescriptor[] streamTypes) {
        Map<RecordClassDescriptor, RecordClassDescriptor> typeTransformationMap = findTypeMatches(topicTypeList, streamTypes);

        // In trace level mode we can log messages
        MessageProcessor messageProcessor;
        if (LOGGER.isEnabled(LogLevel.TRACE)) {
            messageProcessor = message -> {
                baseMessageProcessor.process(message);
                LOGGER.log(LogLevel.TRACE,"Message copied to stream: ts=%s and symbol=%s").with(message.getTimeStampMs()).with(message.getSymbol());
            };
        } else {
            messageProcessor = baseMessageProcessor;
        }

        return message -> {
            RawMessage raw = (RawMessage) message;
            RecordClassDescriptor oldType = raw.type;
            RecordClassDescriptor newType = typeTransformationMap.get(oldType);
            if (newType == null) {
                throw new IllegalStateException("Unexpected type:" + oldType);
            }
            raw.type = newType; // Update type on message
            messageProcessor.process(raw);
        };
    }

    /**
     * Returns map with type matches between topicTypeList and streamTypes.
     *
     * @param topicTypeList list of types ma search match for
     * @param streamTypes list of types ma search match in
     * @return mapping between topic types and stream types.
     * @throws IllegalArgumentException if there is no match for some types from topicTypeList in streamTypes
     */
    private Map<RecordClassDescriptor, RecordClassDescriptor> findTypeMatches(List<RecordClassDescriptor> topicTypeList, RecordClassDescriptor[] streamTypes) {
        if (topicTypeList.size() == 1) {
            RecordClassDescriptor topicType = topicTypeList.get(0);
            return Collections.singletonMap(topicType, findTypeMatch(topicType, streamTypes));
        }
        Map<RecordClassDescriptor, RecordClassDescriptor> result = new HashMap<>(topicTypeList.size());
        for (RecordClassDescriptor topicType : topicTypeList) {
            result.put(topicType, findTypeMatch(topicType, streamTypes));
        }
        return result;
    }

    /**
     * Gets or creates stream that should be able to accept all message from the topic.
     *
     * @param typeList list of types that the stream should be able to hold,
     * @param targetStreamKey stream ket
     * @return stream that can store provided types.
     */
    private DXTickStream getOrCreateStreamForTopic(List<RecordClassDescriptor> typeList, String targetStreamKey) {
        DXTickStream stream = db.getStream(targetStreamKey);
        if (stream == null) {
            // Create Stream
            StreamOptions streamOptions = new StreamOptions();
            streamOptions.distributionFactor = 1;
            if (typeList.size() > 1) {
                streamOptions.setPolymorphic(typeList.toArray(new RecordClassDescriptor[0]));
            } else {
                streamOptions.setFixedType(typeList.get(0));
            }
            stream = db.createStream(targetStreamKey, streamOptions);
        } else {
            // Check if stream can accept all types
            validateExistingStream(stream, typeList);
        }
        return stream;
    }



    public static void preValidateCopyToStreamKey(DXTickDB db, List<RecordClassDescriptor> types, @Nullable String copyToStreamKey) {
        if (copyToStreamKey != null) {
            DXTickStream existingStream = db.getStream(copyToStreamKey);
            if (existingStream != null) {
                // Pre-validate the stream.
                // Note: this is not the main validation. Just a "fail fast" check.
                validateExistingStream(existingStream, types);
            }
        }
    }

    /**
     * Checks if the stream can accept messages of specified types
     */
    private static void validateExistingStream(DXTickStream stream, List<RecordClassDescriptor> typeList) {
        RecordClassDescriptor[] streamTypes = stream.getTypes();
        for (RecordClassDescriptor topicType : typeList) {
            RecordClassDescriptor match = findTypeMatch(topicType, streamTypes);
            assert match != null;
        }
    }

    private static RecordClassDescriptor findTypeMatch(RecordClassDescriptor topicType, RecordClassDescriptor[] streamTypes) {
        RecordClassDescriptor match = com.epam.deltix.qsrv.hf.stream.MessageProcessor.findMatch(topicType, streamTypes);
        if (match == null) {
            throw new IllegalArgumentException("Destination stream does not support all topic's types. Missing type: " + topicType.getName());
        }
        return match;
    }
}