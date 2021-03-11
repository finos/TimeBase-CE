package com.epam.deltix.qsrv.hf.tickdb;

import java.util.*;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

public class StreamManipulator {

    public static void main (String[] args) {

        final Thread thread = new Thread (new Manipulator ());
        thread.start ();
    }

    private static class Manipulator implements Runnable {

        @Override
        public void run () {
            DXTickDB db = new TickDBClient ("localhost",
                                            1001);

            db.open (false);

            Stack<DXTickStream> stack = new Stack<DXTickStream> ();
            StreamOptions options = StreamOptions.fixedType (StreamScope.DURABLE,
                                                             null,
                                                             null,
                                                             0,
                                                             StreamConfigurationHelper.mkEventMessageDescriptor ());
            for (;;) {
                try {
                    if (Thread.interrupted ()) {
                        throw new InterruptedException ();
                    }

                    for (int i = 0; i < 10; i++) {
                        final DXTickStream stream = db.createStream ("Stream" + UUID.randomUUID (),
                                                                     options);
                        stack.push (stream);
                    }

                    synchronized (Manipulator.this) {
                        Manipulator.this.wait (10000L);
                    }

                    while (stack.size () > 0) {
                        final DXTickStream stream = stack.pop ();
                        stream.delete ();
                    }

                } catch (final InterruptedException e) {

                } finally {
                    while (stack.size () > 0) {
                        final DXTickStream stream = stack.pop ();
                        stream.delete ();
                    }
                }

            }
        }

    }
}
