package com.epam.deltix.snmp.mibc;

/**
 *
 */
public abstract class StandardEntityBean implements CompiledEntity {
    private final String                id;

    public StandardEntityBean (String id) {
        this.id = id;
    }

    @Override
    public String                       getId () {
        return (id);
    }        
}
