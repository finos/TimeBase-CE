package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.ParamRef;

/**
 *
 */
public class ParamAccess extends CompiledExpression <DataType> {
    public final ParamRef           ref;
    
    public ParamAccess (ParamRef param) {
        super (param.signature.type);
        this.name = param.signature.name;
        this.ref = param;
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append ("[");
        out.append (ref);
        out.append ("]");
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return (
            super.equals (obj) &&
            ref.signature.equals (((ParamAccess) obj).ref.signature) &&
            ref.index == ((ParamAccess) obj).ref.index
        );
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () * 39 + ref.signature.hashCode () + ref.index;
    }
}
