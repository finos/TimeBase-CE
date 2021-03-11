package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.lang.Disposable;

import java.io.*;

/**
 *  A virtual socket.
 */
public interface VSChannel extends Disposable {

    public int                  getLocalId ();

    public int                  getRemoteId ();

    public String               getRemoteAddress();

    public String               getRemoteApplication();

    public String               getClientId();

    public VSOutputStream       getOutputStream ();

    public DataOutputStream     getDataOutputStream ();

    public InputStream          getInputStream ();

    public DataInputStream      getDataInputStream ();

    public VSChannelState       getState();

    public boolean              setAutoflush(boolean value);

    public boolean              isAutoflush();
    
    public void                 close(boolean terminate);

    public void                 setAvailabilityListener (Runnable lnr);
    
    public Runnable             getAvailabilityListener ();

    public boolean              getNoDelay();

    public void                 setNoDelay(boolean value);
    
    public String               encode(String value);

    public String               decode(String value);
}
