package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.*;
import org.mozilla.javascript.Scriptable;

/**
 * 
 */
public class SSContainer <T extends MOContainer> extends SSNode <T> {    
    SSContainer (
        SSRoot                  root,
        T                       mo
    )
    {
        super (root, mo);        
    }
    
    @Override
    public Object       get (String name, Scriptable start) {
        assert start == this;
        
        return (root.wrap (mo.getNodeByName (name)));
    }

    @Override
    public Object       get (int id, Scriptable start) {
        assert start == this;
        
        return (root.wrap (mo.getDirectChildById (id)));
    }

    @Override
    public boolean      has (String name, Scriptable start) {
        assert start == this;
        
        return (mo.getNodeByName (name) != null);
    }

    @Override
    public boolean      has (int id, Scriptable start) {
        assert start == this;
        
        return (mo.getDirectChildById (id) != null);
    }

    @Override
    public Object []    getIds () { 
        return (mo.getDirectChildIds ());
    }               
}
