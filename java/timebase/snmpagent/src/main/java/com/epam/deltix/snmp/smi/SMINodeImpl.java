package com.epam.deltix.snmp.smi;

/**
 *
 */
abstract class SMINodeImpl <P extends SMIComplexNode> implements SMINode {
    private final P                     parent;
    private final String                name;
    private final SMIOID                oid;
    private final String                description;
    
    protected SMINodeImpl (
        P                   parent,
        SMIOID              oid,
        String              name,
        String              description
    )
    {
        this.parent = parent;
        this.name = name;
        this.oid = oid;   
        this.description = description;
    }

    @Override
    public int              getId () {
        return (oid.getLast ());
    }
    
    @Override
    public P                getParent () {
        return parent;
    }
    
    @Override
    public final SMIOID     getOid () {
        return oid;
    }

    @Override
    public final String     getName () {
        return name;
    }

    @Override
    public final String     getDescription () {
        return description;
    }

    @Override
    public String           toString () {
        return ("node at " + oid);
    }
}
