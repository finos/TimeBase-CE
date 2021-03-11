package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.qsrv.hf.pub.ChannelPerformance;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.impl.SingleChannelStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.concurrent.QuickExecutor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class MessageLatencyTest {

    private static final String STREAM_KEY = "#TEST#MESSAGES#LATENCY#";
    private static final boolean USE_AERON = true;

    public static void run(final LatencyTestOptions options) throws InterruptedException {
        QuickExecutor testExecutor = QuickExecutor.createNewInstance("TestExecutor", null);
        testExecutor.reuseInstance();

        ChannelPerformance channelPerformance = ChannelPerformance.LATENCY_CRITICAL;

        DXTickStream stream = null;
        MessageGenerator generator = null;
        Consumer[] consumers = new Consumer[options.numConsumers];
        try {
            checkOptions(options);

            stream = options.stream;
            System.out.println("Running test using " +
                    options.numConsumers + " consumer" + (options.numConsumers > 1 ? "s." : "."));
            System.out.println("Producer rate: " + options.throughput + " msg/sec.");
            System.out.println("Stream: " + stream.getKey() + ". Scope: " + stream.getStreamOptions().scope + ".");
            System.out.println("Use Aeron: " + USE_AERON);
            System.out.println("Channel Performance: " + channelPerformance);
            if (stream.getStreamOptions().bufferOptions != null)
                System.out.println("Max buffer size: " + stream.getStreamOptions().bufferOptions.maxBufferSize/1024/1024 + "Mb.");

            if (options.stream.getScope() != StreamScope.TRANSIENT)
                stream.truncate(Long.MIN_VALUE);

            Consumer.printHeader();

            SelectionOptions selectionOptions = new SelectionOptions(true, true);
            selectionOptions.channelPerformance = channelPerformance;
            selectionOptions.allowLateOutOfOrder = true;

            Thread[] consumersThreads = new Thread[options.numConsumers];
            for (int i = 0; i < options.numConsumers; i++) {
                InstrumentMessageSource cursor;
                if (stream instanceof SingleChannelStream)
                    cursor = ((SingleChannelStream) stream).createSource(Long.MIN_VALUE, selectionOptions);
                else
                    cursor = stream.select(Long.MIN_VALUE, selectionOptions);

                consumersThreads[i] = new Thread((consumers[i] = new Consumer(cursor, options.warmupSize, options.messagesPerLaunch, options.launches, testExecutor)), "Consumer-" + i);
                consumersThreads[i].setPriority(Thread.MAX_PRIORITY);
                consumersThreads[i].setDaemon(true);
                consumersThreads[i].start();
            }

            Thread.sleep(1000);

            LoadingOptions loadingOptions = new LoadingOptions(true);
            loadingOptions.channelPerformance = channelPerformance;
            loadingOptions.allowExperimentalTransport = USE_AERON;
            TickLoader loader = stream.createLoader(loadingOptions);

            RecordClassDescriptor msgDescriptor = stream.getStreamOptions().getMetaData().getContentClasses()[0];
            generator = new MessageGenerator(loader, msgDescriptor, options.messageDataSize, options.throughput);
            Thread runner = new Thread(generator, "Message Producer");
            runner.setDaemon(true);
            runner.setPriority(Thread.MAX_PRIORITY);
            runner.start();

            for (int i = 0; i < options.numConsumers; ++i)
                consumersThreads[i].join();

            generator.active = false;
            runner.join();
        } finally {
            if (generator != null) {
                generator.close();
            }
            for (int i = 0; i < consumers.length; ++i) {
                if (consumers[i] != null)
                    consumers[i].close();
            }
            if (stream != null && stream.getScope() != StreamScope.TRANSIENT) {
                stream.truncate(Long.MIN_VALUE);
            }
            testExecutor.shutdownInstance();
        }
    }

    public static void main(String[] args) throws Throwable {
        //DXTickDB tickdb = TickDBFactory.create(Home.getPath("temp/qstest/tickdb"));
        //tickdb.format();
        //tickdb.close();

        DXTickDB tickdb = TickDBFactory.connect("localhost", 8057, false);
        tickdb.open(false);

        LatencyTestOptions testOptions = new LatencyTestOptions();
        testOptions.numConsumers = args.length > 0 ? Integer.parseInt(args[0]) : 1;
        StreamScope scope = args.length > 1 ? StreamScope.valueOf(args[1]) : StreamScope.TRANSIENT;
        testOptions.stream = getStream(tickdb, scope);

        run(testOptions);

        tickdb.close();
    }

    private static DXTickStream getStream(DXTickDB db, StreamScope scope) {
        DXTickStream stream = db.getStream(STREAM_KEY);
        if (stream != null) {
            stream.delete();
        }

        RecordClassDescriptor msgDescriptor = Messages.ERROR_MESSAGE_DESCRIPTOR;

        StreamOptions options = StreamOptions.fixedType(scope, STREAM_KEY, STREAM_KEY, 0, msgDescriptor);
        options.bufferOptions = new BufferOptions ();
        options.bufferOptions.initialBufferSize = 8 * 1024 * 1024;
        options.bufferOptions.maxBufferSize = 8 * 1024 * 1024;
        options.bufferOptions.lossless = true;
        return db.createStream(STREAM_KEY, options);
    }

    private static void checkOptions(LatencyTestOptions options) {
        if (options.stream == null)
            throw new NullPointerException("Stream is not specified!");
        if (options.messageDataSize < 0)
            throw new IllegalArgumentException(options.messageDataSize + " <  0!");
        if (options.throughput < 0)
            throw new IllegalArgumentException(options.throughput + " <  0!");
        if (options.numConsumers < 0)
            throw new IllegalArgumentException(options.numConsumers + " <  0!");
        if (options.messagesPerLaunch < 0)
            throw new IllegalArgumentException(options.messagesPerLaunch + " <  0!");
        if (options.launches < 0)
            throw new IllegalArgumentException(options.launches + " <  0!");
    }
}

class MessageGenerator implements Runnable {
    public volatile boolean active = true;
    private final TickLoader loader;
    private int throughput;

    final RawMessage trade = new RawMessage();

    MessageGenerator(TickLoader loader, RecordClassDescriptor msgDescriptor, int messageSize, int throughput) {
        this.loader = loader;
        this.throughput = throughput;

        trade.type = msgDescriptor;
        trade.data = new byte[messageSize];
        trade.length = messageSize;
        trade.setSymbol("DLTX");
    }

    @Override
    public void run() {
        final long intervalBetweenMessagesInNanos = TimeUnit.SECONDS.toNanos(1) / throughput;

        while (active) {
            long nextNanoTime = (intervalBetweenMessagesInNanos != 0) ? System.nanoTime() + intervalBetweenMessagesInNanos : 0;
            while (active) {
                if (intervalBetweenMessagesInNanos != 0) {
                    if (System.nanoTime() < nextNanoTime)
                        continue; // spin-wait
                }

                trade.setNanoTime(System.nanoTime());
                loader.send (trade);

                nextNanoTime += intervalBetweenMessagesInNanos;
            }
        }
    }

    public void         close() {
        loader.close();
    }
}

class Consumer implements Runnable {

    private final static int WARMUP_RESULTS_POS = 0;

    final int               warmupSize;
    final int               messagesPerLaunch;
    final int               launches;
    private final ArrayList<long[]> results = new ArrayList<>();
    private final double[]  avgResults = new double[PTILES.length + 1];
    final MessageSource<InstrumentMessage> source;

    private long        msgSize;

    private class Printer extends QuickExecutor.QuickTask {

        public Printer(QuickExecutor executor) {
            super(executor);
        }

        final StringBuilder sb = new StringBuilder();

        private volatile int launch = -1;
        private volatile boolean submitted = false;

        @Override
        public void run() throws InterruptedException {
            printStatistics(sb, results.get(launch), launch);
            submitted = false;
        }

        public void submit(int launch) {
            submitted = true;
            this.launch = launch;

            super.submit();
        }

        public void waitForFinish() {
            while (submitted) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private final Printer printer;

    Consumer(MessageSource<InstrumentMessage> source, int warmupSize, int messagesPerLaunch, int launches, QuickExecutor testExecutor) {
        this.source = source;
        this.warmupSize = warmupSize;
        this.messagesPerLaunch = messagesPerLaunch;
        this.launches = launches;

        results.add(new long[warmupSize]);
        for (int i = 1; i <= launches; i++)
            results.add(new long[messagesPerLaunch]);

        this.printer = new Printer(testExecutor);
    }

    @Override
    public void run() {
        int index = -1;
        int launch = WARMUP_RESULTS_POS + 1;
        long[] curResults = results.get(launch);

        warmup();

        source.next();
        if (source.getMessage() instanceof RawMessage)
            msgSize = ((RawMessage) source.getMessage()).length;

        while (source.next()) {
            final long now = System.nanoTime();
            long latency = (now - source.getMessage().getNanoTime());

            index++;

            if (index >= messagesPerLaunch) {
                index = 0;
                printer.submit(launch);
                if (++launch > launches)
                    break;
                curResults = results.get(launch);
            }

            curResults[index] = latency;
        }

        printer.waitForFinish();
        printAvgStat();
    }

    private void warmup() {
        int index = -1;

        long[] curResults = results.get(WARMUP_RESULTS_POS);
        while (source.next()) {
            final long now = System.nanoTime();
            long latency = (now - source.getMessage().getNanoTime());

            ++index;
            if (index >= warmupSize) {
                printer.submit(WARMUP_RESULTS_POS);
                break;
            }

            curResults[index] = latency;
        }
    }

    public void     close() {
        source.close();
    }

    private static final double[] PTILES = { 0, 50, 90, 99, 99.9, 99.99, 100 };
    private static final String UNIT = "mks";
    private static final double UNIT_FACTOR = 1.0d / TimeUnit.MICROSECONDS.toNanos(1);

    private void printStatistics(StringBuilder sb, long[] results, int launch) {
        Arrays.sort(results);

        long sum = 0;
        for (long x : results)
            sum += x;

        sb.setLength (0);
        if (launch == WARMUP_RESULTS_POS)
            sb.append("   warmup");
        else
            sb.append(String.format("%9s", launch + "/" + launches));
        sb.append(String.format("%10d", results.length));
        sb.append(String.format("%8d", msgSize));

        for (int i = 0; i < PTILES.length; ++i) {
            double percent = PTILES[i];
            int index = (int) (percent == 100 ? results.length - 1 : percent/100 * results.length);
            double result = results[index] * UNIT_FACTOR;
            sb.append(String.format("%10.3f", result));
            if (launch != WARMUP_RESULTS_POS)
                avgResults[i] += (result / launches);
        }
        double result = (sum/results.length) * UNIT_FACTOR;
        sb.append(String.format("%10.3f", result));
        if (launch != WARMUP_RESULTS_POS)
            avgResults[avgResults.length - 1] += (result / launches);

        flushOutput(sb);
        Arrays.fill (results, 0);
    }

    public static void printHeader() {
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("%9s", "#launch"));
        sb.append(String.format("%10s", "#messages"));
        sb.append(String.format("%8s", "#bytes"));
        for (double percent : PTILES) {
            String value;
            if (percent == 0)
                value = "Min";
            else if (percent == 100)
                value = "Max";
            else if (percent == 50)
                value = "Mean";
            else
                value = String.valueOf(percent);
            sb.append(String.format("%10s", value + "[" + UNIT + "]"));
        }
        sb.append(String.format("%10s", "Avg" + "[" + UNIT + "]"));
        flushOutput(sb);
    }

    private void printAvgStat() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 9 + 10 + 8 + 10*(PTILES.length + 1); ++i)
            sb.append("-");
        sb.append("\n");

        sb.append(String.format("%9d", launches));
        sb.append(String.format("%10d", messagesPerLaunch));
        sb.append(String.format("%8d", msgSize));

        for (double result : avgResults) {
            sb.append(String.format("%10.3f", result));
        }

        flushOutput(sb);
    }

    private static void flushOutput(StringBuilder sb) {
        synchronized (System.out) {
            for (int ii = 0; ii < sb.length (); ii++)
                System.out.print (sb.charAt (ii));
            System.out.println();
        }
    }
}
