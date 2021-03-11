package com.epam.deltix.qsrv.hf;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.util.concurrent.CursorIsClosedException;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class Test_DirectChannelCursor extends BaseTopicReadingTest {
    private MessageSource<InstrumentMessage> messageSource;

    @Test(timeout = TEST_TIMEOUT)
    public void test() throws Exception {
        executeTest();
    }

    @NotNull
    protected Runnable createReader(AtomicLong messagesReceivedCounter, String channel, int dataStreamId, List<RecordClassDescriptor> types, MessageValidator messageValidator) {
        this.messageSource = new DirectReaderFactory().createMessageSource(aeron, false, channel, dataStreamId, types, null, StubData.getStubMappingProvider());

        return () -> {
            RatePrinter ratePrinter = new RatePrinter("Reader");
            ratePrinter.start();
            try {
                while (messageSource.next()) {
                    messageValidator.validate(messageSource.getMessage());
                    ratePrinter.inc();
                    messagesReceivedCounter.incrementAndGet();
                }
            } catch (CursorIsClosedException ignore) {
            }

            messageSource.close();
        };
    }

    protected void stopReader() {
        messageSource.close();
    }

}