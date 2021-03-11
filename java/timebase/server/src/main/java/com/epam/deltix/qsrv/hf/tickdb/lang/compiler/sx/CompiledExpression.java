package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.qsrv.hf.pub.md.DataType;

/**
 *  Note: neither type nor name are included in the equality relationship.
 *  Name is irrelevant and type is derivative of other content.
 */
public abstract class CompiledExpression <T extends DataType> {
    public final T                              type;
    public String                               name;
    public boolean                              impliesAggregation = false;

    protected CompiledExpression (T type) {
        this.type = type;
    }

    public boolean              impliesAggregation () {
        return (impliesAggregation);
    }
    
    protected abstract void     print (StringBuilder out);

    @Override
    public String               toString () {
        StringBuilder               sb = new StringBuilder ();
        
        print (sb);
        
        if (name != null) {
            sb.append (" as ");
            sb.append (name);
        }
        
        return (sb.toString ());
    }

    @Override
    public boolean  equals (Object obj) {
        return this == obj || obj != null && getClass () == obj.getClass ();
    }

    @Override
    public int      hashCode () {
        return getClass ().hashCode ();
    }
}
