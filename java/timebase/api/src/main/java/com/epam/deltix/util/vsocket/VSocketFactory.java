package com.epam.deltix.util.vsocket;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class VSocketFactory {
    private static final AtomicInteger socketNumberGen = new AtomicInteger();

    public enum Transport {
        Socket,
        SharedMemory,
        Memory
    }

    private final static HashMap<String, VSocket> cache =
            new HashMap<String, VSocket>();

    public static volatile Transport transport;

    public static VSocket get(ClientConnection cc, TransportType transportType) throws IOException {
        int socketNumber = nextSocketNumber();

//        synchronized (cache) {
//            String remoteID = getRemoteID(s);
//            if (!cache.containsKey(remoteID)) {
//                MemorySocket socket = new MemorySocket();
//                MemorySocket remote = new MemorySocket(socket);
//                cache.put(getRemoteID(s), remote);
//                cache.put(getID(s), socket);
//
//                return socket;
//            } else {
//                return cache.get(getID(s));
//            }
//        }

        Socket s = cc.getSocket();
        if (transportType == TransportType.AERON_IPC) {
            return new AeronIpcSocket(s, s.hashCode(), false, socketNumber);
        }
        else if (transportType == TransportType.OFFHEAP_IPC)
            return new OffHeapIpcSocket(s, s.hashCode(), false, socketNumber);
        else
            return new VSocketImpl(cc, socketNumber);
    }

    public static VSocket get(ClientConnection cc, VSocket stopped) throws IOException {
        Socket s = cc.getSocket();
        int socketNumber = nextSocketNumber();
        VSocket socket;
        if (stopped instanceof VSocketImpl) {
            socket = new VSocketImpl(cc, socketNumber);
        } else if (stopped instanceof AeronIpcSocket) {
            socket = new AeronIpcSocket(s, stopped.getCode(), false, socketNumber);
        } else if (stopped instanceof OffHeapIpcSocket) {
            socket = new OffHeapIpcSocket(s, stopped.getCode(), false, socketNumber);
        } else {
            throw new IllegalStateException("Unknown VSocket implementation:" + stopped);
        }

        socket.setCode(stopped.getCode());

        //System.out.println("Restore socket from " + stopped);
        stopped.getOutputStream().writeTo(socket.getOutputStream());
        return socket;
    }

    public static VSocket get(Socket socket, BufferedInputStream in, OutputStream out)
        throws IOException
    {
        int socketNumber = nextSocketNumber();
        //return get(socket);

        return new VSocketImpl(socket, in, out, socket.hashCode(), socketNumber);
    }

//    public static VSocket get(Connection c) throws IOException {
//        return c.create();
//    }
    
    private static String getID(Socket s)  {
        return s.getLocalAddress().toString() + ":" + s.getPort() + "\\" + s.getLocalPort();
    }

    private static String getRemoteID(Socket s)  {
        return s.getLocalAddress().toString() + ":" + s.getLocalPort() + "\\" + s.getPort();
    }

    public static int nextSocketNumber() {
        return socketNumberGen.incrementAndGet();
    }
}
