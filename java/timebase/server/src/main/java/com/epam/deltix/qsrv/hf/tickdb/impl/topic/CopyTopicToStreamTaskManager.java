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
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDataLossHandler;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import io.aeron.Aeron;
import org.agrona.concurrent.IdleStrategy;
import org.agrona.concurrent.SleepingIdleStrategy;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    public void subscribeToStreamCopyOrRollback(String topicKey, List<RecordClassDescriptor> types, String copyToStreamKey, @Nullable String copyToSpace, CreateTopicResult createTopicResult) {
        boolean success = false;
        try {
            subscribeToStreamCopy(topicKey, types, copyToStreamKey, copyToSpace, createTopicResult.getTopicDeletedSignal(), createTopicResult.getCopyProcessStoppedFuture());
            success = true;
        } finally {
            if (!success) {
                topicRegistry.deleteDirectTopic(topicKey);
            }
        }
    }

    private void subscribeToStreamCopy(String topicKey, List<RecordClassDescriptor> typeList, String targetStreamKey, @Nullable String targetSpace, AtomicBoolean topicDeletedSignal, CompletableFuture<Void> copyProcessStoppedFuture) {
        CompletableFuture<Void> copyThreadStopped = new CompletableFuture<>();

        // This queue accumulates actions that should be done when copy process stops.
        // If start of the process fails, only already added actions have to be executed.
        // Order of actions must be the opposite the order of creation:
        // stop thread, close loader, release lock, close source (this one can be in different place), resolve "copyProcessStoppedFuture"
        // This is necessary to guarantee that when somebody deletes a topic, all operations (including stream unlock)
        // would be fully complete at the moment when "delete()" call returns.
        Deque<Runnable> shutdownActions = new ArrayDeque<>(3);

        boolean successfulStart = false;
        try {
            ReaderSubscriptionResult result = topicRegistry.addReader(topicKey, true, aeronContext.getPublicAddress(), null);

        DirectReaderFactory factory = new DirectReaderFactory();

            Aeron aeron = aeronContext.getAeron();

            TopicDataLossHandler topicDataLossHandler = () -> {
                LOGGER.warn("Data loss detected in topic \"%s\" (unexpected producer termination)").with(topicKey);

                // We do not want to stop "copy thread" even if we got data loss
                return true;
            };

            // We create poller in the thread that just created this topic (not in the copy-to-stream thread).
            // This way we can be sure that when we return response with created topic the copy-to-thread will be
            // already subscribed so to it. So if client starts to send messages immediately then we still can be sure that
            // copy-to-stream not missed any messages.
            MessagePoller source = factory.createPoller(aeron, true, result.getSubscriberChannel(), result.getDataStreamId(), result.getTypes(), topicDataLossHandler);
            shutdownActions.addFirst(() -> {
                try {
                    source.close();
                } catch (Exception e) {
                    LOGGER.log(LogLevel.WARN).append("Failed to close source: ").append(e).commit();
                }
            });


            DXTickStream stream = getOrCreateStreamForTopic(typeList, targetStreamKey);
            // At this point stream schema may not match topic schema. We will validate it during type mapping step.

            // Lock is needed to ensure that stream schema will not be changed while we copy data to it.
            // We use read locks so other topics would be able to write data into different spaces.
            DBLock lock = stream.lock(LockType.READ);
            shutdownActions.addFirst(() -> {
                try {
                    lock.release();
                } catch (Exception e) {
                    LOGGER.log(LogLevel.WARN).append("Failed to release lock: ").append(e).commit();
                }
            });

            // Setups type mapping.
            // Also validates that all types from topic are supported by stream.
            Map<RecordClassDescriptor, RecordClassDescriptor> typeTransformationMap = TopicReplicationUtil.findTypeMatches(typeList, stream.getTypes());

            LoadingOptions options = new LoadingOptions(true, LoadingOptions.WriteMode.REWRITE);
            if (targetSpace != null) {
                options.space = targetSpace;
            }
            @SuppressWarnings({"unchecked", "resource"})
            TickLoader<InstrumentMessage> loader = stream.createLoader(options);
            shutdownActions.addFirst(() -> {
                try {
                    loader.close();
                } catch (Exception e) {
                    LOGGER.log(LogLevel.WARN).append("Failed to close loader: ").append(e).commit();
                }
            });
            MessageProcessor messageProcessor = TopicReplicationUtil.createTypeTransformingMessageProcessor(loader::send, typeTransformationMap);

            Thread thread = aeronThreadTracker.newCopyTopicToStreamThread(() -> {
                try {
                    LOGGER.info("Started thread to copy data from topic \"%s\" to stream \"%s\"").with(topicKey).with(targetStreamKey);

                    IdleStrategy idleStrategy = aeronContext.getCopyToStreamIdleStrategyFactory().create();
                    while (aeronContext.copyThreadsCanRun() && !Thread.currentThread().isInterrupted()) {
                        int workCount = source.processMessages(100, messageProcessor);
                        if (workCount == 0 && topicDeletedSignal.get()) {
                            // No data in the topic right now and we got stop signal => break
                            break;
                        }
                        idleStrategy.idle(workCount);
                    }

                    LOGGER.info("Stopped thread that copied data from topic \"%s\" to stream \"%s\"").with(topicKey).with(targetStreamKey);
                    copyThreadStopped.complete(null); // Graceful stop
                } catch (Exception e) {
                    LOGGER.error("Exception in the thread that copied data from topic \"%s\" to stream \"%s\": %s").with(topicKey).with(targetStreamKey).with(e);
                    copyThreadStopped.completeExceptionally(e);
                } finally {
                    if (!copyThreadStopped.isDone()) {
                        copyThreadStopped.completeExceptionally(new IllegalStateException("Copy thread stopped unexpectedly"));
                    }
                }
            });
            thread.start();
            successfulStart = true;
        } finally {
            if (!successfulStart) {
                copyThreadStopped.completeExceptionally(new IllegalStateException("Failed to start copy thread"));
            }

            copyThreadStopped.whenComplete((aVoid, throwable) -> {
                for (Runnable shutdownAction : shutdownActions) {
                    shutdownAction.run();
                }

                // Propagate completion status from copyProcessShutdownChain to copyProcessStoppedFuture
                if (throwable != null) {
                    copyProcessStoppedFuture.completeExceptionally(throwable);
                } else {
                    copyProcessStoppedFuture.complete(null);
                }
            });
        }
    }

    public void startCopyToStreamThreadsForAllTopics() {
        topicRegistry.iterateCopyToStreamTopics((topicKey, types, copyToStream, copyToSpace, topicDeletedSignal, copyProcessStoppedFuture) -> {
            try {
                subscribeToStreamCopy(topicKey, types, copyToStream, copyToSpace, topicDeletedSignal, copyProcessStoppedFuture);
            } catch (Exception e) {
                LOGGER.error("Failed to start data copy from topic to a stream: %s").with(e);
            }
        });
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
            RecordClassDescriptor match = TopicReplicationUtil.findTypeMatch(topicType, streamTypes);
            assert match != null;
        }
    }

}