package com.epam.deltix.test.qsrv.hf.tickdb;


import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;

import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.concurrent.*;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.*;
import static org.junit.Assert.*;

//import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

import java.io.File;

/**
 *  Test transient stream features.
 */
@Category(TickDBFast.class)
public class Test_NonBlockingCursor {
    public static final String      STREAM_KEY = "test.stream";
    public static final String      LOCATION = TDBRunner.getTemporaryLocation();

    private DXTickDB     db;

    @Before
    public final void           startup() throws Throwable {
        QSHome.set(new File(LOCATION).getParent());

        db = TickDBFactory.create (LOCATION);

        db.format ();
    }

    @After
    public final void           teardown () {
        db.close ();
    }

    /**
     *  Test that it's possible to query a cursor in a non-blocking way, as
     *  as that the notification is invoked.
     */
    class nbCursorTester
        extends TickDBTest
    {
        /**
         *  See {@link IntermittentlyAvailableResource} for usage.
         */
        private final Object                        myLock = new Object ();

        private final Runnable avlnr =
            new Runnable () {
                public void run () {
                    synchronized (myLock) {
                        myLock.notify ();
                    }
                }
            };

        private final TradeMessage msg = new TradeMessage();
        private TickLoader          loader;
        private TickCursor          cur;
        
        nbCursorTester () {
            msg.setSymbol("DLTX");
        }

        private void                getOneMessage () 
            throws InterruptedException
        {
            synchronized (myLock) {
                for (;;) {
                    try {
                        boolean     b = cur.next ();

                        assertTrue (b);

                        break;
                    } catch (UnavailableResourceException x) {
                        myLock.wait (1000);
                    }
                }
            }
        }

        private void                testNoData (TickCursor cur) {
            synchronized (myLock) {
                try {
                    boolean     b = cur.next ();

                    throw new AssertionError (
                        "next () returned " + b +
                        " instead of throwing UnavailableResourceException"
                    );
                } catch (UnavailableResourceException x) {
                    //  The only acceptable outcome
                }
            }
        }

        private void                sendNewMessage () {
            msg.setTimeStampMs(TimeKeeper.currentTime);
            loader.send (msg);
        }

        @Override
        public void                 run (DXTickDB db) throws Exception {
            StreamOptions               options =
                new StreamOptions (StreamScope.TRANSIENT, null, null, 1);

            options.setFixedType (StreamConfigurationHelper.mkUniversalTradeMessageDescriptor ());

            final DXTickStream      stream = db.createStream (STREAM_KEY, options);

            
            loader = stream.createLoader ();
            cur = stream.select (0, new SelectionOptions (false, true));

            cur.setAvailabilityListener (avlnr);

            for (int ii = 0; ii < 50; ii++) {
                testNoData (cur);

                Thread.sleep (ii);

                testNoData (cur);

                sendNewMessage ();

                Thread.sleep (ii);
                
                getOneMessage ();
            }

            testNoData (cur);

            cur.close ();
            loader.close ();
        }
    }    

    @Test
    public void             localTest () throws Exception {
        new nbCursorTester ().run (db);
    }

    @Test
    public void             remoteTest () throws Exception {
        new nbCursorTester ().runRemote (db);
    } 
}
