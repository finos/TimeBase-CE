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
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.TimeStampedMessage;;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.util.lang.Util;
import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
/** 
 *  Test that we can independently load information into the same stream from
 *  multiple loaders, as long as loaders do not overlap on symbols.
 */
public class Test_MultiLoadersOneStream {
    public static final int         NUM_LOADERS = 100;
    public static final int         NUM_MSGS_PER_SYMBOL = 10000;
    //public static final int         SAVE_INTERVAL = NUM_MSGS_PER_SYMBOL / 10;

    private static final String     STREAM_KEY = "Test Stream";
    private static final String     STREAM_NAME = "Test Name";

    private static final String []  symbols = new String [NUM_LOADERS];

    private File                    dbFile = new File (TDBRunner.getTemporaryLocation());
    private DXTickDB                db;
    private DXTickStream            stream;
    

    static {
        for (int ii = 0; ii < NUM_LOADERS; ii++)
            symbols [ii] = "DLX" + ('A' + ii);  // Deltix' subsidiaries :)
    }

    @Before
    public void         createDB () {
        db = TickDBFactory.create (dbFile);
        
        db.format ();
        
        stream = 
            db.createStream (
                STREAM_KEY, 
                STREAM_NAME, 
                "Test Description",
                0
            );
        
        StreamConfigurationHelper.setTradeNoExchNoCur (stream);
    }

    @After
    public void         removeDB () {
        db.close();
//        if (db != null)
//            db.delete ();
    }

    private TradeMessage createMessage (String symbol) {
        TradeMessage trade = new TradeMessage();

        trade.setSymbol(symbol);
        
        return (trade);
    }

    private void        doLoad (TickLoader loader, TradeMessage trade, int tag) {
        trade.setSize(tag);
        trade.setPrice(tag);
        trade.setTimeStampMs(TimeStampedMessage.TIMESTAMP_UNKNOWN); //System.currentTimeMillis ();

        loader.send (trade);
    }

    private void        checkLoad () {
        for (String symbol : symbols) {

            try (TickCursor          cur = stream.select (0, null, null,
                    new IdentityKey[] { new ConstantIdentityKey(symbol) }
            ))
            {

                for (int ii = 0; ii < NUM_MSGS_PER_SYMBOL; ii++) {
                    assertTrue(cur.next());

                    TradeMessage msg = (TradeMessage) cur.getMessage();

                    assertTrue(Util.equals(symbol, msg.getSymbol()));
                    assertEquals(symbol, ii, (int) msg.getSize());
                    assertEquals(ii, (int) msg.getPrice());
                }

            }
        }
    }
    
    @Test
    public void         syncLoad () {
        TickLoader []       loaders = new TickLoader [NUM_LOADERS];
        TradeMessage msg = createMessage (null);
        
        for (int ii = 0; ii < NUM_LOADERS; ii++) 
            loaders [ii] = stream.createLoader ();        

        for (int ii = 0; ii < NUM_MSGS_PER_SYMBOL; ii++) {
            for (int n = 0; n < NUM_LOADERS; n++) {
                msg.setSymbol(symbols [n]);
                
                doLoad (loaders [n], msg, ii);
            }
        }        
           
        for (TickLoader loader : loaders)
            loader.close ();

        checkLoad ();
    }

    class LoaderThread extends Thread {
        private final TradeMessage msg;

        public LoaderThread (String symbol) {
            super ("Loader for " + symbol);
            msg = createMessage (symbol);
        }

        @Override
        public void         run () {
            try (TickLoader loader = stream.createLoader()) {
                for (int ii = 0; ii < NUM_MSGS_PER_SYMBOL; ii++) {
                    doLoad(loader, msg, ii);
                }
            } catch (Throwable ex) {
                ex.printStackTrace(System.out);
            }

        }
    }

    @Test
    public void         asyncLoad () throws InterruptedException {
        LoaderThread []       threads = new LoaderThread [NUM_LOADERS];

        for (int ii = 0; ii < NUM_LOADERS; ii++)
            threads [ii] = new LoaderThread (symbols [ii]);

        for (LoaderThread t : threads)
            t.start ();

        for (LoaderThread t : threads)
            t.join ();

        checkLoad ();
    }    
}