package com.epam.deltix.util.vsocket;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.cmdline.DefaultApplication;
import com.epam.deltix.util.collections.generated.ObjectHashSet;
import com.epam.deltix.util.time.GlobalTimer;
import com.epam.deltix.util.time.TimerRunner;

import java.lang.reflect.Field;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Establishes connection to TimeBase using given number of stream readers and closes underlying sockets at random */
public class CrazyClientTest extends DefaultApplication {

    public CrazyClientTest(String[] args) {
        super(args);
    }

    public static void main (String [] args) throws Exception {
        new CrazyClientTest (args).start ();
    }

    @Override
    protected void run() throws Throwable {
        String tickDbUrl = getArgValue("-tickdb", "dxtick://localhost:8011");
        String stream = getMandatoryArgValue("-stream");
        int numThreads = getIntArgValue("-count", 1);
        int killIntervalMillis = getIntArgValue("-interval", 5000);

        for (int i=1; i <= numThreads; i++)
            new CrazyClient ( "CrazyClient" + i, tickDbUrl, stream, killIntervalMillis).start();

    }

    private static class CrazyClient extends Thread {
        final static Random rnd = new Random(System.currentTimeMillis());
        final String tickDbUrl;
        final String streamKey;
        final int killIntervalMillis;

        private CrazyClient(String id, String tickDbUrl, String stream, int killIntervalMillis) {
            super(id);
            this.tickDbUrl = tickDbUrl;
            this.streamKey = stream;
            this.killIntervalMillis = killIntervalMillis;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    DXTickDB db = TickDBFactory.createFromUrl(tickDbUrl);
                    db.open(false);

                    AtomicInteger msgCount = new AtomicInteger();
                    scheduleKillTimer(db, msgCount);

                    TickStream stream = db.getStream(streamKey);
                    if (stream == null) {
                        System.err.println("Stream does not exist: " + streamKey);
                        break;
                    }
                    TickCursor cursor = db.select(System.currentTimeMillis(), new SelectionOptions(false, true), stream);
                    while (cursor.next()) {
                        cursor.getMessage();
                        msgCount.incrementAndGet();
                    }

                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }

        private void scheduleKillTimer(final DXTickDB db, final AtomicInteger msgCount) throws NoSuchFieldException, IllegalAccessException {
            long delay = 10 + rnd.nextInt(killIntervalMillis);

                GlobalTimer.INSTANCE.schedule(new TimerRunner() {
                    @Override
                    protected void runInternal() throws Exception {
                        final Socket[] sockets = locateSockets(db);
                        if (sockets != null && sockets.length > 0) {
                            final Socket socket = sockets[rnd.nextInt(sockets.length)];
                            if (rnd.nextBoolean()) {
                                socket.close();
                                System.out.println("Closed socket " + socket + " (" + msgCount.get() + "msgs)");
                            }
                        }
                        scheduleKillTimer(db, msgCount);
                    }
                }, delay);
        }

        private static Socket [] locateSockets(DXTickDB db) throws NoSuchFieldException, IllegalAccessException {
            @SuppressWarnings("unchecked")
            ObjectHashSet<VSTransportChannel> set = (ObjectHashSet<VSTransportChannel>) reflect(db, "session.ds.dispatcher.transportChannels");
            VSTransportChannel[] transportChannels = set.toArray(new VSTransportChannel[set.size()]);
            Socket[] result = new Socket[transportChannels.length];
            for (int i=0; i < transportChannels.length; i++) {
                result [i] = (Socket) reflect(transportChannels[i], "socket.socket");
            }
            return result;
        }

        private static Object reflect(Object root, String path) throws NoSuchFieldException, IllegalAccessException {
            Object result = root;
            for (String field : path.split("\\.") ) {
                Field f = result.getClass().getDeclaredField(field);
                f.setAccessible(true);
                result = f.get(result);
            }
            return result;
        }
    }
}
