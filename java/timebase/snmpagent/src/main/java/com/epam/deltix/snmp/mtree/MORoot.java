package com.epam.deltix.snmp.mtree;

import com.epam.deltix.snmp.smi.*;
import java.util.*;

/**
 *
 */
public final class MORoot extends MOContainer {
    private SNMPSPI                         spi;
    private final Map <String, MONode <?>>  nameToNodeMap = 
        new HashMap <String, MONode <?>> ();
        
    public MORoot (SMICategory cat) {
        this (null, cat);
    }
    
    public MORoot (SNMPSPI spi, SMICategory cat) {
        super (null, cat);
        
        this.spi = spi;
        
        setUpScalarChildren ();
    }

    public void                                         setSPI (SNMPSPI spi) {
        this.spi = spi;
    }
        
    public Iterable <Map.Entry <String, MONode <?>>>    scalars () {
        return (nameToNodeMap.entrySet ());
    }
    
    void                            registerName (MONode node) {
        String          nameIfKnown = node.getProto ().getName ();
        
        if (nameIfKnown != null) {
            MONode      ex = nameToNodeMap.get (nameIfKnown);
            
            if (ex == null)
                nameToNodeMap.put (nameIfKnown, node);  
            else if (ex != node)
                throw new IllegalStateException (
                    ex + " is already registered under the name of '" +
                    nameIfKnown + "'; cannot register " + node
                );
        }            
    }

    public SNMPSPI                  getSPI () {
        return (spi);
    }  
    
    @Override
    public MORoot                   getRoot () {
        return (this);
    } 
    
    @Override
    public MONode <?>               getNodeByName (String name) {
        return (nameToNodeMap.get (name));
    }
    
    public MONode <?>               getNodeByOID (SMIOID oid) {
        MONode <?>      node = this;
        
        for (int ii = 0; ii < oid.getLength (); ii++) {
            if (!(node instanceof MOContainer))
                break;
            
            node = ((MOContainer) node).getDirectChildById (oid.getId (ii));
        }
        
        return (node);
    }
}
