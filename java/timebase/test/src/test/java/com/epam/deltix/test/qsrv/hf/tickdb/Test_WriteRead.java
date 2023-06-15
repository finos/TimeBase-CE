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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.util.concurrent.UncheckedInterruptedException;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import java.util.Arrays;
import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

/**
 *  Tests live cursors.
 */
@Category(TickDBFast.class)
public class Test_WriteRead {
    public static final long    TEST_TIMEOUT = 10000;
    private static IdentityKey[] UNRESTRICTED_FILTER = null;
    private static final long   BASE_TIME = 946684800000L;
    private static final String []  SYMBOLS = {
        "AAPL",
        "GOOG",
        "IBM",
        "MSFT",
        "ORCL",
        "DLTX"
    };
    private static final int    TRADE_SIZE = 12345;

    private static final int    NUM_ENTITIES = SYMBOLS.length;

    private DXTickDB            db = TickDBFactory.create (TDBRunner.getTemporaryLocation());
    private DXTickStream        stream;
    private TradeMessage        generatorMessage = new TradeMessage();
    private long                generatorTime = BASE_TIME;

    private volatile Throwable  error;

    class Reader extends Thread {
        private final TickStream        stream;
        private IdentityKey[]    entities;
        private final long              startTime;
        private final int               cursorOpenDelay;
        private final int               initialReadDelay;
        private final int               iterativeReadDelay;
        private long []                 times = new long [NUM_ENTITIES];

        public Reader (
            TickStream              stream,
            String                  name,
            IdentityKey[]    entities,
            long                    startTime,
            int                     cursorOpenDelay,
            int                     initialReadDelay,
            int                     iterativeReadDelay
        )
        {
            super (name);

            this.stream = stream;
            this.startTime = startTime;
            this.entities = entities;
            this.cursorOpenDelay = cursorOpenDelay;
            this.initialReadDelay = initialReadDelay;
            this.iterativeReadDelay = iterativeReadDelay;
        }

        @Override
        public void         run () {
            Arrays.fill (times, startTime);

            TickCursor      cursor = null;

            try {
                if (cursorOpenDelay != 0)
                    Thread.sleep (cursorOpenDelay);

                SelectionOptions    options = new SelectionOptions ();

                options.live = true;
                options.allowLateOutOfOrder = true;

                cursor = stream.select (startTime, options, null, entities);

                if (initialReadDelay != 0)
                    Thread.sleep (initialReadDelay);

                while (cursor.next ()) {
                    TradeMessage msg = (TradeMessage) cursor.getMessage ();

                    //System.out.println (getName () + ": " + msg);

                    checkMessage (msg);

                    Thread.sleep (iterativeReadDelay);
                }
            } catch (InterruptedException x) {
                //
            } catch (UncheckedInterruptedException x) {
                //
            } catch (Throwable x) {
                synchronized (System.out) {
                    x.printStackTrace ();
                }

                error = x;
            } finally {
                Util.close (cursor);
            }
        }

        private void        checkMessage (TradeMessage msg) {
            int             idx = -1;
            CharSequence    symbol = msg.getSymbol();

            assertEquals (TRADE_SIZE, (int) msg.getSize());

            for (int ii = 0; ii < NUM_ENTITIES; ii++)
                if (Util.equals (symbol, SYMBOLS [ii])) {
                    idx = ii;
                    break;
                }

            assertTrue (getName () + ": Symbol " + symbol + " not found", idx >= 0);

            if (msg.getTimeStampMs() < times [idx] ||
                msg.getTimeStampMs() > times [idx] + 5000)
                throw new AssertionError (
                    getName () + ": Error in " + msg +
                    ": expected " + GMT.formatDateTimeMillis (times [idx]) +
                    "; gap=" + (msg.getTimeStampMs() - times [idx])
                );

            times [idx] = msg.getTimeStampMs() + 1000;
        }
    }

    private void            start (Thread [] threads) {
        error = null;

        for (Thread r : threads)
            r.start ();
    }

    private void            interrupt (Thread [] threads) {
        for (Thread r : threads)
            r.interrupt ();
    }

    private void            join (Thread [] threads) throws InterruptedException {
        long                    absoluteTimeout =
            System.currentTimeMillis () + TEST_TIMEOUT;

        for (Thread r : threads) {
            r.join (absoluteTimeout - System.currentTimeMillis ());

            assertFalse ("Thread " + r.getName () + " did not terminate", r.isAlive ());
        }

        if (error != null) {
            if (error instanceof Error)
                throw (Error) error;

            if (error instanceof RuntimeException)
                throw (RuntimeException) error;

            throw new RuntimeException (error);
        }
    }

    @Before
    public void             setup () {
        db.format ();

        RecordClassDescriptor rcd = StreamConfigurationHelper.mkUniversalTradeMessageDescriptor();

        stream = db.createStream("Test", StreamOptions.fixedType(StreamScope.DURABLE, "Test", null, 0, rcd));

        //StreamConfigurationHelper.setTradeNoExchNoCur (stream);

        generatorTime = BASE_TIME;
        generatorMessage.setSize(TRADE_SIZE);
    }

    @After
    public void             teardown () {
        //stream.delete ();
        db.close ();
    }

    private void            generateData (int numMessages) {
        TickLoader              loader = stream.createLoader ();

        try {
            for (int imsg = 0; imsg < numMessages; imsg++) {
                generatorMessage.setTimeStampMs(generatorTime);

                for (int ii = 0; ii < NUM_ENTITIES; ii++) {
                    generatorMessage.setSymbol(SYMBOLS [ii]);
                    generatorMessage.setPrice(imsg * 0.01 + 1);

                    loader.send (generatorMessage);
                }

                generatorTime += 1000;
            }
        } finally {
            Util.close (loader);
        }
    }

    @Test
    public void             smoke () throws InterruptedException {
        generateData (100);

        Reader []               readers = {
            new Reader (stream, "R1", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0),
            new Reader (stream, "R2", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0),
            new Reader (stream, "R3", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0),
            new Reader (stream, "R4", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0)
        };

        start (readers);

        Thread.sleep (100);

        generateData (2000);

        Thread.sleep (200);

        interrupt (readers);
        join (readers);
    }


    @Test(timeout = 120000)
    public void             rightAway () throws InterruptedException {
        Reader []               readers = {
            new Reader (stream, "R1", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0),
            new Reader (stream, "R2", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0),
            new Reader (stream, "R3", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0),
            new Reader (stream, "R4", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0)
        };
        
        start (readers);   
        
        generateData (10000);
        
        interrupt (readers);        
        join (readers);
    }

    private IdentityKey[]      mkfilter (int idx) {
        return new IdentityKey[] {
                new ConstantIdentityKey(SYMBOLS [idx])
        };
        //return (FeedFilter.createEquitiesFilter (SYMBOLS [idx]));
    }
    
    @Test
    public void             distribute () throws InterruptedException {
        Reader []               readers = {
            new Reader (stream, "R1", mkfilter (0), BASE_TIME, 0, 0, 0),
            new Reader (stream, "R2", mkfilter (1), BASE_TIME, 0, 0, 0),
            new Reader (stream, "R3", mkfilter (2), BASE_TIME, 0, 0, 0),
            new Reader (stream, "R4", UNRESTRICTED_FILTER, BASE_TIME, 0, 0, 0)
        };
        
        start (readers);   
        
        generateData (10000);
        
        interrupt (readers);        
        join (readers);
    }
    
    @Test
    public void             catchUp () throws InterruptedException {
        generateData (1000);
        
        Reader []               readers = {
            new Reader (stream, "R1", UNRESTRICTED_FILTER, BASE_TIME + 1000000, 0, 0, 0),            
        };
        
        start (readers);   
        
        Thread.sleep (100);
        
        generateData (2000);
        
        Thread.sleep (200);
        
        interrupt (readers);        
        join (readers);
    }
    
}