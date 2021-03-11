package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.Disposable;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDB.class)
public class Test_TopicListener extends BaseTimeBaseTopicReadingTest {
    private Disposable worker;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @NotNull
    protected Runnable createReader(AtomicLong messagesReceivedCounter, MessageValidator messageValidator, String topicKey, TopicDB topicDB) {
        return () -> {
            RatePrinter ratePrinter = new RatePrinter("Reader");
            ratePrinter.start();
            this.worker = topicDB.createConsumerWorker(topicKey, null, null, null, message -> {
                messageValidator.validate(message);
                ratePrinter.inc();
                messagesReceivedCounter.incrementAndGet();
            });
        };
    }

    protected void stopReader() {
        worker.close();
    }
}