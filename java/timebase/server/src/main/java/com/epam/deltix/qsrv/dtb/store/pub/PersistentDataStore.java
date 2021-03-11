package com.epam.deltix.qsrv.dtb.store.pub;

import com.epam.deltix.qsrv.dtb.fs.pub.*;

import javax.annotation.Nullable;

/**
 *
 */
public interface PersistentDataStore { 
    public boolean          isStarted ();
    
    public boolean          isReadOnly ();
    
    public void             setReadOnly (boolean readOnly);

    public void             startShutdown ();
    
    public boolean          waitUntilDataStored (int timeout);
    
    public void             setNumWriterThreads (int n);
    
    public void             start ();
    
    public void             shutdown ();
    
    public boolean          waitForShutdown (int timeout);

    public TSRoot           createRoot (@Nullable String space, AbstractFileSystem fs, String path);

    public TSRoot           createRoot(@Nullable String space, AbstractPath path);
    
    /**
     *  Factory method for creating a reusable object for writing 
     *  messages.
     * 
     *  @return         An instance of the DataAccessor interface.
     */
    public DataWriter       createWriter ();
    
    /**
     *  Factory method for creating a reusable object for reading 
     *  messages.
     * 
     *  @return         An instance of the DataAccessor interface.
     */
    public DataReader       createReader (boolean live);

    void setEmergencyShutdownControl(EmergencyShutdownControl shutdownControl);
}
