package com.epam.deltix.snmp.mibc;

import com.epam.deltix.snmp.mibc.errors.*;
import com.epam.deltix.util.parsers.Location;
import java.util.*;

/**
 *
 */
class Env <T extends CompiledEntity> {
    private Map <String, T>        env = 
        new HashMap <String, T> ();
    
    public final Collection <T>         values () {
        return (env.values ());
    }
    
    public final void                   register (T e) {
        register (Location.NONE, e);
    }
    
    public final void                   register (long location, T e) {
        String          id = e.getId ();
        
        if (env.containsKey (id))
            throw new DuplicateIdException (location, id);
        
        env.put (id, e);
    }
    
    public final T                      resolve (String id) {
        return (resolve (Location.NONE, id));
    }
    
    public final T                      resolve (long location, String id) {
        T      e = env.get (id);
        
        if (e == null)
            throw new UnresolvedIdException (location, id);
        
        return (e);
    }        
}
