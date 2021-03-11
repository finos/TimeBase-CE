package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;

import java.util.HashMap;
import java.util.Map;

/**
 *  Allows loading pre-registered classes with arbitrary names, or with names
 *  determined by the {@link deltix.timebase.api.SchemaElement} annotation.
 */
public class MappingTypeLoader implements TypeLoader {
    protected final TypeLoader                parent;
    protected final Map <String, Class <?>>   map = new HashMap <> ();

    public MappingTypeLoader () {
        this (TypeLoaderImpl.DEFAULT_INSTANCE);
    }
    
    public MappingTypeLoader (TypeLoader parent) {
        this.parent = parent;
    }

    public synchronized void                bind (Class <?> cls) {
        map.put (ClassDescriptor.getClassNameWithAssembly (cls), cls);
    }
    
    public synchronized void                bind (String className, Class <?> cls) {
        map.put (className, cls);
    }
    
    public synchronized void                bind (ClassDescriptor cd, Class <?> cls) {
        map.put (cd.getName (), cls);
    }
    
    public synchronized Class <?>           load (ClassDescriptor cd) 
        throws ClassNotFoundException 
    {
        Class <?>       cls = map.get (cd.getName ());
        
        if (cls != null)
            return (cls);
        
        return (parent.load (cd));
    }

    public Map<String, Class<?>> getMap() {
        return map;
    }
}
