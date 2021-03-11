package com.epam.deltix.util.vsocket.transport;

import com.epam.deltix.util.vsocket.TransportType;
import com.epam.deltix.util.vsocket.VSProtocol;
import com.epam.deltix.util.vsocket.VSocket;
import com.epam.deltix.util.vsocket.VSocketFactory;
import com.epam.deltix.util.vsocket.VSocketImpl;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.util.logging.Level;

/**
 *
 */
public class DatagramConnection implements Connection {
    public DatagramSocket       socket;
    private BufferedInputStream in;
    private OutputStream        out;
    int                         remotePort = -1;

    public DatagramConnection(DatagramSocket socket, InetAddress address) throws UnknownHostException {
        this.socket = socket;
        this.in = new BufferedInputStream(new DatagramInputStream(this));
        this.out = new DatagramOutputStream(this, address);
        setUpSocket();
    }

//    public DatagramConnection(DatagramSocket socket, InetSocketAddress address) throws UnknownHostException {
//        this.socket = socket;
//        this.in = new BufferedInputStream(new DatagramInputStream(this));
//        this.out = new DatagramOutputStream(this, address.getAddress());
//        this.remotePort = address.getPort();
//        setUpSocket();
//    }

    private void         setUpSocket () {
        try {
            socket.setSoTimeout (0);

            //socket.setReceiveBufferSize(1 << 14);
            //socket.setSendBufferSize(1 << 14);
        } catch (SocketException e) {
            VSProtocol.LOGGER.log (Level.WARNING, null, e);
        }
    }

    @Override
    public OutputStream         getOutputStream() {
        return out;
    }

    @Override
    public BufferedInputStream  getInputStream() {
        return in;
    }

    @Override
    public VSocket              create(int code) {
        return new VSocketImpl(this, code, VSocketFactory.nextSocketNumber());
    }

    @Override
    public InetAddress          getRemoteAddress() {
        return null;
    }

    @Override
    public void                 close() {
        socket.close();
    }

    @Override
    public VSocket              create(VSocket stopped) throws IOException {
        VSocket socket = create(stopped.getCode());
        stopped.getOutputStream().writeTo(socket.getOutputStream());
        return socket;
    }

    @Override
    public boolean              isLoopback() {
        return socket.getInetAddress().isLoopbackAddress();
    }

    @Override
    public void setTransportType(TransportType transportType) {
        /* not implemented for this class */
    }

    @Override
    public void                 upgradeToSSL(SSLSocketFactory sslSocketFactory) throws IOException {
        /* not implemented for this class */
    }
}
