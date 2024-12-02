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
package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.lang.Util;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
abstract class BaseTimeBaseTopicReadingTest {
    protected static final Log LOG = LogFactory.getLog(BaseTimeBaseTopicReadingTest.class.getName());

    // Value for that parameter can be changed to get more stable results or reduce test time
    private static final int TEST_DURATION_MS = 5 * 1000;

    static final int TIMEBASE_START_TIME_MS = 10 * 1000;
    private static final int TIME_TO_WAIT_FOR_READER = 20 * 1000;

    public static final int TEST_TIMEOUT = TIMEBASE_START_TIME_MS + TEST_DURATION_MS + TIME_TO_WAIT_FOR_READER + 10 * 1000;

    private final boolean isRemote;
    protected static TDBRunner runner;
    protected Long finalMessageSentCount;

    protected BaseTimeBaseTopicReadingTest(boolean isRemote) {
        this.isRemote = isRemote;
    }

    protected BaseTimeBaseTopicReadingTest() {
        this(true);
    }

    @Before
    public void      start() throws Throwable {
        runner = new TDBRunner(isRemote, true, TDBRunner.getTemporaryLocation(),
                new TomcatServer(null, 0, 0, true), true);
        runner.startup();
    }

    @After
    public void      stop() throws Throwable {
        runner.setCleanup(true);
        runner.shutdown();
        runner = null;
    }

    void executeTest() throws Exception {
        executeTest(1);
    }

    void executeTest(int loaderThreadCount) throws Exception {
        DXTickDB tickDb = runner.getTickDb();
        TopicDB topicDB = tickDb.getTopicDB();


        String topicKey = "TopicReadingTest_" + RandomStringUtils.randomAlphanumeric(8);

        createTopic(topicDB, topicKey, new RecordClassDescriptor[]{StubData.makeTradeMessageDescriptor()});

        CountDownLatch readerReady = new CountDownLatch(1);
        AtomicLong messagesSentCounter = new AtomicLong(0);
        AtomicLong messagesReceivedCounter = new AtomicLong(0);
        AtomicBoolean senderStopFlag = new AtomicBoolean(false);

        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        List<Thread> loaderThreads = startLoaderThreads(topicDB, topicKey, messagesSentCounter, senderStopFlag, exceptions, loaderThreadCount, readerReady);

        MessageValidator messageValidator = getMessageValidator(loaderThreadCount);
        Runnable runnable = createReader(messagesReceivedCounter, messageValidator, topicKey, topicDB, readerReady);

        Thread readerThread = new Thread(runnable);
        readerThread.setName("READER");
        readerThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
        readerThread.start();

        // Let test to work
        Thread.sleep(TEST_DURATION_MS);

        checkExceptions(exceptions);

        // Ask sender to stop
        senderStopFlag.set(true);
        for (Thread loaderThread : loaderThreads) {
            loaderThread.join(2000);
            Assert.assertFalse("Loader not stopped", loaderThread.isAlive());
        }

        // Let reader finish off the queue
        waitForReaderToFinish(TIME_TO_WAIT_FOR_READER, messagesSentCounter, messagesReceivedCounter);

        // Stop reader
        stopReader();
        readerThread.join(2000);
        Assert.assertFalse("Reader not stopped", readerThread.isAlive());

        checkExceptions(exceptions);

        long receivedCount = messagesReceivedCounter.get();
        long speedEstimate = receivedCount * 1000 / TEST_DURATION_MS;
        System.out.println("Received " + receivedCount + " messages. Speed estimate: " + (speedEstimate / 1000) + " Kmsg/s");

        this.finalMessageSentCount = messagesSentCounter.get();
        Assert.assertTrue(this.finalMessageSentCount > 0);
        Assert.assertTrue(receivedCount > 0);

        Assert.assertEquals("Sent message count must match received count", this.finalMessageSentCount.longValue(), receivedCount);

        topicDB.deleteTopic(topicKey);
    }

    @NotNull
    protected MessageValidator getMessageValidator(int loaderThreadCount) {
        return new MessageValidatorImpl(loaderThreadCount == 1);
    }

    @Nonnull
    private List<Thread> startLoaderThreads(TopicDB topicDB, String topicKey, AtomicLong messagesSentCounter, AtomicBoolean senderStopFlag, List<Throwable> exceptions, int loaderThreadCount, CountDownLatch readerReady) {
        List<Thread> result = new ArrayList<>(loaderThreadCount);
        for (int i = 1; i <= loaderThreadCount; i++) {
            Thread loaderThread = new Thread(createLoaderRunnable(topicDB, topicKey, messagesSentCounter, senderStopFlag, readerReady));
            loaderThread.setName("LOADER-" + i);
            loaderThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
            loaderThread.start();

            result.add(loaderThread);
        }

        return result;
    }

    protected boolean isUdpTopic() {
        return false;
    }

    @Nonnull
    Runnable createLoaderRunnable(TopicDB topicDB, String topicKey, AtomicLong messagesSentCounter, AtomicBoolean senderStopFlag, CountDownLatch readerReady) {
        return () -> runProducer(topicDB, topicKey, messagesSentCounter, senderStopFlag, readerReady);
    }

    void runProducer(TopicDB topicDB, String topicKey, AtomicLong messagesSentCounter, AtomicBoolean senderStopFlag, CountDownLatch readerReady) {
        LOG.info("Starting Publisher...");
        try (MessageChannel<InstrumentMessage> messageChannel = topicDB.createPublisher(topicKey, getPublisherPreferences(), null)) {
            LOG.info("Publisher started");
            long waitStart = System.currentTimeMillis();
            try {
                boolean success = readerReady.await(10, TimeUnit.SECONDS);
                if (!success) {
                    throw new IllegalStateException("Reader not ready");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            LOG.info("Waited for reader to start: %s ms").with(System.currentTimeMillis() - waitStart);

            if (isUdpTopic()) {
                try {
                    // Subscriber still may need some time to connect to producer
                    Thread.sleep(1_000);
                    LOG.info("Waited for additional 1s for subscriber to connect");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }

            sendMessages(messagesSentCounter, senderStopFlag, messageChannel);
        }
    }

    protected PublisherPreferences getPublisherPreferences() {
        return null;
    }

    void sendMessages(AtomicLong messagesSentCounter, AtomicBoolean senderStopFlag, MessageChannel<InstrumentMessage> messageChannel) {
        TradeMessage msg = new TradeMessage();
        msg.setSymbol("ABC");
        msg.setOriginalTimestamp(234567890);
        long messageSentCounter = 0;
        while (!senderStopFlag.get()) {
            messageSentCounter++;
            setMessageTimestamp(msg, messageSentCounter);
            messageChannel.send(msg);
            long sentCount = messagesSentCounter.incrementAndGet();
            Thread.yield(); // Let reader work on busy machine like CI
                /*
                if (sentCount <= 10) {
                    System.out.println("Message sent");
                }
                */
        }
    }

    protected void setMessageTimestamp(TradeMessage msg, long messageSentCounter) {
        msg.setTimeStampMs(messageSentCounter); // We store message number in the timestamp field.
    }

    private static void waitForReaderToFinish(int timeToWaitForReader, AtomicLong sentCount, AtomicLong receivedCount) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeToWaitForReader;
        long oldReceivedCount = receivedCount.get();
        long prevCount = oldReceivedCount;
        long currentCount;
        while ((currentCount = receivedCount.get()) < sentCount.get() && System.currentTimeMillis() < endTime) {
            Thread.sleep(50);
            prevCount = currentCount;
        }
        long newReceivedCount = receivedCount.get();
        if (newReceivedCount > oldReceivedCount) {
            System.out.println("Got " + (newReceivedCount - oldReceivedCount) + " after the writer stopped");
            System.out.println("Got " + (currentCount - prevCount) + " on last check");
        }
    }

    private static void checkExceptions(List<Throwable> exceptions) {
        if (!exceptions.isEmpty()) {
            System.out.println("Exceptions found in threads:");
            for (Throwable exception : exceptions) {
                exception.printStackTrace(System.out);
            }
            throw new AssertionError("Exception in threads", exceptions.get(0));
        }
    }

    protected void createTopic(TopicDB topicDB, String topicKey, RecordClassDescriptor[] types) {
        topicDB.createTopic(topicKey, types, null);
    }

    protected abstract Runnable createReader(AtomicLong messagesReceivedCounter, MessageValidator messageValidator, String topicKey, TopicDB topicDB, CountDownLatch readerReady);

    protected abstract void stopReader();

    public interface MessageValidator {
        void validate(InstrumentMessage message);
    }

    public static class MessageValidatorImpl implements MessageValidator {

        final boolean validateOrder;
        long messageNumber = 0;

        MessageValidatorImpl(boolean validateOrder) {
            this.validateOrder = validateOrder;
        }

        @Override
        public void validate(InstrumentMessage message) {
            messageNumber ++;
            TradeMessage msg = (TradeMessage) message;

            // Se store message number in the timestamp field.
            if (validateOrder && msg.getTimeStampMs() != messageNumber) {
                throw new IllegalStateException("Invalid message order: expected " + messageNumber + " but got " + msg.getTimeStampMs());
            }
            if (!Util.equals(msg.getSymbol(), "ABC")) {
                throw new AssertionError("Wrong symbol");
            }
            if (msg.getOriginalTimestamp() != 234567890) {
                throw new AssertionError("Wrong OriginalTimestamp");
            }
        }
    }
}
