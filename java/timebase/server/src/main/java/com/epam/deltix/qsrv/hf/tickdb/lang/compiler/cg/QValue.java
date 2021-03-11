package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.util.jcg.*;
import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.*;

/**
 *  Represents a value stored as a constant, variable or object field.
 */
public abstract class QValue {
    public final QType          type;
    
    protected QValue (QType type) {
        this.type = type;
    }

    /**
     *  Reads the value. The type of what is read is QType-dependent.
     */
    public abstract JExpr       read ();

    /**
     *  Writes the value. The type of the argument is QType-dependent.
     */
    public JStatement           write (JExpr arg) {
        return (read ().assign (arg));
    }

    /**
     *  Generates code to test for null.
     */
    public JExpr                readIsNull (boolean eq) {
        return (type.checkNull (read (), eq));
    }

    public JStatement           writeNull () {
        return (write (type.getNullLiteral ()));
    }
    
    public final void           encode (JExpr out, JCompoundStatement addTo) {
        type.encode (this, out, addTo);
    }
    
    public final JStatement     decode (JExpr in) {
        return (type.decode (in, this));
    }
       
    public JStatement           decodeRelative(
        JExpr                       input,
        QValue                      base
    )
    {
        JCompoundStatement  cs = CTXT.compStmt ();
        
        JStatement          dec = decode (input);
        
        cs.add (dec);
        
        cs.add (
            CTXT.ifStmt (
                base.readIsNull (false), 
                write (CTXT.binExpr (read (), "+", base.read ()))
            )
        );
        
        return (cs);
    }    
}
