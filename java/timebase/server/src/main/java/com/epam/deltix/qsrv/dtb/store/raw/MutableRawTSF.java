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
package com.epam.deltix.qsrv.dtb.store.raw;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.store.codecs.BlockCompressor;
import com.epam.deltix.qsrv.dtb.store.codecs.BlockCompressorFactory;
import com.epam.deltix.qsrv.dtb.store.codecs.TSFFormat;
import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.store.impl.FileUtils;
import com.epam.deltix.util.collections.generated.ByteArrayList;

import java.io.*;
import java.util.Properties;


public class MutableRawTSF extends RawTSF {

    private static final int FILE_HEADER_SIZE = 14;
    private static final int FILE_FORMAT = 3;

    private final AbstractPath propertyFile;

    public MutableRawTSF(AbstractPath properties) {
        this.propertyFile = properties;
    }

    public long resetStartTS() throws IOException {
        readIndex(new DiagListener());
        read(Long.MIN_VALUE, Long.MAX_VALUE, true);

        for (int ii = 0; ii < numEntities; ii++) {
            if (index[ii].getFirstTimestamp() == Long.MIN_VALUE)
                index[ii].setFirstTimestamp(actualStartTimestamp);
        }

        return actualStartTimestamp;
    }

    public void saveTo() throws IOException {
        int sizeOnDisk;

        AbstractPath tmp = path.getParentPath().append(TSNames.TMP_PREFIX + path.getName());
        int[] compLengths;
        int indexSize = computeIndexBlockSize(compressed, numEntities);
        ByteArrayList compressedData = new ByteArrayList();
        BlockCompressor compressor = null;
        if (compressed) {
            Properties props = FileUtils.readProperties(propertyFile);

            String compressionLevel = props.getProperty("compressionLevel");
            String compression = BlockCompressorFactory.getAlgorithm(algorithm).toString();
            compressor = BlockCompressorFactory.createCompressor(compression + "(" + compressionLevel + ")", compressedData);

            compressedData = compressor.getReusableBuffer();
            compressedData.setSize(0);
            compLengths = new int[numEntities];

            for (int ii = 0; ii < numEntities; ii++) {
                RawDataBlock db = index[ii];
                if (db.getDataLength() == 0)
                    compLengths[ii] = 0;
                else
                    compLengths[ii] =
                            compressor.deflate(
                                    db.getData(), 0, db.getDataLength(),
                                    compressedData
                            );
            }

            sizeOnDisk = indexSize + compressedData.size();
        } else {
            int blockOffset = numEntities * (compressed ? 28 : 24) + formatVersion > 3 ? 14 : 10;
            for (int ii = 0; ii < numEntities; ii++) {
                RawDataBlock db = index[ii];
                blockOffset += db.getDataLength();
            }
            sizeOnDisk = blockOffset;
            compLengths = null;
        }

        try (OutputStream os = new BufferedOutputStream(tmp.openOutput(sizeOnDisk))) {
            DataOutputStream dos = new DataOutputStream(os);

            dos.writeShort(TSFFormat.FILE_FORMAT_VERSION);
            dos.writeLong(version);

            int flags = numEntities;

            if (compressed)
                flags = TSFFormat.setAlgorithmCode(flags, compressor.code());

            dos.writeInt(flags);

            int offset = indexSize;

            for (int ii = 0; ii < numEntities; ii++) {
                RawDataBlock db = index[ii];

                dos.writeInt(db.getEntity());
                dos.writeInt(db.getDataLength());

                if (compressed)
                    dos.writeInt(compLengths[ii]);

                dos.writeLong(db.getFirstTimestamp());
                dos.writeLong(db.getLastTimestamp());

                offset += db.getDataLength();
            }

            if (compressed) {
                os.write(compressedData.getInternalBuffer(), 0, compressedData.size());
            } else {
                for (int ii = 0; ii < numEntities; ii++)
                    index[ii].store(os);
            }
        }

        path.deleteIfExists();
        tmp.renameTo(path.getName());
    }

    private int computeIndexBlockSize(
            boolean compressed,
            int numEntities
    ) {
        return (numEntities * (compressed ? 28 : 24) + FILE_HEADER_SIZE);
    }

}