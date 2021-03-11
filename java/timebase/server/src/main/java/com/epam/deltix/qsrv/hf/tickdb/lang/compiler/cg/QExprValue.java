package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;

/**
 *
 */
public class QExprValue extends QValue {
    private final JExpr             variable;
    
    public QExprValue (QType type, JExpr variable) {
        super (type);
        
        this.variable = variable;
    }

    @Override
    public JExpr        read () {
        return (variable);
    }      
}
