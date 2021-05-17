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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.IdGenerator;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.channels.FileLock;

/**
 * @author Alexei Osipov
 */
class FileBasedIdGenerator implements IdGenerator {

    private final RandomAccessFile counterFile;
    private final int upperBound;
    private int currentNextValue; // Contains next value to be used (so it's current value is not is a value that is already used


    @SuppressWarnings("unused")
    private final FileLock lock; // Holds lock till the JVM shutdown

    private FileBasedIdGenerator(int upperBound, int currentNextValue, RandomAccessFile counterFile, FileLock lock) {
        this.counterFile = counterFile;
        this.upperBound = upperBound;
        this.currentNextValue = currentNextValue;
        this.lock = lock;
    }

    /**
     * Creates an {@link IdGenerator} that uses provided range and local file storage
     */
    @Nonnull
    static IdGenerator createFileBasedIdGenerator(String idRange) {
        String[] parts = idRange.split(":", 2); // We use ":" to avoid conflict with minus sign
        Long lowLong;
        Long hiLong;
        try {
            lowLong = Long.parseLong(parts[0]);
            hiLong = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Unable to parse Aeron stream id range: " + e.getMessage());
        }
        if (lowLong > hiLong) {
            throw new IllegalArgumentException("Provided Aeron stream id range is invalid: lower range is higher than upper range");
        }
        if (lowLong < Integer.MIN_VALUE || hiLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Provided Aeron stream id range is out ot Integer value");
        }
        int low = lowLong.intValue();
        int hi = hiLong.intValue();

        String path = AeronWorkDirManager.getExplicitStreamIdCounterFile();
        File file = new File(path);
        boolean fileExists = file.exists();
        if (!fileExists) {
            // Ensure that parent directory exist
            File parentFile = file.getParentFile();
            if (!parentFile.exists()) {
                if (!parentFile.mkdir()) {
                    throw new RuntimeException("Failed to create folder " + parentFile.getPath());
                }
            }
        }

        RandomAccessFile raf;
        FileLock lock;
        int currentValue;

        try {
            raf = new RandomAccessFile(path, "rw");
            lock = raf.getChannel().tryLock();
            if (lock == null) {
                throw new RuntimeException("File \"" + path + "\" is locked by another process");
            }


            if (fileExists && raf.length() > 0) {
                currentValue = getCurrentValue(raf);
                if (currentValue < low || currentValue > hi) {
                    throw new IllegalStateException("Current stream id value " + currentValue + " is out of range " + low + ":" + hi);
                }
            } else {
                currentValue = low;
            }

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new FileBasedIdGenerator(hi, currentValue, raf, lock);
    }

    @Override
    public synchronized int nextId() {
        int result = this.currentNextValue;
        if (result >= upperBound) {
            throw new IllegalStateException("Reached upper bound for permitted Aeron stream id to be used. Change id range or manually reset id values to be used");
        }
        this.currentNextValue ++;
        writeValue(counterFile, this.currentNextValue);
        try {
            counterFile.getChannel().force(false);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return result;
    }

    private static int getCurrentValue(RandomAccessFile file) {
        try {
            file.seek(0);
            byte[] buf = new byte[11]; // Number of digits in Integer.MAX_VALUE and 1 char for minus sign
            int byteLen = file.read(buf);
            String strValue = new String(buf, 0, byteLen);
            return Integer.parseInt(strValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeValue(RandomAccessFile file, int value) {
        try {
            file.setLength(0);
            String strValue = Integer.toString(value);
            file.writeBytes(strValue);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
