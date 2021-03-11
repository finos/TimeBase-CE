package com.epam.deltix.test.qsrv.hf.tickdb.stress;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Attempts to reproduce a situation when TB client is closed while other thread attempts to execute operation that creates new data channel.
 *
 * @author Alexei Osipov
 */
// Stress test - don't run in generic build
public class TestCloseDuringOperation extends BaseStressTest {
    public static final int DURATION_MINUTES = 10;
    public static final int CURSOR_COUNT = 10;

    public DXTickStream createTestStream(String name) {
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
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(DURATION_MINUTES);
        String streamName = "test";
        DXTickStream stream = createTestStream(streamName);
        try {
            while (System.currentTimeMillis() < deadline) {
                DXTickDB client = createClient();
                client.open(false);

                CountDownLatch allStarted = new CountDownLatch(CURSOR_COUNT);

                List<Thread> threads = new ArrayList<>();
                for (int i = 0; i < CURSOR_COUNT; i++) {
                    threads.add(createConsumerThread(client, streamName, allStarted));
                }
                for (Thread thread : threads) {
                    thread.start();
                }
                allStarted.await();

                Thread closerThread = new CloserThread(client);
                closerThread.start();

                closerThread.join();

                for (Thread thread : threads) {
                    thread.interrupt();
                }
                for (Thread thread : threads) {
                    thread.join(5_000);
                }
            }
        } finally {
            stream.delete();
        }
    }

    private Thread createConsumerThread(DXTickDB client, String streamName, CountDownLatch allStarted) {
        return new Thread(() -> {
            int delay = new Random().nextInt(200);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            boolean firstTime = true;
            while (!Thread.currentThread().isInterrupted()) {
                if (!client.isOpen()) {
                    return;
                }
                try {
                    DXTickStream stream = client.getStream(streamName);
                    if (firstTime) {
                        firstTime = false;
                        allStarted.countDown();
                    }
                    final TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(true, false));
                    cursor.close();
                } catch (IllegalStateException e) {
                    if (!e.getMessage().equals("Database is not open")) {
                        throw e;
                    }
                }
            }
        });
    }


    private class CloserThread extends Thread {
        private final DXTickDB client;

        CloserThread(DXTickDB client) {
            this.client = client;
        }

        @Override
        public void run() {
            client.close();
        }
    }
}