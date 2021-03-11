package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.QQLCompiler;

/**
 *
 */
public class TimestampSelector extends CompiledExpression <DataType> {
    public TimestampSelector () {
        super (StandardTypes.CLEAN_TIMESTAMP);
        name = "$" + QQLCompiler.KEYWORD_TIMESTAMP;
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append (QQLCompiler.KEYWORD_TIMESTAMP);
    }   
}
