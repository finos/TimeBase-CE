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
package com.epam.deltix.util.collections.latency;

import com.epam.deltix.util.io.aeron.DXAeron;
import com.epam.deltix.util.io.*;
import com.epam.deltix.util.io.offheap.OffHeap;
import com.epam.deltix.util.vsocket.TransportType;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSClient;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class VSLatencyTestClient implements TestSettings {

    public VSLatencyTestClient() throws IOException {
        if (USE_VSOCKET) {
            runWithVSocket();
        } else if (TRANSPORT_TYPE == TransportType.OFFHEAP_IPC) {
            runWithOffHeap();
        } else if (TRANSPORT_TYPE == TransportType.AERON_IPC) {
            runWithAeron();
        } else {
            runWithSocket();
        }
    }

    private void                runWithVSocket() throws IOException {
        VSClient client = new VSClient("localhost", PORT);
        client.setNumTransportChannels(3);
        client.connect();
        VSChannel channel = client.openChannel();

        DataInputStream is = new DataInputStream(channel.getInputStream());
        DataOutputStream os = new DataOutputStreamEx(channel.getOutputStream());

        bench(is, os);

        is.close();
        os.close();

        channel.close();
        client.close();
    }

    private void            runWithOffHeap() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setTcpNoDelay(true);
        RandomAccessFile serverFile = new RandomAccessFile(SERVER_FILE, "rw");
        RandomAccessFile clientFile = new RandomAccessFile(CLIENT_FILE, "rw");

        InputStream rin = OffHeap.createInputStream(clientFile);
        OutputStream rout = OffHeap.createOutputStream(serverFile);

        DataInputStream is = new DataInputStream(rin);
        DataOutputStream os = new DataOutputStream(rout);

        bench(is, os);

        rin.close();
        rout.close();
        is.close();
        os.close();
        s.close();
    }

    private void            runWithAeron() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setTcpNoDelay(true);

        InputStream in = DXAeron.createInputStream(43);
        OutputStream out = DXAeron.createOutputStream(42);

        DataInputStream is = new DataInputStream(in);
        DataOutputStream os = new DataOutputStream(out);

        bench(is, os);

        in.close();
        out.close();
        is.close();
        os.close();
        s.close();
    }

    private void            runWithSocket() throws IOException {
        Socket s = new Socket("localhost", PORT);
        s.setTcpNoDelay(true);
        DataInputStream is = new DataInputStream(s.getInputStream());
        DataOutputStream os = new DataOutputStream(s.getOutputStream());

        bench(is, os);

        is.close();
        os.close();
        s.close();
    }

    public void                 bench(DataInputStream is, DataOutputStream os) throws IOException {
        doRun(is, os, "Warming up", WARMUP);
        long[] nanos = doRun(is, os, "Running test", REPS);
        dumpResults(nanos);
    }

    private long[]                    doRun(DataInputStream is, DataOutputStream os, String msg, int reps) throws IOException {
        long[] results = new long[REPS];

        final long waitInterval = TimeUnit.SECONDS.toNanos(1) / THROUGHPUT;

        System.out.printf("%-15s: %10d reps...",
            msg,
            reps);

        int messages = 0;
        long nextNanoTime = System.nanoTime() + waitInterval;
        long time = System.currentTimeMillis();
        while (messages < reps) {
            //wait a bit not to load system
            if (waitInterval != 0) {
                if (System.nanoTime() < nextNanoTime)
                    continue; // spin-wait
            }

            //do test
            long nanos = iter(is, os);
            if (messages < results.length)
                results[messages % reps] = nanos;

            nextNanoTime += waitInterval;
            ++messages;
        }
        time = System.currentTimeMillis() - time;

        System.out.printf(" done in %dms\n", time);

        return results;
    }

    public long                     iter(DataInputStream is, DataOutputStream os) throws IOException {
        os.writeLong(System.nanoTime());
        os.flush();
        return System.nanoTime() - is.readLong();
    }

    //---results
    private static final double[] PTILES =
        { 0, 1, 10, 50, 99, 99.9, 99.99, 99.999, 99.9999 };

    private void                    dumpResults(long[] nanos) throws IOException {
        long[] sorted = nanos.clone();
        Arrays.sort(sorted);

        for (double pc : PTILES) {
            logPctile(pc, sorted, 0.001, "us");
        }
    }

    private void                    logPctile(double pc, long[] sorted, double factor, String unit) {
        int index = (int) (pc / 100 * sorted.length);
        System.out.printf("%-12s  (%7d) : %8.2f (%s)\n",
            pc + "%",
            sorted.length - index,
            sorted[index] * factor,
            unit);
    }

    public static void main(String[] args) throws Throwable {

        final TransportType transportType;
        if (args.length > 0 && args[0].equals("-ipc")) {
            transportType = TransportType.OFFHEAP_IPC;
        } else if (args.length > 0 && args[0].equals("-aeron")) {
            transportType = TransportType.AERON_IPC;
        } else {
            transportType = TransportType.SOCKET_TCP;
        }

        Thread th;
        if (LAUNCH_SERVER_THREAD) {
            th = new Thread() {
                @Override
                public void run() {
                    try {
                        new VSLatencyTestServer(transportType);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            th.start();
        }

        new VSLatencyTestClient();

        if (LAUNCH_SERVER_THREAD) {
            th.join();
        }
    }
}