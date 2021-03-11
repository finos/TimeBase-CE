package com.epam.deltix.qsrv.hf;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
@Category(Object.class)
public class Test_DirectChannelPoller extends BaseTopicReadingTest {
    private AtomicBoolean running;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @Override
    protected Runnable createReader(AtomicLong messagesReceivedCounter, String channel, int dataStreamId, List<RecordClassDescriptor> types, MessageValidator messageValidator) {
        AtomicBoolean runningFlag = new AtomicBoolean(true);
        running = runningFlag;



        return () -> {
            MessagePoller messagePoller = new DirectReaderFactory().createPoller(aeron, false, channel, dataStreamId, types, StubData.getStubMappingProvider());

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