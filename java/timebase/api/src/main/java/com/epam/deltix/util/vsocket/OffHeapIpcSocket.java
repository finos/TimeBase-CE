package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.io.*;
import com.epam.deltix.util.io.offheap.OffHeap;
import com.epam.deltix.util.lang.Util;

import java.io.*;
import java.net.Socket;

/**
 *
 */
public class OffHeapIpcSocket implements VSocket {
    private Socket                      socket;
    private InputStream                 in;
    private OutputStream                out;
    private VSocketInputStream          vin;
    private VSocketOutputStream         vout;
    private int                         code;
    private int socketNumber;

    public OffHeapIpcSocket(Socket socket, int code, boolean isServer, int socketNumber) throws IOException {
        this.socket = socket;
        this.code = code;
        this.socketNumber = socketNumber;

        String inName;
        String outName;
        if (isServer) {
            inName = "__dtx_" + socket.getLocalPort() + "_" + code + "s.tmp";
            outName = "__dtx_" + socket.getLocalPort() + "_" + code + "c.tmp";
        } else {
            inName = "__dtx_" + socket.getPort() + "_" + code + "c.tmp";
            outName = "__dtx_" + socket.getPort() + "_" + code + "s.tmp";
        }

        in = OffHeap.createInputStream(inName);
        out = OffHeap.createOutputStream(outName);

        vin = new VSocketInputStream(in, getSocketIdStr());
        vout = new VSocketOutputStream(out, getSocketIdStr());
    }

    @Override
    public VSocketInputStream          getInputStream() {
        return vin;
    }

    @Override
    public VSocketOutputStream         getOutputStream() {
        return vout;
    }

    @Override
    public String                      getRemoteAddress() {
        return String.valueOf(code);
    }

    @Override
    public void                        close() {
        Util.close(in);
        Util.close(out);
        IOUtil.close(socket);
    }

    @Override
    public int                         getCode() {
        return code;
    }

    @Override
    public void                        setCode(int code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return getClass().getName() + getSocketIdStr();
    }

    @Override
    public int getSocketNumber() {
        return socketNumber;
    }
}
