package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

/**
 *  Specifies special grouping by entity (optimized 
 *  implementation of which uses the entity index).
 */
public final class GroupByEntity extends GroupBySpec {
    @Override
    public String       toString () {
        return "group by entity";
    }    
}
