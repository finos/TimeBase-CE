package com.epam.deltix.qsrv.dtb.store.pub;

/**
 *
 */
public class SingleEntityFilter implements AbstractSingleEntityFilter {
    public int              entity;

    public SingleEntityFilter (int entity) {
        this.entity = entity;
    }

    @Override
    public boolean          accept (int entity) {
        return (this.entity == entity);
    }

    @Override
    public boolean          acceptAll () {
        return (false);
    }      
    
    @Override
    public int              getSingleEntity () {
        return (entity);
    }

    @Override
    public String           toString () {
        return "SingleEntityFilter [" + entity + "]";
    }

    @Override
    public boolean          restrictAll() {
        return false;
    }

    @Override
    public long             acceptFrom(int entity) {
        return Long.MIN_VALUE;
    }
}
