package com.epam.deltix.util.vsocket.util;


import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.*;
import java.util.Arrays;

import static com.epam.deltix.util.vsocket.util.TestServerSocketFactory.LatencyServerSocket.NUM_MESSAGES;
import static com.epam.deltix.util.vsocket.util.TestServerSocketFactory.LatencyServerSocket.NUM_PER_BURST;


public class SocketTestUtilities {
    public static int DEFAULT_PACKET_SIZE = 4096;
    public static int DEFAULT_PORT = 0;

    public static int parsePacketSize(String[] args) {
        return parsePacketSize(args, DEFAULT_PACKET_SIZE);
    }

    public static int parsePacketSize(String[] args, int defaultPacketSize) {
        int packetSize = defaultPacketSize;
        if (args.length >= 2)
            packetSize = Integer.parseInt(args[1]);

        return packetSize;
    }

    public static int parsePort(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length >= 1)
            port = Integer.parseInt(args[0]);

        return port;
    }

    public static void measureInputThroughput(InputStream os, byte[] buffer, int cycles) throws IOException {
        double avgPackagesPerSec = 0;
        double maxPackagesPerSec = 0;
        int reportedCounter = 0;
        int packages = 0;
        int packetSize = buffer.length;
        int lastReportedPackages = 0;

        System.out.printf("Measuring throughput by reading packages of size %,d\n", packetSize);
        System.out.println("Measurements will be taken approximately every second. They will be printed.");
        System.out.println("At the end of test, average throughput will be printed.");
        System.out.printf("Test will stop in %,d seconds\n", cycles);

        long lastReportTime = TimeKeeper.currentTime;
        long nextReportTime = lastReportTime + 1000;
        long stopTime = lastReportTime + cycles * 1000;

        for (;;) {
            int offset = 0;
            int bytesRead = 0;
            int size = packetSize;
            while (bytesRead != packetSize)
            {
                int read = os.read(buffer, offset, size);
                bytesRead += read;
                offset += read;
                size -= read;
            }

            packages++;

            long now = TimeKeeper.currentTime;
            if (now > nextReportTime) {
                long packagesSent = packages - lastReportedPackages;
                double secondsExpired = (now - lastReportTime) * 0.001;
                double packagesPerSec = packagesSent / secondsExpired;

                System.out.printf("%,d packages/s; %.2f Bytes/s; %.3f MB/s\n", (int) packagesPerSec, packagesPerSec * packetSize, (packagesPerSec * packetSize / (1024*1024)));
                avgPackagesPerSec = (packagesPerSec + avgPackagesPerSec * reportedCounter) / (++reportedCounter);
                maxPackagesPerSec = Math.max(packagesPerSec, maxPackagesPerSec);

                lastReportedPackages = packages;
                lastReportTime = TimeKeeper.currentTime;
                nextReportTime = now + 1000;

                if (lastReportTime > stopTime)
                {
                    break;
                }
            }
        }
        System.out.println("------------ Average ---------");
        System.out.printf("%,d packages/s; %.2f Bytes/s; %.3f MB/s\n", (int) avgPackagesPerSec, avgPackagesPerSec * packetSize, (avgPackagesPerSec * packetSize / (1024*1024)));

        System.out.println("------------ Max ---------");
        System.out.printf("%,d packages/s; %.2f Bytes/s; %.3f MB/s\n", (int) maxPackagesPerSec, maxPackagesPerSec * packetSize, (maxPackagesPerSec * packetSize / (1024*1024)));
    }

    public static void measureOutputThroughput(OutputStream os, byte[] buffer, int cycles) throws IOException {
        double avgPackagesPerSec = 0;
        double maxPackagesPerSec = 0;
        int reportedCounter = 0;
        int packages = 0;
        int packetSize = buffer.length;
        int lastReportedPackages = 0;

        System.out.printf("Measuring throughput by writing packages of size %,d\n", packetSize);
        System.out.println("Measurements will be taken approximately every second. They will be printed.");
        System.out.println("At the end of test, average throughput will be printed.");
        System.out.printf("Test will stop in %,d seconds\n", cycles);

        long lastReportTime = TimeKeeper.currentTime;
        long nextReportTime = lastReportTime + 1000;
        long stopTime = lastReportTime + cycles * 1000;

        for (;;) {
            os.write(buffer);
            packages++;

            long now = TimeKeeper.currentTime;
            if (now > nextReportTime) {
                long packagesSent = packages - lastReportedPackages;
                double secondsExpired = (now - lastReportTime) * 0.001;
                double packagesPerSec = packagesSent / secondsExpired;

                System.out.printf("%,d packages/s; %.2f Bytes/s; %.3f MB/s\n", (int) packagesPerSec, packagesPerSec * packetSize, (packagesPerSec * packetSize / (1024*1024)));
                avgPackagesPerSec = (packagesPerSec + avgPackagesPerSec * reportedCounter) / (++reportedCounter);
                maxPackagesPerSec = Math.max(packagesPerSec, maxPackagesPerSec);

                lastReportedPackages = packages;
                lastReportTime = TimeKeeper.currentTime;
                nextReportTime = now + 1000;

                if (lastReportTime > stopTime)
                {
                    break;
                }
            }
        }
        System.out.println("------------ Average ---------");
        System.out.printf("%,d packages/s; %.2f Bytes/s; %.3f MB/s\n", (int) avgPackagesPerSec, avgPackagesPerSec * packetSize, (avgPackagesPerSec * packetSize / (1024*1024)));

        System.out.println("------------ Max ---------");
        System.out.printf("%,d packages/s; %.2f Bytes/s; %.3f MB/s\n", (int) maxPackagesPerSec, maxPackagesPerSec * packetSize, (maxPackagesPerSec * packetSize / (1024*1024)));
    }

    public static void measureLatency(OutputStream os, DataInputStream is, byte[] buffer, int cycles, boolean measure) throws IOException, InterruptedException {
        int counter = 0;

        double minLatency = Long.MAX_VALUE;
        double maxLatency = 0;
        double avgLatency = 0;

        System.out.printf("Measuring latency by writing package of size %,d with time and waiting for the responce.", buffer.length);
        System.out.println("Measurements will be taken every time, after the responce is received.");
        System.out.println("Measurement resolution is defined by TimeKeeper resolution. ");
        System.out.println("This test can take some time.\n");

        for (int cycle = 0; cycle < cycles;) {
            double minCycleLatency = Long.MAX_VALUE;
            double maxCycleLatency = 0;
            double avgCycleLatency = 0;

            while (counter++ < NUM_MESSAGES) {
                for (int i = 0; i < NUM_PER_BURST; i++) {
                    DataExchangeUtils.writeLong(buffer, 0, System.nanoTime());

                    long start = measure ? System.nanoTime() : 0;

                    os.write(buffer);
                    os.flush();
                    long count = is.readLong();

                    if (measure) {
                        long latency = (System.nanoTime() - start) / 1000;

                        if (latency > 0) {
                            minCycleLatency = Math.min(minCycleLatency, latency);
                            maxCycleLatency = Math.max(maxCycleLatency, latency);

                            avgCycleLatency = (avgCycleLatency * (count) + latency) / (++count);
                        }
                    }

                    counter++;
                }

                os.flush();

                Thread.sleep(10);
            }

            counter = 0;

            if (measure) {
                System.out.printf("Intermediate latency: Max  %,.1f ns; Min  %,.1f ns; AVG %,.1f ns\n", maxCycleLatency * 1000, minCycleLatency * 1000, avgCycleLatency * 1000);
            }

            minLatency = Math.min(minCycleLatency, minLatency);
            maxLatency = Math.max(maxCycleLatency, maxLatency);

            avgLatency = (avgLatency * (cycle) + avgCycleLatency) / (++cycle);
        }

        System.out.println("Test final result latency:");
        System.out.printf("Latency: Max  %,.1f ns; Min  %,.1f ns; AVG %,.1f ns\n", maxLatency * 1000, minLatency * 1000, avgLatency * 1000);
    }

    public static void proccessLatencyRequests(DataOutputStream os, DataInputStream is, byte[] buffer, boolean measure) {
        int count = 0;
        int TOTAL = NUM_MESSAGES * NUM_PER_BURST;

        long[] results = new long[TOTAL];

        StringBuilder sb = new StringBuilder();

        try {
            System.out.println("Server: connection accepted.");

            for (; ; ) {

                is.readFully(buffer);
                os.writeLong(count);
                os.flush();

                long time = DataExchangeUtils.readLong(buffer, 0);
                results[count] = (System.nanoTime() - time) / 1000;

                //results[count] = latency = (System.nanoTime() - time) / 1000;

//                    if (latency > 0 && count > 0) {
//                        minLatency = Math.min(minLatency, latency);
//                        maxLatency = Math.max(maxLatency, latency);
//                        avgLatency = (avgLatency * (count - 1) + latency) / count;
//                    }
                //result[(int)count] = latency;

                count++;

                if (count % NUM_MESSAGES * NUM_PER_BURST == 0) {

                    results[0] = 0; // clear first result

                    Arrays.sort(results);

                    double avg = 0;
                    for (int i = 0; i < results.length; i++)
                        avg = (avg * (count - 1) + results[i]) / count;

                    long threshold = results[TOTAL / 100000 * 99999];

                    int index = results.length - 1;
                    while (index > 0 && results[index] > threshold)
                        index--;

                    if (measure) {
                        sb.append("Server latency report:-----------------\n");
                        sb.append("MIN: ").append(results[0]).append(" mks\n");
                        sb.append("MAX: ").append(results[TOTAL - 1]).append(" mks\n");
                        sb.append("AVG: ").append((long) avg).append(" mks\n");
                        sb.append("> ").append(threshold).append(" mks = ").append(TOTAL - index).append(" results ").append("(99.999%)\n");
                        sb.append("--------------------------------------\n");

                        synchronized (System.out) {
                            for (int i = 0; i < sb.length(); i++)
                                System.out.write(sb.charAt(i));
                        }
                    }

                    Arrays.fill(results, 0);
                    count = 0;
                }
            }
        } catch (EOFException eof) {
            // disconnect
        } catch (Throwable x) {
            x.printStackTrace();
        }
    }
}
