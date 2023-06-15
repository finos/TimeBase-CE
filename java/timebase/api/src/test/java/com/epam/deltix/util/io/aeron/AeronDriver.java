/*
 * Copyright 2021 EPAM Systems, Inc
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