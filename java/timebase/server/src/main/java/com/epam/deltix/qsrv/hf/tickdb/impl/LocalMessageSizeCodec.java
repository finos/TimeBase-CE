package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.ramdisk.FD;

import java.io.IOException;

/*
    Totally depends on MessageSizeCodec code. Changes should be synchronized!
 */

class LocalMessageSizeCodec {

    private final MessageSizeCodec instance = new MessageSizeCodec();

    public static long          write (long destOffset, int t, FD out) throws IOException {
        if (t < 0)
            throw new IllegalArgumentException (t + " is negative");
        else if (t < 0x80)
            out.write (destOffset++, (byte) t);
        else if (t < 0x4000) {
            out.write (destOffset++, (byte) (0x80 | t & 0x3F));
            out.write (destOffset++, (byte) (t >> 6));
        }
        else if (t < 0x400000) {
            out.write (destOffset++, (byte) (0xC0 | t & 0x3F));
            out.write (destOffset++, (byte) (t >> 6));
            out.write (destOffset++, (byte) (t >> 14));
        }
        else
            throw new IllegalArgumentException (t + " is too large");

        return (destOffset);
    }
}
