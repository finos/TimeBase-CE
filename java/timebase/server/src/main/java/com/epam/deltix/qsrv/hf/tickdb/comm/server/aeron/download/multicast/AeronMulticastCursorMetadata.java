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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.download.multicast;

import com.epam.deltix.util.lang.Util;

/**
 * @author Alexei Osipov
 */
class AeronMulticastCursorMetadata {
    private final GrowingData entityData = new GrowingData();
    private final GrowingData typeData = new GrowingData();

    public void addEntityData(byte[] buffer, int offset, int length) {
        entityData.addData(buffer, offset, length);
    }

    public void addTypeData(byte[] buffer, int offset, int length) {
        typeData.addData(buffer, offset, length);
    }

    public byte[] getEntityDataBuffer() {
        return entityData.data;
    }

    public int getEntityDataLength() {
        return entityData.dataLength;
    }

    public byte[] getTypeDataBuffer() {
        return typeData.data;
    }

    public int getTypeDataLength() {
        return typeData.dataLength;
    }

    private static final class GrowingData {
        private volatile int dataLength = 0;
        private volatile byte[] data = new byte[16];

        synchronized void addData(byte[] buffer, int offset, int length) {
            byte[] d;
            int newDataLength = dataLength + length;
            if (newDataLength > data.length) {
                // Expand array
                d = new byte[Util.doubleUntilAtLeast(data.length, newDataLength)];
                System.arraycopy(data, 0, d, 0, dataLength);
            } else {
                d = data;
            }
            // Append new data
            System.arraycopy(buffer, offset, d, dataLength, length);

            // Update data array first
            data = d;
            // Then update length. This way it's guarantied that if reader got new length then it's sure to get new data.
            dataLength = newDataLength;
        }
    }
}
