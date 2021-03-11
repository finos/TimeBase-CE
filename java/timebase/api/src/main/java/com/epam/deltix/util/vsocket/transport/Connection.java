package com.epam.deltix.util.vsocket.transport;

import com.epam.deltix.util.vsocket.TransportType;
import com.epam.deltix.util.vsocket.VSocket;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

/**
 *
 */
public interface Connection {

    public OutputStream             getOutputStream();

    public BufferedInputStream      getInputStream();

    public VSocket                  create(int code) throws IOException;

    public VSocket                  create(VSocket stopped) throws IOException;

    public InetAddress              getRemoteAddress();

    public void                     close();

    public boolean                  isLoopback();

    public void                     setTransportType(TransportType transportType);

    public void                     upgradeToSSL(SSLSocketFactory sslSocketFactory) throws IOException;

}
