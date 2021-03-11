package com.epam.deltix.qsrv.hf.tickdb.lang.errors;

import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;

/**
 *  Encoding is incompatible with type.
 */
public class IllegalEncodingException extends CompilationException {
    public IllegalEncodingException (SimpleDataTypeSpec dts) {
        super (
            "Encoding " + dts.encoding + " is illegal in type " + dts.typeId, 
            dts
        );
    }    
}
