package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *
 */
public class QVarcharValue extends QValue {
    private final JExpr             variable;
    
    public QVarcharValue (QVarcharType type, JExpr variable) {
        super (type);
        
        this.variable = variable;
    }

    @Override
    public JExpr        read () {
        return (variable.call ("get"));
    }

    @Override
    public JStatement   write (JExpr arg) {
        return (variable.call ("set", arg).asStmt ());
    }  
    
    public JStatement   decode (JExpr input, int nSizeBits, int length) {
        return (
            variable.call (
                "readAlphanumeric",                        
                input, 
                CTXT.intLiteral (nSizeBits),
                CTXT.intLiteral (length)                        
            ).asStmt ()
        );
    }
}
