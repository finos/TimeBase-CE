/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf;

import io.aeron.Aeron;
import io.aeron.driver.MediaDriver;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Base class for tests that directly depend on Aeron driver.
 *
 * @author Alexei Osipov
 */
public abstract class BaseAeronTest {
    protected Aeron aeron;
    private MediaDriver mediaDriver;

    @Before
    public void setUp() throws Exception {
        mediaDriver = startDriver();
        aeron = createAeron(mediaDriver.aeronDirectoryName());
    }

    @After
    public void tearDown() {
        aeron.close();
        mediaDriver.close();
        String pathname = mediaDriver.aeronDirectoryName();
        try {
            FileUtils.deleteDirectory(new File(pathname));
            System.out.println("Deleted " + pathname);
        } catch (IOException ignored) {
            System.out.println("Failed to delete " + pathname);
        }
    }


    public static Aeron createAeron(String aeronDir) {
        Aeron.Context context = new Aeron.Context();
        context.aeronDirectoryName(aeronDir);

        // Set high timeouts to simplify debugging. In fact we don't use Aeron's timeouts.
        context.driverTimeoutMs(TimeUnit.MINUTES.toNanos(5));

        return Aeron.connect(context);
    }


    private MediaDriver startDriver() throws IOException {
        final MediaDriver.Context context = new MediaDriver.Context();

        //* min latency
/*        context.threadingMode(ThreadingMode.DEDICATED)
                .dirsDeleteOnStart(true)
                .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
                .receiverIdleStrategy(new NoOpIdleStrategy())
                .senderIdleStrategy(new NoOpIdleStrategy())
                .sharedIdleStrategy(new NoOpIdleStrategy());*/
        //*/

        File dir = Files.createTempDirectory("aeron-test-").toFile();
        dir.deleteOnExit();
        String driverDirName = dir.getPath() + File.separator + "driver";
        System.out.println("Starting embedded driver at: " + driverDirName);
        context.aeronDirectoryName(driverDirName);
        context.clientLivenessTimeoutNs(TimeUnit.MINUTES.toNanos(5));
        MediaDriver mediaDriver = MediaDriver.launchEmbedded(context);
        System.out.println("Driver started");
        return mediaDriver;
    }
}