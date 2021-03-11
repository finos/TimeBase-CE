package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

/**
 *
 */
public class SimpleFunction extends CompiledComplexExpression {
    public final SimpleFunctionCode     code;

    public SimpleFunction (SimpleFunctionCode code, CompiledExpression ... args) {
        super (code.getOuputType (args), args);
        this.code = code;
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return
            super.equals (obj) && code == ((SimpleFunction) obj).code;
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + code.hashCode ();
    }

    @Override
    protected void                      print (StringBuilder out) {
        out.append (code);
        out.append (" (");
        printArgs (out);
        out.append (")");
    }
}
