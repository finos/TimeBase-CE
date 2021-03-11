package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.values.*;
import com.epam.deltix.util.jcg.JContext;
import com.epam.deltix.util.jcg.JExpr;
import com.epam.deltix.util.jcg.JStatement;
import com.epam.deltix.util.jcg.scg.JavaSrcGenContext;


/**
 *
 */
public class QCGHelpers {
    public static final boolean        DEBUG_DUMP_CODE =
        Boolean.getBoolean ("deltix.qql.dump");
       
    public static final JContext       CTXT =  new JavaSrcGenContext();
    
    static JStatement   throwNVX () {
        return (CTXT.staticVarRef (NullValueException.class, "INSTANCE").throwStmt ());
    }

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

    public static Class<?>        primitiveWrapper (Class<?> clazz) {

        if (clazz.equals(boolean.class))
            return Boolean.class;
        else if (clazz.equals(char.class))
            return Character.class;
        else if (clazz.equals(byte.class))
            return Byte.class;
        else if (clazz.equals(Short.class))
            return Short.class;
        else if (clazz.equals(int.class))
            return Integer.class;
        else if (clazz.equals(float.class))
            return Float.class;
        else if (clazz.equals(double.class))
            return Double.class;
        else if (clazz.equals(long.class))
            return Long.class;

        return Object.class;
    }

    static JExpr []     objtoex (Object ... args) {
        int         n = args.length;
        JExpr []    ret = new JExpr [n];

        for (int ii = 0; ii < n; ii++)
            ret [ii] = objtoex (args [ii]);

        return (ret);
    }

    static JStatement   throwRX (Object ... args) {
        return (
            CTXT.newExpr (
                RuntimeException.class,
                CTXT.sum (objtoex (args))
            ).throwStmt ()
        );
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
    
    public static Class <? extends ValueBean>   getValueBeanClass (DataType type) {
        if (type instanceof IntegerDataType)
            return (IntegerValueBean.class);

        if (type instanceof FloatDataType)
            return (((FloatDataType) type).isFloat () ? FloatValueBean.class : DoubleValueBean.class);

        if (type instanceof BooleanDataType)
            return (BooleanValueBean.class);

        if (type instanceof VarcharDataType)
            return (StringValueBean.class);

        throw new UnsupportedOperationException (type.getBaseName ());
    }
}
