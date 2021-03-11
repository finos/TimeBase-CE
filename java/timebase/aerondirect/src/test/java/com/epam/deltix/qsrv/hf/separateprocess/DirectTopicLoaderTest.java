package com.epam.deltix.qsrv.hf.separateprocess;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.timebase.messages.service.ErrorMessage;
import com.epam.deltix.util.time.TimeKeeper;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import static com.epam.deltix.qsrv.hf.BaseAeronTest.createAeron;

/**
 * Note: this test requires running Aeron driver (see deltix.util.io.aeron.AeronDriver).
 * TODO: Make this test independent of external driver.
 *
 * @author Alexei Osipov
 */
@Ignore // This test requires external driver and DirectTopicReaderTest
public class DirectTopicLoaderTest {

    static final int INT = 300;
    static final List<ConstantIdentityKey> mapping = Collections.singletonList(new ConstantIdentityKey("ABC"));

    @Test
    public void send() throws Exception {
        Aeron aeron = createAeron("/home/deltix/aeron_test");

        String channel = CommonContext.IPC_CHANNEL;
        int dataStreamId = INT;
        int serverMetadataStreamId = dataStreamId + 1;
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);
        byte loaderNumber = 1;

        Thread loaderThread = new Thread(new Runnable() {
            @Override
            public void run() {

                MessageChannel<InstrumentMessage> channel2 = new DirectLoaderFactory().create(aeron, false, channel, channel, dataStreamId, serverMetadataStreamId, types, loaderNumber, new ByteArrayOutputStream(8 * 1024), mapping, null, null);

                ErrorMessage msg = new ErrorMessage();
                msg.setSymbol("ABC");
                msg.setSeqNum(234567890);

                while (true) {
                    msg.setTimeStampMs(TimeKeeper.currentTime);
                    channel2.send(msg);
                }
            }
        });
        loaderThread.setName("SENDER");
        loaderThread.start();
        loaderThread.join();
    }

}