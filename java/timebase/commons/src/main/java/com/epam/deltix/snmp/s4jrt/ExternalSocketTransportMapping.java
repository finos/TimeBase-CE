/*
 * Copyright 2021 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.snmp.s4jrt;

import com.epam.deltix.util.lang.Util;
import java.util.*;
import java.io.*;
import java.io.BufferedInputStream;
import java.net.*;

import java.nio.ByteBuffer;

import com.epam.deltix.util.tomcat.ConnectionHandshakeHandler;
import org.snmp4j.*;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.smi.*;
import org.snmp4j.transport.*;

/**
 *
 */
public class ExternalSocketTransportMapping 
    extends TcpTransportMapping implements ConnectionHandshakeHandler
{
    class SocketData {
        final TcpAddress              address;
        final Socket                  socket;
        final InputStream             is;
        final OutputStream            os;

        public SocketData (TcpAddress peer, Socket socket, InputStream is, OutputStream os) {
            this.socket = socket;
            this.is = is;
            this.os = os;            
            this.address = peer;
        }                
    }
        
    private final Map <TcpAddress, SocketData>      sockets =
        new HashMap <> ();

    private MessageLengthDecoder                    messageLengthDecoder =
      new DefaultTcpTransportMapping.SnmpMesssageLengthDecoder();
    
    public ExternalSocketTransportMapping (InetAddress ip, int port) {
        super (
            new TcpAddress (ip == null ? IpAddress.ANY_IPADDRESS : ip, port)
        );               
    }

    @Override
    public boolean                 handleHandshake (
        Socket                      socket,
        BufferedInputStream         bis,
        OutputStream                os
    )
        throws IOException 
    {
        final TcpAddress    address = 
            new TcpAddress (socket.getInetAddress (), socket.getPort ());

        try {
            final SocketData    data = new SocketData (address, socket, bis, os);

            synchronized (sockets) {
                sockets.put (address, data);
            }

            byte []             buffer = new byte [20000];
            
            for (;;) {
                int             n = bis.read (buffer);

                if (n < 0)
                    break;

                ByteBuffer          bb = ByteBuffer.wrap (buffer, 0, n);

                TransportStateReference tsr =
                    new TransportStateReference (
                        this, 
                        address, 
                        null,
                        SecurityLevel.undefined, 
                        SecurityLevel.undefined,
                        false, 
                        data
                    );

                fireProcessMessage (address, bb, tsr);
            }
        } catch (Throwable x) {
            Util.logException("Error during handshake: %s", x);
        } finally {            
            synchronized (sockets) {
                sockets.remove (address);
            }
        }
        return (true);
    }

    @Override
    public void                 close () throws IOException {
        // close all sockets
        synchronized (sockets) {
            for (SocketData data : sockets.values()) {

                try {
                    data.socket.close();
                } catch (IOException e) {
                    S4JUtils.LOGGER.info("Closing error: %s").with(e);
                }
            }

            sockets.clear();
        }
    }

    @Override
    public int                  getMaxInboundMessageSize () {
        return (Integer.MAX_VALUE);
    }

    @Override
    public boolean              isListening () {
        return (true);
    }

    @Override
    public void                 listen () throws IOException {
    }

    @Override
    public void                 sendMessage (
        TcpAddress                  address, 
        byte []                     bytes,
        TransportStateReference     tsr // unused
    )
        throws IOException 
    {
        SocketData                  data;
        
        synchronized (sockets) {
            data = sockets.get (address);
        }
        
        if (data == null)   // should open a socket in this case
            throw new UnsupportedOperationException (
                "(Notification?) Sending to non-currently-open address " + address
            );
        
        data.os.write (bytes);
        data.os.flush ();
    }

    @Override
    public MessageLengthDecoder getMessageLengthDecoder () {
        return (messageLengthDecoder);
    }

    @Override
    public void                 setMessageLengthDecoder (MessageLengthDecoder mld) {
        messageLengthDecoder = mld;
    }

    @Override
    public void                 setConnectionTimeout (long l) {
        System.out.println ("setConnectionTimeout (" + l + ")");
    }

    @Override
    public boolean              close (TcpAddress address) throws IOException {
        synchronized (sockets) {
            SocketData      data = sockets.remove (address);
            
            if (data == null)
                return (false);
            
            data.socket.close ();
        }
        
        return (true);
    }    
}
