package com.epam.deltix.util.vsocket.util;

import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.memory.DataExchangeUtils;
import com.epam.deltix.util.vsocket.ChannelClosedException;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSConnectionListener;
import com.epam.deltix.util.vsocket.VSServer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class TestVServerSocketFactory {
    public static VSServer createInputThroughputVServer(int port, int packetSize) throws IOException {
        return createVServerSocket(port, ((executor, serverChannel) -> new InputThroughputServer(executor, serverChannel, packetSize).submit()));
    }

    public static VSServer createOutputThroughputVServer(int port, int packetSize) throws IOException {
        return createVServerSocket(port, ((executor, serverChannel) -> new OutputThroughputServer(executor, serverChannel, packetSize).submit()));
    }

    public static VSServer createLatencyVServer(int port, int packetSize) throws IOException {
        return createVServerSocket(port, ((executor, serverChannel) -> new LatencyServer(executor, serverChannel, packetSize).submit()));
    }

    public static VSServer createEchoVServer(int port) throws IOException {
        return createVServerSocket(port, ((executor, serverChannel) -> new EchoServer(executor, serverChannel).submit()));
    }

    public static VSServer createReadingVServer(int port, int packetSize) throws IOException {
        return createVServerSocket(port, ((executor, serverChannel) -> new ReadingServer(executor, serverChannel, packetSize).submit()));
    }

    public static VSServer createEmptyVServer(int port) throws IOException {
        return createVServerSocket(port, ((executor, serverChannel) -> new EmptyServer(executor, serverChannel).submit()));
    }

    private static VSServer createVServerSocket(int port, VSConnectionListener listener) throws IOException {
        VSServer server = new VSServer(port);
        server.setConnectionListener(listener);
        return server;
    }

    static class ReadingServer extends QuickExecutor.QuickTask {
        private VSChannel           channel;
        private final byte []       buffer;

        public ReadingServer (QuickExecutor executor, VSChannel channel, int bufferCapacity) throws IOException {
            super (executor);
            this.channel = channel;
            this.buffer = new byte [bufferCapacity];
        }

        @Override
        public void run () {
            try {
                DataInputStream     in = new DataInputStream (channel.getInputStream ());

                long index = 0;
                for (;;) {

                    in.readFully (buffer);

                    long first = DataExchangeUtils.readLong(buffer, 0);
                    long second = DataExchangeUtils.readLong(buffer, 8);
                    if (second - first != 1)
                        throw new IllegalStateException("mismatch: first = " + first + "; second = " + second);

                    if (first != index)
                        throw new IllegalStateException("mismatch: first(" + first + ") != index (" + index + ")");

                    index += 2;

                }
            } catch (Throwable x) {
                x.printStackTrace ();
            } finally {
                channel.close ();
            }
        }
    }

    static class EchoServer extends QuickExecutor.QuickTask {
        private VSChannel channel;

        public EchoServer(QuickExecutor executor, VSChannel channel) throws IOException {
            super(executor);
            this.channel = channel;
        }

        @Override
        public void run() throws InterruptedException {
            try {
                String s = channel.getDataInputStream().readUTF();

                DataOutputStream out = channel.getDataOutputStream();
                out.writeUTF(s);
                out.flush();
            } catch (Throwable x) {
                x.printStackTrace();
            } finally {
                channel.close();
            }
        }
    }

    static class InputThroughputServer extends QuickExecutor.QuickTask {
        private VSChannel channel;
        private byte[] buffer;

        public InputThroughputServer(QuickExecutor executor, VSChannel channel, int packetSize) throws IOException {
            super(executor);
            this.channel = channel;
            buffer = new byte[packetSize];
            for (int index = 0; index < packetSize; index++)
            {
                buffer[index] = (byte) index;
            }
        }

        @Override
        public void run() throws InterruptedException {
            try {
                DataOutputStream out = channel.getDataOutputStream();

                for (;;) {
                    out.write(buffer);
                }
            } catch(ChannelClosedException x) {
                // do nothing
            } catch (Throwable x) {
                x.printStackTrace();
            } finally {
                channel.close();
            }
        }
    }

    static class OutputThroughputServer extends QuickExecutor.QuickTask {
        private VSChannel channel;
        private byte[] buffer;

        public OutputThroughputServer(QuickExecutor executor, VSChannel channel, int packetSize) throws IOException {
            super(executor);
            this.channel = channel;
            buffer = new byte[packetSize];
        }

        @Override
        public void run() throws InterruptedException {
            try {
                DataInputStream in = new DataInputStream(channel.getInputStream());

                for (;;) {
                    in.readFully(buffer);
                }
            } catch (Throwable x) {
                x.printStackTrace();
            } finally {
                channel.close();
            }
        }
    }

    static class LatencyServer extends QuickExecutor.QuickTask {
        private VSChannel channel;
        private byte[] buffer;

        public LatencyServer(QuickExecutor executor, VSChannel channel, int packetSize) throws IOException {
            super(executor);
            this.channel = channel;
            buffer = new byte[packetSize];
        }

        @Override
        public void run() throws InterruptedException {
            try {
                DataInputStream in = channel.getDataInputStream();
                DataOutputStream out = channel.getDataOutputStream();
                SocketTestUtilities.proccessLatencyRequests(out, in, buffer, false);
            }
            catch (Throwable x) {
                x.printStackTrace();
            }
        }
    }

    static class EmptyServer extends QuickExecutor.QuickTask {
        private VSChannel channel;

        public EmptyServer(QuickExecutor executor, VSChannel channel) throws IOException {
            super(executor);
            this.channel = channel;
        }

        @Override
        public void run() throws InterruptedException {
        }
    }
}
