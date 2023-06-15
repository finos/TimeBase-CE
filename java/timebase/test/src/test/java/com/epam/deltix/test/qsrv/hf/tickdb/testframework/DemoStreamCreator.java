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
package com.epam.deltix.test.qsrv.hf.tickdb.testframework;


import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BestBidOfferMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.io.Home;
import java.io.File;

/**
 *  Creates the stream used in the QQL tutorial.
 */
public class DemoStreamCreator {
    public static final long                T = 1318872100000L;
    public static final String              STREAM_KEY = "tickquerydemo";
    
    public static final DataType            PRICE_TYPE =
        new FloatDataType ("DECIMAL(2)", true);
    
    public static final DataType            SIZE_TYPE =
        new FloatDataType ("DECIMAL(0)", true);
    
    public static RecordClassDescriptor     BBO_RCD =
        new RecordClassDescriptor (
            BestBidOfferMessage.class.getName (),
            "Quote Message",
            false,
            null,
            new NonStaticDataField ("offerPrice", "Offer Price", PRICE_TYPE), 
            new NonStaticDataField ("offerSize", "Offer Size", SIZE_TYPE),
            new NonStaticDataField ("bidPrice", "Bid Price", PRICE_TYPE, "offerPrice"),
            new NonStaticDataField ("bidSize", "Bid Size", SIZE_TYPE)
        );
    
    public static RecordClassDescriptor     TRADE_RCD =
        new RecordClassDescriptor (
            TradeMessage.class.getName (),
            "Trade Message",
            false,
            null,
            new NonStaticDataField ("price", "Trade Price", PRICE_TYPE), 
            new NonStaticDataField ("size", "Trade Size", SIZE_TYPE)
        );
    
    private final DXTickDB                  db;
    private DXTickStream                    stream;

    public DemoStreamCreator (DXTickDB db) {
        this.db = db;
    }    
    
    public void                             createStream () {
        stream = db.getStream (STREAM_KEY);
        
        if (stream != null)
            stream.delete ();
        
        StreamOptions       options = 
            new StreamOptions (StreamScope.DURABLE, STREAM_KEY, null, 1);
        
        options.setPolymorphic (BBO_RCD, TRADE_RCD);
        
        stream = db.createStream (STREAM_KEY, options);
    }
    
    public void                             loadTestData () {
        BestBidOfferMessage bbo = new BestBidOfferMessage();
        TradeMessage trade = new TradeMessage();
        
        TickLoader              loader = stream.createLoader ();
        
        // LOAD DATA
        bbo.setTimeStampMs(T);
        bbo.setSymbol("GREATCO");
        bbo.setBidPrice(42.50);
        bbo.setBidSize(200);
        bbo.setOfferPrice(43.50);
        bbo.setOfferSize(100);
        loader.send (bbo);
        
        bbo.setTimeStampMs(T);
        bbo.setSymbol("XBANK");
        bbo.setBidPrice(301.25);
        bbo.setBidSize(800);
        bbo.setOfferPrice(301.75);
        bbo.setOfferSize(40000);
        loader.send (bbo);
        
        trade.setSymbol("XBANK");
        trade.setTimeStampMs(T + 1000);
        trade.setPrice(301.25);
        trade.setSize(800);
        loader.send (trade);
        
        bbo.setTimeStampMs(T + 2000);
        bbo.setSymbol("XBANK");
        bbo.setBidPrice(298.50);
        bbo.setBidSize(800);
        bbo.setOfferPrice(301.50);
        bbo.setOfferSize(60000);
        loader.send (bbo);
        
        bbo.setTimeStampMs(T + 3000);
        bbo.setSymbol("GREATCO");
        bbo.setBidPrice(43);
        bbo.setBidSize(400);
        bbo.setOfferPrice(45);
        bbo.setOfferSize(100);
        loader.send (bbo);
        
        bbo.setTimeStampMs(T + 3000);
        bbo.setSymbol("XBANK");
        bbo.setBidPrice(295);
        bbo.setBidSize(300);
        bbo.setOfferPrice(299.50);
        bbo.setOfferSize(40000);
        loader.send (bbo);
        
        trade.setSymbol("GREATCO");
        trade.setTimeStampMs(T + 4000);
        trade.setPrice(44);
        trade.setSize(100);
        loader.send (trade);
        
        loader.close ();
    }        
    
    public static void main (String [] args) throws Exception {
        File        f = Home.getFile ("temp/test/qdb");
        
        f.mkdirs ();
        
        DXTickDB    db = TickDBFactory.create (f);
        
        try {
            db.format ();

            DemoStreamCreator   c = new DemoStreamCreator (db);

            c.createStream ();        
            c.loadTestData ();            
        } finally {
            db.close ();
        }
    }    
}