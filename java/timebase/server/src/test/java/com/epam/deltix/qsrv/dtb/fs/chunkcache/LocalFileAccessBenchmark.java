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

import com.epam.deltix.qsrv.dtb.fs.cache.CachingFileSystem;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * Simple benchmark to test performance of {@link ChunkCachingFileSystem} and {@link CachingFileSystem}
 * at sequential reading of local files.
 *
 * @author Alexei Osipov
 */
@State(Scope.Thread)
@Fork(3)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 10, time = 3)
public class LocalFileAccessBenchmark {

    @Param({"none", "old", "new"})
    String cacheType;

    long cacheSizeInBytes = 2 * 1024L * 1024 * 1024; // 2 Gb
    int maxFileSizeInBytes =  256 * 1024 * 1024; // 245Mb
    //int maxFileSizeInBytes =  Integer.MAX_VALUE / 2;
    //int testFileSizeInBytes =  1 * 1024 * 1024 * 1024 + 12; // 177 Mb + 12 bytes


    static final int testFileSizeInBytes1 =  12;
    static final int testFileSizeInBytes2 =  128;
    static final int testFileSizeInBytes3 =  1024;
    static final int testFileSizeInBytes4 =  8 * 1024 ;
    static final int testFileSizeInBytes5 =  1024 * 1024;
    static final int testFileSizeInBytes6 =  4 * 1024 * 1024;
    static final int testFileSizeInBytes7 =  16 * 1024 * 1024;
    static final int testFileSizeInBytes8 =  64 * 1024 * 1024;
    static final int testFileSizeInBytes9 =  128 * 1024 * 1024;
    static final int testFileSizeInBytes10 =  256 * 1024 * 1024;

    /*
    static final int testFileSizeInBytes11 =  177 * 1024 * 1024 + 12; // 177 Mb + 12 bytes
    */

    @Param({
            testFileSizeInBytes1 + "",
            testFileSizeInBytes2 + "",
            testFileSizeInBytes3 + "",
            testFileSizeInBytes4 + "",
            testFileSizeInBytes5 + "",
            testFileSizeInBytes6 + "",
            testFileSizeInBytes7 + "",
            testFileSizeInBytes8 + "",
            testFileSizeInBytes9 + "",
            testFileSizeInBytes10 + "",
    })
    int testFileSizeInBytes;

    AbstractFileSystem fs;
    AbstractPath filePath;

    @Setup(Level.Invocation)
    public void setup() throws IOException {
        AbstractFileSystem baseFs = new LocalFS();
        switch (cacheType) {
            case "none":
                fs = baseFs;
                break;
            case "old":
                fs = new CachingFileSystem(baseFs, 1, maxFileSizeInBytes);
                break;
            case "new":
                fs = new ChunkCachingFileSystem(baseFs, cacheSizeInBytes, maxFileSizeInBytes, 1);
                break;
        }
        AbstractPath testFolderPath = ChunkCacheTestUtils.createTestFolderPath(fs, baseFs);
        this.filePath = ChunkCacheTestUtils.getOrCreateTestFile(testFolderPath, testFileSizeInBytes, false, baseFs);
    }


    @Benchmark
    public long readEntireFile() throws IOException {
        return readWholeFile(filePath);
    }

    private long readWholeFile(AbstractPath filePath) throws IOException {
        byte[] readBuffer = new byte[8 * 1024];
        //long t0 = System.currentTimeMillis();
        InputStream inputStream = filePath.openInput(0);
        int bytesRead;
        long totalBytesRead = 0;
        while ((bytesRead = inputStream.read(readBuffer)) > 0) {
            totalBytesRead += bytesRead;
        }

        //long t1 = System.currentTimeMillis();
        //System.out.println("Total time: " + (t1 - t0) + " ms");
        //System.out.println("Read speed: " + (totalBytesRead * 1000 / 1024 / 1024 / (t1 - t0)));
        inputStream.close();
        return totalBytesRead;
    }



    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(LocalFileAccessBenchmark.class.getSimpleName())
                .shouldFailOnError(true)
                .shouldDoGC(true)
                //.result("ReadLongBenchmark.json")
                .resultFormat(ResultFormatType.JSON)
                .build();

        new Runner(opt).run();
        //Main.main(args);
    }
}