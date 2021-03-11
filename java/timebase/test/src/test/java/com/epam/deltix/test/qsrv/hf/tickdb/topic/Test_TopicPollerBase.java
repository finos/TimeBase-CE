package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import org.junit.Ignore;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@Ignore // TODO: ENABLE
public abstract class Test_TopicPollerBase extends BaseTimeBaseTopicReadingTest {
    private AtomicBoolean running;

    protected Test_TopicPollerBase(boolean isRemote) {
        super(isRemote);
    }

    protected Test_TopicPollerBase() {
    }

    @Override
    protected Runnable createReader(AtomicLong messagesReceivedCounter, MessageValidator messageValidator, String topicKey, TopicDB topicDB) {
        AtomicBoolean runningFlag = new AtomicBoolean(true);
        running = runningFlag;

        return () -> {
            System.out.println("Creating poller...");
            MessagePoller messagePoller = topicDB.createPollingConsumer(topicKey, null);
            System.out.println("Poller created");

            RatePrinter ratePrinter = new RatePrinter("Reader");
            YieldingIdleStrategy idleStrategy = new YieldingIdleStrategy();

            MessageProcessor processor = m -> {
                messageValidator.validate(m);
                messagesReceivedCounter.incrementAndGet();
                ratePrinter.inc();
            };

            while (runningFlag.get()) {
                idleStrategy.idle(
                        messagePoller.processMessages(100, processor)
                );
            }
            messagePoller.close();
        };
    }

    @Override
    protected void stopReader() {
        running.set(false);
    }
}