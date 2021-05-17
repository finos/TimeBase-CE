/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.google.common.base.Splitter;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.DXClientAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.ServerException;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.ConsumerPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.io.idlestrat.BusySpinIdleStrategy;
import com.epam.deltix.util.time.TimeKeeper;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.IdleStrategy;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
public class TestTopicsStandalone extends DefaultApplication {
    private TestTopicsStandalone(String[] args) {
        super(args);
    }

    private final ExecutorService es = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Throwable {
        new TestTopicsStandalone(args).run();
    }

    @Override
    protected void run() throws InterruptedException, IOException {
        String host = getArgValue("-host", "localhost");
        int port = getIntArgValue("-port", 8057);
        int loaderTimeToRun = getIntArgValue("-lt", 60);
        int loaderMessageRatePerMs = getIntArgValue("-lr", 10);
        String topicKey = getArgValue("-topic", "multicast_test_stream");

        String modeString = getArgValue("-mode", "reader");

        CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(latch::countDown));


        Iterable<String> parts = Splitter.on(',').trimResults().omitEmptyStrings().split(modeString);

        int consumerCount = 0;
        boolean hasDriverOrServer = false;

        List<Future> futures = new ArrayList<>();

        for (String mode : parts) {
            switch (mode) {
                case "reader":
                    consumerCount ++;
                    futures.add(startConsumer(host, port, topicKey, latch, consumerCount));
                    break;
                case "loader":
                    futures.add(startPublisher(host, port, topicKey, latch, loaderTimeToRun, loaderMessageRatePerMs));
                    break;
                case "driver":
                    String aeronDir = DXClientAeronContext.getAeronDirForRemoteServer();
                    if (aeronDir == null) {
                        throw new IllegalStateException("Client is not configured for interaction with remote Aeron server. Specify " +
                                DXClientAeronContext.ENV_VAR_AERON_DIR + " environment variable or " +
                                DXClientAeronContext.PROP_NAME_AERON_DIRECTORY + " property");
                    }
                    startDriver(aeronDir);
                    hasDriverOrServer = true;
                    break;
                case "server":
                    startServer(host, port, topicKey);
                    hasDriverOrServer = true;
                    break;
                default:
                    throw new IllegalArgumentException("Unknown parameter: " + mode);
            }
        }

        es.shutdown();
        if (hasDriverOrServer) {
            latch.await();
        } else {
            for (Future future : futures) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

        }
        System.out.println("Main Stop");
    }

    private Future<?> startConsumer(String host, int port, String topicKey, CountDownLatch latch, int consumerNumber) {
        RemoteTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);

        Future<?> future = es.submit(() -> {
            Thread.currentThread().setName("CONSUMER-" + consumerNumber);

            MessageProcessor messageProcessor = new MessageProcessor() {
                int msgCount = 0;
                long startTime = TimeKeeper.currentTime;
                long prevTime = startTime;
                int checkTimeEachMessages = 1_000;
                long timeIntervalMs = TimeUnit.SECONDS.toMillis(10);


                @Override
                public void process(InstrumentMessage message) {
                    msgCount++;
                    if (msgCount % checkTimeEachMessages == 0) {
                        long currentTime = TimeKeeper.currentTime;
                        long timeDelta = currentTime - prevTime;

                        if (timeDelta > timeIntervalMs) {
                            long secondsFromStart = (currentTime - startTime) / 1000;
                            //System.out.println("#" + consumerNumber + ": Message rate: " + ((float) Math.round(msgCount * 1000 / timeDelta))/1000 + " k msg/s");
                            System.out.printf("%6d: #%s: Message rate: %.3f k msg/s\n", secondsFromStart, consumerNumber, ((float) msgCount) / timeDelta);
                            prevTime = currentTime;
                            msgCount = 0;
                        }
                    }
                }
            };
            MessagePoller consumerRunner = client.getTopicDB().createPollingConsumer(topicKey, new ConsumerPreferences().setRaw(true));
            IdleStrategy idleStrategy = new BackoffIdleStrategy(1, 100, 1000, 1_000_000);
            while (latch.getCount() > 0) {
                idleStrategy.idle(consumerRunner.processMessages(100, messageProcessor));
            }
        });
        System.out.println("Started reader #" + consumerNumber);
        return future;
    }

    private Future<?> startPublisher(String host, int port, String topicKey, CountDownLatch latch, int loaderTimeToRun, int loaderMessageRatePerMs) {
        RemoteTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);
        createTopicIfMissing(topicKey, client);

        MessageChannel<InstrumentMessage> loader = client.getTopicDB().createPublisher(topicKey, null, new BusySpinIdleStrategy());

        Future<?> future = es.submit(() -> {
            try {
                Thread.currentThread().setName("LOADER");
                TradeMessage msg = createTradeMessage();
                long startTime = TimeKeeper.currentTime;
                long stopTime = startTime + TimeUnit.SECONDS.toMillis(loaderTimeToRun);
                long msgCount = 0;
                long currentTime = startTime;

                while (latch.getCount() > 0 && currentTime < stopTime) {
                    currentTime = TimeKeeper.currentTime;
                    if ((currentTime - startTime) * loaderMessageRatePerMs < msgCount) {
                        Thread.yield();
                    } else {
                        msg.setTimeStampMs(currentTime);
                        loader.send(msg);
                        msgCount++;
                    }

                /*
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                */
                }
                loader.close();
                client.close();
                System.out.println("Stopped loader");
            } catch (Exception e) {
                throw e;
            }
        });
        System.out.println("Started loader");
        return future;
    }

    public static final int IPC_TERM_BUFFER_LENGTH = Integer.getInteger("TimeBase.transport.aeron.ipc.term.buffer.length", 2 * 1024 * 1024); // 2Mb default

    private void startDriver(String aeronDir) throws IOException {
        File dir = new File(aeronDir);
        if (dir.exists() && dir.isDirectory()) {
            FileUtils.deleteDirectory(dir);
        }
        final MediaDriver.Context context = new MediaDriver.Context();

        // We not use network part of Aeron so no reason for dedicated threads // TODO: Investigate
        context.threadingMode(ThreadingMode.SHARED);
        context.ipcTermBufferLength(IPC_TERM_BUFFER_LENGTH);
        //context.mtuLength(65504);
        context.aeronDirectoryName(aeronDir);

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        context.clientLivenessTimeoutNs(TimeUnit.MINUTES.toNanos(5));
        context.driverTimeoutMs(TimeUnit.MINUTES.toNanos(5));

        MediaDriver.launchEmbedded(context);
        System.out.println("Started driver");
    }

    private static void startServer(String host, int port, String topicKey) throws UnknownHostException {
        DXTickDB server = TickDBFactory.create(TDBRunner.getTemporaryLocation());
        server.open(false);

        TickDBServer dbServer = new TickDBServer(port, server);
        //dbServer.setAddress(InetAddress.getByName(host));
        dbServer.start();
        createTopicIfMissing(topicKey, port);
        System.out.println("Started server");
    }

    private static void createTopicIfMissing(String topicKey, int port) {
        RemoteTickDB client = TickDBFactory.connect("localhost", port,  false);
        client.open(false);
        createTopicIfMissing(topicKey, client);
        client.close();
    }

    private static void createTopicIfMissing(String topicKey, RemoteTickDB db) {
        if (db.getTopicDB().listTopics().contains(topicKey)) {
            // Topic already exists
            return;
        }
        try {
            db.getTopicDB().createTopic(topicKey, new RecordClassDescriptor[]{makeTradeMessageDescriptor()}, null);
        } catch (ServerException e) {
            if (e.getMessage().contains("Topic already exists")) {
                // Somebody created topic
                e.printStackTrace();
            } else {
                throw e;
            }
        }
    }


    private static TradeMessage createTradeMessageOld() {
        TradeMessage msg = new TradeMessage();
        msg.setSymbol("MSFT");
        msg.setOriginalTimestamp(System.currentTimeMillis());
        msg.setTimeStampMs(msg.getOriginalTimestamp());
        msg.setPrice(25);
        msg.setSize(10);
        return msg;
    }

    private static TradeMessage createTradeMessage() {
        TradeMessage msg = new TradeMessage();
        msg.setOriginalTimestamp(234567890);
        msg.setSymbol("ABC");
        return msg;
    }


    public static RecordClassDescriptor makeTradeMessageDescriptor ()
    {
        RecordClassDescriptor marketMsgDescriptor = mkMarketMessageDescriptor (840);


        return (StreamConfigurationHelper.mkTradeMessageDescriptor (marketMsgDescriptor, "", 840, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO));
    }

    private static RecordClassDescriptor     mkMarketMessageDescriptor (
            Integer                 staticCurrencyCode
    )
    {
        final String            name = MarketMessage.class.getName ();
        final DataField []      fields = {
                new NonStaticDataField(
                        "originalTimestamp", "Original Time",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                StreamConfigurationHelper.mkField (
                        "currencyCode", "Currency Code",
                        new IntegerDataType (IntegerDataType.ENCODING_INT16, true), null,
                        staticCurrencyCode
                )
        };

        return (new RecordClassDescriptor (name, name, true, null, fields));
    }
}
