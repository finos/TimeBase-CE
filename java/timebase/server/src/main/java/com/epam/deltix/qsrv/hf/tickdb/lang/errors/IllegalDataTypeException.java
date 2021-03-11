package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.qsrv.hf.tickdb.lang.pub.DataTypeSpec;
import com.epam.deltix.util.parsers.CompilationException;

import java.util.Arrays;

/**
 *
 */
public class IllegalDataTypeException extends CompilationException {
    public IllegalDataTypeException(DataTypeSpec dts, Class<?> actual, Class<?>... expected) {
        super("Unexpected type of object. \n" +
              "Expected: " + Arrays.toString(expected) + "; Actual: " + actual + ".",
              dts);
    }
}
