package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx.CompiledExpression;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.*;
import com.epam.deltix.util.lang.*;

/**
 *
 */
public class PredicateCompiler {
    private final StdEnvironment            stdEnv = new StdEnvironment (null);
    private final RecordClassDescriptor []  inputTypes;
    
    public PredicateCompiler (RecordClassDescriptor ... inputTypes) {    
        this.inputTypes = inputTypes;
    }
    
    public MessagePredicate   compile (Expression e) {
        QQLExpressionCompiler   ecomp = 
            new QQLExpressionCompiler (stdEnv);
        
        ecomp.setUpClassSetEnv (inputTypes);
        
        CompiledExpression      ce = 
            ecomp.compile (e, StandardTypes.NULLABLE_BOOLEAN);
        
        PredicateGenerator      pg = new PredicateGenerator (inputTypes, ce);
        
        JavaCompilerHelper      helper = 
            new JavaCompilerHelper (MessagePredicate.class.getClassLoader ());
        
        return (pg.finish (helper));        
    }
}
