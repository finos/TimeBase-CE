package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.epam.deltix.qsrv.QSHome;
import io.aeron.Aeron;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * @author Alexei Osipov
 */
class TopicTestUtils {

    static void initTempQSHome() throws IOException {
        Path tempDirWithPrefix = Files.createTempDirectory("deltix-test-qshome");
        QSHome.set(tempDirWithPrefix.toString());
    }

    static Aeron createAeron() {
        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName("/home/deltix/aeron_test");

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        context.driverTimeoutMs(TimeUnit.MINUTES.toNanos(5));

        return Aeron.connect(context);
    }
}
