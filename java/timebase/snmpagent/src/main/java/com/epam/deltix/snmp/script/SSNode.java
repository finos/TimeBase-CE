package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.*;

/**
 *
 */
public abstract class SSNode <T extends MONode> extends AbstractScriptable {
    protected final SSRoot      root;
    protected final T           mo;
    
    protected SSNode (SSRoot root, T mo) {
        super ("Node");
        
        this.root = root;
        this.mo = mo;        
    }

    public Object       getValue () {
        return (this);
    }        
        
    @Override
    public String       toString () {
        return ("SSNode (" + mo.toString () + ")");
    }        
}
