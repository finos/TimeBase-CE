/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.io.BasicIOUtil;
import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestVServerSocketFactory;
import org.junit.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Random;

/**
 * Sends random data to an echo server and checks that correct data is sent back.
 */
@SuppressWarnings("SameParameterValue")
public class Test_VSocket_Correctness {

    public static final int WRITE_COUNT = 1_000_000;

    @Test
    public void testSocket() throws Throwable {
        Test_VSocket_Correctness.main(new String[0]);
    }

    public static void main(String[] args) throws Throwable {
        int port = SocketTestUtilities.parsePort(args);

        VSServer server = TestVServerSocketFactory.createBinaryEchoVServer(port);
        server.setDaemon(true);
        server.start();
        System.out.println("Server started on " + server.getLocalPort());

        try {
            client("localhost", server.getLocalPort());
        } finally {
            server.close();
        }
    }

    private static void client(String host, int port) {
        try (VSClient client = new VSClient(host, port)) {
            client.connect();

            assertEchoClientCorrectness(client, WRITE_COUNT);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    static void assertEchoClientCorrectness(VSClient client, int writeCount) {
        Random rngSrc = new Random(0);
        Random rngDst = new Random(0);

        // Fills data with values 1..127
        byte[] data = makeTestData();

        try (VSChannel channel = client.openChannel()) {
            DataOutputStream os = channel.getDataOutputStream();

            // Data writer thread
            new Thread(() -> {
                Thread.currentThread().setName("PRODUCER");
                sendData(rngSrc, os, data, writeCount);
            }).start();

            // Reader
            readData(channel, data, rngDst, writeCount);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void sendData(Random rngSrc, DataOutputStream os, byte[] data, int writeCount) {
        long totalSent = 0;
        try {
            for (int i = 0; i < writeCount; i++) {
                int size = generateNextSize(rngSrc);
                //rngSrc.nextBytes(data);
                os.write(data, 0, size);
                totalSent += size;
                os.write(Byte.MIN_VALUE);
                totalSent += 1;
            }
            os.flush();
            System.out.println("Producer finished to send data. Sent: " + totalSent + " bytes");
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    private static void readData(VSChannel channel, byte[] expected, Random rngDst, int readCount) throws IOException {
        long totalRead = 0;
        DataInputStream is = channel.getDataInputStream();
        byte[] actual = new byte[8 * 1024];
        for (int i = 0; i < readCount; i++) {
            int size = generateNextSize(rngDst);
            //rngDst.nextBytes(expected);
            BasicIOUtil.readFully(is, actual, 0, size);

            int mismatch = Arrays.mismatch(expected, 0, size, actual, 0, size);
            if (mismatch >= 0) {
                throw new AssertionError("Data mismatch at position " + (totalRead + mismatch));
            }
            totalRead += size;

            int single = is.read();
            if (single < 0) {
                throw new AssertionError("Unexpected end of stream");
            }
            if (single - 256 != Byte.MIN_VALUE) {
                throw new AssertionError("Unexpected value " + single + " at position " + totalRead);
            }
            totalRead += 1;
        }
        System.out.println("Consumer finished to read data. Read: " + totalRead + " bytes");
    }

    private static int generateNextSize(Random rngSrc) {
        return 10 + rngSrc.nextInt(1000);
    }

    private static byte[] makeTestData() {
        byte[] data = new byte[8 * 1024];
        byte val = 0;
        for (int ii = 0; ii < data.length; ii++) {
            val++;
            data[ii] = val;
            if (val == Byte.MAX_VALUE) {
                val = 0;
            }
        }
        return data;
    }
}
