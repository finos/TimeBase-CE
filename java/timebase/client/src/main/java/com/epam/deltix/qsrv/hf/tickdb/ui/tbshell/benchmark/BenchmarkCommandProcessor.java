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
package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

import com.google.common.base.Joiner;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.util.cmdline.ShellCommandException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Processes shell commands for throughput and latency benchmarks.
 *
 * @author Alexei Osipov
 */
public class BenchmarkCommandProcessor {

    private final TickDBShell shell;

    //@SuppressWarnings("WeakerAccess")
    private static class Config {
        int warmUpDurationSeconds = 10;
        int measurementDurationSeconds = 30;
        int targetMessageRatePerSecond = 1_000_000;
        int iterations = 1;
        int payloadSize = 0; // No extra payload by default
        List<BenchmarkChannelType> channelTypes = Arrays.asList(BenchmarkChannelType.values());
    }

    private final Config config = new Config();

    public BenchmarkCommandProcessor(TickDBShell shell) {
        this.shell = shell;
    }

    public boolean doCommand(String key, String args) {
        // TODO: Adapt exception handling to shell API

        switch (key.toLowerCase()) {
            case "benchmark":
                return processBenchmarkCommand(args);
            default:
                return false;
        }
    }

    private boolean processBenchmarkCommand(String args) {
        if (StringUtils.isBlank(args)) {
            System.err.println("Specify benchmark type. One of: thr, lat" );
            return true;
        }

        String[] parts = args.split(" ", 2);
        String subCommand = parts[0];
        switch (subCommand) {
            case "thr":
                assertArgumentCount(parts, 1);
                benchmarkThroughput();
                return true;
            case "lat":
                assertArgumentCount(parts, 1);
                benchmarkLatency();
                return true;
        }
        return false;
    }

    private void benchmarkThroughput() {
        printBenchmarkMode("throughput");
        try {
            ThroughputBenchmarkManager.execute(
                    (RemoteTickDB) shell.dbmgr.getDB(),
                    TimeUnit.SECONDS.toMillis(config.warmUpDurationSeconds),
                    TimeUnit.SECONDS.toMillis(config.measurementDurationSeconds),
                    config.iterations,
                    config.channelTypes,
                    System.out,
                    config.payloadSize
            );
        } catch (InterruptedException e) {
            System.out.println("Benchmark was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void benchmarkLatency() {
        printBenchmarkMode("latency");

        try {
            LatencyBenchmarkManager.execute(
                    (RemoteTickDB) shell.dbmgr.getDB(),
                    TimeUnit.SECONDS.toMillis(config.warmUpDurationSeconds),
                    TimeUnit.SECONDS.toMillis(config.measurementDurationSeconds),
                    config.iterations,
                    config.channelTypes,
                    System.out,
                    config.targetMessageRatePerSecond,
                    config.payloadSize
            );
        } catch (InterruptedException e) {
            System.out.println("Benchmark was interrupted");
            Thread.currentThread().interrupt();
        }
    }

    private void printBenchmarkMode(String mode) {
        System.out.println("Starting " + mode + " benchmark for channel types " + getChannelTypeNames(config.channelTypes) + " with settings:");
        doSet();
    }

    private void assertArgumentCount(String[] parts, int expectedArgumentCount) {
        int argumentCount = parts.length;
        if (argumentCount != expectedArgumentCount) {
            throw new ShellCommandException(String.format(
                    "Unexpected argument count: got %d while %d was expected", argumentCount, expectedArgumentCount
            ));
        }
    }

    public boolean doSet(String option, String value) {
        switch (option.toLowerCase()) {
            case "warmup":
                config.warmUpDurationSeconds = Integer.parseInt(value);
                shell.confirm("WarmUp time: " + config.warmUpDurationSeconds + " s");
                return true;

            case "duration":
                config.measurementDurationSeconds = Integer.parseInt(value);
                shell.confirm("Measurement duration: " + config.measurementDurationSeconds + " s");
                return true;

            case "iterations":
                config.iterations = Integer.parseInt(value);
                shell.confirm("Iteration count: " + config.iterations);
                return true;

            case "rate":
                config.targetMessageRatePerSecond = Integer.parseInt(value);
                shell.confirm("Message rate: " + config.targetMessageRatePerSecond);
                return true;

            case "payloadsize":
                config.payloadSize = Integer.parseInt(value);
                shell.confirm("Test message payload size: " + config.payloadSize);
                return true;

            case "channeltype":
                String[] parts = value.split("[\n\r\t ,;]+");

                ArrayList<BenchmarkChannelType> channelTypes = new ArrayList<>();
                for (String part : parts) {
                    if (part.length() > 0) {
                        BenchmarkChannelType type = BenchmarkChannelType.getByKey(part);
                        channelTypes.add(type);
                    }
                }

                config.channelTypes = channelTypes;
                shell.confirm("Channel types: " + getChannelTypeNames(config.channelTypes));
                return true;


            default:
                return false;
        }
    }

    public void doSet() {
        System.out.println(pad("warmup:") + config.warmUpDurationSeconds);
        System.out.println(pad("duration:") + config.measurementDurationSeconds);
        System.out.println(pad("iterations:") + config.iterations);
        System.out.println(pad("rate:") + config.targetMessageRatePerSecond);
        System.out.println(pad("payloadsize:") + config.payloadSize);
        System.out.println(pad("channeltype:") + getChannelTypeNames(config.channelTypes));
    }

    private String getChannelTypeNames(List<BenchmarkChannelType> channelTypes) {
        return Joiner.on(", ").join(channelTypes.stream().map(BenchmarkChannelType::getName).collect(Collectors.toList()));
    }

    private String pad(String str) {
        return StringUtils.rightPad(str, 15);
    }

    private String pad(String str, int size) {
        return StringUtils.rightPad(str, size);
    }

    private String getPrintable(Object value) {
        return value != null ? value.toString() : "not set";
    }

}
