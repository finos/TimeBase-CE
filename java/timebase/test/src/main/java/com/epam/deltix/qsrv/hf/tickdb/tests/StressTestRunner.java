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
package com.epam.deltix.qsrv.hf.tickdb.tests;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.tests.reports.JsonReport;
import com.epam.deltix.util.cmdline.DefaultApplication;
import org.agrona.concurrent.ShutdownSignalBarrier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.deltix.qsrv.hf.tickdb.tests.TestUtils.*;

public class StressTestRunner extends DefaultApplication {

    private final static Log LOG = LogFactory.getLog(StressTestRunner.class);

    private final Path reportsPath;

    protected StressTestRunner(String[] args) {
        super(args);
        File reportsDir = getMandatoryFileArg("-reports");
        reportsPath = reportsDir.toPath().toAbsolutePath();
    }

    private Process tbProcess;
    private List<Process> loadProcesses;
    private List<Process> readProcesses;

    @Override
    protected void run() throws Throwable {
        prepareReports(reportsPath, isArgSpecified("-cleanReports"));

        int readers = getIntArgValue("-readers", 1);
        int loaders = getIntArgValue("-loaders", 1);
        int loadRate = getIntArgValue("-loadRate", 100000);
        int symbols = getIntArgValue("-loadSymbols", 100);
        int payload = getIntArgValue("-loadBytes", 100);
        String streamKey = getArgValue("-stream", "testStream");
        int port = getIntArgValue("-port", 8011);
        String tbVersion = getArgValue("-tbVersion", "5.0");
        String home = getMandatoryArgValue("-home");
        String tbHeap = getArgValue("-tbHeap", "4G");
        String loaderHeap = getArgValue("-loadersHeap", "128m");
        String readerHeap = getArgValue("-readersHeap", "128m");
        boolean withoutTomcat = isArgSpecified("-withoutTomcat");
        int distributionFactor = getIntArgValue("-distributionFactor", StreamOptions.MAX_DISTRIBUTION);
        boolean deleteSymbol = isArgSpecified("-deleteSymbol");

        String dbUrl = String.format("dxtick://localhost:%d", port);

        List<String> streams = getStreamKeys(streamKey, loaders);

        ProcessBuilder tbPB = TestUtils.getTbProcess(port, home, tbVersion, tbHeap, withoutTomcat);
        List<ProcessBuilder> loadPBs = getLoadPBs(streams, dbUrl, loadRate, symbols, reportsPath.toString(), loaderHeap,
                payload, deleteSymbol);
        List<ProcessBuilder> readPBs = getReadPBs(streams, dbUrl, readers, reportsPath.toString(), readerHeap, symbols);

        Runtime.getRuntime().addShutdownHook(new Thread(this::exitAllProcesses));

        ShutdownSignal shutdownSignal = new ShutdownSignal();
        try {
            tbProcess = tbPB.start();
            createStreams(dbUrl, streams, distributionFactor);
            loadProcesses = loadPBs.stream().map(pb -> {
                try {
                    return pb.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            readProcesses = readPBs.stream().map(pb -> {
                try {
                    return pb.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            shutdownSignal.await();
        } catch (IOException exc) {
            LOG.error().append(exc).commit();
        }
    }

    private List<String> getStreamKeys(String baseKey, int loaders) {
        List<String> streams = new LinkedList<>();
        for (int i = 0; i < loaders; i++) {
            streams.add(String.format("%s-%d", baseKey, i));
        }
        return streams;
    }

    private void createStreams(String dbUrl, List<String> streams, int distributionFactor) {
        try (DXTickDB db = new DBWrapper(dbUrl).getDB()) {
            for (String stream : streams) {
                createBinaryStream(db, stream, distributionFactor);
            }
        }
    }

    private void exitAllProcesses() {
        LOG.info().append("Destroying all processes.").commit();
        if (readProcesses != null) {
            for (Process readProcess : readProcesses) {
                exitGracefully(readProcess);
            }
        }
        if (loadProcesses != null) {
            for (Process loadProcess : loadProcesses) {
                exitGracefully(loadProcess);
            }
        }
        JsonReport.mergeReports(reportsPath, "merged-reports");
        destroy(tbProcess);
    }

    private List<ProcessBuilder> getLoadPBs(List<String> streams, String dbUrl, int loadRate, int symbols,
                                            String reportsDir, String heap, int payload, boolean deleteSymbol) {
        List<ProcessBuilder> result = new LinkedList<>();
        for (String stream : streams) {
            result.add(TestUtils.getStressLoadProcess(dbUrl, stream, loadRate, symbols, reportsDir, heap, payload, deleteSymbol));
        }
        return result;
    }

    private List<ProcessBuilder> getReadPBs(List<String> streams, String dbUrl, int readers, String reportsDir, String heap, int symbols) {
        List<ProcessBuilder> result = new LinkedList<>();
        for (String stream : streams) {
            result.add(TestUtils.getReadRawProcess(dbUrl, stream, readers, reportsDir, heap, symbols));
        }
        return result;
    }

    public static void main(String[] args) {
        new StressTestRunner(args).start();
    }
}