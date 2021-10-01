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
package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.memory.*;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;

import java.io.*;
import java.nio.ByteBuffer;

/**
 */
public class TimeIntervalCodec {
    public static final int     BAR_N_SECOND =              0;
    public static final int     BAR_N_MINUTE =              1 << 6;
    public static final int     BAR_N_HOUR =                2 << 6;
    public static final int     BAR_OTHER =                 3 << 6;

    public static int           read (MemoryDataInput in) {
        byte        head = in.readByte ();
        int         ret = head & 0x3F;
        int         scale = head & 0xC0;

        switch (scale) {
            case BAR_N_SECOND:  return (ret * 1000);
            case BAR_N_MINUTE:  return (ret * 60000);
            case BAR_N_HOUR:    return (ret * 3600000);
            case BAR_OTHER:     return (in.readInt ());

            default:
                throw new com.epam.deltix.util.io.UncheckedIOException("Illegal time scale in byte: " + head);
        }
    }

    public static int           read (DataInput in) throws IOException {
        byte        head = in.readByte ();
        if (head == (byte) IntegerDataType.PINTERVAL_NULL)
            return IntegerDataType.PINTERVAL_NULL;

        int         ret = head & 0x3F;
        int         scale = head & 0xC0;

        switch (scale) {
            case BAR_N_SECOND:  return (ret * 1000);
            case BAR_N_MINUTE:  return (ret * 60000);
            case BAR_N_HOUR:    return (ret * 3600000);
            case BAR_OTHER:     return (in.readInt ());

            default:
                throw new com.epam.deltix.util.io.UncheckedIOException("Illegal time scale in byte: " + head);
        }
    }

    public static void          write (long t, MemoryDataOutput out) {
        assert t >= 0 : t;
        if (t == IntegerDataType.PINTERVAL_NULL) {
            out.writeByte((byte) IntegerDataType.PINTERVAL_NULL);
            return;
        }
        if (t > Integer.MAX_VALUE)
            throw new UnsupportedOperationException("Interval geater then 2147483647 is not supported " + t);

        // Binary decision tree for determining the scale
        int             scale = -1;
        int             value = 0;
        
        if (t % 60000 == 0)
            if (t % 3600000 == 0) {
                value = ((int)t) / 3600000;
                scale = BAR_N_HOUR;
            }
            else {
                value = ((int)t) / 60000;
                scale = BAR_N_MINUTE;
            }
        else if (t % 1000 == 0) {
            value = ((int)t) / 1000;
            scale = BAR_N_SECOND;
        }

        if (value != 0 && value < 64)
            out.writeByte (scale | value);
        else {
            out.writeByte (BAR_OTHER);
            out.writeInt ((int)t);
        }
    }

    public static void          write (int t, ByteBuffer out) {
        assert t >= 0 : t;
        if (t == IntegerDataType.PINTERVAL_NULL) {
            out.put((byte) IntegerDataType.PINTERVAL_NULL);
            return;
        }

        // Binary decision tree for determining the scale
        int             scale = -1;
        int             value = 0;
        
        if (t % 60000 == 0)
            if (t % 3600000 == 0) {
                value = t / 3600000;
                scale = BAR_N_HOUR;
            }
            else {
                value = t / 60000;
                scale = BAR_N_MINUTE;
            }
        else if (t % 1000 == 0) {
            value = t / 1000;
            scale = BAR_N_SECOND;
        }

        if (value != 0 && value < 64)
            out.put ((byte) (scale | value));
        else {
            out.put ((byte) BAR_OTHER);
            out.putInt (t);
        }
    }
}