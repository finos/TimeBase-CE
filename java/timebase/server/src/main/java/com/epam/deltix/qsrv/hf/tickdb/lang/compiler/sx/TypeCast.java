package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class TypeCast extends CompiledComplexExpression {
    public final ClassDataType           targetType;

    public TypeCast (CompiledExpression arg, ClassDataType targetType) {
        super (targetType, arg);
        this.targetType = targetType;
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) && targetType.equals (((TypeCast) obj).targetType);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + targetType.hashCode ();
    }

    @Override
    protected void                      print (StringBuilder out) {
        out.append ("cast (");
        printArgs (out);
        out.append (" as ");
        out.append (targetType.getFixedDescriptor ().getName ());
        out.append (")");
    }
}
