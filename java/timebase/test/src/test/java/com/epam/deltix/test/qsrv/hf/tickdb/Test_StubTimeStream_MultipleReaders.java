package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickDBFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Alexei Osipov
 */
public class Test_StubTimeStream_MultipleReaders extends TDBTestBase {
    private static final int NUMBER_OF_READERS = 4;
    private static final long DURATION_IN_SECONDS = TimeUnit.MINUTES.toSeconds(1);

    private static final String streamKey = "stubStream";

    public Test_StubTimeStream_MultipleReaders() {
        super(false);
    }

    @Test
    public void testStubStreamWithMultipleReaders() throws Exception {
        testStubStreamWithMultipleReaders(getTickDb(), NUMBER_OF_READERS, DURATION_IN_SECONDS, 54);
    }

    private double testStubStreamWithMultipleReaders(DXTickDB tickDb, int numberOfReaders, long durationInSeconds, int bufferSizeKb) throws InterruptedException {
        DXTickStream oldStream = tickDb.getStream(streamKey);
        if (oldStream != null) {
            oldStream.delete();
        }

        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setFlag(TDBProtocol.AF_STUB_STREAM, true);
        streamOptions.scope = StreamScope.RUNTIME;

        DXTickStream stream = tickDb.createStream(streamKey, streamOptions);

        AtomicBoolean stopFlag = new AtomicBoolean(false);

        SelectionOptions options = new SelectionOptions();
        options.channelBufferSize = bufferSizeKb << 10;

        List<Reader> readers = new ArrayList<>();
        for (int i = 0; i < numberOfReaders; i++) {

            readers.add(new Reader(stream.createCursor(options), stopFlag));
        }
        for (Reader reader : readers) {
            reader.start();
        }
        System.out.println("Running...");
        Thread.sleep(TimeUnit.SECONDS.toMillis(durationInSeconds));
        stopFlag.set(true);

        long totalTimeNanos = 0;
        long totalMessages = 0;
        for (int i = 0; i < readers.size(); i++) {
            Reader reader = readers.get(i);
            reader.join();
            long count = reader.getMessageCount();
            long readerTimeNanos = reader.getReaderTime();
            totalMessages += count;
            totalTimeNanos += readerTimeNanos;

            double readerSpeed = count * TimeUnit.SECONDS.toNanos(1) / readerTimeNanos; // Msgs/sec
            System.out.println(String.format("Reader %d: Speed: %.3f Mmsg/s.   Count: %.1f M. Time: %.1f s",
                    i + 1,
                    readerSpeed / 1_000_000,
                    ((double) count) / 1_000_000,
                    ((double) readerTimeNanos) / TimeUnit.SECONDS.toNanos(1)
            ));
        }
        double totalSpeed = totalMessages / durationInSeconds;
        double avgSpeedPerReader = totalSpeed / numberOfReaders;
        System.out.println(String.format("Total speed: %.1f Mmsg/s (avg: %.3f Mmsg/s).   Count: %.1f M. Time: %.1f s",
                totalSpeed / 1_000_000,
                avgSpeedPerReader / 1_000_000,
                ((double) totalMessages) / 1_000_000,
                ((double) totalTimeNanos / numberOfReaders) / TimeUnit.SECONDS.toNanos(1)
        ));
        return totalSpeed;
    }

    private static class Reader extends Thread {
        private final TickCursor cursor;
        private final AtomicBoolean stopFlag;

        private volatile long readerTime = -1;
        private volatile long messageCount = -1;

        private Reader(TickCursor cursor, AtomicBoolean stopFlag) {
            this.cursor = cursor;
            this.stopFlag = stopFlag;
        }

        @Override
        public void run() {
            cursor.reset(Long.MIN_VALUE);
            long count = 0;
            long blackHole = 0;
            long t0 = System.nanoTime();
            while (!stopFlag.get() && cursor.next()) {
                count++;
                // Touch message time to emulate message object access
                blackHole = blackHole & cursor.getMessage().getNanoTime();
            }
            long t1 = System.nanoTime();
            readerTime = t1 - t0;
            assert blackHole == 0;
            messageCount = count + blackHole;
            cursor.close();
        }

        public long getReaderTime() {
            return readerTime;
        }

        public long getMessageCount() {
            return messageCount;
        }
    }

    // Args: port number_of_readers test_duration_s buffer_size_kb number_of_iterations
    // Example: 8011 12 128 60 1
    public static void main(String[] args) throws InterruptedException {
        int port = 8011;
        int numberOfReaders = NUMBER_OF_READERS;
        long durationInSeconds = DURATION_IN_SECONDS;
        int bufferSize = 128; // Kb
        int iterations = 1;

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            numberOfReaders = Integer.parseInt(args[1]);
        }
        if (args.length >= 3) {
            durationInSeconds = Integer.parseInt(args[2]);
        }
        if (args.length >= 4) {
            bufferSize = Integer.parseInt(args[3]);
        }
        if (args.length >= 5) {
            iterations = Integer.parseInt(args[4]);
        }
        System.out.println("numberOfReaders: " + numberOfReaders);
        System.out.println("durationInSeconds: " + durationInSeconds);
        System.out.println("bufferSize: " + bufferSize + "k");
        System.out.println("iterations: " + iterations);
        System.out.println("TimeBase.sockets: " + System.getProperty("TimeBase.sockets"));

        assert iterations > 0;
        double[] results = new double[iterations];
        for (int i = 0; i < iterations; i++) {
            DXTickDB remoteDB = TickDBFactory.connect("localhost", port, false);
            remoteDB.open(false);
            try {
                Test_StubTimeStream_MultipleReaders test = new Test_StubTimeStream_MultipleReaders();
                double speed = test.testStubStreamWithMultipleReaders(remoteDB, numberOfReaders, durationInSeconds, bufferSize);
                results[i] = speed / 1_000_000;
            } finally {
                remoteDB.close();
            }
        }
        Arrays.sort(results);
        System.out.println("Results: " + Arrays.toString(results));
        //noinspection ConstantConditions
        System.out.println("Avg: " + Arrays.stream(results).average().getAsDouble());
    }
}