package com.epam.deltix.qsrv.hf.tickdb.lang.pub;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledExpression;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;

/**
 *
 */
public interface QuantQueryCompiler {
    CompiledExpression  compile (Expression e, DataType expectedType);
    
    PreparedQuery       compileStatement (Statement s);
}
