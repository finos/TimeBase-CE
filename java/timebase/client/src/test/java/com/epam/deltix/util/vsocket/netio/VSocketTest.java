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
package com.epam.deltix.util.vsocket.netio;

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.*;
import java.io.*;
import java.util.*;

class ServerConnectHandler extends QuickExecutor.QuickTask {
    private final VSChannel     channel;

    public ServerConnectHandler (QuickExecutor executor, VSChannel serverChannel) {
        super (executor);
        this.channel = serverChannel;
    }

    @Override
    public void         run () {
        byte []             b = new byte [256];
        
        try {
            DataInputStream     is = new DataInputStream (channel.getInputStream ());
            DataOutputStream    os = new DataOutputStream (channel.getOutputStream ());
            
            for (;;) {
                int         x = is.readShort ();

                is.readFully (b, 0, x);

                os.writeShort (x);
                os.write (b, 0, x);
                os.flush ();
            }
        }
        catch (EOFException iox) {
            // ignore 
        }
        catch (IOException iox) {
            iox.printStackTrace ();
        } finally {
            channel.close ();
        }
    }
}

class ClientThread extends Thread {
    public static final boolean PRINT_STATS = true;

    private final int           numMessages;
    private final int           numMessagesPerSession;
    private final int           port;

    public ClientThread (String name, int port, int nps, int n) throws IOException {
        super (name);
        this.port = port;
        numMessagesPerSession = nps;
        numMessages = n;
    }

    @Override
    public void run () {
        Random                  r = new Random ();
        byte []                 b = new byte [256];
        byte []                 b2 = new byte [256];
        int                     n = 0;
        VSClient                client = null;

        try {
            client = new VSClient ("localhost", port);
            client.connect ();

            while (n < numMessages) {
                VSChannel           channel = null;

                try {                    
                    channel = client.openChannel ();

                    DataOutputStream        os = new DataOutputStream (channel.getOutputStream ());
                    DataInputStream         is = new DataInputStream (channel.getInputStream ());

                    for (int ii = 0; ii < numMessagesPerSession; ii++) {
                        int             x = 1 + r.nextInt (255);

                        r.nextBytes (b);

                        os.writeShort (x);
                        os.write (b, 0, x);
                        os.flush ();

                        int             x2 = is.readShort ();

                        if (x2 != x)
                            throw new RuntimeException ("Wrong response length");

                        is.readFully (b2, 0, x);

                        for (int jj = 0; jj < x; jj++)
                            if (b2 [jj] != b [jj])
                                throw new RuntimeException ("Wrong response found");

                        n++;

                        if (n % 1000 == 0)
                            System.out.println (getName () + ": " + n + " OK");
                        //if (ii % 10 == 0)
                        //    Thread.sleep (r.nextInt (80));
                    }

                    os.close ();
                } finally {
                    Util.close (channel);
                }
            }
        } catch (Exception x) {
            x.printStackTrace ();
            System.exit (1);
        } finally {
            Util.close (client);
        }
    }
}

public class VSocketTest {
    public static final int         NUM_CLIENTS = 4;
    public static final int         NUM_LONG = 4;
    public static final int         NUM_SHORT = 2;

    public static void      main (String args []) throws Throwable {
        final VSServer    server = new VSServer ();

        server.setConnectionListener (
            new VSConnectionListener () {
                public void connectionAccepted (QuickExecutor executor, final VSChannel serverChannel) {
                    new ServerConnectHandler (executor, serverChannel).submit ();
                }
            }
        );

        server.start ();

        ArrayList <ClientThread>   threads = new ArrayList <ClientThread> ();

        for (int ii = 0; ii < NUM_CLIENTS; ii++) {
            for (int jj = 0; jj < NUM_LONG; jj++)
                threads.add (new ClientThread ("L" + ii + "." + jj, server.getLocalPort (), 2000, 10000));

            for (int jj = 0; jj < NUM_SHORT; jj++)
                threads.add (new ClientThread ("S" + ii + "." + jj, server.getLocalPort (), 1, 10000));
        }

        for (ClientThread ct : threads)
            ct.start ();

        for (ClientThread ct : threads)
            ct.join ();

        System.out.println ("TEST: all client threads are done.");

        server.close ();
    }
}
