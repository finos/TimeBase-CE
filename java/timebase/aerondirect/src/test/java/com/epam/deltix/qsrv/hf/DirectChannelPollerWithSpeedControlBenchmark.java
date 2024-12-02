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
package com.epam.deltix.qsrv.hf;

import com.epam.deltix.qsrv.hf.tickdb.pub.Messages;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.timebase.messages.service.ErrorMessage;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;
import com.epam.deltix.util.time.TimeKeeper;
import io.aeron.Aeron;
import io.aeron.CommonContext;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Alexei Osipov
 */
@Ignore // This test does not stop by itself
public class DirectChannelPollerWithSpeedControlBenchmark extends BaseAeronTest {

    @Test
    public void testSpeedControlWithFastAndSlowConsumers() {

        String channel = CommonContext.IPC_CHANNEL;
        int dataStreamId = new Random().nextInt();
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);

        Thread loaderThread = new Thread(new MessageSenderRunnable(aeron, channel, dataStreamId, types));
        loaderThread.setName("SENDER");
        loaderThread.start();

        RatePrinter ratePrinter = new RatePrinter("Reader");
        MessagePoller messagePoller = new DirectReaderFactory().createPoller(aeron, false, channel, dataStreamId, types, null);
        ratePrinter.start();
        YieldingIdleStrategy idleStrategy = new YieldingIdleStrategy();

        MessageProcessor mainProcessor = m -> {
            ErrorMessage msg = (ErrorMessage) m;
            if (!msg.getSymbol().equals("ABC")) {
                throw new AssertionError("Wrong symbol");
            }
            if (msg.getSeqNum() != 234567890) {
                throw new AssertionError("Wrong OriginalTimestamp");
            }
            ratePrinter.inc();
            // Do something slow
        };

        MessageProcessor fastProcessor = m -> {
            // This processor just skips messages
        };


        while (true) {
            int workCount;
            if (messagePoller.getBufferFillPercentage() < 70) {
                // Main path
                workCount = messagePoller.processMessages(100, mainProcessor);
            } else {
                // Fast path
                workCount = messagePoller.processMessages(1000, fastProcessor);
            }
            idleStrategy.idle(workCount);
        }
    }

    @Test
    public void testSpeedControlWithDiscardOnSlow() {

        String channel = CommonContext.IPC_CHANNEL;
        int dataStreamId = new Random().nextInt();
        List<RecordClassDescriptor> types = Collections.singletonList(Messages.ERROR_MESSAGE_DESCRIPTOR);

        Thread loaderThread = new Thread(new MessageSenderRunnable(aeron, channel, dataStreamId, types));
        loaderThread.setName("SENDER");
        loaderThread.start();

        RatePrinter ratePrinter = new RatePrinter("Reader");
        MessagePoller messagePoller = new DirectReaderFactory().createPoller(aeron, false, channel, dataStreamId, types, null);
        ratePrinter.start();
        YieldingIdleStrategy idleStrategy = new YieldingIdleStrategy();

        MessageProcessor messageProcessor = m -> {
            if (messagePoller.getBufferFillPercentage() < 70) {
                // Do something slow
                ErrorMessage msg = (ErrorMessage) m;
                if (!msg.getSymbol().equals("ABC")) {
                    throw new AssertionError("Wrong symbol");
                }
                if (msg.getSeqNum() != 234567890) {
                    throw new AssertionError("Wrong OriginalTimestamp");
                }
                ratePrinter.inc();
            } else {
                // Do something fast
                // Just skip message
            }
        };


        while (true) {
            idleStrategy.idle(messagePoller.processMessages(1000, messageProcessor));
        }
    }

    private static class MessageSenderRunnable implements Runnable {
        private final Aeron aeron;
        private final String channel;
        private final int dataStreamId;
        private final List<RecordClassDescriptor> types;

        MessageSenderRunnable(Aeron aeron, String channel, int dataStreamId, List<RecordClassDescriptor> types) {
            this.aeron = aeron;
            this.channel = channel;
            this.dataStreamId = dataStreamId;
            this.types = types;
        }

        @Override
        public void run() {

            MessageChannel<InstrumentMessage> channel2 = new DirectLoaderFactory().create(aeron, false, channel, dataStreamId, types, null, null, null, false);

            ErrorMessage msg = new ErrorMessage();
            msg.setSymbol("ABC");
            msg.setSeqNum(234567890);

            while (true) {
                msg.setTimeStampMs(TimeKeeper.currentTime);
                channel2.send(msg);
            }
        }
    }
}