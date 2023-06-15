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
package com.epam.deltix.test.qsrv.hf.tickdb;


import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;

import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.lang.Util;
import org.junit.*;
import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;
import java.text.ParseException;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Test correct behavior of live stream truncation.
 */
@Category(TickDBFast.class)
public class Test_Truncate {
    protected DXTickDB         db;
    protected DXTickStream     stream;
    
    @Before
    public void                     setup () {
        db = TickDBCreator.createTickDB(TDBRunner.getTemporaryLocation(), false);
        stream = db.getStream (TickDBCreator.BARS_STREAM_KEY);
    }

    @After
    public void         tearDown () throws InterruptedException {
        db.close();
        db.delete ();
    }

    @Test
    public void                     chopAppleSequential () {

        TickCursor          cur1 = TickCursorFactory.create(
                stream, 0, new SelectionOptions(false, false, false, SelectionOptions.ABORT_CURSOR),
                 "AAPL");

//        TickCursor          cur1 = stream.select(0, FeedFilter.createEquitiesFilter("AAPL"),
//                new SelectionOptions(false, false, false, SelectionOptions.ABORT_CURSOR));

        TickCursor cur2 = TickCursorFactory.create(stream, 0);
        BarMessage msg;
        
        for (int ii = 0; ii < 500; ii++)
            cur1.next ();        
        
        //  Save bar number 500 to later write it back, thus truncating the stream
        msg = (BarMessage) cur1.getMessage ().clone();
        
        //  Now start reading all, just to test multiplexing
        cur1.subscribeToAllEntities();

        for (int ii = 0; ii < 500; ii++) {
            cur1.next ();
        }

        //IdentityKey key = new ConstantIdentityKey(msg.getInstrumentType(), msg.getSymbol());
        stream.truncate(msg.getTimeStampMs(), msg);

        int count = 0;
        try {
            while (cur1.next()) {
                count++;
            }

            //assertTrue ("StreamTruncatedException not thrown", false);
        } catch (StreamTruncatedException x) {
            // the only acceptable outcome
        }
        
        //  cur2 must work.
        for (int ii = 0; ii < 10000; ii++) 
            cur2.next ();
        
        cur1.close ();
        cur2.close ();
    }

    @Test(timeout = 30000)
    public void                     chopAppleSequentialShift () {
        TickCursor          cur1 = TickCursorFactory.create(stream, 0, "AAPL");
        TickCursor          cur2 = TickCursorFactory.create(stream, 0);
        BarMessage msg;

        for (int ii = 0; ii < 500; ii++)
            cur1.next ();

        //  Save bar number 500 to later write it back, thus truncating the stream
        msg = (BarMessage) cur1.getMessage ().clone();

        //  Now start reading all, just to test multiplexing
        cur1.subscribeToAllEntities();
        //cur1.setFilter (FeedFilter.createUnrestricted ());

        for (int ii = 0; ii < 500; ii++)
            cur1.next ();

        //  Open a loader and write hback the saved APPL bar
        TickLoader          loader = stream.createLoader ();

        loader.send (msg);
        //  Should not even have to save changes!

        for (int ii = 0; ii < 1000; ii++)
            cur1.next ();

        loader.close ();

        //  cur2 must work.
        for (int ii = 0; ii < 10000; ii++)
            cur2.next ();
    
        cur1.close ();
        cur2.close ();
    }

    private class AppleEater extends Thread {
        final Object            signal = new Object ();
        boolean                 ready = false;
        boolean                 gotException = false;
        int                     count;

        public AppleEater () {
            super ("Apple Eater");
        }

        @Override
        public void     run () {
            long []         range = 
                stream.getTimeRange (new ConstantIdentityKey ("AAPL"));

            //  Open a LIVE cursor that is guaranteed to immediately go into a wait
            TickCursor          cur = TickCursorFactory.create(
                    stream, range [1] + 1,
                    new SelectionOptions (true, true, false, SelectionOptions.ABORT_CURSOR), "AAPL");


            synchronized (signal) {
                ready = true;
                signal.notify ();
            }

            try {
                cur.next();
            } catch (StreamTruncatedException x) {
                // the only acceptable outcome
                gotException = true;
            } finally {
                cur.close ();
            }
        }
    }

    @Ignore("StreamTruncatedException is deprecated")
    public void                     chopAppleConcurrent () 
        throws InterruptedException
    {
        AppleEater          t = new AppleEater ();

        t.start ();

        //  In the meantime, save the 500th APPL bar
        TickCursor          cur = TickCursorFactory.create(stream, 0, "AAPL");

        BarMessage msg;

        try {
            for (int ii = 0; ii < 500; ii++)
                cur.next ();

            //  Save bar number 100 to later write it back, thus truncating the stream
            msg = (BarMessage) cur.getMessage ().clone();
            
        } finally {
            Util.close(cur);
        }

        synchronized (t.signal) {
            while (!t.ready)
                t.signal.wait ();
        }

        //  Sleep for another a second to let the cursor enter a wait.
        //  This is unfortunately not **guaranteed** to work...
        Thread.sleep (500);

        stream.truncate(msg.getTimeStampMs(), msg);

        t.join (3000);
        
        assertFalse ("Failed to join reader thread", t.isAlive ());
        assertTrue (t.gotException);
    }

    private static final int NUMBER_OF_MESSAGES = 20;

    private class AppleSupplier extends Thread {

        public AppleSupplier() {
            super ("Apple Supplier");
        }

        @Override
        public void     run () {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TickCursor cur = TickCursorFactory.create(stream, 0, "AAPL");

            //  append 20 messages
            TickLoader          loader = stream.createLoader ();
            final long ONE_YEAR = TimeUnit.DAYS.toMillis(365);  

            try {
                for (int i = 0; i < NUMBER_OF_MESSAGES && cur.next(); i++) {
                    BarMessage msg = (BarMessage) cur.getMessage();
                    // add 1 year
                    msg.setTimeStampMs(msg.getTimeStampMs() + ONE_YEAR);
                    loader.send(msg);
                }
            } finally {
                cur.close();
                loader.close();
            }
        }
    }

    @Test(timeout = 10000)
    public void                     chopAppleConcurrentShift ()
        throws InterruptedException, ParseException
    {
        AppleSupplier t;
        int count;
        TickCursor          cur = null;
        try {
             cur = stream.select(GMT.parseDate("2005-03-31").getTime(),
                        new SelectionOptions(false, true, false));

            // 1. start live cursor
            for (int ii = 0; ii < 500; ii++)
                cur.next ();

            // 2. truncate it in the current position.
            BarMessage msg = (BarMessage) cur.getMessage ().clone();
            TickLoader          loader = stream.createLoader ();
            loader.send (msg);
            loader.close ();

            // 3. start another thread, which supply data to the end of the stream
            t = new AppleSupplier();
            t.start();

            final long ts2006 = GMT.parseDate("2006-01-01").getTime();
            count = 0;

            // scroll up till we found 20 appended messages
            for (;;) {
                cur.next ();
                //System.out.println(cur.getMessage());
                if (cur.getMessage().getTimeStampMs() > ts2006) {
                    count++;
                    if (count >= NUMBER_OF_MESSAGES)
                        break;
                }
            }
        } finally {
            Util.close(cur);
        }

        assertEquals(NUMBER_OF_MESSAGES, count);

        t.join(); // we should wait for closed loaders 
    }
}