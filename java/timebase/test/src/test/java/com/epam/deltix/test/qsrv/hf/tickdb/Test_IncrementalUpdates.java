/*
 * Copyright 2023 EPAM Systems, Inc
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
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static junit.framework.Assert.assertEquals;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBStress;

@Category(TickDBStress.class)
public class Test_IncrementalUpdates {

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(false, true);
        runner.startup();        
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }
    
    public void testUpdate() {

        DXTickDB db = runner.getServerDb();
        DXTickStream stream = db.getStream("zzz");
        long[] range = stream.getTimeRange();

        GregorianCalendar c = new GregorianCalendar(2010, 1, 1);
        c.setTimeInMillis(range[1] - BarMessage.BAR_DAY);
        DataGenerator generator = new DataGenerator(c, 1000, "MSFT");

        for (int i = 0; i < 100; i++) {
           c.add(Calendar.HOUR, -1);
           update(stream, generator, 10000);
        }
    }

    public void testUpdate1() {

        DXTickDB db = runner.getServerDb();
        DXTickStream stream = db.getStream("zzz");
        long[] range = stream.getTimeRange();

        GregorianCalendar c = new GregorianCalendar(2000, 1, 1);
        //c.setTimeInMillis(range[1] - BarMessage.BAR_DAY);
        DataGenerator generator = new DataGenerator(c, 1000, "MSFT");

        for (int i = 0; i < 10; i++) {
           c.add(Calendar.HOUR, -1);
           update(stream, generator, 10000);
        }
    }

    @Test
    public void test() throws InterruptedException {

        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
            true, "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
            "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        DXTickDB db = runner.getServerDb();
        if (!db.isOpen())
            db.open(false);

        DXTickStream st = db.getStream("zzz");
        if (st != null)
            st.delete();

        DXTickStream stream = db.createStream("zzz",
                StreamOptions.polymorphic(StreamScope.DURABLE, "zzz", null, 1, bbo, trade));

        DataGenerator generator = new DataGenerator(new GregorianCalendar(2000, 1, 1), 1000,
                "MSFT", "ORCL");

        TickLoader loader = stream.createLoader(new LoadingOptions());

        try {
            long count = 0;
            while (generator.next() && count < 100000) {
                loader.send(generator.getMessage());
                count++;
            }
        } finally {
            Util.close(loader);
        }

        db.close();
        db.open(false);

        testUpdate();

        testRange(db.getStream("zzz"));

        testUpdate();

        testRange(db.getStream("zzz"));

        db.close();
    }

    public static void testRange(DXTickStream stream) {

        long[] range = stream.getTimeRange();

        long[] rangeAll = stream.getTimeRange(stream.listEntities());

        assertEquals(GMT.formatDateTimeMillis(range[0]) + " : " + GMT.formatDateTimeMillis(rangeAll[0]), range[0], rangeAll[0]);
        assertEquals(GMT.formatDateTimeMillis(range[1]) + " : " + GMT.formatDateTimeMillis(rangeAll[1]), range[1], rangeAll[1]);
    }

    @Test
    public void test1() throws InterruptedException {
                
        RecordClassDescriptor marketMsgDescriptor =
                StreamConfigurationHelper.mkMarketMessageDescriptor(840);

        RecordClassDescriptor bbo = StreamConfigurationHelper.mkBBOMessageDescriptor(marketMsgDescriptor,
            true, "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        RecordClassDescriptor trade = StreamConfigurationHelper.mkTradeMessageDescriptor(marketMsgDescriptor,
            "", 840, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

        DXTickDB db = runner.getServerDb();

        if (!db.isOpen())
            db.open(false);

        DXTickStream st = db.getStream("zzz");
        if (st != null)
            st.delete();

        DXTickStream stream = db.createStream("zzz",
                StreamOptions.polymorphic(StreamScope.DURABLE, "zzz", null, 1, bbo, trade));

        DataGenerator generator = new DataGenerator(new GregorianCalendar(2000, 1, 1), 1000,
                "MSFT", "ORCL");

//        TickLoader loader = stream.createLoader(new LoadingOptions());
//
//        try {
//            long count = 0;
//            while (generator.next() && count < 10) {
//                loader.send(generator.getMessage());
//                count++;
//            }
//        } finally {
//            Util.close(loader);
//        }

        testUpdate1();

        testUpdate1();

        db.close();
    }

    public void update(DXTickStream stream, DataGenerator gen, int count) {
        TickLoader loader = stream.createLoader();

        int total = 0;
        while (gen.next() && total < count) {
            //System.out.println(gen.getMessage());
            loader.send(gen.getMessage());
            total++;
        }

        loader.close();
    }
}