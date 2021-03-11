package com.epam.deltix.util.io.aeron;

import io.aeron.driver.MediaDriver;

import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */

public class AeronDriver {
    private static final String aeronDir = System.getProperty("aeronDir", "/home/deltix/aeron_test");

    private static MediaDriver createDriver(String aeronDir) {
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
        context.clientLivenessTimeoutNs(TimeUnit.MINUTES.toNanos(5));
        context.publicationUnblockTimeoutNs(TimeUnit.MINUTES.toNanos(10));
        return MediaDriver.launchEmbedded(context);
    }

    public static void main(String[] args) {
        createDriver(aeronDir);
    }
}

