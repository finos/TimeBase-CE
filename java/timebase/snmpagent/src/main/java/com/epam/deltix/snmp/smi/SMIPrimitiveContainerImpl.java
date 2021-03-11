package com.epam.deltix.snmp.smi;

import java.util.*;

/**
 *
 */
class SMIPrimitiveContainerImpl <P extends SMIComplexNode>
    extends SMINodeImpl <P>
    implements SMIPrimitiveContainer 
{
    private TreeMap <Integer, SMINode>               idToChildNodeMap = 
        new TreeMap <Integer, SMINode> ();
    
    SMIPrimitiveContainerImpl (
        P                       parent, 
        SMIOID                  oid, 
        String                  name,
        String                  description
    )
    {
        super (parent, oid, name, description);
    }

    @Override
    public Collection <SMINode> children () {
        return (idToChildNodeMap.values ());
    }
        
    protected void              checkChild (int id, String name) {
        SMINode         ex = idToChildNodeMap.get (id);
        
        if (ex != null)
            throw new IllegalStateException (
                ex + " is already registered under the id of " + id
            );                
    }
    
    protected void              registerChild (SMINode child) {
        int             id = child.getOid ().getLast ();        
        SMINode         ex = idToChildNodeMap.get (id);
        
        if (ex == null)
            idToChildNodeMap.put (id, child);
        else if (ex != child)
            throw new IllegalStateException (
                ex + " is already registered under the id of '" +
                id + "'; cannot register " + child
            );                
    }
    
    @Override
    public SMINode          getChildById (int i) {
        return (idToChildNodeMap.get (i));
    }

    @Override
    public Integer []       getChildIds () {
        return (idToChildNodeMap.keySet ().toArray (new Integer [idToChildNodeMap.size ()]));
    }     
    
    @Override
    public int              getIndexLength (SMIOID oid, int start) {
        SMIComplexNode          parent = this;
        int                     ii = start;
        int                     oidLength = oid.getLength ();
        
        while (ii < oidLength) {
            int         childId = oid.getId (ii++);        
            SMINode     child = parent.getChildById (childId);

            if (child == null)
                return (MATCH_ERROR_UNKNOWN_ID);

            if (child instanceof SMIPrimitive)
                return (oidLength - ii);   
            
            if (child instanceof SMITable)
                return (MATCH_ERROR_IS_A_TABLE);
            
            parent = (SMICategory) child;
        }
        
        return (MATCH_ERROR_NON_PRIMITIVE_NODE);
    }
}
