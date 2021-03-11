package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.*;
import java.util.*;
import org.mozilla.javascript.Scriptable;

/**
 *
 */
public final class SSRoot extends SSContainer <MORoot> {
    private final Map <MONode <?>, SSNode <?>>      namedScalars =
        new HashMap <MONode <?>, SSNode <?>> ();
        
    public SSRoot (MORoot mo) {
        super (null, mo);    
        
        namedScalars.put (mo, this);
        
        for (Map.Entry <String, MONode <?>> e : mo.scalars ())
            namedScalars.put (e.getValue (), forceWrap (e.getValue ()));
    }

    SSNode <?>                  forceWrap (MONode <?> mo) {
        if (mo instanceof MOContainer)
            return (new SSContainer <MOContainer> (this, (MOContainer) mo));
        
        if (mo instanceof MOPrimitive)
            return (new SSPrimitive (this, (MOPrimitive) mo));

        if (mo instanceof MOTable)
            return (new SSTable (this, (MOTable) mo));

        throw new UnsupportedOperationException (mo.toString ());
    }
    
    Object                      wrap (MONode <?> mo) {
        if (mo == null)
            return (NOT_FOUND);
        
        if (mo == this.mo)
            return (this);
        
        Object              cached = namedScalars.get (mo);
        
        if (cached != null)
            return (cached);
        
        return (forceWrap (mo));
    }

    public void                     exportAllNames (Scriptable scope) {
        for (Map.Entry <String, MONode <?>> e : mo.scalars ())
            scope.put (e.getKey (), scope, wrap (e.getValue ()));
    }       
}
