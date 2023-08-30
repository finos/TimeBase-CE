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
package com.epam.deltix.qsrv.dtb.fs.azure;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.Core;
import com.microsoft.azure.datalake.store.OperationResponse;
import com.microsoft.azure.datalake.store.RequestOptions;
import com.microsoft.azure.datalake.store.oauth2.AzureADAuthenticator;
import com.microsoft.azure.datalake.store.oauth2.AzureADToken;
import com.microsoft.azure.datalake.store.retrypolicies.ExponentialBackoffPolicy;
import com.epam.deltix.util.io.IOUtil;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Tests how latency and block reading time depends on offset in file and block size.
 *
 * @author Alexei Osipov
 */
public class LatencyTest {
    private static PrintStream fileOut;
    private static final PrintStream systemOut = System.out;

    static {
        try {
            fileOut = new PrintStream(new FileOutputStream("LatencyTest.log"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public LatencyTest() throws FileNotFoundException {
    }

    private static void println(String x) {
        systemOut.println(x);
        fileOut.println(x);
        fileOut.flush();
    }
/*
    private static String account = "deltixlake";
    private static String accountFqdn = "deltixlake.azuredatalakestore.net";

    private static String clientId = "4f5db66f-0f42-4233-9477-df58b08b7e44";

    private static String clientKey = "G8KZhnhwfUWu/B8Zxwey18jvBgqpisihzoVq+gftl90=";
*/


/*

    private static String account = "deltixlake";
    private static String accountFqdn = "deltixlake.azuredatalakestore.net";*/






    //private static String accountFqdn = "aosipovdeltixlab.onmicrosoft.com"; // domain

    private static String accountFqdn = "deltixlake.azuredatalakestore.net"; // Datalake account
    private static String subscriptionId = "";

    private static String clientId = "4f5db66f-0f42-4233-9477-df58b08b7e44";

    private static String clientKey = "G8KZhnhwfUWu/B8Zxwey18jvBgqpisihzoVq+gftl90=";
    private static String authTokenEndpoint = "https://login.microsoftonline.com/f87ea646-b858-4dd3-aedc-bee803a4bdf7/oauth2/token";

    public static void main(String[] args) throws IOException {

        AzureADToken token = AzureADAuthenticator.getTokenUsingClientCreds(authTokenEndpoint, clientId, clientKey);

        //UserPasswordTokenProvider tokenProvider = new UserPasswordTokenProvider(clientId, account, clientKey);

        //Azure azure = Azure.authenticate(new File("my.azureauth")).withDefaultSubscription();


        //ClientCredsTokenProvider tokenProvider = new ClientCredsTokenProvider("deltixlake.azuredatalakestore.net", clientId, clientKey);
        // adl://deltixldperftest.azuredatalakestore.net/testRoot/ticks/data


        ADLStoreClient client = ADLStoreClient.createClient(accountFqdn, token);
        //InputStream in = client.getReadStream("/1116594581/CLFront/symbols.dat");



        String filePath = "/8886594581/ticks/u00c8/z0000.dat";
        int fileSize = 45_196_768;


        List<Integer> blockSizes = Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16 * 1024);
        int blocSizeCount = blockSizes.size();
        int maxOffset = fileSize - blockSizes.get(blocSizeCount -1) * 1024;
        List<Integer> offsets = new ArrayList<>();
        offsets.addAll(Arrays.asList(0, 4*1024*1024, 16*1024*1024, maxOffset));
        Random random = new Random(0);
        for (int i = 0; i < 6; i++) {
            int randomOffset = random.nextInt(maxOffset);
            offsets.add(randomOffset);
        }
        Collections.sort(offsets);

        Multimap<SampleKey, SampleValue> map = HashMultimap.create();
        println("Starting measurements");
        println("==========================================");
        for (int i = 0; i < blocSizeCount; i++) {
            int blockSize = blockSizes.get(i) * 1024;

            for (int j = 0; j < offsets.size(); j++) {
                int offset = offsets.get(j);
                for (int k = 0; k < 3; k++) {
                    testOne(map, filePath, client, offset, blockSize, true);
                    testOne(map, filePath, client, offset, blockSize, false);
                }
            }
        }

        println("==========================================");
        println("Printing results");
        StringBuilder header = new StringBuilder("");
        for (int i = 0; i < blocSizeCount * 4; i++) {
            header.append("\t");
            header.append(blockSizes.get(i / 4));
            header.append(" ");
            if (i % 4 == 0 || i % 4 == 1 ) {
                header.append("lat ");
            } else {
                header.append("read ");
            }
            if (i % 4 == 0 || i % 4 == 2 ) {
                header.append("no");
            } else {
                header.append("set");
            }
        }
        println(header.toString());


        for (int j = 0; j < offsets.size(); j++) {
            int offset = offsets.get(j);
            StringBuilder line = new StringBuilder();
            line.append(offset);

            for (int i = 0; i < blocSizeCount * 4; i++) {
                int blockSize = blockSizes.get(i / 4) * 1024;

                boolean setLen = i % 4 == 1 || i % 4 == 3;
                boolean lat = i % 4 == 0 || i % 4 == 1;
                Collection<SampleValue> samples = map.get(new SampleKey(setLen, offset, blockSize));

                int sum = 0;
                for (SampleValue sample : samples) {
                    sum += lat ? sample.latency : sample.readTime;
                }

                int avg = sum / samples.size();
                line.append("\t");
                line.append(avg);
            }
            println(line.toString());
        }

    }

    private static byte[] tempBuffer = new byte[16 * 1024 * 1024];

    private static void testOne(Multimap<SampleKey, SampleValue> map, String filePath, ADLStoreClient client, int offset, int blockSize, boolean setLen) throws IOException {
        RequestOptions opts = new RequestOptions();
        opts.retryPolicy = new ExponentialBackoffPolicy();
        OperationResponse resp = new OperationResponse();
        long t0 = System.currentTimeMillis();
        InputStream in = Core.open(filePath, offset, setLen ? blockSize : 0, null, client, opts, resp);
        long t1 = System.currentTimeMillis();
        IOUtil.readFully(in, tempBuffer, 0, blockSize);
        long t2 = System.currentTimeMillis();
        long latency = t1 - t0;
        long readTime = t2 - t1;
        in.close();

        println("Sample:\t" + (setLen ? "yes" : " no") + "\t" + blockSize + "\t" + offset + "\t" + latency + "\t" + readTime);
        map.put(new SampleKey(setLen, offset, blockSize), new SampleValue(latency, readTime));
    }

    private static class SampleKey {
        final boolean setLen;
        final int offset;
        final int blockSize;

        private SampleKey(boolean setLen, int offset, int blockSize) {
            this.setLen = setLen;
            this.offset = offset;
            this.blockSize = blockSize;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            SampleKey sampleKey = (SampleKey) o;

            if (setLen != sampleKey.setLen) return false;
            if (offset != sampleKey.offset) return false;
            return blockSize == sampleKey.blockSize;
        }

        @Override
        public int hashCode() {
            int result = (setLen ? 1 : 0);
            result = 31 * result + offset;
            result = 31 * result + blockSize;
            return result;
        }
    }

    private static class SampleValue {
        final long latency;
        final long readTime;

        private SampleValue(long latency, long readTime) {
            this.latency = latency;
            this.readTime = readTime;
        }
    }
}