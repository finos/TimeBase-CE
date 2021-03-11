package com.epam.deltix.qsrv.dtb.store.codecs;

/**
 *
 */
public class TSFFormat {
    public static final int     NUM_ENTS_MASK =       0xFFFFFF;
    public static final int     COMPRESSED_FLAG =   0x80000000;
    public static final int     ALGORITHM_FLAG =    0x70000000;

    public final static short   INDEX_FORMAT_VERSION = 2;
    public final static short   FILE_FORMAT_VERSION = 3;

    public static byte          getAlgorithmCode(int flags) {
        return (byte)((flags & ALGORITHM_FLAG) >> 28);
    }

    public static int          setAlgorithmCode(int flags, int code) {
        return flags | COMPRESSED_FLAG | (code << 28);
    }
}
