package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.*;

/**
 *
 */
public class TypeCheck extends CompiledComplexExpression {
    public final ClassDescriptor           checkType;

    public TypeCheck (CompiledExpression arg, ClassDescriptor targetType) {
        super (
            arg.type.isNullable () ?
                StandardTypes.NULLABLE_BOOLEAN : 
                StandardTypes.CLEAN_BOOLEAN, 
            arg
        );

        this.checkType = targetType;
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) && checkType.equals (((TypeCheck) obj).checkType);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + checkType.hashCode ();
    }

    @Override
    protected void                  print (StringBuilder out) {
        printArgs (out);
        out.append (" is ");
        out.append (checkType.getName ());
    }
}
