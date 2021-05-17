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
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.Util;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 
 *  Test that we can independently load information into the same stream from
 *  multiple loaders, as long as loaders do not overlap on symbols.
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_MultiStreamCursor {
    public static final int         NUM_MESSAGES = 10000;
    public static final String      SYMBOL = "DLTX";
    public static final int         TRADE_INTERVAL = 10;

    private File                    dbFile = new File (TDBRunner.getTemporaryLocation());
    
    @Before
    public void         createDB () {
        DXTickDB                db = TickDBFactory.create (dbFile);
        
        db.format ();
        
        DXTickStream            ts = db.createStream ("trades", null, null, 0);
        DXTickStream            bbos = db.createStream ("bbos", null, null, 0);

        StreamConfigurationHelper.setTradeNoExchNoCur (ts);
        StreamConfigurationHelper.setBBONoExchNoCur (bbos);

        TradeMessage trade = new TradeMessage();
        BestBidOfferMessage bbo = new BestBidOfferMessage();

        bbo.setSymbol(SYMBOL);
        trade.setSymbol(SYMBOL);

        TickLoader              tl = ts.createLoader ();
        TickLoader              bbol = bbos.createLoader ();
        
        for (int ii = 0; ii < NUM_MESSAGES; ii++) {
            if (ii % TRADE_INTERVAL == (TRADE_INTERVAL - 1)) {
                trade.setTimeStampMs(ii);
                trade.setSize(ii);
                trade.setPrice(ii);

                tl.send (trade);
            }
            else {
                bbo.setTimeStampMs(ii);
                bbo.setBidPrice(ii);
                bbo.setBidSize(ii);
                bbo.setOfferPrice(ii);
                bbo.setOfferSize(ii);

                bbol.send (bbo);
            }
        }

        tl.close ();
        bbol.close ();
        
        db.close ();
    }

    //@After
    public void         removeDB () {
        TickDBFactory.create (dbFile).delete ();
    }   

    private void        checkLoad (DXTickDB db) {
        TickCursor          cur = 
            db.select (0, null, db.listStreams ());

        for (int ii = 0; ii < NUM_MESSAGES; ii++) {
            assertTrue ("Failed to get message #" + ii, cur.next ());

            InstrumentMessage msg = cur.getMessage ();

            assertEquals (ii, msg.getTimeStampMs());
            assertTrue (Util.equals (SYMBOL, msg.getSymbol()));

            if (ii % TRADE_INTERVAL == (TRADE_INTERVAL - 1)) {
                TradeMessage trade = (TradeMessage) msg;

                assertEquals (ii, (int) trade.getSize());
                assertEquals (ii, (int) trade.getPrice());
            }
            else {
                BestBidOfferMessage bbo = (BestBidOfferMessage) msg;

                assertEquals (ii, (int) bbo.getBidPrice());
                assertEquals (ii, (int) bbo.getBidSize());
                assertEquals (ii, (int) bbo.getOfferPrice());
                assertEquals (ii, (int) bbo.getOfferSize());
            }
        }

        cur.close ();
    }
    
    @Test
    public void         local () {
        DXTickDB            db = TickDBFactory.create (dbFile);

        db.open (true);
        checkLoad (db);
        db.close ();
    }

    @Test
    public void         remote () throws InterruptedException {
        DXTickDB            db = TickDBFactory.create (dbFile);

        db.open (true);

        try {
            TickDBServer        server = new TickDBServer (0, db);

            server.start ();
            
            DXTickDB            conn = new TickDBClient ("localhost", server.getPort ());

            conn.open (true);

            if (!Boolean.getBoolean ("quiet"))
                System.out.println ("Connected to " + conn.getId ());

            checkLoad (conn);

            conn.close ();
            server.shutdown (true);
        } finally {
            db.close ();
        }
    }
}
