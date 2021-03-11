package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.ClassDataType;

/**
 *
 */
public class ThisRef {
    public final ClassDataType      type;

    public ThisRef (ClassDataType type) {
        this.type = type;
    }
}
