/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.timebase.messages.service.ErrorMessage;
import io.aeron.CommonContext;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public abstract class BaseTopicReadingTest extends BaseAeronTest {

    private static final int TEST_DURATION_MS = 20 * 1000;
    static final int TEST_TIMEOUT = TEST_DURATION_MS + 6 * 1000;

    void executeTest() throws Exception {
        String channel = CommonContext.IPC_CHANNEL;
        int dataStreamId = new Random().nextInt();
        int serverMetadataStreamId = dataStreamId + 1;
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);
        byte loaderNumber = 1;

        AtomicLong messagesSentCounter = new AtomicLong(0);
        AtomicLong messagesReceivedCounter = new AtomicLong(0);
        AtomicBoolean senderStopFlag = new AtomicBoolean(false);

        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<Throwable>());

        Thread loaderThread = new Thread(() -> {

            MessageChannel<InstrumentMessage> messageChannel = new DirectLoaderFactory().create(aeron, false, channel, channel, dataStreamId, serverMetadataStreamId, types, loaderNumber, new ByteArrayOutputStream(8 * 1024), Collections.emptyList(), null, null);

            ErrorMessage msg = new ErrorMessage();
            msg.setSymbol("ABC");
            long messageSentCounter = 0;

            while (!senderStopFlag.get()) {
                messageSentCounter ++;
                msg.setTimeStampMs(messageSentCounter); // Se store message number in the timestamp field.
                messageChannel.send(msg);
                messagesSentCounter.set(messageSentCounter);
            }
            messageChannel.close();
        });
        loaderThread.setName("SENDER");
        loaderThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
        loaderThread.start();

        MessageValidator messageValidator = new MessageValidator();
        Runnable runnable = createReader(messagesReceivedCounter, channel, dataStreamId, types, messageValidator);

        Thread readerThread = new Thread(runnable);
        readerThread.setName("READER");
        readerThread.setUncaughtExceptionHandler((t, e) -> exceptions.add(e));
        readerThread.start();

        // Let test to work
        Thread.sleep(TEST_DURATION_MS);

        // Ask sender to stop
        senderStopFlag.set(true);
        loaderThread.join(1000);
        Assert.assertFalse("Publisher thread must stop", loaderThread.isAlive());

        // Let reader finish off the queue
        Thread.sleep(1000);

        // Stop reader
        stopReader();
        readerThread.join(1000);
        Assert.assertFalse("Subscriber thread must stop", readerThread.isAlive());

        Assert.assertTrue("Exception in threads", exceptions.isEmpty());

        Assert.assertTrue(messagesSentCounter.get() > 0);
        Assert.assertTrue(messagesReceivedCounter.get() > 0);

        Assert.assertEquals(messagesSentCounter.get(), messagesReceivedCounter.get());
    }

    protected abstract Runnable createReader(AtomicLong messagesReceivedCounter, String channel, int dataStreamId, List<RecordClassDescriptor> types, MessageValidator messageValidator);

    protected abstract void stopReader();

    public static class MessageValidator {
        long messageNumber = 0;

        public void validate(InstrumentMessage message) {
            messageNumber ++;
            ErrorMessage msg = (ErrorMessage) message;

            // Se store message number in the timestamp field.
            if (msg.getTimeStampMs() != messageNumber) {
                throw new IllegalStateException("Invalid message order");
            }
            if (!msg.getSymbol().equals("ABC")) {
                throw new AssertionError("Wrong symbol");
            }
//            if (msg.getOriginalTimestamp() != 234567890) {
//                throw new AssertionError("Wrong OriginalTimestamp");
//            }
        }
    }
}