package com.epam.deltix.qsrv.dtb.store.raw;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import java.io.*;
import java.util.Stack;

/**
 *
 */
public class Verifier {
    static final long                      TS_UNKNOWN = Long.MAX_VALUE;
    
    private static class VerificationTask {
        final boolean               isFile;
        final AbstractPath          path;
        final long                  startTimestamp;
        final long                  limitTimestamp;

        public VerificationTask (boolean isFile, AbstractPath path, long startTimestamp, long limitTimestamp) {
            this.isFile = isFile;
            this.path = path;
            this.startTimestamp = startTimestamp;
            this.limitTimestamp = limitTimestamp;
        }                
    };
    
    private final DiagListener              dlnr;
    private final Stack <VerificationTask>  stack;
    private final VerificationMode          mode;
    
    public Verifier (DiagListener dlnr) {
        this (dlnr, VerificationMode.SINGLE_LEVEL);        
    }
    
    public Verifier (DiagListener dlnr, VerificationMode mode) {
        this.dlnr = dlnr;
        this.mode = mode;
        
        stack = 
            mode == VerificationMode.SINGLE_LEVEL ? 
                null : 
                new Stack <VerificationTask> ();
    }
        
    public void                 verifyObject (AbstractPath path)
        throws IOException
    {
        if (RawTSF.isTSFile (path))
            verifyFile (path);
        else
            verifyFolder (path);
    }
    
    public void                 verifyFolder (
        AbstractPath                path,
        long                        startTimestamp,
        long                        limitTimestamp
    )
        throws IOException
    {
        verifyOneFolder (path, startTimestamp, limitTimestamp);
        
        if (stack != null)
            processStack ();
    }
    
    public void                 verifyFolder (AbstractPath path)
        throws IOException
    {
        verifyFolder (path, TS_UNKNOWN, TS_UNKNOWN);
    }       
    
    public void                 verifyFile (AbstractPath path)
        throws IOException
    {
        verifyFile (path, TS_UNKNOWN, TS_UNKNOWN);
    }
    
    public void                 verifyFile (
        AbstractPath                path,
        long                        startTimestamp,
        long                        limitTimestamp
    )
        throws IOException
    {
        RawTSF                  f = new RawTSF ();
        
        f.setPath (path);
        
        f.verify (dlnr, startTimestamp, limitTimestamp);           
    }
    
    private void                verifyOneFolder (
        AbstractPath                path,
        long                        startTimestamp,
        long                        limitTimestamp
    )
        throws IOException
    {
        RawFolder               f = new RawFolder ();
        
        f.setPath (path);
        
        f.verify (dlnr, startTimestamp, limitTimestamp);           
        
        if (stack != null) {
            int         numChildren = f.getNumChildren ();
            
            for (int ii = 0; ii < numChildren; ii++) {
                RawFolderEntry      child = f.getChild (ii);
                int                 nextIdx = ii + 1;
                
                if (!child.isFile () || mode == VerificationMode.COMPLETE)
                    stack.add (
                        new VerificationTask (
                            child.isFile (), 
                            f.getChildPath (ii),
                            child.getStartTimestamp (),
                            nextIdx == numChildren ? 
                                limitTimestamp : 
                                f.getChild (nextIdx).getStartTimestamp ()
                        )
                    );
            }
        }
    }
    
    private void            processStack () throws IOException {
        while (!stack.isEmpty ()) {
            VerificationTask    task = stack.pop ();
            
            if (task.isFile)
                verifyFile (task.path, task.startTimestamp, task.limitTimestamp);
            else
                verifyOneFolder (task.path, task.startTimestamp, task.limitTimestamp);
        }
    }    
}
