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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.DuplicateTopicException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Disposable;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * TimeBase topics API.
 *
 * Topics are lightweight "Streams". Topics have much less functionality but may provide better latency for some applications.
 *
 * Please note, that most of patterns of interaction with topics assume that you can afford a dedicated CPU core for
 * a thread that publishes data to a topic or reads it from a topic.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public interface TopicDB {
    /**
     * Creates pub/sub style topic (IPC).
     * You can use this type of topics only when client is local (on same machine with TimeBase).
     *
     * @param topicKey topic identifier
     * @param types list of message types
     * @param settings custom topic settings
     */
    DirectChannel createTopic(
            String topicKey,
            RecordClassDescriptor[] types,
            @Nullable
            TopicSettings settings
    ) throws DuplicateTopicException;

    @Nullable
    DirectChannel getTopic(String topicKey);

    /**
     * Deletes topic data from registry.
     * Does not check for existing publishers/consumers.
     */
    void deleteTopic(String topicKey) throws TopicNotFoundException;

    /**
     * @return list of all topic keys
     */
    @CheckReturnValue
    List<String> listTopics();

    /**
     * Returns list of types that can be written to and read from the specified topic.
     *
     * @param topicKey topic key
     * @return types for topic
     */
    @CheckReturnValue
    RecordClassDescriptor[] getTypes(String topicKey) throws TopicNotFoundException;

    /**
     * Creates channel for message publishing.
     * @param topicKey topic identifier
     * @param preferences configuration for this publisher
     * @param idleStrategy strategy to be used when publishing is not possible due to back pressure. If {@code null} then determined by {@code preferences}.
     */
    @CheckReturnValue
    MessageChannel<InstrumentMessage> createPublisher(
            String topicKey,
            @Nullable
            PublisherPreferences preferences,
            @Nullable
            IdleStrategy idleStrategy
    ) throws TopicNotFoundException;

    /**
     * Creates worker-style message consumer.
     *
     * @param topicKey topic identifier
     * @param preferences configuration for this consumer
     * @param idleStrategy strategy to be used when there are no messages to consume. If {@code null} then determined by {@code preferences}.
     * @param threadFactory thread factory that will be used to run the {@code processor}. If {@code null} then default thread factory will be used.
     * @param processor instance to process arrived messages
     * @return returns a {@link Disposable} that can be used to stop message processing
     */
    Disposable createConsumerWorker(
            String topicKey,
            @Nullable
            ConsumerPreferences preferences,
            @Nullable
            IdleStrategy idleStrategy,
            @Nullable
            ThreadFactory threadFactory,
            MessageProcessor processor
    ) throws TopicNotFoundException;

    /**
     * Creates a non-blocking poll-style message consumer.
     * @param topicKey topic identifier
     * @param preferences configuration for this consumer
     * @return returns {@link MessagePoller} that can be used to poll messages from subscription.
     */
    @CheckReturnValue
    MessagePoller createPollingConsumer(
            String topicKey,
            @Nullable
            ConsumerPreferences preferences
    ) throws TopicNotFoundException;

    /**
     * Creates {@link MessageSource}-style message consumer (blocking).
     *
     * @param topicKey topic identifier
     * @param preferences configuration for this consumer
     * @param idleStrategy strategy to be used when there are no messages to consume. If {@code null} then determined by {@code preferences}.
     * @return {@link MessageSource} for topic. Calling {@link MessageSource#next()} will block.
     */
    @CheckReturnValue
    MessageSource<InstrumentMessage> createConsumer(
            String topicKey,
            @Nullable
            ConsumerPreferences preferences,
            @Nullable
            IdleStrategy idleStrategy
    ) throws TopicNotFoundException;
}