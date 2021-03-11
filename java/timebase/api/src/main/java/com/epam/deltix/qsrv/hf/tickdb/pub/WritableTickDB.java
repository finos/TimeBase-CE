package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *  <p>The top-level interface to the Deltix Tic Database engine. Instances of
 *  this interface are created by static methods of {@link TickDBFactory}.</p>
 * 
 *  <p>At the physical level, a database consists of a number of folders 
 *  (directories) on the hard disk. While the database is closed, the files 
 *  can be freely moved around in order to manage disk space and/or take advantage
 *  of parallel access to several hard disk devices. It is even possible to 
 *  increase or reduce the number of folders, as long as all necessary folders
 *  are supplied when a database instance is constructed. While a database is
 *  open, external processes must obviously not interfere with the files.</p>
 */
public interface WritableTickDB extends TickDB {
    /**
     *  Looks up an existing stream by key.
     *
     *  @param key      Identifies the stream.
     *  @return         A stream object, or <code>null</code> if the key was not found.
     *  @throws         java.security.AccessControlException when user is not authorized to READ given stream
     */
    WritableTickStream                       getStream (
        String                                  key
    );
    
    /**
     *  Enumerates existing streams.
     * 
     *  @return         An array of existing stream objects.
     */
    WritableTickStream []                    listStreams ();
}
