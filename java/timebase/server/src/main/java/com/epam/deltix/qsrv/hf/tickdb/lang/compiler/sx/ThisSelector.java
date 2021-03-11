package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class ThisSelector extends CompiledExpression <ClassDataType> {
    public ThisSelector (ClassDataType type) {
        super (type);
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append ("this {");
        out.append (type);
        out.append ("}");
    }   
}
