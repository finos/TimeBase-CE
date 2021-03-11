package com.epam.deltix.qsrv.dtb.store.dataacc;

import java.util.Comparator;

/**
 *
 */
class LinkComparatorByTime implements Comparator <AccessorBlockLink> {
    static final Comparator <AccessorBlockLink>     ASCENDING = new LinkComparatorByTime (true);
    static final Comparator <AccessorBlockLink>     DESCENDING = new LinkComparatorByTime (false);
    
    private final boolean       ascending;

    private LinkComparatorByTime (boolean ascending) {
        this.ascending = ascending;
    }
            
    @Override
    public int  compare (AccessorBlockLink a, AccessorBlockLink b) {
        long        ta = a.getNextTimestamp ();
        long        tb = b.getNextTimestamp ();
        
        if (ta == tb)
            return (0);
        
        return (ta < tb == ascending ? -1 : 1);
    }
}
