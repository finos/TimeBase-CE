package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;
import java.util.*;

/**
 *
 */
public class MORow extends MOContainer {
    private final Map <String, MOPrimitive>      nameToNodeMap = 
        new HashMap <String, MOPrimitive> ();
    
    MORow (MOTable table, SMIOID index) {
        super (table, table.getProto ().getConceptualRow (), index);
        
        for (SMINode childPrototype : getProto ().children ()) {
            SMIPrimitive        pcp = (SMIPrimitive) childPrototype;            
            MOPrimitive         child = new MOPrimitive (this, pcp, index);        
            
            children.put (childPrototype.getOid ().getLast (), child);
            
            if (pcp.getName () != null)
                nameToNodeMap.put (pcp.getName (), child);
        }
    } 
    
    @Override
    public MOPrimitive              getNodeByName (String name) {
        return (nameToNodeMap.get (name));
    }            
}
