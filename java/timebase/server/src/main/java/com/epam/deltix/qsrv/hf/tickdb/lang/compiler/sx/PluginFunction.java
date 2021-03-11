package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.FunctionDescriptor;

/**
 *
 */
public class PluginFunction extends CompiledComplexExpression {
    public final FunctionDescriptor fd;

    public PluginFunction (FunctionDescriptor fd, CompiledExpression ... args) {
        super (fd.returnType, args);
        this.fd = fd;
        this.name = toString ();
    }

    @Override
    public boolean                  impliesAggregation () {
        return (fd.aggregate || super.impliesAggregation ());
    }
    
    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean                  equals (Object obj) {
        return super.equals (obj) && fd.equals (((PluginFunction) obj).fd);
    }

    @Override
    public int                      hashCode () {
        return super.hashCode () + fd.hashCode ();
    }

    @Override
    protected void                  print (StringBuilder out) {
        out.append (fd.info.id ());
        out.append (" (");
        printArgs (out);
        out.append (")");
    }
}
