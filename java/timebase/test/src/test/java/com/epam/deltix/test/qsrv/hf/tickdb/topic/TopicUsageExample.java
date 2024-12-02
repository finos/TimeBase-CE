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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.io.idlestrat.YieldingIdleStrategy;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

public class TopicUsageExample {
    /**
     * Needs configured Aeron dir property and running Aeron driver on that dir:
     * -DTimeBase.transport.aeron.directory=/YOUR/AERON/DIR
     */
    public static void main(String[] args) throws InterruptedException {
        String tbServerUrl = "dxtick://localhost:8011";

        try (DXTickDB db = TickDBFactory.openFromUrl(tbServerUrl, false)) {
            TopicDB topicDB = db.getTopicDB();

            try {
                topicDB.deleteTopic("testTopic");
            } catch (TopicNotFoundException ignore) {
                // Ignore
            }

            createTopic(topicDB, "testTopic");

            AtomicBoolean stopPublisher = new AtomicBoolean(false);
            AtomicBoolean stopConsumer = new AtomicBoolean(false);

            Thread producerThread = new Thread(() -> runPublisher(topicDB, stopPublisher));
            Thread consumerThread = new Thread(() -> runConsumer(topicDB, stopConsumer));
            Thread pollingConsumerThread = new Thread(() -> runPollingConsumer(topicDB, stopConsumer));

            producerThread.start();
            consumerThread.start();
            pollingConsumerThread.start();

            System.out.println("Running...");

            // Let it run for some time
            Thread.sleep(20_000);

            System.out.println("Stopping...");

            stopConsumer.set(true);
            stopPublisher.set(true);

            // Consumer thread uses blocking read, so we may need to interrupt it to stop it.
            // Polling consumer thread uses non-blocking read, so we don't need to interrupt it to stop it.
            consumerThread.interrupt();

            producerThread.join();
            consumerThread.join();
            pollingConsumerThread.join();

            topicDB.deleteTopic("testTopic");
            System.out.println("Done");
        }
    }

    public static void createTopic(TopicDB topicDB, String topicKey) {
        RecordClassDescriptor rcd = StreamConfigurationHelper.mkUniversalTradeMessageDescriptor();

        TopicSettings settings = new TopicSettings()
                .setSinglePublisherUdpMode("localhost"); // Address of the publisher. You have to specify it on topic creation.
        topicDB.createTopic(topicKey, new RecordClassDescriptor[]{rcd}, settings);
    }

    private static void runPublisher(TopicDB topicDB, AtomicBoolean stopPublisher) {
        TradeMessage msg = new TradeMessage();
        msg.setSymbol("ABC");

        Random rng = new Random();

        try (MessageChannel<InstrumentMessage> channel = topicDB.createPublisher("testTopic", null, null)) {
            while (!stopPublisher.get()) {
                // Prepare message
                msg.setSymbol("ABC");
                msg.setSize(10 + rng.nextInt(100));
                msg.setPrice(100.00 * rng.nextDouble());

                // Send it
                channel.send(msg);
            }
        }
    }

    private static void runConsumer(TopicDB topicDB, AtomicBoolean stopConsumer) {
        double totalSize = 0;
        try (MessageSource<InstrumentMessage> messageSource = topicDB.createConsumer("testTopic", null, null)) {
            // Note: "messageSource.next()" for topics will always return true.
            while (messageSource.next() && !stopConsumer.get()) {
                TradeMessage tradeMsg = (TradeMessage) messageSource.getMessage();
                double size = tradeMsg.getSize();
                totalSize += size;
            }
        } finally {
            System.out.println("Total size for consumer: " + totalSize);
        }
    }

    // Recommended way to consume data from topic
    private static void runPollingConsumer(TopicDB topicDB, AtomicBoolean stopConsumer) {
        YieldingIdleStrategy idleStrategy = new YieldingIdleStrategy();

        CustomMessageProcessor processor = new CustomMessageProcessor();

        // IMPORTANT: MessagePoller#close() method must be called from the same thread as MessagePoller#processMessages()
        try (MessagePoller messagePoller = topicDB.createPollingConsumer("testTopic", null)) {

            while (!stopConsumer.get()) {
                // Non-blocking call
                int messagesProcessing = messagePoller.processMessages(1000, processor);

                // Not necessary. This is only needed if your thread does not have other work to do, and you want to reduce CPU usage.
                idleStrategy.idle(messagesProcessing);
            }
        } finally {
            System.out.println("Total size for polling consumer: " + processor.totalSize);
        }
    }

    private static class CustomMessageProcessor implements MessageProcessor {
        double totalSize = 0;

        @Override
        public void process(InstrumentMessage msg) {
            // Message processing logic

            TradeMessage tradeMsg = (TradeMessage) msg;
            double size = tradeMsg.getSize();
            totalSize += size;
        }
    }
}
