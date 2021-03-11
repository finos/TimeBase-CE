package com.epam.deltix.qsrv.hf;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.qsrv.hf.topic.consumer.SubscriptionWorker;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class Test_DirectChannelListener extends BaseTopicReadingTest {
    private AtomicBoolean running;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @NotNull
    protected Runnable createReader(AtomicLong messagesReceivedCounter, String channel, int dataStreamId, List<RecordClassDescriptor> types, MessageValidator messageValidator) {
        AtomicBoolean runningFlag = new AtomicBoolean(true);
        running = runningFlag;
        return () -> {
            RatePrinter ratePrinter = new RatePrinter("Reader");
            SubscriptionWorker directMessageListener = new DirectReaderFactory().createListener(aeron, false, channel, dataStreamId, types, message -> {
                messageValidator.validate(message);
                ratePrinter.inc();
                messagesReceivedCounter.incrementAndGet();
            }, null, StubData.getStubMappingProvider());
            ratePrinter.start();
            directMessageListener.processMessagesWhileTrue(runningFlag::get);
        };
    }

    protected void stopReader() {
        running.set(false);
    }
}