package com.epam.deltix.qsrv.dtb.store.impl;

import com.epam.deltix.qsrv.dtb.store.pub.*;

/**
 *
 */
class TSRefImpl implements TSRef {
    private final String                fullPath;
    private final int []                idPath;
    
    TSRefImpl (TSFile tsf) {
        StringBuilder       sb = new StringBuilder (tsf.getName ());
        int                 depth = 1;
        
        for (TSFolderEntry e = tsf.getParent (); ; e = e.getParent ()) {            
            sb.insert (0, '/');
            
            if (e instanceof TSRootFolder) {
                sb.insert (0, e.getPath().getPathString());
                break;
            }
            
            depth++;
            sb.insert (0, e.getName ());                        
        }        
        
        fullPath = sb.toString ();
        
        idPath = new int [depth];
                        
        for (TSFolderEntry e = tsf; !(e instanceof TSRootFolder); e = e.getParent ()) 
            idPath [--depth] = e.getId ();        
        
        assert depth == 0;
    }

    int []                      getIdPath () {
        return (idPath);
    }
    
    @Override
    public String               getPath () {
        return (fullPath);
    }

    @Override
    public String               toString () {
        return "TSRefImpl [" + getPath () + ']';
    }        
}
