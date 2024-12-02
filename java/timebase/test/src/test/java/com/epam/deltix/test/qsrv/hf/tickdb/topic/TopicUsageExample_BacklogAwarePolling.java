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
package com.epam.deltix.test.qsrv.hf.tickdb.topic;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class illustrates possible implementation of a polling consumer that is aware of backlog
 * and can switch to "fast processing mode" when unprocessed message backlog is too big.
 *
 * <p>In this file only consumer code is shown. For full example see {@link TopicUsageExample}.
 */
public class TopicUsageExample_BacklogAwarePolling {
    /**
     * Needs configured Aeron dir property and running Aeron driver on that dir:
     * -DTimeBase.transport.aeron.directory=/YOUR/AERON/DIR
     */
    public static void main(String[] args) throws InterruptedException {
        String tbServerUrl = "dxtick://localhost:8011";

        try (DXTickDB db = TickDBFactory.openFromUrl(tbServerUrl, false)) {
            TopicDB topicDB = db.getTopicDB();

            AtomicBoolean stopConsumer = new AtomicBoolean(false);
            Thread pollingConsumerThread = new Thread(() -> runPollingConsumer(topicDB, stopConsumer));

            pollingConsumerThread.start();

            System.out.println("Running...");

            // Let it run for some time
            Thread.sleep(20_000);

            System.out.println("Stopping...");

            stopConsumer.set(true);
            pollingConsumerThread.join();

            System.out.println("Done");
        }
    }



    // Recommended way to consume data from topic
    private static void runPollingConsumer(TopicDB topicDB, AtomicBoolean stopConsumer) {
        YieldingIdleStrategy idleStrategy = new YieldingIdleStrategy();

        CustomMessageProcessor processor = new CustomMessageProcessor();

        // IMPORTANT: MessagePoller#close() method must be called from the same thread as MessagePoller#processMessages()
        try (MessagePoller messagePoller = topicDB.createPollingConsumer("testTopic", null)) {
            int messageCountLimit = 1000;
            int bufferFillPercentageForFastMode = 50;
            while (!stopConsumer.get()) {
                // Non-blocking call
                int messagesProcessed = messagePoller.processMessages(messageCountLimit, processor);
                if (messagesProcessed >= messageCountLimit) {
                    if (!processor.fastMode) {
                        byte bufferFillPercentage = messagePoller.getBufferFillPercentage();
                        if (bufferFillPercentage >= bufferFillPercentageForFastMode) {
                            processor.fastMode = true;
                        }
                    }
                } else {
                    processor.fastMode = false;

                    // Alternatively we can wait till "messagesProcessed < messageCountLimit" happens multiple times
                    // in a row and only then trigger off "fast mode". This may be necessary if mode switch is costly,
                    // and we do not want to turn it off if we are not certain that backlog is empty.
                }

                // Not necessary. This is only needed if your thread does not have other work to do, and you want to reduce CPU usage.
                idleStrategy.idle(messagesProcessed);
            }
        } finally {
            System.out.println("Total size for polling consumer: " + processor.totalSize);
        }
    }

    private static class CustomMessageProcessor implements MessageProcessor {
        double totalSize = 0;
        boolean fastMode = false;
        long skippedMessages = 0;

        @Override
        public void process(InstrumentMessage msg) {
            // Message processing logic
            if (fastMode) {
                // Do something fast to avoid queueing up unprocessed messages
                skippedMessages++;
            } else {
                // Normal processing
                // Assume that it is much slower than "fast mode" processing
                TradeMessage tradeMsg = (TradeMessage) msg;
                double size = tradeMsg.getSize();
                totalSize += size;
            }
        }
    }
}
