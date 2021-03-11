package com.epam.deltix.qsrv.hf.pub.codec;

/**
 *
 */
public interface NonStaticFieldInfo extends DataFieldInfo {
    public int              getOrdinal ();

    public boolean          isPrimaryKey ();

    public boolean          isBound();
}
