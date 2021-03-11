package com.epam.deltix.util.io.aeron;

import com.epam.deltix.util.time.TimeKeeper;
import io.aeron.Aeron;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.FragmentHandler;
import org.agrona.concurrent.BackoffIdleStrategy;
import org.agrona.concurrent.SigIntBarrier;

/**
 * @author Alexei Osipov
 */
public class AeronMulticastCheck {
    public static final String CHANNEL = System.getProperty("aeronChannel", "aeron:udp?endpoint=224.0.1.37:40456");
    public static final int streamId = 1;

    private final Aeron aeron;

    public AeronMulticastCheck() {
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

        private DataReceiver(Aeron aeron, String channel) {
            this.subscription = aeron.addSubscription(channel, streamId);
        }

        @Override
        public void run() {

            BackoffIdleStrategy idleStrategy = new BackoffIdleStrategy(1, 1, 1, 1000);

            //PrintingCounter bytesCounter = new PrintingCounter("Bytes read");

            ReaderState state = new ReaderState();

            FragmentHandler fragmentHandler = (buffer, offset, length, header) -> {
                System.out.println("Got data: [ " + offset + ", " + length + " ]");
                //bytesCounter.add(length);
            };
            //bytesCounter.start();
            state.lastReportedTime = TimeKeeper.currentTime;
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
        AeronMulticastCheck aeronClient = new AeronMulticastCheck();
        new Thread(new DataReceiver(aeronClient.aeron, CHANNEL)).start();
        new SigIntBarrier().await();
        aeronClient.aeron.close();
    }
}
