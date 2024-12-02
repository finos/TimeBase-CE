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
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.topic.consumer.ClosedDueToDataLossException;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.io.idlestrat.adapter.IdleStrategyAdapter;
import org.agrona.concurrent.SleepingIdleStrategy;

import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.epam.deltix.test.qsrv.hf.tickdb.topic.TopicUsageExample.createTopic;

/**
 * This app checks how many topics can be created and used in same time.
 *
 * <p>See <a href="https://gitlab.deltixhub.com/Deltix/QuantServer/QuantServer/-/issues/1098">Issue 1098</a>.
 */
@SuppressWarnings({"BusyWait", "CallToPrintStackTrace"})
public class TopicNumberOverflowTest {

    private static final int MAX_TOPICS = 100;

    @SuppressWarnings("BusyWait")
    public static void main(String[] args) throws InterruptedException {

        String tbServerUrl;
        if (args.length > 0) {
            tbServerUrl = args[0];
        } else {
            tbServerUrl = "dxtick://localhost:8011";
        }

        try (DXTickDB db = TickDBFactory.openFromUrl(tbServerUrl, false)) {
            TopicDB topicDB = db.getTopicDB();

            deleteTopics(topicDB);
            try {
                AtomicLong receivedMessageCounter = new AtomicLong(0);

                AtomicBoolean stopPublisher = new AtomicBoolean(false);
                AtomicBoolean stopConsumer = new AtomicBoolean(false);

                Thread statsThread = new Thread(() -> {
                    while (!stopConsumer.get()) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        long prevValue = receivedMessageCounter.getAndSet(0);
                        System.out.println("Rate: " + prevValue + " msg/s");
                    }
                });
                statsThread.setDaemon(true);

                CopyOnWriteArrayList<TopicData> topicRecords = new CopyOnWriteArrayList<>();

                Thread producerThread = new Thread(() -> runPublisher(stopPublisher, topicRecords));
                Thread consumerThread = new Thread(() -> runPollingConsumer(stopConsumer, receivedMessageCounter, topicRecords));
                producerThread.start();
                consumerThread.start();


                //statsThread.start();

                for (int i = 1; i <= MAX_TOPICS; i++) {

                    String topicKey = "testTopic" + i;
                    createTopic(topicDB, topicKey);
                    MessageChannel<InstrumentMessage> channel = topicDB.createPublisher(topicKey, null, null);
                    MessagePoller poller = topicDB.createPollingConsumer(topicKey, null);

                    topicRecords.add(new TopicData(topicKey, channel, poller));

                    System.out.println("Added topic: " + topicKey);

                    if (i == 1) {
                        statsThread.start();
                    }

                    Thread.sleep(2000);
                }
                System.out.println("All topics started");
                System.out.println("Alive topics: " + topicRecords.stream().filter(tr -> !tr.closed).count());
                Thread.sleep(30_000);
                System.out.println("Alive topics: " + topicRecords.stream().filter(tr -> !tr.closed).count());

                stopConsumer.set(true);
                stopPublisher.set(true);
            } finally {
                deleteTopics(topicDB);
            }
        }
    }

    private static void deleteTopics(TopicDB topicDB) {
        for (int i = 1; i <= MAX_TOPICS; i++) {
            try {
                String topicKey = "testTopic" + i;
                topicDB.deleteTopic(topicKey);
            } catch (TopicNotFoundException ignore) {
                // Ignore
            }
        }
    }



    private static void runPublisher(AtomicBoolean stopPublisher, CopyOnWriteArrayList<TopicData> topicRecords) {
        TradeMessage msg = new TradeMessage();
        msg.setSymbol("ABC");

        Random rng = new Random();

        while (!stopPublisher.get()) {
            for (TopicData topicRecord : topicRecords) {
                if (topicRecord.closed) {
                    topicRecords.remove(topicRecord);
                    try {
                        topicRecord.channel.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                MessageChannel<InstrumentMessage> channel = topicRecord.channel;

                // Prepare message
                msg.setSymbol("ABC");
                msg.setSize(10 + rng.nextInt(100));
                msg.setPrice(100.00 * rng.nextDouble());

                // Send it
                channel.send(msg);

                try {
                    Thread.sleep(1); // Up to 1k msg/s
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }


    // Recommended way to consume data from topic
    private static void runPollingConsumer(AtomicBoolean stopConsumer, AtomicLong receivedMessageCounter, CopyOnWriteArrayList<TopicData> topicRecords) {
        IdleStrategy idleStrategy = IdleStrategyAdapter.adapt(new SleepingIdleStrategy(100_000L));

        CustomMessageProcessor processor = new CustomMessageProcessor(receivedMessageCounter);

        try {
            long prevTs = System.nanoTime();
            while (!stopConsumer.get()) {
                int messagesProcessing = 0;
                long currentTs = System.nanoTime();
                if (currentTs - prevTs > 10_000_000L) {
                    System.out.println("Time between invocations: " + ((currentTs - prevTs) / 1_000_000L) + " ms");
                }
                prevTs = currentTs;

                for (TopicData topicRecord : topicRecords) {
                    if (topicRecord.closed) {
                        topicRecords.remove(topicRecord);
                        try {
                            topicRecord.poller.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        continue;
                    }
                    // Non-blocking call

                    try {
                        messagesProcessing += topicRecord.poller.processMessages(1000, processor);
                    } catch (ClosedDueToDataLossException e) {
                        e.printStackTrace();
                        System.out.println("Topic " + topicRecord.topicKey + " was closed due to data loss");
                        topicRecord.closed = true;
                    }
                }

                // Not necessary. This is only needed if your thread does not have other work to do, and you want to reduce CPU usage.
                idleStrategy.idle(messagesProcessing);
            }
        } finally {
            System.out.println("Total size for polling consumer: " + processor.totalSize);
        }
    }

    private static class CustomMessageProcessor implements MessageProcessor {
        private final AtomicLong receivedMessageCounter;
        double totalSize = 0;

        public CustomMessageProcessor(AtomicLong receivedMessageCounter) {
            this.receivedMessageCounter = receivedMessageCounter;
        }

        @Override
        public void process(InstrumentMessage msg) {
            // Message processing logic

            TradeMessage tradeMsg = (TradeMessage) msg;
            double size = tradeMsg.getSize();
            totalSize += size;
            receivedMessageCounter.incrementAndGet();
        }
    }

    private static class TopicData {
        private final String topicKey;
        private final MessageChannel<InstrumentMessage> channel;
        private final MessagePoller poller;
        private volatile boolean closed;

        public TopicData(String topicKey, MessageChannel<InstrumentMessage> channel, MessagePoller poller) {
            this.topicKey = topicKey;
            this.channel = channel;
            this.poller = poller;
        }
    }
}
