package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;

/**
 * Implements access to unpublic tickdb function
 */
public abstract class TDBHelper {

    public static void setMetaData(TickStreamImpl stream, RecordClassDescriptor... types) {
        RecordClassSet set = new RecordClassSet();
        set.addContentClasses(types);
        stream.setMetaData(stream.isPolymorphic(), set);
    }
}
