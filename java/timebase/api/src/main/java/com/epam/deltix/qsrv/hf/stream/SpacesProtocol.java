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
package com.epam.deltix.qsrv.hf.stream;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 *
 */
public class SpacesProtocol {

    public static boolean isSpacesDeflatedMessageFile(File file) {
        return file.getName().toLowerCase().endsWith(".zip");
    }

    public static MessageFileHeader readHeader(File file) throws IOException {
        if (isSpacesDeflatedMessageFile(file)) {
            try (ZipFile zipFile = new ZipFile(file)) {
                return readHeader(zipFile);
            }
        } else {
            return Protocol.readHeader(file);
        }
    }

    public static long getStartTime(File file) throws IOException {
        if (isSpacesDeflatedMessageFile(file)) {
            return 0;
        } else {
            return Protocol.getStartTime(file.getAbsolutePath());
        }
    }

    public static ConsumableMessageSource<InstrumentMessage> openRawReader(ZipFile zipFile, ZipEntry zipEntry)
        throws IOException
    {
        return new MessageReader2(
            zipFile.getInputStream(zipEntry),
            zipEntry.getSize(),
            true,
            1 << 20,
            null
        );
    }

    public static MessageFileHeader readHeader(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(zipFile.getInputStream(zipEntry))) {
            return MessageReader2.readHeader(inputStream);
        }
    }

    private static MessageFileHeader readHeader(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.stream().filter(e -> e.getName().toLowerCase().endsWith(".qsmsg.gz"))
            .findFirst().orElse(null);
        if (entry != null) {
            return readHeader(zipFile, entry);
        } else {
            throw new IOException("File " + zipFile.getName() + " should contain .qsmsg.gz files");
        }
    }

    private static byte readVersion(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        try (GZIPInputStream inputStream = new GZIPInputStream(zipFile.getInputStream(zipEntry))) {
            return MessageReader2.readVersion(inputStream);
        }
    }

}