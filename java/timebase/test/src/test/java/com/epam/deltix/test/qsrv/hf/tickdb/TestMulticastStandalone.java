package com.epam.deltix.test.qsrv.hf.tickdb;

import com.google.common.base.Splitter;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.DXClientAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.time.TimeKeeper;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Alexei Osipov
 */
public class TestMulticastStandalone extends DefaultApplication {
    private TestMulticastStandalone(String[] args) {
        super(args);
    }

    private final ExecutorService es = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Throwable {
        new TestMulticastStandalone(args).run();
    }

    @Override
    protected void run() throws InterruptedException, IOException {
        String host = getArgValue("-host", "localhost");
        int port = getIntArgValue("-port", 8057);
        int loaderTimeToRun = getIntArgValue("-lt", 60);
        int loaderMessageRatePerMs = getIntArgValue("-lr", 10);
        String streamKey = getArgValue("-stream", "multicast_test_stream");

        String aeronDir = DXClientAeronContext.getAeronDirForRemoteServer();
        if (aeronDir == null) {
            throw new IllegalStateException("Client is not configured for interaction with remote Aeron server. Specify " +
                    DXClientAeronContext.ENV_VAR_AERON_DIR + " environment variable or " +
                    DXClientAeronContext.PROP_NAME_AERON_DIRECTORY + " property");
        }

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
                    futures.add(startConsumer(host, port, streamKey, latch, consumerCount));
                    break;
                case "loader":
                    futures.add(startPublisher(host, port, streamKey, latch, loaderTimeToRun, loaderMessageRatePerMs));
                    break;
                case "driver":
                    startDriver(aeronDir);
                    hasDriverOrServer = true;
                    break;
                case "server":
                    startServer(host, port, streamKey);
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

    private Future<?> startConsumer(String host, int port, String streamKey, CountDownLatch latch, int consumerNumber) {
        DXTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);
        DXTickStream stream = client.getStream(streamKey);
        MessageSource<InstrumentMessage> cursor = stream.selectMulticast(false);

        Future<?> future = es.submit(() -> {
            int msgCount = 0;
            long startTime = TimeKeeper.currentTime;
            long prevTime = startTime;
            int checkTimeEachMessages = 1_000;
            long timeIntervalMs = TimeUnit.SECONDS.toMillis(10);
            while (latch.getCount() > 0) {
                if (cursor.next()) {
                    msgCount++;
                    cursor.getMessage();
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
                } else {
                    System.out.println("Reader got false from next()");
                }
            }
        });
        System.out.println("Started reader #" + consumerNumber);
        return future;
    }

    private Future<?> startPublisher(String host, int port, String streamKey, CountDownLatch latch, int loaderTimeToRun, int loaderMessageRatePerMs) {
        DXTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);
        DXTickStream stream = createStreamIfMissing(streamKey, client);

        LoadingOptions lo = new LoadingOptions(false);
        TickLoader loader = stream.createLoader(lo);

        Future<?> future = es.submit(() -> {
            TradeMessage msg = createTradeMessage("MSFT");
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

    private static void startServer(String host, int port, String streamKey) throws UnknownHostException {
        DXTickDB server = TickDBFactory.create(TDBRunner.getTemporaryLocation());
        server.open(false);
        createStreamIfMissing(streamKey, server);

        TickDBServer dbServer = new TickDBServer(port, server);
        //dbServer.setAddress(InetAddress.getByName(host));
        dbServer.start();
        System.out.println("Started server");
    }

    private static DXTickStream createStreamIfMissing(String streamKey, DXTickDB db) {
        DXTickStream stream = db.getStream(streamKey);
        if (stream == null) {
            StreamOptions options = new StreamOptions ();
            options.scope = StreamScope.RUNTIME;
            options.bufferOptions = new BufferOptions();
            options.bufferOptions.initialBufferSize = 1*1024*1024;
            options.bufferOptions.maxBufferSize = 4*1024*1024;
            options.bufferOptions.lossless = true;
            options.setFixedType(makeTradeMessageDescriptor());

            stream = db.createStream(streamKey, options);
        }
        return stream;
    }


    private static TradeMessage createTradeMessage(String symbol) {
        TradeMessage msg = new TradeMessage();
        msg.setSymbol(symbol);
        msg.setOriginalTimestamp(System.currentTimeMillis());
        msg.setTimeStampMs(msg.getOriginalTimestamp());
        msg.setPrice(25);
        msg.setSize(10);
        return msg;
    }

    private static RecordClassDescriptor makeTradeMessageDescriptor ()
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
