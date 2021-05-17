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
package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.util.collections.ByteQueue;
import com.epam.deltix.util.collections.ByteContainer;
import com.epam.deltix.util.memory.*;

import java.io.*;

/**
 */
public final class MessageSizeCodec {
    public static final int     MIN_SIZE = 1;
    public static final int     MAX_SIZE = 3;

    public static int           read (byte [] bytes, int offset) {
        byte        head = bytes [offset];

        if ((head & 0x80) == 0)
            return (head);
        else if ((head & 0x40) == 0){
            int     ret = head & 0x3F;

            ret |= (bytes [offset + 1] & 0xFF) << 6;

            return (ret);
        }
        else {
            int     ret = head & 0x3F;

            ret |= (bytes [offset + 1] & 0xFF) << 6;
            ret |= (bytes [offset + 2] & 0xFF) << 14;

            return (ret);
        }
    }

    public static int           read (ByteQueue in) {
        byte        head = in.poll ();

        if ((head & 0x80) == 0)
            return (head);
        else if ((head & 0x40) == 0){
            int     ret = head & 0x3F;

            ret |= (in.poll () & 0xFF) << 6;

            return (ret);
        }
        else {
            int     ret = head & 0x3F;

            ret |= (in.poll () & 0xFF) << 6;
            ret |= (in.poll () & 0xFF) << 14;

            return (ret);
        }
    }

    public static int           readNoPoll (ByteContainer in, int offset) {
        byte        head = in.get (offset);

        if ((head & 0x80) == 0)
            return (head);
        else if ((head & 0x40) == 0){
            int     ret = head & 0x3F;

            ret |= (in.get (offset + 1) & 0xFF) << 6;

            return (ret);
        }
        else {
            int     ret = head & 0x3F;

            ret |= (in.get (offset + 1) & 0xFF) << 6;
            ret |= (in.get (offset + 2) & 0xFF) << 14;

            return (ret);
        }
    }

    public static int           read (MemoryDataInput in) {
        byte        head = in.readByte ();

        if ((head & 0x80) == 0)
            return (head);
        else if ((head & 0x40) == 0){
            int     ret = head & 0x3F;

            ret |= in.readUnsignedByte () << 6;
            
            return (ret);
        }
        else {
            int     ret = head & 0x3F;

            ret |= in.readUnsignedByte () << 6;
            ret |= in.readUnsignedByte () << 14;

            return (ret);
        }
    }

    public static int           read (InputStream in) throws IOException {
        int         head = in.read ();

        if (head < 0)
            return (-1);

        if ((head & 0x80) == 0)
            return (head);
        else if ((head & 0x40) == 0){
            int     b = in.read ();

            if (b < 0)
                return (-1);

            return (head & 0x3F | (b << 6));
        }
        else {
            int     b1 = in.read ();

            if (b1 < 0)
                return (-1);

            int     b2 = in.read ();

            if (b2 < 0)
                return (-1);

            return (head & 0x3F | (b1 << 6) | (b2 << 14));
        }
    }

    public static int           read (DataInput in) throws IOException {
        int         head = in.readUnsignedByte ();

        if ((head & 0x80) == 0)
            return (head);
        else if ((head & 0x40) == 0){
            int     b = in.readUnsignedByte ();

            if (b < 0)
                return (-1);

            return (head & 0x3F | (b << 6));
        }
        else {
            int     b1 = in.readUnsignedByte ();

            if (b1 < 0)
                return (-1);

            int     b2 = in.readUnsignedByte ();

            if (b2 < 0)
                return (-1);

            return (head & 0x3F | (b1 << 6) | (b2 << 14));
        }
    }

    public static void          write (int t, MemoryDataOutput out) {
        if (t < 0)
            throw new IllegalArgumentException (t + " is negative");
        else if (t < 0x80)
            out.writeUnsignedByte (t);
        else if (t < 0x4000) {
            out.writeUnsignedByte (0x80 | t & 0x3F);
            out.writeUnsignedByte (t >> 6);
        }
        else if (t < 0x400000) {
            out.writeUnsignedByte (0xC0 | t & 0x3F);
            out.writeUnsignedByte (t >> 6);
            out.writeUnsignedByte (t >> 14);
        }
        else
            throw new IllegalArgumentException (t + " is too large");        
    }

    public static void          write (int t, ByteQueue out) {
        if (t < 0)
            throw new IllegalArgumentException (t + " is negative");
        else if (t < 0x80)
            out.offer (t);
        else if (t < 0x4000) {
            out.offer (0x80 | t & 0x3F);
            out.offer (t >> 6);
        }
        else if (t < 0x400000) {
            out.offer (0xC0 | t & 0x3F);
            out.offer (t >> 6);
            out.offer (t >> 14);
        }
        else
            throw new IllegalArgumentException (t + " is too large");
    }

    public static int           write (int t, byte [] dest, int offset)
        throws IOException
    {
        if (t < 0)
            throw new IllegalArgumentException (t + " is negative");
        else if (t < 0x80)
            dest [offset++] = (byte) t;
        else if (t < 0x4000) {
            dest [offset++] = (byte) (0x80 | t & 0x3F);
            dest [offset++] = (byte) (t >> 6);
        }
        else if (t < 0x400000) {
            dest [offset++] = (byte) (0xC0 | t & 0x3F);
            dest [offset++] = (byte) (t >> 6);
            dest [offset++] = (byte) (t >> 14);
        }
        else
            throw new IllegalArgumentException (t + " is too large");

        return (offset);
    }

    public static void          write (int t, OutputStream out) throws IOException {
        if (t < 0)
            throw new IllegalArgumentException (t + " is negative");
        else if (t < 0x80)
            out.write (t);
        else if (t < 0x4000) {
            out.write (0x80 | t & 0x3F);
            out.write (t >> 6);
        }
        else if (t < 0x400000) {
            out.write (0xC0 | t & 0x3F);
            out.write (t >> 6);
            out.write (t >> 14);
        }
        else
            throw new IllegalArgumentException (t + " is too large");
    }

//    public static long          write (long destOffset, int t, FD out) throws IOException {
//        if (t < 0)
//            throw new IllegalArgumentException (t + " is negative");
//        else if (t < 0x80)
//            out.write (destOffset++, (byte) t);
//        else if (t < 0x4000) {
//            out.write (destOffset++, (byte) (0x80 | t & 0x3F));
//            out.write (destOffset++, (byte) (t >> 6));
//        }
//        else if (t < 0x400000) {
//            out.write (destOffset++, (byte) (0xC0 | t & 0x3F));
//            out.write (destOffset++, (byte) (t >> 6));
//            out.write (destOffset++, (byte) (t >> 14));
//        }
//        else
//            throw new IllegalArgumentException (t + " is too large");
//
//        return (destOffset);
//    }

    // Calculates number of bytes necessary to keep message size value
    public static int           fieldSize(int t) {
        assert t >= 0;
        
        if (t < 0x80)
            return 1;

        if (t < 0x4000) 
            return 2;

        if (t < 0x400000) 
            return 3;
        
        throw new IllegalArgumentException(t + " is too large");
    }
}
