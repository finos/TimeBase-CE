package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;

/**
 *
 */
public class QBinaryValue extends QValue {
    private final JExpr             variable;
    
    public QBinaryValue (QBinaryType type, JExpr variable) {
        super (type);
        
        this.variable = variable;
    }

    @Override
    public JExpr        read () {
        return (variable);
    }

    @Override
    public JExpr        readIsNull (boolean eq) {
        JExpr   e = variable.call ("isNull");
        
        return (eq ? e : e.not ());
    }
    
    @Override
    public JStatement   write (JExpr arg) {
        return (variable.call ("set", arg).asStmt ());
    }        
}
