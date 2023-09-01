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
package com.epam.deltix.qsrv.hf.tickdb.tests;

import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.JsonReport;
import com.epam.deltix.util.cmdline.DefaultApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.*;

/**
 * This class runs 3 tasks in different processes:
 * 1. Load task: loads random messages to stream.
 * 2. Read task: opens 10 live unordered cursors and reads messages.
 * 3. Purge task: purges stream periodically.
 */
public class PurgeTestRunner extends DefaultApplication {

    private final Path reportsPath;
    private final Path logsPath;

    private Process purge, load, read, timebase;

    protected PurgeTestRunner(String[] args) {
        super(args);
        File reportsDir = getMandatoryFileArg("-reports");
        reportsPath = reportsDir.toPath().toAbsolutePath();
        String logsDir = getArgValue("-logs", ".");
        logsPath = Paths.get(logsDir);
    }

    @Override
    public void run() throws IOException {
        prepareReports(reportsPath, isArgSpecified("-cleanReports"));
        prepareLogs(logsPath, isArgSpecified("-cleanLogs"));
        int readers = getIntArgValue("-readers", 10);
        int loadRate = getIntArgValue("-loadRate", 100_000);
        int payload = getIntArgValue("-loadBytes", 100);
        int symbols = getIntArgValue("-symbols", 100);
        long purgePeriod = getLongArgValue("-purgePeriod", 60 * 1000);
        long purgeInterval = getLongArgValue("-purgeInterval", 2 * 60 * 1000);
        String streamKey = getArgValue("-stream", "testStream");
        String loaderHeap = getArgValue("-loadersHeap", "128m");
        String readerHeap = getArgValue("-readersHeap", "128m");
        int port = getIntArgValue("-port", 8011);
        String tbVersion = getArgValue("-tbVersion", "5.0");
        String home = getMandatoryArgValue("-home");
        String tbHeap = getArgValue("-tbHeap", "4G");
        String dbUrl = String.format("dxtick://localhost:%d", port);

        ShutdownSignal shutdownSignal = new ShutdownSignal();
        ProcessBuilder tbPB = TestUtils.getTbProcess(port, home, tbVersion, tbHeap,false);
        ProcessBuilder purgeProcessBuilder = getPurgeProcess(dbUrl, streamKey, purgePeriod, purgeInterval, logsPath);
        ProcessBuilder loadProcessBuilder = getStressLoadProcess(dbUrl, streamKey, loadRate, symbols, reportsPath.toString(),
                loaderHeap, payload, false, logsPath);
        ProcessBuilder readProcessBuilder = getReadRawProcess(dbUrl, streamKey, readers, reportsPath.toString(),
                readerHeap, logsPath);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            destroy(purge);
            exitGracefully(load);
            exitGracefully(read);
            JsonReport.mergeReports(reportsPath, "merged-reports");
            destroy(timebase);
        }));
        try {
            timebase = tbPB.start();
            createBinaryStream(dbUrl, streamKey, StreamOptions.MAX_DISTRIBUTION);
            load = loadProcessBuilder.start();
            read = readProcessBuilder.start();
            purge = purgeProcessBuilder.start();
            shutdownSignal.await();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new PurgeTestRunner(args).start();
    }
}