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

import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.memory.*;
import com.epam.deltix.util.time.TimeConstants;

/**
 *<table border="" style="padding:3px;border-spacing:1px">
 *     <caption>Time is binary-encoded in the following ways, depending on scale</caption>
 *  <tr><th>Scale</th>              <th># Bytes</th>
 *          <th>Head Bits</th>          <th># Bits in Value</th>    <th>Range, Years</th></tr>
 *  <tr><td>s*10<sup>-6</sup></td>  <td>7</td>
 *          <td>000</td>                <td>53</td>                 <th>569</th></tr>
 *  <tr><td>s*10<sup>-3</sup></td>  <td>6</td>
 *          <td>001</td>                <td>45</td>                 <th>2225</th></tr>
 *  <tr><td>s</td>                  <td>5</td>
 *          <td>010</td>                <td>37</td>                 <th>8692</th></tr>
 *  <tr><td>s*10</td>               <td>4</td>
 *          <td>011</td>                <td>29</td>                 <th>339</th></tr>
 *  <tr><td>h</td>                  <td>3</td>
 *          <td>100</td>                <td>21</td>                 <th>477</th></tr>
 *</table>
 */
public class TimeCodec {

    public static final int     MAX_SIZE = 9;
    
    public static final long    BASE = -70L * 366L * 24L * 3600000L;
    public static final int     TIME_SCALE_INVALID =   0;
    public static final int     TIME_SCALE_MILLISECONDS =   1 << 5;
    public static final int     TIME_SCALE_SECONDS =        2 << 5;
    public static final int     TIME_SCALE_10_SECONDS =     3 << 5;
    public static final int     TIME_SCALE_HOURS =          4 << 5;
    public static final int     TIME_SCALE_SPECIAL =        5 << 5;
    public static final int     TIME_SCALE_NANOS =          6 << 5;
    public static final int     TIME_SCALE_LONG_NANOS =     7 << 5;

    // Size of time field with specific scale
    public static final int     TIME_SCALE_MILLISECONDS_FIELD_SIZE = 6;

    public static long          readTime (MemoryDataInput in) {
        byte        head = in.readByte();
        int         scale = head & 0xE0;

        return readTime(head, scale, in);
    }

    private static long          readTime (byte head, int scale, MemoryDataInput in) {
        long        ret = head & 0x1F;
        
        switch (scale) {
            case TIME_SCALE_INVALID:
                throw new RuntimeException ("invalid timestamp");

            case TIME_SCALE_LONG_NANOS:
                return readTimeInternalLongNanos(in, ret);

            case TIME_SCALE_NANOS:
                return readTimeInternalNanos(in, ret);

            case TIME_SCALE_MILLISECONDS:
                return readTimeInternalMilliseconds(in, ret);

            case TIME_SCALE_SECONDS:
                return readTimeInternalSeconds(in, ret);

            case TIME_SCALE_10_SECONDS:
                return readTimeInternal10Seconds(in, ret);

            case TIME_SCALE_HOURS:
                return readTimeInternalHours(in, ret);

            case TIME_SCALE_SPECIAL:
                switch ((byte) ret) {
                    case 0:     return (TimeConstants.TIMESTAMP_UNKNOWN);
                    default:
                        // Falls through to next "default" statement with UncheckedIOException
                }

            default:
                throw new com.epam.deltix.util.io.UncheckedIOException("Illegal time scale in byte: " + head);
        }
    }

    // This group of "readTimeInternal*" methods was part of "readTime" method (ABOVE) but was split out
    // to let JIT inline only hot cases instead of failing to inline anything because of too big method.

    private static long readTimeInternalLongNanos(MemoryDataInput in, long ret) {
        ret |= in.readUnsignedByte () << 5;
        ret |= in.readUnsignedByte () << 13;
        ret |= in.readUnsignedByte() << 21;
        ret |= in.readLongUnsignedByte () << 29;
        ret |= in.readLongUnsignedByte () << 37;
        ret |= in.readLongUnsignedByte () << 45;
        ret |= in.readLongUnsignedByte () << 53;
        ret |= in.readLongUnsignedByte () << 61;
        return (BASE + ret) / 1000000L;
    }

    private static long readTimeInternalNanos(MemoryDataInput in, long ret) {
        ret |= in.readUnsignedByte () << 5;
        ret |= in.readUnsignedByte () << 13;
        ret |= in.readUnsignedByte () << 21;
        ret |= in.readLongUnsignedByte () << 29;
        ret |= in.readLongUnsignedByte () << 37;
        ret |= in.readLongUnsignedByte () << 45;
        ret |= in.readLongUnsignedByte () << 53;
        return (BASE + ret) / 1000000L;
    }

    private static long readTimeInternalMilliseconds(MemoryDataInput in, long ret) {
        ret |= in.readUnsignedByte () << 5;
        ret |= in.readUnsignedByte () << 13;
        ret |= in.readUnsignedByte () << 21;
        ret |= in.readLongUnsignedByte () << 29;
        ret |= in.readLongUnsignedByte () << 37;
        return (BASE + ret);
    }

    private static long readTimeInternalSeconds(MemoryDataInput in, long ret) {
        ret |= in.readUnsignedByte () << 5;
        ret |= in.readUnsignedByte () << 13;
        ret |= in.readUnsignedByte () << 21;
        ret |= in.readLongUnsignedByte () << 29;
        return (BASE + ret * 1000);
    }

    private static long readTimeInternal10Seconds(MemoryDataInput in, long ret) {
        ret |= in.readUnsignedByte () << 5;
        ret |= in.readUnsignedByte () << 13;
        ret |= in.readUnsignedByte () << 21;
        return (BASE + ret * 10000);
    }

    private static long readTimeInternalHours(MemoryDataInput in, long ret) {
        ret |= in.readUnsignedByte () << 5;
        ret |= in.readUnsignedByte () << 13;
        return (BASE + ret * 3600000);
    }


    public static void          readTime (MemoryDataInput in, TimeStamp time) {
        byte    head = in.readByte();
        int     scale = head & 0xE0;

        if (scale == TIME_SCALE_NANOS) {
            long        ret = head & 0x1F;
            
            ret |= in.readUnsignedByte () << 5;
            ret |= in.readUnsignedByte () << 13;
            ret |= in.readUnsignedByte () << 21;
            ret |= in.readLongUnsignedByte () << 29;
            ret |= in.readLongUnsignedByte () << 37;
            ret |= in.readLongUnsignedByte () << 45;
            ret |= in.readLongUnsignedByte () << 53;

            time.setNanoTime(BASE + ret);
        }
        else if (scale == TIME_SCALE_LONG_NANOS) {
            long ret = head & 0x1F;

            ret |= in.readUnsignedByte () << 5;
            ret |= in.readUnsignedByte () << 13;
            ret |= in.readUnsignedByte () << 21;
            ret |= in.readLongUnsignedByte () << 29;
            ret |= in.readLongUnsignedByte () << 37;
            ret |= in.readLongUnsignedByte () << 45;
            ret |= in.readLongUnsignedByte () << 53;
            ret |= in.readLongUnsignedByte () << 61;

            time.setNanoTime(BASE + ret);
        }
        else {
            time.timestamp = readTime(head, scale, in);
            time.nanosComponent = 0;
        }
    }

    public static long         readNanoTime (MemoryDataInput in) {
        byte    head = in.readByte();
        int     scale = head & 0xE0;

        if (scale == TIME_SCALE_NANOS) {
            long ret = head & 0x1F;

            ret |= in.readUnsignedByte () << 5;
            ret |= in.readUnsignedByte () << 13;
            ret |= in.readUnsignedByte () << 21;
            ret |= in.readLongUnsignedByte () << 29;
            ret |= in.readLongUnsignedByte () << 37;
            ret |= in.readLongUnsignedByte () << 45;
            ret |= in.readLongUnsignedByte () << 53;

            return BASE + ret;
        } else if (scale == TIME_SCALE_LONG_NANOS) {
            long ret = head & 0x1F;

            ret |= in.readUnsignedByte () << 5;
            ret |= in.readUnsignedByte () << 13;
            ret |= in.readUnsignedByte () << 21;
            ret |= in.readLongUnsignedByte () << 29;
            ret |= in.readLongUnsignedByte () << 37;
            ret |= in.readLongUnsignedByte () << 45;
            ret |= in.readLongUnsignedByte () << 53;
            ret |= in.readLongUnsignedByte () << 61;

            return BASE + ret;
        } else {
            return TimeStamp.getNanoTime(readTime(head, scale, in));
        }
    }

    public static void          skipTime (MemoryDataInput in) {
        byte        head = in.readByte ();
        int         scale = head & 0xE0;

        switch (scale) {
            case TIME_SCALE_INVALID:
                throw new RuntimeException ("invalid timestamp");

            case TIME_SCALE_LONG_NANOS:
                in.skipBytes (8);
                break;

            case TIME_SCALE_NANOS:
                in.skipBytes (7);
                break;

            case TIME_SCALE_MILLISECONDS:
                in.skipBytes (5);
                break;

            case TIME_SCALE_SECONDS:
                in.skipBytes (4);
                break;

            case TIME_SCALE_10_SECONDS:
                in.skipBytes (3);
                break;

            case TIME_SCALE_HOURS:
                in.skipBytes (2);
                break;

            case TIME_SCALE_SPECIAL:
                int ret = head & 0x1F;
                switch (ret) {
                    case 0:
                        return;

                    default:
                        throw new com.epam.deltix.util.io.UncheckedIOException("Illegal value in byte: " + head);
                }

            default:
                throw new com.epam.deltix.util.io.UncheckedIOException("Illegal time scale in byte: " + head);
        }
    }

//    public static long          readTime (DataInput in) throws IOException {
//        byte        head = in.readByte ();
//        long        ret = head & 0x1F;
//        int         scale = head & 0xE0;
//
//        switch (scale) {
//            case TIME_SCALE_MICROSECONDS:
//                throw new UnsupportedOperationException ("microsecond scale");
//
//            case TIME_SCALE_MILLISECONDS:
//                ret |= in.readUnsignedByte () << 5;
//                ret |= in.readUnsignedByte () << 13;
//                ret |= in.readUnsignedByte () << 21;
//                ret |= ((long) in.readUnsignedByte ()) << 29;
//                ret |= ((long) in.readUnsignedByte ()) << 37;
//                return (BASE + ret);
//
//            case TIME_SCALE_SECONDS:
//                ret |= in.readUnsignedByte () << 5;
//                ret |= in.readUnsignedByte () << 13;
//                ret |= in.readUnsignedByte () << 21;
//                ret |= ((long) in.readUnsignedByte ()) << 29;
//                return (BASE + ret * 1000);
//
//            case TIME_SCALE_10_SECONDS:
//                ret |= in.readUnsignedByte () << 5;
//                ret |= in.readUnsignedByte () << 13;
//                ret |= in.readUnsignedByte () << 21;
//                return (BASE + ret * 10000);
//
//            case TIME_SCALE_HOURS:
//                ret |= in.readUnsignedByte () << 5;
//                ret |= in.readUnsignedByte () << 13;
//                return (BASE + ret * 3600000);
//
//            case TIME_SCALE_SPECIAL:
//                switch ((byte) ret) {
//                    case 0:     return (TimeStampedMessage.TIMESTAMP_UNKNOWN);
//                    default:    throw new com.epam.deltix.util.io.UncheckedIOException("Illegal value in byte: " + head);
//                }
//
//            default:
//                throw new com.epam.deltix.util.io.UncheckedIOException("Illegal time scale in byte: " + head);
//        }
//    }

    public static int           getFieldSize(long nanoSeconds) {
        if (nanoSeconds == TimeConstants.TIMESTAMP_UNKNOWN)
            return 1;

        int nanosComponent = (int) (nanoSeconds % TimeStamp.NANOS_PER_MS);
        long ms = (nanoSeconds - nanosComponent) / TimeStamp.NANOS_PER_MS;
        
        return getFieldSize(ms, nanosComponent);
    }

    public static int           getFieldSize(long milliseconds, int nanosComponent) {
        long t = milliseconds;
        
        if (t == TimeConstants.TIMESTAMP_UNKNOWN)
            return 1;

        t -= BASE;

        if (t < 0)
            throw new IllegalArgumentException ("Time too small: " + t);

        if (nanosComponent != 0) {
            return (t >>> 61 != 0) ? 9 : 8;
        }

        // Binary decision tree for determining the scale
        if (t % 10000 == 0)
            if (t % 3600000 == 0) {
                int     value = (int) (t / 3600000);

                if (value >>> 21 != 0)
                    throw new IllegalArgumentException ("Time too big: " + t);

                return 3;
            }
            else {
                int     value = (int) (t / 10000);

                if (value >>> 29 != 0)
                    throw new IllegalArgumentException ("Time too big: " + t);

                return 4;
            }
        else
            if (t % 1000 == 0) {
                long     value = t / 1000;

                if (value >>> 37 != 0)
                    throw new IllegalArgumentException ("Time too big: " + t);

                return 5;
            }
            else {
                if (t >>> 45 != 0)
                    throw new IllegalArgumentException ("Time too big: " + t);

                return 6;
            }
    }

    /**
     * @param forceLongNotation is set then long notation will be used even if it is not necessary
     */
    private static void          writeNanos(long nanos, MemoryDataOutput out, boolean forceLongNotation) {
        if (nanos == TimeConstants.TIMESTAMP_UNKNOWN) {
            assert !forceLongNotation;
            out.writeByte (TIME_SCALE_SPECIAL);
        } else {
            nanos -= BASE;

            if (nanos < 0)
                throw new IllegalArgumentException ("Time too small: " + nanos);

            boolean longNotation = (nanos >>> 61 > 0) || forceLongNotation;
            if (longNotation)
                out.writeByte (TIME_SCALE_LONG_NANOS | (nanos & 0x1F));
            else
                out.writeByte (TIME_SCALE_NANOS | (nanos & 0x1F));

            out.writeByte (nanos >>> 5);
            out.writeByte (nanos >>> 13);
            out.writeByte (nanos >>> 21);
            out.writeByte (nanos >>> 29);
            out.writeByte (nanos >>> 37);
            out.writeByte (nanos >>> 45);
            out.writeByte (nanos >>> 53);
            if (longNotation)
                out.writeByte (nanos >>> 61);
        }
    }

    public static void          writeNanoTime (long nanos, MemoryDataOutput out) {
        long nanosComponenet = nanos % TimeStamp.NANOS_PER_MS;
        if (nanosComponenet != 0)
            writeNanos(nanos, out, false);
        else
            writeTime((nanos - nanosComponenet) / TimeStamp.NANOS_PER_MS, out);
    }

    /**
     * Writes timestamp without value compression.
     * Similar to {@link #writeNanoTime} but will always use TIME_SCALE_LONG_NANOS scale so field size is always has {@link #MAX_SIZE} size.
     */
    public static void           writeNanoTimeNoScale(long nanos, MemoryDataOutput out) {
        if (nanos == TimeConstants.TIMESTAMP_UNKNOWN) {
            throw new IllegalArgumentException("Unknown timestamp can't be encoded with maximum number of bytes");
        }
        writeNanos(nanos, out, true);
    }

    public static void          writeTime (MessageInfo msg, MemoryDataOutput out) {
        if (msg.hasNanoTime())
            writeNanos(msg.getNanoTime(), out, false);
        else
            writeTime(msg.getTimeStampMs(), out);
    }

    /**
     *  Write time in milliseconds
     *  @param t milliseconds
     *  @param out output
     */

    public static void          writeTime (long t, MemoryDataOutput out) {
        if (t == TimeConstants.TIMESTAMP_UNKNOWN) {
            out.writeByte (TIME_SCALE_SPECIAL);
            return;
        }

        t -= BASE;

        if (t < 0)
            throw new IllegalArgumentException ("Time too small: " + t);

        long value;
        byte size;
        int scale;
        // Binary decision tree for determining the scale
        boolean timeTooBig = false;
        if (t % 10000 == 0) {
            if (t % 3600000 == 0) {
                value = (t / 3600000);

                if (value >>> 21 != 0)
                    timeTooBig = true;

                scale = TIME_SCALE_HOURS;
                size = 3;
            } else {
                value = (t / 10000);

                if (value >>> 29 != 0)
                    timeTooBig = true;

                scale = TIME_SCALE_10_SECONDS;
                size = 4;
            }
        } else {
            if (t % 1000 == 0) {
                value = t / 1000;

                if (value >>> 37 != 0)
                    timeTooBig = true;

                scale = TIME_SCALE_SECONDS;
                size = 5;
            } else {
                value = t;
                if (t >>> 45 != 0)
                    timeTooBig = true;

                scale = TIME_SCALE_MILLISECONDS;
                size = 6;
            }
        }
        if (timeTooBig) {
            throw new IllegalArgumentException("Time too big: " + t);
        }
        out.makeRoom(size);
        out.writeByteUnsafe(scale | (value & 0x1F));
        out.writeByteUnsafe(value >>> 5);
        out.writeByteUnsafe(value >>> 13);
        byte shift = 21;
        switch (size) {
            case 6:
                out.writeByteUnsafe(value >>> shift);
                shift += 8;
            case 5:
                out.writeByteUnsafe(value >>> shift);
                shift += 8;
            case 4:
                out.writeByteUnsafe(value >>> shift);
        }
    }

//    public static void          writeTime (long t, DataOutput out)
//        throws IOException
//    {
//        if (t == TimeStampedMessage.TIMESTAMP_UNKNOWN) {
//            out.writeByte (TIME_SCALE_SPECIAL | 0);
//            return;
//        }
//
//        t -= BASE;
//
//        if (t < 0)
//            throw new IllegalArgumentException ("Time too small: " + t);
//
//        // Binary decision tree for determining the scale
//        if (t % 10000 == 0)
//            if (t % 3600000 == 0) {
//                int     value = (int) (t / 3600000);
//
//                if (value >>> 21 != 0)
//                    throw new IllegalArgumentException ("Time too big: " + t);
//
//                out.writeByte (TIME_SCALE_HOURS | (value & 0x1F));
//                out.writeByte (value >>> 5);
//                out.writeByte (value >>> 13);
//            }
//            else {
//                int     value = (int) (t / 10000);
//
//                if (value >>> 29 != 0)
//                    throw new IllegalArgumentException ("Time too big: " + t);
//
//                out.writeByte (TIME_SCALE_10_SECONDS | (value & 0x1F));
//                out.writeByte (value >>> 5);
//                out.writeByte (value >>> 13);
//                out.writeByte (value >>> 21);
//            }
//        else
//            if (t % 1000 == 0) {
//                long     value = t / 1000;
//
//                if (value >>> 37 != 0)
//                    throw new IllegalArgumentException ("Time too big: " + t);
//
//                out.writeByte ((byte) (TIME_SCALE_SECONDS | (value & 0x1F)));
//                out.writeByte ((byte) (value >>> 5));
//                out.writeByte ((byte) (value >>> 13));
//                out.writeByte ((byte) (value >>> 21));
//                out.writeByte ((byte) (value >>> 29));
//            }
//            else {
//                if (t >>> 45 != 0)
//                    throw new IllegalArgumentException ("Time too big: " + t);
//
//                out.writeByte ((byte) (TIME_SCALE_MILLISECONDS | (t & 0x1F)));
//                out.writeByte ((byte) (t >>> 5));
//                out.writeByte ((byte) (t >>> 13));
//                out.writeByte ((byte) (t >>> 21));
//                out.writeByte ((byte) (t >>> 29));
//                out.writeByte ((byte) (t >>> 37));
//            }
//    }

//    public static void          writeTime (long t, ByteBuffer out) {
//        if (t == TimeStampedMessage.TIMESTAMP_UNKNOWN) {
//            out.put ((byte) (TIME_SCALE_SPECIAL | 0));
//            return;
//        }
//
//        t -= BASE;
//
//        assert t >= 0 : "Time too small: " + t;
//
//        // Binary decision tree for determining the scale
//        if (t % 10000 == 0)
//            if (t % 3600000 == 0) {
//                int     value = (int) (t / 3600000);
//
//                assert value >>> 21 == 0 : "Time too big: " + t;
//
//                out.put ((byte) (TIME_SCALE_HOURS | (value & 0x1F)));
//                out.put ((byte) (value >>> 5));
//                out.put ((byte) (value >>> 13));
//            }
//            else {
//                int     value = (int) (t / 10000);
//
//                assert value >>> 29 == 0 : "Time too big: " + t;
//
//                out.put ((byte) (TIME_SCALE_10_SECONDS | (value & 0x1F)));
//                out.put ((byte) (value >>> 5));
//                out.put ((byte) (value >>> 13));
//                out.put ((byte) (value >>> 21));
//            }
//        else
//            if (t % 1000 == 0) {
//                long     value = t / 1000;
//
//                assert value >>> 37 == 0 : "Time too big: " + t;
//
//                out.put ((byte) (TIME_SCALE_SECONDS | (value & 0x1F)));
//
//                out.put ((byte) (value >>> 5));
//                out.put ((byte) (value >>> 13));
//                out.put ((byte) (value >>> 21));
//                out.put ((byte) (value >>> 29));
//            }
//            else {
//                assert t >>> 45 == 0  : "Time too big: " + t;
//
//                out.put ((byte) (TIME_SCALE_MILLISECONDS | (t & 0x1F)));
//
//                out.put ((byte) (t >>> 5));
//                out.put ((byte) (t >>> 13));
//                out.put ((byte) (t >>> 21));
//                out.put ((byte) (t >>> 29));
//                out.put ((byte) (t >>> 37));
//            }
//    }
}