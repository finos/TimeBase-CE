package com.epam.deltix.qsrv.hf.separateprocess;

import com.epam.deltix.qsrv.hf.RatePrinter;
import com.epam.deltix.qsrv.hf.StubData;
import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import com.epam.deltix.qsrv.hf.topic.consumer.SubscriptionWorker;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static com.epam.deltix.qsrv.hf.BaseAeronTest.createAeron;

/**
 * Note: this test requires running Aeron driver (see deltix.util.io.aeron.AeronDriver).
 * TODO: Make this test independent of external driver.
 *
 * @author Alexei Osipov
 */
@Ignore // This test requires external driver and DirectTopicLoaderTest
public class DirectTopicReaderTest {
    @Test
    public void read() throws Exception {
        Aeron aeron = createAeron("/home/deltix/aeron_test");

        String channel = CommonContext.IPC_CHANNEL;
        int dataStreamId = DirectTopicLoaderTest.INT;
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);
        byte loaderNumber = 1;


        RatePrinter ratePrinter = new RatePrinter("Listener");
        ConstantIdentityKey[] mapping = DirectTopicLoaderTest.mapping.toArray(new ConstantIdentityKey[0]);
        SubscriptionWorker directMessageListener = new DirectReaderFactory().createListener(aeron, false, channel, dataStreamId, types, new MessageProcessor() {
            @Override
            public void process(InstrumentMessage message) {
                ratePrinter.inc();
            }
        }, null, new MappingProvider() {
            @Override
            public ConstantIdentityKey[] getMappingSnapshot() {
                return mapping;
            }

            @Override
            public IntegerToObjectHashMap<ConstantIdentityKey> getTempMappingSnapshot(int neededTempEntityIndex) {
                return new IntegerToObjectHashMap<>();
            }
        });
        ratePrinter.start();
        directMessageListener.processMessagesUntilStopped();
    }

}