package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.time.TimeKeeper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 */
public class VSocketThroughputTest {
    public static class Server extends QuickExecutor.QuickTask {
        private VSChannel channel;
        private final byte []       buffer;

        public Server (QuickExecutor executor, VSChannel channel, int bufferCapacity) throws IOException {
            super (executor);
            this.channel = channel;
            buffer = new byte [bufferCapacity];
        }

        @Override
        public void run () {
            try {
                DataInputStream in = new DataInputStream(channel.getInputStream ());
                DataOutputStream out = new DataOutputStream(channel.getOutputStream ());
                                
                for (;;) {
                    in.readFully (buffer);
//                    for (int i = 0; i < buffer.length - 1; i++)
//                        assert(buffer[i + 1] - buffer[i] == 1) : buffer[i + 1] + " vs " + buffer[i];                    
                }
            } catch (Throwable x) {
                x.printStackTrace ();
            } finally {
                channel.close ();
            }
        }
    }

    public static void main (String[] args) throws Exception {
        if (args.length == 0)
            args = new String[] { "4096" };

        if (args.length == 1) {
            // server

            final int           packetSize = Integer.parseInt (args [0]);
            VSServer server = new VSServer();

            server.setConnectionListener (
                new VSConnectionListener() {
                    public void connectionAccepted (QuickExecutor executor, final VSChannel serverChannel) {
                        System.out.println ("Server: connection accepted.");

                        try {
                            new Server (executor, serverChannel, packetSize).submit ();
                        } catch (Throwable x) {
                            x.printStackTrace ();
                            System.exit (1);
                        }
                    }
                }
            );

            server.start ();
            System.out.println("Server listening on port " + server.getLocalPort() + "; packet size: " + packetSize);
            client ("localhost", server.getLocalPort(), packetSize);
        } else {
            client (args [0], Integer.parseInt (args [1]), Integer.parseInt (args [2]));
        }
    }

    public static void  client (String host, int port, int packetSize)
        throws IOException
    {
        byte []             buffer = new byte [packetSize];

        for (int ii = 0; ii < packetSize; ii++)
            buffer [ii] = (byte) ii;

        VSClient c = new VSClient(host, port);
        c.connect ();

        VSChannel s = c.openChannel ();
        s.setAutoflush (true);
        OutputStream os = s.getOutputStream ();
        int                 counter = 0;
        long                lastReportTime = TimeKeeper.currentTime;
        long                nextReportTime = lastReportTime + 1000;
        long                lastReportedCount = 0;
        
        for (;;) {
            os.write (buffer);

            counter++;

            long            now = TimeKeeper.currentTime;

            if (now > nextReportTime) {
                long        num = counter - lastReportedCount;
                double      sec = (now - lastReportTime) * 0.001;
                double      rate = num / sec;

                System.out.printf (
                    "%,d m/s; %,d B/s \n",
                    (int) rate,
                    num * packetSize
                );                
                
                lastReportedCount = counter;
                lastReportTime = now;
                nextReportTime = now + 1000;
            }
        }               
    }
}
