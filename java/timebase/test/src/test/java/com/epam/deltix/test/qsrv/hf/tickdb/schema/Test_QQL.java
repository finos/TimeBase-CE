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
package com.epam.deltix.test.qsrv.hf.tickdb.schema;

/*  ##TICKDB.QQL## */

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.test.qsrv.hf.tickdb.TDBRunnerBase;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBQQL;

@Category(TickDBQQL.class)
public class Test_QQL extends TDBRunnerBase {

    final static String CREATE_STATEMENT = "CREATE DURABLE STREAM \"bbo\" 'bbo' (\n" +
            "    CLASS \"deltix.qsrv.hf.pub.MarketMessage\" (\n" +
            "        STATIC \"originalTimestamp\" 'Original Time' TIMESTAMP = NULL,\n" +
            "        \"currencyCode\" 'Currency Code' INTEGER SIGNED (16),\n" +
            "        \"sequenceNumber\" 'Sequence Number' INTEGER\n" +
            "    )\n" +
            "        NOT INSTANTIABLE;\n" +
            "    CLASS \"deltix.qsrv.hf.pub.BestBidOfferMessage\" UNDER \"deltix.qsrv.hf.pub.MarketMessage\" (\n" +
            "        STATIC \"isNational\" 'National BBO' BOOLEAN = true,\n" +
            "        \"bidPrice\" 'Bid Price' FLOAT DECIMAL,\n" +
            "        \"bidSize\" 'Bid Size' FLOAT DECIMAL,\n" +
            "        \"bidExchange\" 'Bid Exchange' VARCHAR ALPHANUMERIC (10),\n" +
            "        \"offerPrice\" 'Offer Price' FLOAT DECIMAL,\n" +
            "        \"offerSize\" 'Offer Size' FLOAT DECIMAL,\n" +
            "        \"offerExchange\" 'Offer Exchange' VARCHAR ALPHANUMERIC (10)\n" +
            "    );\n" +
            ")\n" +
            "OPTIONS (FIXEDTYPE; DF = 12; HIGHAVAILABILITY = FALSE)\n" +
            "COMMENT 'bbo'";

    final static String MODIFY_STATEMENT =  "MODIFY STREAM \"bbo\" (\n" +
            "    CLASS \"deltix.qsrv.hf.pub.MarketMessage\" (\n" +
            "        STATIC \"originalTimestamp\" 'Original Time' TIMESTAMP = NULL,\n" +
            "        \"currencyCode\" 'Currency Code' INTEGER SIGNED (16),\n" +
            "        \"sequenceNumber\" 'Sequence Number' INTEGER\n" +
            "    )\n" +
            "        NOT INSTANTIABLE;\n" +
            "    CLASS \"deltix.qsrv.hf.pub.BestBidOfferMessage\" UNDER \"deltix.qsrv.hf.pub.MarketMessage\" (\n" +
            "        STATIC \"isNational\" 'National BBO' BOOLEAN = true,\n" +
            "        \"bidPrice\" 'Bid Price' FLOAT DECIMAL,\n" +
            "        \"bidSize\" 'Bid Size' FLOAT DECIMAL,\n" +
            "        \"bidExchange\" 'Bid Exchange' VARCHAR ALPHANUMERIC (10),\n" +
            "        \"offerPrice\" 'Offer Price' FLOAT DECIMAL,\n" +
            "        \"offerSize\" 'Offer Size' FLOAT DECIMAL,\n" +
            "        \"offerExchange\" 'Offer Exchange' VARCHAR ALPHANUMERIC (10)\n" +
            "    );\n" +
            ")\n" +
            "OPTIONS (FIXEDTYPE; DF = 132; HIGHAVAILABILITY = FALSE)\n" +
            "COMMENT 'bbo' CONFIRM DROP_DATA";


    @BeforeClass
    public static void start() throws Throwable {
        runner = new TDBRunner(true, true, new TomcatServer());
        runner.startup();
    }

    @Test
    public void testLocal() throws InterruptedException {
        test(getServerDb());
    }

    @Test
    public void testRemote() throws InterruptedException {
        test(getTickDb());
    }

    private BackgroundProcessInfo waitForExecution(DXTickStream stream) throws InterruptedException {

        boolean complete = false;
        while (!complete) {
            BackgroundProcessInfo process = stream.getBackgroundProcess();
            complete = process != null && process.isFinished();
            Thread.sleep(100);
        }

        return stream.getBackgroundProcess();
    }

    public void test(DXTickDB db) throws InterruptedException {

        try( InstrumentMessageSource source = db.executeQuery(CREATE_STATEMENT, new SelectionOptions(true, false)) ) {
            assertTrue(source.next());

            InstrumentMessage message = source.getMessage();
            assertTrue(message.toString(), message.toString().contains("SUCCESS"));
        }

        DXTickStream stream = db.getStream("bbo");

        assertEquals("bbo", stream.getName());

        try ( InstrumentMessageSource source = db.executeQuery(MODIFY_STATEMENT, new SelectionOptions(true, false)) ) {
            assertTrue(source.next());

            InstrumentMessage message = source.getMessage();
            assertTrue(message.toString(), message.toString().contains("SUCCESS"));
        }

        // execution runs in background - wait for it
        waitForExecution(stream);

        assertEquals(null, stream.getName());
        stream.delete();
    }
}
