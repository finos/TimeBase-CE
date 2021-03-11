package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.qcache;

import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.*;
import java.util.Arrays;

/**
 *
 */
class PQKey {
    public final ParamSignature []      paramSignature;
    public final Element                select;

    public PQKey (Element select, ParamSignature [] paramSignature) {
        this.select = select;
        this.paramSignature = paramSignature;        
    }

    @Override
    @SuppressWarnings ("EqualsWhichDoesntCheckParameterClass")
    public boolean      equals (Object obj) {
        final PQKey other = (PQKey) obj;
        
        if (!Arrays.equals (this.paramSignature, other.paramSignature))
            return false;
        
        if (!this.select.equals (other.select)) 
            return false;
        
        return true;
    }

    @Override
    public int          hashCode () {
        return (4 * Arrays.hashCode (paramSignature) + select.hashCode ());
    }           
}

