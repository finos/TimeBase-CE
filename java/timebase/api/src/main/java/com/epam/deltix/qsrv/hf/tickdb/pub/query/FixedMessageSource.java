package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.ClassSet;

/**
 * Message source with fixed schema.
 */

public interface FixedMessageSource {
    /*
     *   Returns schema of the messages returning by this messages source.
    */
    ClassSet<ClassDescriptor> getSchema();
}
