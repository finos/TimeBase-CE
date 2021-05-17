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

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.util.MultiThreadedTest;
import com.epam.deltix.qsrv.hf.tickdb.util.TestThread;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.util.collections.*;
import com.epam.deltix.util.lang.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Tests live cursors.
 */
public abstract class LiveCursorTestBase
    extends MultiThreadedTest<LiveCursorTestBase.Reader>
{
    protected static final int        NUM_MSGS_PER_ENTITY =
        Integer.getInteger ("nm", 2000);

    protected static final int        NUM_ENTITIES =
        Integer.getInteger ("ne", 20);

    protected static final int      TIME_INCREMENT = 1000;
    protected static final String   DIST_STREAM = "Distributed";
    protected static final String   CONS_STREAM = "Consolidated";
    protected static final long     BASE_TIME = 1225000000000L; // 1/1/2000
    private static final String []  SYMBOLS = new String [NUM_ENTITIES];
    private static final int        TRADE_SIZE = 12345;
    
    static {
        for (int ii = 0; ii < NUM_ENTITIES; ii++)
            SYMBOLS [ii] = "DLX" + ii;  // Deltix' subsidiaries :)
    }

    protected final DXTickDB        localDB = TickDBFactory.create (TDBRunner.getTemporaryLocation());
    private TradeMessage generatorMessage = new TradeMessage();
    private int                     generatorCount;

    public static long              countToTime (int count) {
        return (BASE_TIME + count * TIME_INCREMENT);
    }

    public static class Reader extends TestThread {
        private final TickStream            stream;
        private IdentityKey[]        entities;
        //private final FeedFilter            filter;
        private final int                   cursorOpenDelay;
        private final int                   initialReadDelay;
        private final int                   iterativeReadDelay;
        private final int                   from;
        private final int                   to;
        private int []                      expectedCounts;
        private CharSequenceToIntegerMap    map = new CharSequenceToIntegerMap ();

        public Reader (
            TickStream      stream,
            String          name, 
            long            mask,
            int             from,
            int             to,
            int             cursorOpenDelay, 
            int             initialReadDelay, 
            int             iterativeReadDelay
        )
        {
            super (name);

            this.stream = stream;
            this.from = from;
            this.to = to;
            this.cursorOpenDelay = cursorOpenDelay;
            this.initialReadDelay = initialReadDelay;
            this.iterativeReadDelay = iterativeReadDelay;

            ArrayList<IdentityKey> ids = new ArrayList<IdentityKey>();
            //filter = FeedFilter.createUnrestrictedNoSymbols ();

            int     numEntities = 0;
            
            for (int ii = 0; ii < NUM_ENTITIES; ii++) {
                String      symbol = SYMBOLS [ii];

                if ((mask & (1L << ii)) != 0) {
                    ids.add(new ConstantIdentityKey(symbol));

                    map.put (symbol, numEntities);
                    numEntities++;
                }
            }

            if (numEntities == 0)
                throw new IllegalArgumentException (
                    "Nothing is selected; mask = " + mask + "; #E = " + NUM_ENTITIES
                );

            entities = ids.toArray(new IdentityKey[ids.size()]);
            
            expectedCounts = new int [numEntities];
            
            Arrays.fill (expectedCounts, from);
        }                

        @Override
        public void         doRun () 
            throws InterruptedException
        {
            TickCursor      cursor = null;
            
            try {            
                if (cursorOpenDelay != 0) {
                    Thread.sleep (cursorOpenDelay);
                }
                
                SelectionOptions    options = new SelectionOptions ();

                options.live = true;
                options.allowLateOutOfOrder = true;
                
                cursor = stream.select (countToTime (from), options, null, entities);
            
                if (initialReadDelay != 0) {
                    Thread.sleep (initialReadDelay);
                }

                for (;;) {
                    boolean         v = cursor.next ();

                    assertTrue ("next () = false on a live cursor???", v);

                    TradeMessage msg = (TradeMessage) cursor.getMessage ();
                    int             count = (int) msg.getPrice();

                    if (count + 1 == to)
                        break;
                    
                    CharSequence    symbol = msg.getSymbol();
                    int             idx = map.get (symbol, -1);
                    int             expCount = expectedCounts [idx];
                    
                    assertEquals ("Message loss?", expCount, count);

                    /*
                    if (delta > 0) {
                        if (!Boolean.getBoolean ("quiet"))
                            System.out.println ("Lost " + delta + " messages...");
                    }
                    */

                    assertEquals (countToTime (count), msg.getTimeStampMs());
                    
                    expectedCounts [idx] = count + 1;
                    
                    Thread.sleep (iterativeReadDelay);
                }
            } finally {
                Util.close (cursor);
            }
        }       
    }       

    protected void                  addRandomReaders (
        TickStream                      stream,
        int                             numReaders
    )
    {
        Random                          random = new Random (2009);

        // First reader reads all
        addReader (stream, -1);

        for (int ir = 1; ir < numReaders; ir++)
            addReader (stream, 1 + random.nextInt ((1 << NUM_ENTITIES) - 1));
    }

    @Before
    public void             setup () {
        localDB.format ();
        
        DXTickStream        ds =
            localDB.createStream (DIST_STREAM, null, null, 0);

        DXTickStream        cs =
            localDB.createStream (CONS_STREAM, null, null, 1);

        StreamConfigurationHelper.setTradeNoExchNoCur (ds);
        StreamConfigurationHelper.setTradeNoExchNoCur (cs);

        generatorCount = 0;
        generatorMessage.setSize(TRADE_SIZE);

        clearThreads ();
    }
    
    @After
    public void             teardown () {
        localDB.close ();
    }
    
    protected void          generateData (
        WritableTickStream      stream,
        int                     numMessages,
        long                    delay
    )
        throws InterruptedException
    {
        TickLoader              loader = stream.createLoader ();
        
        try {            
            for (int imsg = 0; imsg < numMessages; imsg++) {
                generatorMessage.setTimeStampMs(BASE_TIME + generatorCount * TIME_INCREMENT);
                generatorMessage.setPrice(generatorCount);

                for (int ient = 0; ient < NUM_ENTITIES; ient++) {
                    generatorMessage.setSymbol(SYMBOLS [ient]);
                   
                    loader.send (generatorMessage);

                    if (delay != 0)
                        Thread.sleep (delay);
                }
                
                generatorCount++;
            }
        } finally {
            Util.close (loader);
        }
    }    

    protected void          addReader (
        TickStream              stream,
        long                    mask,
        int                     from,
        int                     to,
        int                     cursorOpenDelay,
        int                     initialReadDelay,
        int                     iterativeReadDelay
    )
    {
        if (mask == 0)
            throw new IllegalArgumentException ("mask == 0");
        
        add (
            new Reader (
                stream,
                "Reader-" + getNumThreads (),
                mask,
                from,
                to,
                cursorOpenDelay,
                initialReadDelay,
                iterativeReadDelay
            )
        );
    }

    protected void          addReader (
        TickStream              stream,
        long                    mask,
        int                     from,
        int                     to
    )
    {
        addReader (stream, mask, from, to, 0, 0, 0);
    }

    protected void          addReader (
        TickStream              stream,
        long                    mask
    )
    {
        addReader (stream, mask, 0, NUM_MSGS_PER_ENTITY);
    }   
}
