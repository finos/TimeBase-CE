package com.epam.deltix.util.io.aeron;

import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.SigIntBarrier;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alexei Osipov
 */
public class AeronMulticastCheckSpeed {
    public static final int SLEEP_TIME_MS = Integer.getInteger("sleepTime", 1000);
    public static final String CHANNEL = System.getProperty("aeronChannel", "aeron:udp?endpoint=224.0.1.37:40456");
    public static final int streamId = Integer.getInteger("streamId", 1);

    private final Aeron aeron;

    public AeronMulticastCheckSpeed() {
        String aeronDir = System.getProperty("aeronDir", "/home/deltix/aeron_test");

        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName(aeronDir);
        this.aeron = Aeron.connect(context);
    }

    private MediaDriver createDriver(String aeronDir) {
        final MediaDriver.Context context = new MediaDriver.Context();

        //* min latency
/*        context.threadingMode(ThreadingMode.DEDICATED)
                .dirsDeleteOnStart(true)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .sharedIdleStrategy(new NoOpIdleStrategy());*/
        //*/

        context.aeronDirectoryName(aeronDir);
        return MediaDriver.launchEmbedded(context);
    }



    private static class DataReceiver implements Runnable {
        private final Subscription subscription;
        private final AtomicLong counter;

        private DataReceiver(Aeron aeron, String channel, AtomicLong counter) {
            this.counter = counter;
            this.subscription = aeron.addSubscription(channel, streamId);
        }

        @Override
        public void run() {

            BackoffIdleStrategy idleStrategy = new BackoffIdleStrategy(1, 1, 1, 1000);

            //PrintingCounter bytesCounter = new PrintingCounter("Bytes read");

            ReaderState state = new ReaderState();

            FragmentHandler fragmentHandler = (buffer, offset, length, header) -> {
                counter.incrementAndGet();
                //bytesCounter.add(length);
            };
            //bytesCounter.start();
            while (true) {

                final int fragmentsRead = subscription.poll(fragmentHandler, 10);
                if (fragmentsRead > 0) {
                    state.fragmentCounter += fragmentsRead;
                }
                idleStrategy.idle(fragmentsRead);
            }
        }

        private class ReaderState {
            long counter = 0;
            long gotBytes = 0;
            long reportedBytes = 0;
            long lastReportedTime = 0;
            long fragmentCounter = 0;
        }
    }

    public static void main(String[] args) {
        AeronMulticastCheckSpeed aeronClient = new AeronMulticastCheckSpeed();
        AtomicLong counter = new AtomicLong(0);
        new Thread(new DataReceiver(aeronClient.aeron, CHANNEL, counter)).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                long startTime = System.currentTimeMillis();
                long prevTime = startTime;
                long reportedMessageCount = 0;
                while (true) {
                    try {
                        Thread.sleep(SLEEP_TIME_MS);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    long currentTime = System.currentTimeMillis();
                    long currentCount = counter.get();
                    long countDelta = currentCount - reportedMessageCount;
                    long timeDelta = currentTime - prevTime;
                    long secondsFromStart = (currentTime - startTime) / 1000;
                    System.out.printf("%6d: Count: %7d: Rate: %8.3f k msg/s Time: %5d \n", secondsFromStart, countDelta, ((float) countDelta) / timeDelta, timeDelta);
                    prevTime = currentTime;
                    reportedMessageCount = currentCount;
                }
            }
        }).start();
        new SigIntBarrier().await();
        aeronClient.aeron.close();
    }
}
