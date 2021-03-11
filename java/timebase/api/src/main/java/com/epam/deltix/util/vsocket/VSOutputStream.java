package com.epam.deltix.util.vsocket;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Date: Mar 29, 2010
 */
public abstract class VSOutputStream extends OutputStream {

    public abstract void    enableFlushing() throws IOException;

    /*
     *  Disables flushing internal buffer when it reach capacity.
     *  Buffer will be extended when new data is written until flashing will be enabled again
     */

    public abstract void    disableFlushing();

    /*
     *  Flushes portion of data that can recieved on the remote side immediately.
     */
    public abstract void    flushAvailable() throws IOException;

    //public abstract int     available();
}
