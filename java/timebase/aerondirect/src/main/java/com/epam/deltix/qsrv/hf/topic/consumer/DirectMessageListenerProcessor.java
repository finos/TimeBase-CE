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
package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.AeronClientThread;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.AnyThread;
import com.epam.deltix.qsrv.hf.topic.consumer.annotation.ReaderThreadOnly;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import io.aeron.Aeron;
import io.aeron.ControlledFragmentAssembler;
import io.aeron.Image;
import io.aeron.Subscription;

import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;

/**
 * @author Alexei Osipov
 */
class DirectMessageListenerProcessor implements SubscriptionWorker {
    private static final int MESSAGES_PER_POLL = 100;

    private final Subscription subscription;
    private final IdleStrategy idleStrategy;
    private final MessageFragmentHandler fragmentHandler;

    // Indicates that poller should be stopped OR already stopped
    private volatile boolean stopFlag = false;

    // Reaches zero when reader successfully stops.
    private final CountDownLatch stopSignal = new CountDownLatch(1);

    // Indicates that poller detected a data loss before graceful stop
    private volatile boolean dataLoss = false;

    DirectMessageListenerProcessor(MessageProcessor processor, Aeron aeron, boolean raw, String channel, int dataStreamId,
                                   CodecFactory codecFactory, TypeLoader typeLoader, List<RecordClassDescriptor> types,
                                   IdleStrategy idleStrategy, MappingProvider mappingProvider) {
        // TODO: Implement loading of temp indexes from server

        if (!ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            throw new IllegalArgumentException("Only LITTLE_ENDIAN byte order supported");
        }

        this.subscription = aeron.addSubscription(channel, dataStreamId, null, this::onUnavailableImage);
        // We must get mapping after we completed subscription
        ConstantIdentityKey[] mappingSnapshot = mappingProvider.getMappingSnapshot();

        this.idleStrategy = idleStrategy;
        this.fragmentHandler = new MessageFragmentHandler(raw, codecFactory, typeLoader, types, mappingSnapshot, mappingProvider);
        this.fragmentHandler.setProcessor(processor);
    }

    @ReaderThreadOnly
    @Override
    public void processMessagesUntilStopped() throws CursorIsClosedException {
        if (stopFlag) {
            throw new IllegalStateException("Already stopped");
        }
        MessageFragmentHandler fragmentHandler = this.fragmentHandler;
        ControlledFragmentAssembler fragmentAssembler = new ControlledFragmentAssembler(fragmentHandler);
        while (!stopFlag) {
            int workCount = subscription.controlledPoll(fragmentAssembler, MESSAGES_PER_POLL);
            fragmentHandler.checkException();
            this.idleStrategy.idle(workCount);
        }
        cleanup();
        stopSignal.countDown();
    }

    @ReaderThreadOnly
    @Override
    public void processMessagesWhileTrue(BooleanSupplier condition) throws CursorIsClosedException {
        if (stopFlag) {
            throw new IllegalStateException("Already stopped");
        }
        MessageFragmentHandler fragmentHandler = this.fragmentHandler;
        ControlledFragmentAssembler fragmentAssembler = new ControlledFragmentAssembler(fragmentHandler);
        while (!stopFlag && condition.getAsBoolean()) {
            int workCount = subscription.controlledPoll(fragmentAssembler, MESSAGES_PER_POLL);
            fragmentHandler.checkException();
            this.idleStrategy.idle(workCount);
        }
        cleanup();
        stopSignal.countDown();
        if (dataLoss) {
            throw new ClosedDueToDataLossException();
        }
    }

    @ReaderThreadOnly
    private void cleanup() {
        subscription.close();
    }

    @AnyThread
    // Tells worker to stop and waits till it stops.
    public void close() {
        // Tell consumer that it must stop
        stopFlag = true;

        // Wait for the stop procedure to complete
        try {
            stopSignal.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedInterruptedException(e);
        }
    }

    @AeronClientThread
    // That will be executed from an Aeron's thread
    private void onUnavailableImage(Image image) {
        int sessionId = image.sessionId();
        // Note: fragmentHandler can be null during the initialization process
        if (!stopFlag && fragmentHandler != null && !fragmentHandler.checkIfSessionGracefullyClosed(sessionId)) {
            // Not a graceful close
            onDataLossDetected();
        }
    }

    @AeronClientThread
    // That will be executed from an Aeron's thread
    private void onDataLossDetected() {
        dataLoss = true;
        stopFlag = true;
    }
}