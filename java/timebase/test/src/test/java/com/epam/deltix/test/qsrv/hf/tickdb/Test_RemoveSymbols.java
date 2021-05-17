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
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;

import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.InstrumentKey;
import com.epam.deltix.util.lang.Util;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.GregorianCalendar;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_RemoveSymbols extends TDBRunnerBase {

    private final SelectionOptions options = new SelectionOptions(true, false);

    private DXTickStream        createTestStream(DXTickDB db, String name) {

        RecordClassDescriptor classDescriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null, "DECIMAL(4)", "DECIMAL(0)"
        );

        DXTickStream stream = db.createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, null, 0, classDescriptor));

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2012, 1, 1), (int) BarMessage.BAR_MINUTE, 100,
                "MSFT", "AAPL", "ORCL", "IBM");

        TickLoader loader = stream.createLoader();
        while (gn.next())
            loader.send(gn.getMessage());
        loader.close();

        return stream;
    }

    @Test
    public void Test1() {

        DXTickDB tdb = getServerDb();

        DXTickStream stream = createTestStream (tdb, "bars1");
        stream.clear(new InstrumentKey("ORCL"));

        assertEquals(3, stream.listEntities().length);

        try (TickCursor tickCursor = TickCursorFactory.create(stream, 0, options, "ORCL")){
            boolean hasNext = tickCursor.next();

            assertTrue(!hasNext);
        }

//        tdb.close();
//
//        tdb.open(false);
//
//        stream = tdb.getStream("bars1");
//
//        assertEquals(3, stream.listEntities().length);
//
//        try (TickCursor tickCursor = TickCursorFactory.create(stream, 0, options, InstrumentType.EQUITY, "ORCL")){
//            boolean hasNext = tickCursor.next();
//
//            assertTrue(!hasNext);
//        }
    }

    @Test
    public void Test3() {

        DXTickDB tdb = getServerDb();

        RecordClassDescriptor rd = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "1", 9, FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);
        DXTickStream stream = tdb.createStream("bars3",
                StreamOptions.fixedType(StreamScope.DURABLE, "bars3", null, 1, rd));

          TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(2009, 1, 1), (int) BarMessage.BAR_MINUTE, 1000,
                "AAPL", "MSFT", "ORCL");

        TickLoader      loader = stream.createLoader ();

        try {
             while (gn.next())
                loader.send(gn.getMessage());                        
        } finally {
            Util.close(loader);
        }
        
        stream.clear();
        assertEquals(0, stream.listEntities().length);

        TickCursor tickCursor = null;

        try {
            tickCursor = TickCursorFactory.create(stream, 0, options, "ORCL");
            assertTrue(!tickCursor.next());
        } finally {
            Util.close(tickCursor);
        }
    }

    @Test
    public void Test2() {

        DXTickDB tdb = getServerDb();

        DXTickStream stream = createTestStream(tdb, "bars2");

        TickCursor tickCursor = null;

        try {
            tickCursor = TickCursorFactory.create(stream, 0, options, "ORCL");

            for (int i = 0; i < 10; i++) {
                tickCursor.next();
            }
            stream.clear(new InstrumentKey("ORCL"));

            while (tickCursor.next());
        } catch (CursorException ex) {
          // valid case
        } finally {
            Util.close(tickCursor);
        }

        assertEquals(3, stream.listEntities().length);
    }
}
