package com.epam.deltix.qsrv.hf.codec.cg;

import com.epam.deltix.util.jcg.JContext;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.util.jcg.scg.JavaSrcGenContext;

/**
 *
 */
public class QCGHelpers {

    public static final JContext       CTXT = new JavaSrcGenContext();
    
    public static JExpr        objtoex (Object obj) {
        if (obj instanceof JExpr)
            return ((JExpr) obj);

        if (obj instanceof Boolean)
            return (CTXT.booleanLiteral ((Boolean) obj));

        if (obj instanceof Integer)
            return (CTXT.intLiteral ((Integer) obj));

        if (obj instanceof Long)
            return (CTXT.longLiteral ((Long) obj));

        if (obj instanceof Short)
            return (CTXT.intLiteral ((Short) obj));

        if (obj instanceof Byte)
            return (CTXT.intLiteral ((Byte) obj));

        if (obj instanceof Double)
            return (CTXT.doubleLiteral ((Double) obj));
        
        if (obj instanceof Float)
            return (CTXT.floatLiteral ((Float) obj));

        return (CTXT.stringLiteral (obj.toString ()));
    }

    static JExpr []     objtoex (Object ... args) {
        int         n = args.length;
        JExpr []    ret = new JExpr [n];

        for (int ii = 0; ii < n; ii++)
            ret [ii] = objtoex (args [ii]);

        return (ret);
    }

    static JStatement   throwIAX (String msg) {
        return throwIAX(objtoex(msg));
    }

    static JStatement   throwIAX (JExpr msgExpr) {
        return (
            CTXT.newExpr(
                IllegalArgumentException.class,
                msgExpr
            ).throwStmt()
        );
    }

    static JStatement   throwISX (Object ... args) {
        return (
            CTXT.newExpr (
                IllegalStateException.class,
                CTXT.sum (objtoex (args))
            ).throwStmt ()
        );
    }
}
