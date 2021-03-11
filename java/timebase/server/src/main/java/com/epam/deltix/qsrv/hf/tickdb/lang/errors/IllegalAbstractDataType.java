package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.DataTypeSpec;
import com.epam.deltix.util.parsers.CompilationException;

/**
 *
 */
public class IllegalAbstractDataType extends CompilationException {
    public IllegalAbstractDataType(DataTypeSpec dts, RecordClassDescriptor descriptor) {
        super("Illegal abstract type: " + descriptor + ".", dts);
    }
}
