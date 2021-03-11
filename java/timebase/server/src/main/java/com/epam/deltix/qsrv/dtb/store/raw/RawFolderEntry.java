package com.epam.deltix.qsrv.dtb.store.raw;

import com.epam.deltix.qsrv.dtb.store.codecs.TSNames;
import com.epam.deltix.qsrv.dtb.store.impl.*;

/**
 *
 */
public class RawFolderEntry {
    private final boolean           isFile;
    private final int               id;
    private final long              startTimestamp;  
    private final int               idxInParent;
    
    public RawFolderEntry (int idxInParent, boolean isFile, int id, long startTimestamp) {
        this.idxInParent = idxInParent;
        this.isFile = isFile;
        this.id = id;
        this.startTimestamp = startTimestamp;
    }

    public int                  getIdxInParent () {
        return idxInParent;
    }
    
    public boolean              isFile () {
        return isFile;
    }

    public int                  getId () {
        return id;
    }

    public long                 getStartTimestamp () {
        return startTimestamp;
    }        
    
    public String               getName () {
        return (isFile ? TSNames.buildFileName (id) : TSNames.buildFolderName (id));
    }
}
