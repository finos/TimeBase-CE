package com.epam.deltix.qsrv.dtb.store.raw;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import java.io.*;

/**
 *
 */
public abstract class RawNode {
    protected AbstractPath                      path;
    protected int                               formatVersion;
    protected long                              version;
    protected int                               numEntities;    

    public AbstractPath         getPath () {
        return path;
    }

    public int                  getFormatVersion () {
        return formatVersion;
    }

    public long                 getVersion () {
        return version;
    }

    public int                  getNumEntities () {
        return numEntities;
    }

    public abstract void        setPath (AbstractPath path);
    
    public final void           readIndex (DiagListener dlnr)
        throws IOException
    {
        readIndex (Verifier.TS_UNKNOWN, Verifier.TS_UNKNOWN, dlnr);
    }

    public abstract void        readIndex (
        long                        startTimestamp,
        long                        limitTimestamp,
        DiagListener                dlnr
    )
        throws IOException;

    public abstract int         getEntity (int idx);
    
    
}
