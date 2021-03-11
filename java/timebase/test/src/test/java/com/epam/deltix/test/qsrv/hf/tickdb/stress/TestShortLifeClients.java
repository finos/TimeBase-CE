package com.epam.deltix.test.qsrv.hf.tickdb.stress;


import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.MessageInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tests TB stability.
 *
 * Creates multiple streams. For each stream there are two threads:
 * 1) Creates a loader, writes some random number of messages and closes the loader. Starts over.
 * 2) Creates a reader, reads some random number of messages and closes the reader. Starts over.
 *
 * All loaders and readers create a new TB client on each iteration.
 *
 * @author Alexei Osipov
 */
// Stress test - don't run in generic build
public class TestShortLifeClients extends BaseStressTest {
    private static final int STREAM_COUNT = 8;
    private static final int DURATION_MINUTES = 10;

    private DXTickStream createTestStream(String name) {
        DXTickDB db = createClient();
        db.open(false);
        DXTickStream oldStream = db.getStream(name);
        if (oldStream != null) {
            oldStream.delete();
        }

        return db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, name, 0,
                        StreamConfigurationHelper.mkBarMessageDescriptor(null, null, null,
                                FloatDataType.ENCODING_SCALE_AUTO,
                                FloatDataType.ENCODING_SCALE_AUTO)));
    }

    @Test
    public void test() throws InterruptedException {
        List<Thread> threads = new ArrayList<>();
        List<DXTickStream> streams = new ArrayList<>();
        List<AtomicLong> loaderCounters = new ArrayList<>();
        List<AtomicLong> consumerCounters = new ArrayList<>();
        try {
            for (int i = 0; i < STREAM_COUNT; i++) {
                String streamName = "test" + i;
                DXTickStream stream = createTestStream(streamName);
                streams.add(stream);
                AtomicLong loaderCounter = new AtomicLong();
                AtomicLong consumerCounter = new AtomicLong();
                threads.add(createLoaderThread(streamName, loaderCounter));
                threads.add(createConsumerThread(streamName, consumerCounter));
                loaderCounters.add(loaderCounter);
                consumerCounters.add(consumerCounter);
            }

            threads.add(new PrinterThread(loaderCounters, consumerCounters));

            for (Thread thread : threads) {
                thread.start();
            }

            Thread.sleep(TimeUnit.MINUTES.toMillis(DURATION_MINUTES));
            for (Thread thread : threads) {
                thread.interrupt();
            }
            for (Thread thread : threads) {
                thread.join(5_000);
            }
        } finally {
            for (DXTickStream stream : streams) {
                stream.delete();
            }
        }
    }

    private Thread createConsumerThread(String streamName, AtomicLong consumerCounter) {
        return new Thread(() -> {
            Random rnd = new Random();
            int count = 0;
            while (!Thread.currentThread().isInterrupted()) {
                DXTickDB client = createClient();
                client.open(false);
                DXTickStream stream = client.getStream(streamName);

                final TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false));
                int targetCursorMessages = rnd.nextInt(1000);
                int cursorMessages = 0;
                while (cursorMessages < targetCursorMessages && cursor.next()) {
                    count++;
                    cursorMessages++;
                    consumerCounter.set(count);
                }
                cursor.close();

                client.close();
            }
        });
    }

    private Thread createLoaderThread(String streamName, AtomicLong loaderCounter) {
        return new Thread(() -> {
            int count = 0;
            Random rnd = new Random();
            LoadingOptions options = new LoadingOptions();

            BarMessage message = new BarMessage();
            while (!Thread.currentThread().isInterrupted()) {
                DXTickDB client = createClient();
                client.open(false);
                DXTickStream stream = client.getStream(streamName);

                TickLoader<MessageInfo> loader = stream.createLoader(options);
                int numMessages = rnd.nextInt(100);
                for (int i = 0; i < numMessages; i++) {
                    /*
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    */
                    message.setSymbol("ES" + (count % 100));

                    message.setHigh(rnd.nextDouble() * 100);
                    message.setOpen(message.getHigh() - rnd.nextDouble() * 10);
                    message.setClose(message.getHigh() - rnd.nextDouble() * 10);
                    message.setLow(Math.min(message.getOpen(), message.getClose()) - rnd.nextDouble() * 10);
                    message.setVolume(rnd.nextInt(10000));
                    message.setCurrencyCode((short) 840);
                    loader.send(message);
                    count++;
                    loaderCounter.set(count);
                }
                loader.close();
                client.close();
                /*
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    return;
                }
                */
            }
        });
    }

    private class PrinterThread extends Thread {
        private final List<AtomicLong> loaderCounters;
        private final List<AtomicLong> consumerCounters;

        public PrinterThread(List<AtomicLong> loaderCounters, List<AtomicLong> consumerCounters) {
            this.loaderCounters = loaderCounters;
            this.consumerCounters = consumerCounters;
        }

        @Override
        public void run() {
            long[] prevLoaderVal = new long[loaderCounters.size()];
            long[] prevConsumerVal = new long[consumerCounters.size()];
            long prevTime = System.currentTimeMillis();
            int iteration = 0;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    return;
                }
                iteration++;
                long now = System.currentTimeMillis();
                printRate(iteration, "Loader", loaderCounters, prevLoaderVal, prevTime, now);
                printRate(iteration, "Consumer", consumerCounters, prevConsumerVal, prevTime, now);
                System.out.println("        =================================");
                prevTime = now;
            }
        }

        private void printRate(int iteration, String label, List<AtomicLong> values, long[] prevValues, long prevTime, long now) {
            long timeDiff = now - prevTime;
            for (int i = 0; i < values.size(); i++) {
                long prevValue = prevValues[i];
                long value = values.get(i).get();
                long diff = value - prevValue;
                System.out.printf("%6d: %8s %2d: Rate: %6.3f k msg/s\n", iteration, label, i, ((float) diff) / timeDiff);
                prevValues[i] = value;
            }
        }
    }
}