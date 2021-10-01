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
package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2FS;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.io.Home;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * @author Alexei Osipov
 */
public class ChunkCacheTestUtils {
    static final String azureTestTempFilePath = "/tmp/chunkedcache/test";

    static final String localTestTempFilePath = getLocalTempDirectory();

    private static String getLocalTempDirectory() {
        Path path;
        try {
            path = Files.createTempDirectory("dx-test-chunk-cache");
        } catch (IOException e) {
            throw new RuntimeException();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                FileUtils.deleteDirectory(path.toFile());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }));
        return path.toString();
    }

    static AbstractPath createTestFolderPath(AbstractFileSystem fs, AbstractFileSystem baseFs) {
        if (baseFs instanceof Azure2FS) {
            return fs.createPath(azureTestTempFilePath);
        } else if (baseFs instanceof LocalFS) {
            return fs.createPath(localTestTempFilePath);
        } else {
            throw new IllegalArgumentException("Unknown base FS");
        }
    }

    static void createTestFile(AbstractPath filePath, int testFileSizeInBytes) throws IOException {
        OutputStream out = filePath.openOutput(testFileSizeInBytes);
        BufferedOutputStream bout = new BufferedOutputStream(out);
        DataOutputStream outputStream = new DataOutputStream(bout);

        int fileSize = 0;
        int counter = 0;
        while (fileSize < testFileSizeInBytes) {
            counter ++;
            outputStream.writeInt(counter);
            fileSize += Integer.BYTES;
        }
        outputStream.flush();
        outputStream.close();
    }

    @Nonnull
    static AbstractPath getOrCreateTestFile(AbstractPath testFolderPath, int testFileSizeInBytes, String suffix, boolean requireNew, AbstractFileSystem baseFs) throws IOException {
        testFolderPath.makeFolderRecursive();
        String fileName = "test_file_" + suffix + "_" + testFileSizeInBytes + ".blob";
        AbstractPath path = testFolderPath.append(fileName);
        if (requireNew) {
            path.deleteIfExists();
        }
        if (!path.exists()) {
            System.out.println("Creating test file: " + fileName);
            // We create the file using baseFs, not the caching
            AbstractPath baseFsPath = baseFs.createPath(path.getPathString());
            ChunkCacheTestUtils.createTestFile(baseFsPath, testFileSizeInBytes);
            Assert.assertTrue(path.exists());
            System.out.println("Test file created");
        }
        return path;
    }

    @Nonnull
    static AbstractPath getOrCreateTestFile(AbstractPath testFolderPath, int testFileSizeInBytes, boolean requireNew, AbstractFileSystem baseFs) throws IOException {
        return getOrCreateTestFile(testFolderPath, testFileSizeInBytes, "", requireNew, baseFs);
    }

    @Nonnull
    static Azure2FS getAzure2FS() throws IOException {
        String pathToProperties = Home.getPath("sys.properties");

        Properties properties = new Properties();
        properties.load(new FileInputStream(pathToProperties));
        return Azure2FS.create(properties);
    }

    static void testFileReadByByteWithValidation(AbstractPath filePath, long expectedSize) throws IOException {
        long bytesRead = testFileReadByByteWithValidation(filePath);
        if (bytesRead != expectedSize) {
            Assert.assertEquals(expectedSize, bytesRead);
        }
    }

    /**
     * Reads test file content and validates data from file.
     */
    static long testFileReadByByteWithValidation(AbstractPath filePath) throws IOException {
        try (InputStream in = filePath.openInput(0)) {
            long totalBytesRead = 0;
            int counter = 0;

            while (true) {
                int ch1 = in.read();
                if (ch1 < 0) {
                    break;
                }
                int ch2 = in.read();
                int ch3 = in.read();
                int ch4 = in.read();
                if ((ch1 | ch2 | ch3 | ch4) < 0) {
                    throw new AssertionError("Partial ints are not expected");
                }
                //noinspection PointlessBitwiseExpression
                int readValue = (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0);
                counter++;
                if (counter != readValue) {
                    throw new AssertionError("Value mismatch");
                }
                totalBytesRead += 4;
            }

            return totalBytesRead;
        }
    }

    static void testFileReadUsingBufferWithoutValidation(AbstractPath filePath, long expectedSize) throws IOException {
        long bytesRead = testFileReadUsingBufferWithoutValidation(filePath);
        if (bytesRead != expectedSize) {
            Assert.assertEquals(expectedSize, bytesRead);
        }
    }

    /**
     * Reads file content into a buffer but not validates content.
     */
    static long testFileReadUsingBufferWithoutValidation(AbstractPath filePath) throws IOException {
        byte[] readBuffer = new byte[8 * 1024];
        InputStream inputStream = filePath.openInput(0);
        int bytesRead;
        long totalBytesRead = 0;
        while ((bytesRead = inputStream.read(readBuffer)) > 0) {
            totalBytesRead += bytesRead;
        }
        return totalBytesRead;
    }
}