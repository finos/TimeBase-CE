package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

/**
 *
 */
public abstract class TimeBaseServerRegistry {
    private static final IntegerToObjectHashMap <DXTickDB>   registry = 
        new IntegerToObjectHashMap <DXTickDB> ();
    
    public static synchronized void      registerServer (int port, DXTickDB instance) {
        if (registry.containsKey (port))
            throw new IllegalArgumentException (
                "TB server on port " + port + " is already registered"
            );
        
        registry.put (port, instance);
    }
    
    public static synchronized void      unregisterServer (int port) {
        if (registry.remove (port, null) == null)
            throw new IllegalArgumentException (
                "TB server on port " + port + " is not registered"
            );        
    }
    
    public static synchronized DXTickDB     getDirectConnection (int port) {
        DXTickDB      impl = registry.get (port, null);
        
        if (impl == null)
            return (null);
        
        return (new DirectTickDBClient (impl));
    }

    public static synchronized DXTickDB     getServer(int port) {
        return registry.get (port, null);
    }

    public static synchronized void clear() {
        registry.clear();
    }
}
