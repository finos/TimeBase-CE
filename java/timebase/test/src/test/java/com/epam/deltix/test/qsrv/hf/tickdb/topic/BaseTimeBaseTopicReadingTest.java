package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
abstract class BaseTimeBaseTopicReadingTest {
    protected static final Log LOG = LogFactory.getLog(BaseTimeBaseTopicReadingTest.class.getName());

    private static final int TEST_DURATION_MS = 20 * 1000;
    private static final int TIMEBASE_START_TIME_MS = 10 * 1000;
    private static final int TIME_TO_WAIT_FOR_READER = 20 * 1000;
    static final int TEST_TIMEOUT = TIMEBASE_START_TIME_MS + TEST_DURATION_MS + TIME_TO_WAIT_FOR_READER + 10 * 1000;

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
        runner = new TDBRunner(isRemote, true, TDBRunner.getTemporaryLocation(), new TomcatServer(), true);
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

        AtomicLong messagesSentCounter = new AtomicLong(0);
        AtomicLong messagesReceivedCounter = new AtomicLong(0);
        AtomicBoolean senderStopFlag = new AtomicBoolean(false);

        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        List<Thread> loaderThreads = startLoaderThreads(topicDB, topicKey, messagesSentCounter, senderStopFlag, exceptions, loaderThreadCount);

        MessageValidator messageValidator = new MessageValidator(loaderThreadCount == 1);
        Runnable runnable = createReader(messagesReceivedCounter, messageValidator, topicKey, topicDB);

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

        Assert.assertEquals(this.finalMessageSentCount.longValue(), receivedCount);
    }

    @NotNull
    private List<Thread> startLoaderThreads(TopicDB topicDB, String topicKey, AtomicLong messagesSentCounter, AtomicBoolean senderStopFlag, List<Throwable> exceptions, int loaderThreadCount) {
        List<Thread> result = new ArrayList<>(loaderThreadCount);
        for (int i = 1; i <= loaderThreadCount; i++) {
            Thread loaderThread = new Thread(createLoaderRunnable(topicDB, topicKey, messagesSentCounter, senderStopFlag));
            loaderThread.setName("LOADER-" + i);
            loaderThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
            loaderThread.start();

            result.add(loaderThread);
        }

        return result;
    }

    @NotNull
    Runnable createLoaderRunnable(TopicDB topicDB, String topicKey, AtomicLong messagesSentCounter, AtomicBoolean senderStopFlag) {
        return () -> {
            LOG.info("Starting Publisher...");
            MessageChannel<InstrumentMessage> messageChannel = topicDB.createPublisher(topicKey, null, null);
            LOG.info("Publisher started");

            TradeMessage msg = new TradeMessage();
            msg.setSymbol("ABC");
            msg.setOriginalTimestamp(234567890);
            long messageSentCounter = 0;
            while (!senderStopFlag.get()) {
                messageSentCounter ++;
                msg.setTimeStampMs(messageSentCounter); // Se store message number in the timestamp field.
                messageChannel.send(msg);
                long sentCount = messagesSentCounter.incrementAndGet();
                /*
                if (sentCount <= 10) {
                    System.out.println("Message sent");
                }
                */
            }
            messageChannel.close();
        };
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
            Assert.fail("Exception in threads");
        }
    }

    protected void createTopic(TopicDB topicDB, String topicKey, RecordClassDescriptor[] types) {
        topicDB.createTopic(topicKey, types, null);
    }

    protected abstract Runnable createReader(AtomicLong messagesReceivedCounter, MessageValidator messageValidator, String topicKey, TopicDB topicDB);

    protected abstract void stopReader();

    static class MessageValidator {

        final boolean validateOrder;
        long messageNumber = 0;

        MessageValidator(boolean validateOrder) {
            this.validateOrder = validateOrder;
        }

        void validate(InstrumentMessage message) {
            messageNumber ++;
            TradeMessage msg = (TradeMessage) message;

            // Se store message number in the timestamp field.
            if (validateOrder && msg.getTimeStampMs() != messageNumber) {
                throw new IllegalStateException("Invalid message order");
            }
            if (!msg.getSymbol().equals("ABC")) {
                throw new AssertionError("Wrong symbol");
            }
            if (msg.getOriginalTimestamp() != 234567890) {
                throw new AssertionError("Wrong OriginalTimestamp");
            }
        }
    }
}
