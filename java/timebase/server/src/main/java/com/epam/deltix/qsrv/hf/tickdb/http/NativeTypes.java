package com.epam.deltix.qsrv.hf.tickdb.http;

/**
 *
 */
public enum NativeTypes {
    INT64(8),
    INT32(4),
    INT16(2),
    INT8(1),
    IEEE64(8),
    IEEE32(4),
    BOOL(1),
    CHAR(2),
    ENUM8(1),
    ENUM16(2),
    ENUM32(4),
    ENUM64(8),
    STRING(-1);

    private final short size;

    private NativeTypes(int size) {
        this.size = (byte) size;
    }

    short getSize() {
        return size;
    }
}
