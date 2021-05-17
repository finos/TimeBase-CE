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
package com.epam.deltix.qsrv.dtb.fs.azure2;

import com.google.common.collect.ImmutableList;
import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.ExpiryOption;
import com.microsoft.azure.datalake.store.IfExists;
import org.junit.Test;

import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Checks how some Azure FS operations work.
 *
 * @author Alexei Osipov
 */
public class Azure2FSTest {
    String clientId = "4f5db66f-0f42-4233-9477-df58b08b7e44";
    String secret = "G8KZhnhwfUWu/B8Zxwey18jvBgqpisihzoVq+gftl90=";
    String fullAccount = "deltixlake.azuredatalakestore.net";
    String authTokenEndpoint = "https://login.microsoftonline.com/f87ea646-b858-4dd3-aedc-bee803a4bdf7/oauth2/token";

    @Test
    public void testExpire() throws Exception {

        Azure2FS fs = Azure2FS.create(clientId, secret, fullAccount, authTokenEndpoint, 1024L * 8, 10);
        //runTestOnFs(fs);

        ADLStoreClient client = fs.getAzureClient();
        client.createDirectory("/tmp/aosipov/exipiretest");
        String filePath = "/tmp/aosipov/exipiretest/myfile.txt";
        client.createFile(filePath, IfExists.FAIL);
        client.setExpiryTime(filePath, ExpiryOption.RelativeToNow, 5000);
        System.out.println("1: " + client.checkExists(filePath));
        Thread.sleep(6000);
        System.out.println("2: " + client.checkExists(filePath));
    }

    @Test
    public void testConcat() throws Exception {

        Azure2FS fs = Azure2FS.create(clientId, secret, fullAccount, authTokenEndpoint, 1024L * 8, 10);
        //runTestOnFs(fs);

        ADLStoreClient client = fs.getAzureClient();
        client.deleteRecursive("/tmp/aosipov/concattest");
        client.createDirectory("/tmp/aosipov/concattest");
        String filePath1 = "/tmp/aosipov/concattest/myfile1.txt";
        String filePath2 = "/tmp/aosipov/concattest/myfile2.txt";
        //String filePath3 = "/tmp/aosipov/concattest/myfile3.txt";
        String filePathResult1 = "/tmp/aosipov/concattest/result1.txt";
        //String filePathResult2 = "/tmp/aosipov/concattest/result2.txt";
        try (OutputStream os = client.createFile(filePath1, IfExists.FAIL)) {
            os.write(1);
        }
        try (OutputStream os = client.createFile(filePath2, IfExists.FAIL)) {
            os.write(2);
        }
        try {
            client.concatenateFiles(filePathResult1, ImmutableList.of(filePath1, filePath2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testConcatConcurrent() throws Exception {
        String clientId = "4f5db66f-0f42-4233-9477-df58b08b7e44";
        String secret = "G8KZhnhwfUWu/B8Zxwey18jvBgqpisihzoVq+gftl90=";
        String fullAccount = "deltixlake.azuredatalakestore.net";
        String authTokenEndpoint = "https://login.microsoftonline.com/f87ea646-b858-4dd3-aedc-bee803a4bdf7/oauth2/token";

        Azure2FS fs0 = Azure2FS.create(clientId, secret, fullAccount, authTokenEndpoint, 1024L * 8, 10);
        Azure2FS fs1 = Azure2FS.create(clientId, secret, fullAccount, authTokenEndpoint, 1024L * 8, 10);
        Azure2FS fs2 = Azure2FS.create(clientId, secret, fullAccount, authTokenEndpoint, 1024L * 8, 10);
        //runTestOnFs(fs);

        ADLStoreClient client0 = fs0.getAzureClient();
        ADLStoreClient client1 = fs1.getAzureClient();
        ADLStoreClient client2 = fs2.getAzureClient();
        client0.deleteRecursive("/tmp/aosipov/concattest");
        client0.createDirectory("/tmp/aosipov/concattest");

        String filePath1 = "/tmp/aosipov/concattest/myfile1.txt";
        String filePath2 = "/tmp/aosipov/concattest/myfile2.txt";
        String filePathResult1 = "/tmp/aosipov/concattest/result1.txt";
        String filePathResult2 = "/tmp/aosipov/concattest/result2.txt";
        try (OutputStream os = client0.createFile(filePath1, IfExists.FAIL)) {
            os.write(1);
        }
        try (OutputStream os = client0.createFile(filePath2, IfExists.FAIL)) {
            os.write(2);
        }

        // Init clients
        client1.delete(filePathResult1);
        client2.delete(filePathResult2);

        AtomicBoolean r1 = new AtomicBoolean(false);
        AtomicBoolean r2 = new AtomicBoolean(false);

        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client1.concatenateFiles(filePathResult1, ImmutableList.of(filePath1, filePath2));
                    r1.set(true);
                    System.out.println("1 success");
                } catch (Exception e) {
                    System.out.println("1 failed");
                    e.printStackTrace();
                }
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client2.concatenateFiles(filePathResult2, ImmutableList.of(filePath1, filePath2));
                    r2.set(true);
                    System.out.println("2 success");
                } catch (Exception e) {
                    System.out.println("2 failed");
                    e.printStackTrace();
                }
            }
        });

        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();

        if (r1.get() == r2.get()) {
            throw new AssertionError("Both threads got same result");
        }

        //client.setExpiryTime(filePath1, ExpiryOption.RelativeToNow, 5000);
        //System.out.println("1: " + client.checkExists(filePath1));
        //Thread.sleep(6000);
        //System.out.println("2: " + client.checkExists(filePath1));
        System.out.println();
        System.out.println("==============");
        System.out.println();
    }


    @Test
    public void testConcatConcurrentIterations() throws Exception {
        // TODO: We can reuse client instances
        for (int i = 0; i < 20; i++) {
            testConcatConcurrent();
        }
    }
}