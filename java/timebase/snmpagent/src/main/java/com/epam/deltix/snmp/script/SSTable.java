package com.epam.deltix.snmp.script;

import com.epam.deltix.snmp.mtree.MOTable;
import com.epam.deltix.snmp.smi.*;
import org.mozilla.javascript.Scriptable;

/**
 *
 */
public class SSTable extends SSNode <MOTable> {
    public SSTable (
        SSRoot                      root, 
        MOTable                     mo
    )
    {
        super (root, mo);        
    }

    @Override
    public Object []                getIds () {
        int         n = mo.getNumRows ();
        String []   ret = new String [n];
        int         ii = 0;
        
        for (SMIOID oid : mo.getIds ())
            ret [ii++] = oid.toString ();
        
        return (ret);
    }

    @Override
    public Object                   get (int i, Scriptable start) {
        //  Rhino converts strings that look like integers into integers!
        //  Work around this weirdness.
        return (get (String.valueOf (i), start));
    }
    
    @Override
    public boolean                  has (int i, Scriptable start) {
        //  Rhino converts strings that look like integers into integers,
        //  This does not normally affect has(), only get(), but just in case...
        return (has (String.valueOf (i), start));
    }
    
    @Override
    public Object                   get (String name, Scriptable start) {
        assert start == this;
        
        return (root.wrap (mo.getRow (SMIOID.valueOf (name))));
    }

    @Override
    public boolean                  has (String name, Scriptable start) {
        assert start == this;
        
        return (mo.getRow (SMIOID.valueOf (name)) != null);
    }        
}
