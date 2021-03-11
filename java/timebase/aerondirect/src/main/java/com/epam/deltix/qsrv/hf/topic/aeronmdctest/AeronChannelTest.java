package com.epam.deltix.qsrv.hf.topic.aeronmdctest;

import io.aeron.Aeron;
import io.aeron.ConcurrentPublication;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.driver.ThreadingMode;
import org.agrona.concurrent.UnsafeBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * Tool for testing different Aeron channel configurations.
 * Supports running as driver, simple publisher and simple subscriber.
 *
 * @author Alexei Osipov
 */
public class AeronChannelTest {

    public static final int STREAM_ID = 555;

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length < 1) {
            System.out.println("No arguments provided. pub/sub/driver is expected as first argument");
            return;
        }
        String type = args[0];



        String channel;
        switch (type) {
            case "pub":
                channel = args[1];
                createPublisher(channel);
                break;
            case "sub":
                channel = args[1];
                createSubscriber(channel);
                break;
            case "driver":
                createDriver();
                break;
        }


    }

    private static void createDriver() throws IOException {
        final MediaDriver.Context context = new MediaDriver.Context();

        //* min latency
/*        context.threadingMode(ThreadingMode.DEDICATED)
                .dirsDeleteOnStart(true)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .sharedIdleStrategy(new NoOpIdleStrategy());*/
        //*/

        // We not use network part of Aeron so no reason for dedicated threads // TODO: Investigate
        context.threadingMode(ThreadingMode.SHARED);
        //context.aeronDirectoryName(aeronDir);

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        //context.clientLivenessTimeoutNs(TimeUnit.MINUTES.toNanos(5));
        //context.driverTimeoutMs(TimeUnit.MINUTES.toNanos(5));

        context.dirDeleteOnStart(true);

        MediaDriver.launch(context);
        System.out.println("Aeron dir: " + context.aeronDirectoryName());
    }

    private static void createPublisher(String channel) throws InterruptedException {
        Aeron aeron = createAeron();


        ConcurrentPublication publication = aeron.addPublication(channel, STREAM_ID);

        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);

        UnsafeBuffer buffer = new UnsafeBuffer(byteBuffer);
        System.out.println("Publisher created");
        while (!Thread.currentThread().isInterrupted()) {
            long value = System.currentTimeMillis();
            byteBuffer.putLong(0, value);
            long result = publication.offer(buffer, 0, Long.BYTES);
            System.out.println(value + " " + resultToTExt(result));
            Thread.sleep(1000);
        }
    }

    private static String resultToTExt(long result) {

        if (result >= 0) {
            return "OK";
        }
        if (result == Publication.NOT_CONNECTED) {
            return "NOT_CONNECTED";
        }
        if (result == Publication.CLOSED) {
            return "CLOSED";
        }
        if (result == Publication.ADMIN_ACTION) {
            return "ADMIN_ACTION";
        }
        if (result == Publication.BACK_PRESSURED) {
            return "BACK_PRESSURED";
        }
        if (result == Publication.MAX_POSITION_EXCEEDED) {
            return "MAX_POSITION_EXCEEDED";
        }
        return "UNKNOWN";
    }

    private static void createSubscriber(String channel) {
        Aeron aeron = createAeron();

        Subscription subscription = aeron.addSubscription(channel, STREAM_ID);

        ByteBuffer byteBuffer = ByteBuffer.allocate(Long.BYTES);

        FragmentAssembler fragmentHandler = new FragmentAssembler((buffer, offset, length, header) -> {
            buffer.getBytes(offset, byteBuffer, 0, length);
            long now = System.currentTimeMillis();
            long value = byteBuffer.getLong(0);

            System.out.println(value + " " + (now - value));
        });
        System.out.println("Subscriber created");

        while (!Thread.currentThread().isInterrupted()) {
            subscription.poll(fragmentHandler, 100);
        }
    }

    public static Aeron createAeron() {
        Aeron.Context context = new Aeron.Context();
        //context.aeronDirectoryName(aeronDir);

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        context.driverTimeoutMs(TimeUnit.MINUTES.toNanos(5));

        Aeron aeron = Aeron.connect(context);
        System.out.println("Aeron dir: " + context.aeronDirectoryName());
        return aeron;
    }
}
