package com.epam.deltix.qsrv.hf.pub.md;

/**
 *  Used in {@link MetaData#getClassDescriptors}.
 */
public class ClassDescriptorSearchOptions {
    public static final int         INCLUDE_ABSTRACT_RECORD =   1 << 0;
    public static final int         INCLUDE_CONCRETE_RECORD =   1 << 1;
    public static final int         INCLUDE_ENUM =              1 << 2;
}
