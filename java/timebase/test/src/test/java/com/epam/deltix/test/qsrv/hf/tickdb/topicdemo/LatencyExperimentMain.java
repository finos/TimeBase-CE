package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo;

import com.google.common.base.Splitter;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.DXClientAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.ServerException;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.DuplicateTopicException;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.EchoMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message.MessageWithNanoTime;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.testmode.CommunicationType;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.ExperimentFormat;
import com.epam.deltix.thread.affinity.Affinity;
import com.epam.deltix.util.cmdline.DefaultApplication;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * This class is designed to measure latency for topics and sockets.
 * In general, a producer sends messages with fixed rate into a topic (or stream) and when messages get back to the
 *
 * There are few possible experiment configurations controlled by "-ef" parameter:
 * <ul>
 *  <li>
 *      Value "two": Two processes (real IPC, "two process mode").
 *  </li>
 *  <li>
 *      Value "simple": Single process (simplified experiment, "simplified mode").
 *  </li>
 * </ul>
 *
 * <h3>Two processes mode</h3>
 * Producer writes data to topic X in process 1, a consumer A in process 2
 * takes data from topic X and puts it into topic Y. Consumer B in process 1 takes data from topic Y and measures message latency.
 *
 * To run the test in "two process mode", run this class in 3 JVM instances with parameters:
 * <ol>
 *     <li>-mode server -ef two</li>
 *     <li>-mode reader -ef two</li>
 *     <li>-mode loader -ef two</li>
 * </ol>
 *
 * <h3>Simplified mode</h3>
 * Producer writes data to topic X in process 1,
 * a consumer A in process 1 takes data from topic A and measures message latency.
 * To run the test in "simplified mode", run this class in 2 JVM instances with parameters:
 * <ol>
 *     <li>-mode server -ef simple</li>
 *     <li>-mode loader -ef simple</li>
 * </ol>
 *
 * @author Alexei Osipov
 */
public class LatencyExperimentMain extends DefaultApplication {
    private LatencyExperimentMain(String[] args) {
        super(args);
    }

    private final ExecutorService es = Executors.newCachedThreadPool();

    public static void main(String[] args) throws Throwable {
        new LatencyExperimentMain(args).run();
    }

    @Override
    protected void run() throws InterruptedException, IOException {
        //System.setProperty("TimeBase.transport.aeron.directory", "G:\\tmp\\aeron_dir");

        String host = getArgValue("-host", "localhost");
        int port = getIntArgValue("-port", 8011);
        int loaderWarmUpTime = getIntArgValue("-wt", 20);
        int loaderTimeToRun = getIntArgValue("-lt", 200);
        int loaderMessageRatePerMs = getIntArgValue("-mr", 20);
        //int readerCount = getIntArgValue("-rc", 1);
        CommunicationType communicationType = getCommunicationType(getArgValue("-ct", "topic"));
        System.out.println("Type: " + communicationType.name().toLowerCase());

        String modeString = getArgValue("-mode", "reader");

        String generatorMode = getArgValue("-gen", "nanos-yield");

        boolean printReadRate = Boolean.parseBoolean(getArgValue("-prr", "true"));

        ExperimentFormat experimentFormat = getExperimentFormat(getArgValue("-ef", "two"));
        System.out.println("ExperimentFormat: " + experimentFormat.name().toLowerCase());

        String affinitySettings = getArgValue("-af");
        IntSupplier affinityProvider = null;
        if (affinitySettings != null) {
            String[] split = affinitySettings.split(",");
            List<Integer> cpuList = new ArrayList<>();
            for (String s : split) {
                cpuList.add(Integer.valueOf(s));
            }

            affinityProvider = new IntSupplier() {
                Iterator<Integer> iterator = cpuList.iterator();

                @Override
                public int getAsInt() {
                    if (iterator.hasNext()) {
                        return iterator.next();
                    }
                    // Return last
                    return cpuList.get(cpuList.size() - 1);
                }
            };
            System.out.println("affinitySettings=" + cpuList);
        }
        System.out.println("Default affinity is " + Affinity.getAffinity());



        CountDownLatch stopSignal = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(stopSignal::countDown));


        Iterable<String> parts = Splitter.on(',').trimResults().omitEmptyStrings().split(modeString);

        byte consumerCount = 0;
        boolean hasDriverOrServer = false;

        List<Future> futures = new ArrayList<>();

        for (String mode : parts) {
            switch (mode) {
                case "reader":
                    consumerCount ++;
                    futures.add(startConsumer(host, port, stopSignal, consumerCount, communicationType, affinityProvider, printReadRate, experimentFormat));
                    break;
                case "loader":
                    CountDownLatch loaderFinishedSignal = new CountDownLatch(1);
                    int experimentId = new Random().nextInt();
                    futures.add(startEchoReader(host, port, stopSignal, loaderFinishedSignal, loaderMessageRatePerMs, communicationType, experimentFormat, affinityProvider, experimentId));
                    futures.add(startPublisher(host, port, stopSignal, loaderWarmUpTime, loaderTimeToRun, loaderMessageRatePerMs, loaderFinishedSignal, communicationType, generatorMode, affinityProvider, experimentId));
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
                    startServer(port, experimentFormat);
                    hasDriverOrServer = true;
                    break;
                case "channels": // Just creates necessary channels
                    createTopicsIfMissing(port, experimentFormat);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown parameter: " + mode);
            }
        }

        es.shutdown();
        if (hasDriverOrServer) {
            stopSignal.await();
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

    private Future<?> startConsumer(String host, int port, CountDownLatch latch, byte consumerNumber, CommunicationType communicationType, IntSupplier affinityProvider, boolean printReadRate, ExperimentFormat experimentFormat) {
        RemoteTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);
        setupTopicsAndStreams(client, experimentFormat);

        Future<?> future = es.submit(() -> {
            try {
                Thread.currentThread().setName("CONSUMER-" + consumerNumber);
                setAffinity(affinityProvider);

                communicationType.getReader().runReader(client, latch, consumerNumber, printReadRate, experimentFormat);
                client.close();
                System.out.println("Stopped reader #" + consumerNumber);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        System.out.println("Started reader #" + consumerNumber);
        return future;
    }

    private Future<?> startEchoReader(String host, int port, CountDownLatch stopSignal, CountDownLatch loaderFinishedSignal, int loaderMessageRatePerMs, CommunicationType communicationType, ExperimentFormat experimentFormat, IntSupplier affinityProvider, int experimentId) {
        RemoteTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);

        BooleanSupplier stopCondition = () -> stopSignal.getCount() == 0 || loaderFinishedSignal.getCount() == 0;

        ReadEchoBase echoReader = communicationType.getEchoReader(experimentFormat);


        es.submit(() -> {
            Thread.currentThread().setName("ECHO-READER-STOP");

            // Stops reader upon stopSignal.
            // This is necessary for blocking reader implementations. Like socket client.
            try {
                loaderFinishedSignal.await();
                Thread.sleep(100);
                echoReader.stop();
            } catch (InterruptedException ignore) {
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });

        Future<?> future = es.submit(() -> {
            System.out.println("Started echo reader");
            Thread.currentThread().setName("ECHO-READER");
            try {
                setAffinity(affinityProvider);

                echoReader.runEchoReader(client, stopCondition, loaderMessageRatePerMs, communicationType, experimentId);

                client.close();
                System.out.println("Stopped echo reader");
            } catch (Throwable e) {
                e.printStackTrace();
            }
        });
        System.out.println("Scheduled echo reader");
        return future;
    }

    private void setAffinity(IntSupplier affinityProvider) {
        if (affinityProvider == null) {
            BitSet currentAffinity = Affinity.getAffinity();
            System.out.println("Default affinity (" + currentAffinity + ") is used for thread " + Thread.currentThread().getName());
            return;
        }
        int cpuIndex = affinityProvider.getAsInt();
        BitSet mask = new BitSet();
        mask.set(cpuIndex);
        Affinity.setAffinity(mask);
        System.out.println("Affinity is set to use CPU #" + cpuIndex + " for thread " + Thread.currentThread().getName());
    }

    private Future<?> startPublisher(String host, int port, CountDownLatch latch, int loaderWarmUpTime, int loaderTimeToRun, int loaderMessageRatePerMs, CountDownLatch loaderFinishedSignal, CommunicationType communicationType, String generatorMode, IntSupplier affinityProvider, int experimentId) {
        RemoteTickDB client = TickDBFactory.connect(host, port, false);
        client.open(false);

        BooleanSupplier stopCondition = () -> latch.getCount() == 0;

        Future<?> future = es.submit(() -> {
            try {
                System.out.println("Started loader");
                Thread.currentThread().setName("LOADER");
                setAffinity(affinityProvider);

                long messageCount = communicationType.getWriter().runLoader(stopCondition, loaderWarmUpTime, loaderTimeToRun, loaderMessageRatePerMs, client, generatorMode, experimentId);
                // Publish final number of messages sent
                System.out.println("Total messages sent: " + messageCount);
                loaderFinishedSignal.countDown();
                client.close();
                System.out.println("Stopped loader");
            } catch (Throwable e) {
                e.printStackTrace();
                throw e;
            }
        });
        System.out.println("Scheduled loader");
        return future;
    }

    private static final int IPC_TERM_BUFFER_LENGTH = Integer.getInteger("TimeBase.transport.aeron.ipc.term.buffer.length", 2 * 1024 * 1024); // 2Mb default

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

    private static void startServer(int port, ExperimentFormat experimentFormat) throws UnknownHostException {
        String temporaryLocation = TDBRunner.getTemporaryLocation();
        QSHome.set(temporaryLocation);
        DXTickDB server = TickDBFactory.create(temporaryLocation);
        server.open(false);

        TickDBServer dbServer = new TickDBServer(port, server);
        //dbServer.setAddress(InetAddress.getByName(host));
        dbServer.start();
        createTopicsIfMissing(port, experimentFormat);
        System.out.println("Started server");
    }

    private static void createTopicsIfMissing(int port, ExperimentFormat experimentFormat) {
        RemoteTickDB client = TickDBFactory.connect("localhost", port,  false);
        client.open(false);
        setupTopicsAndStreams(client, experimentFormat);
        client.close();
    }

    private static void setupTopicsAndStreams(RemoteTickDB db, ExperimentFormat experimentFormat) {
        createTopicIfMissing(db, DemoConf.DEMO_MAIN_TOPIC, MessageWithNanoTime.getRecordClassDescriptor());
        createStreamIfMissing(db, DemoConf.DEMO_MAIN_STREAM, MessageWithNanoTime.getRecordClassDescriptor());

        if (experimentFormat.useEchoChannel()) {
            RecordClassDescriptor rc;
            if (experimentFormat.getEchoMessageClass().equals(MessageWithNanoTime.class)) {
                rc = MessageWithNanoTime.getRecordClassDescriptor();
            } else {
                rc = EchoMessage.getRecordClassDescriptor();
            }

            createTopicIfMissing(db, DemoConf.DEMO_ECHO_TOPIC, rc);
            createStreamIfMissing(db, DemoConf.DEMO_ECHO_STREAM, rc);
        }
    }

    private static void createTopicIfMissing(RemoteTickDB db, String topicKey, RecordClassDescriptor o) {
        if (db.getTopicDB().listTopics().contains(topicKey)) {
            // Topic already exists
            return;
        }
        try {
            createTopic(db, topicKey, o);
        } catch (DuplicateTopicException ignored) {
        }
    }

    private static void createStreamIfMissing(RemoteTickDB db, String streamKey, RecordClassDescriptor o) {
        if (db.getStream(streamKey) != null) {
            // Stream already exists
            return;
        }
        try {
            StreamOptions options = new StreamOptions();
            options.scope = DemoConf.STREAM_SCOPE;
            System.out.println("Created stream scope: " + options.scope.name());
            options.setFixedType(o);
            db.createStream(streamKey, options);
        } catch (ServerException e) {
            //noinspection StatementWithEmptyBody
            if (e.getMessage().contains("Duplicate stream key")) {
                // Somebody else created stream with same name
                // e.printStackTrace();
            } else {
                throw e;
            }
        }
    }

    private static void createTopic(RemoteTickDB db, String topicKey, RecordClassDescriptor o) {
        db.getTopicDB().createTopic(topicKey, new RecordClassDescriptor[]{o}, null);
    }

    private CommunicationType getCommunicationType(String name) {
        switch (name) {
            case "ipc":
                return CommunicationType.IPC_STREAM;
            case "socket":
                return CommunicationType.SOCKET_STREAM;
            case "topic":
                return CommunicationType.TOPIC;
        }
        throw new RuntimeException("Wrong CommunicationType: " + name);
    }

    private ExperimentFormat getExperimentFormat(String format) {
        switch (format) {
            case "simple": // F2
                return ExperimentFormat.ONE_TOPIC;
            case "two": // F1
                return ExperimentFormat.TWO_TOPIC_TWO_MESSAGES;
            case "twos": // F4
                return ExperimentFormat.TWO_TOPIC_ONE_MESSAGE;
        }
        throw new RuntimeException("Wrong ExperimentFormat: " + format);
    }
}
