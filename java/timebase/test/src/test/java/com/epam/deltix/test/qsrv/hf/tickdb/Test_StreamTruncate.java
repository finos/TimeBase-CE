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

/*  ##TICKDB.FAST## */

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.TestServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.time.GMT;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

/**
 * User: alex
 * Date: Nov 18, 2010
 */
@Category(Long.class)
public class Test_StreamTruncate extends TDBTestBase {

    static DataCacheOptions options = new DataCacheOptions(Integer.MAX_VALUE, 200 * 1024 * 1024);
    static {
        options.fs.withMaxFileSize(1024 * 1024).withMaxFolderSize(10);
    }

    static String location = getTemporaryLocation();

    public Test_StreamTruncate() {
        super(true, true, location, new TestServer(options, new File(location)));
    }

    protected void increment(StringBuffer symbol, int index) {
        if (symbol.charAt(index) == (int)'Z') {
            increment(symbol, index - 1);
            symbol.setCharAt(index, 'A');
        }
        else
            symbol.setCharAt(index, (char) (symbol.charAt(index) + 1));
    }

    @Test
    public void                     simpleTest() {
        DXTickStream stream = getServerDb().createStream("testme",
                StreamOptions.fixedType(StreamScope.DURABLE, "testme", null, 0,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

        Date start = null;
        StringBuffer ch = new StringBuffer("AAAAAA");
        String[] symbols = new String[1000];
        for (int i = 0; i < symbols.length; i++) {
            symbols[i] = ch.toString();
            increment(ch, 4);
        }

        for (int i = 0; i < 5; i++) {
            loadData(stream, start, symbols, 1_000_001);

            long[] range = stream.getTimeRange();
            long middle = (range[0] + range[1]) / 2;

            middle -= (middle % BarMessage.BAR_MINUTE);

            //if (i == 1) break;
            System.out.println("Truncate to the " + GMT.formatDateTimeMillis(middle));
            stream.truncate(middle);

            long[] r = stream.getTimeRange();
            assertEquals(range[0], r[0]);
            assertEquals(GMT.formatDateTimeMillis(r[1] + BarMessage.BAR_MINUTE), middle, r[1] + BarMessage.BAR_MINUTE);

            checkRange(stream);

            start = new Date(r[1] + BarMessage.BAR_MINUTE);

            getServerDb().close();
            getServerDb().open(false);
            stream = getTickDb().getStream("testme");
            r = stream.getTimeRange();

            assertEquals(range[0], r[0]);
            assertEquals(middle, r[1] + BarMessage.BAR_MINUTE);
        }
    }

    private void checkRange(DXTickStream stream) {
        long[] range = stream.getTimeRange();

        try (TickCursor cursor = stream.select(0, new SelectionOptions())) {
            assert cursor.next();
            assert cursor.getMessage().getTimeStampMs() == range[0];

            while (cursor.next());

            assertEquals(GMT.formatDateTimeMillis(range[1]) + " vs " + GMT.formatDateTimeMillis(cursor.getMessage().getTimeStampMs()),
                    range[1], cursor.getMessage().getTimeStampMs());
        }
    }

    @Test
    public void testLarge() {
        DXTickStream stream = getTickDb().createStream("large",
                StreamOptions.fixedType(StreamScope.DURABLE, "large", null, 1,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

        String[] symbols = new String[100];
        for (int i = 0; i < symbols.length; i++)
            symbols[i] = String.valueOf(i) + "00000";

        loadData(stream, null, symbols, 15_000_000);
        loadData(stream, null, symbols, 15_000_000);

        long[] range = stream.getTimeRange();

        //stream.enableVersioning();

//        long[] range = stream.getTimeRange();
//        loadData(stream, new Date((range[0] + range[1])/2), symbols);

        getServerDb().close();
        getServerDb().open(false);
        long[] range1 = getTickDb().getStream("large").getTimeRange();

        assertArrayEquals(range, range1);
    }

    @Test
    public void testPurge() {
        String name = "purge";

        DXTickStream stream = getTickDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, null, 1,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

        String[] symbols = new String[1000];
        for (int i = 0; i < symbols.length; i++)
            symbols[i] = String.valueOf(i) + "00000";

        loadData(stream, null, symbols, 15_000_000);
        long[] timeRange = stream.getTimeRange();
        long time = (timeRange[1] + timeRange[0]) / 2;
        time = time - time % 60_000; // round to minutes
        stream.purge(time);
        //stream.enableVersioning();

//        long[] range = stream.getTimeRange();
//        loadData(stream, new Date((range[0] + range[1])/2), symbols);

        getServerDb().close();
        getServerDb().open(false);
        long[] range = getTickDb().getStream(name).getTimeRange();
        assertNotNull(range);
        assertEquals(range[0], time);

    }

    public void                     dfTest() {
        DXTickStream stream = getTickDb().createStream("dftest",
                StreamOptions.fixedType(StreamScope.DURABLE, "dftest", null, 2,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

        Date start = null;
        loadData(stream, start, new String[] {"AA1", "AA2", "AA3", "AA4", "AA5", "AA6", "AA7", "AA8"}, 1_000_001);
        long[] range = stream.getTimeRange();
        loadData(stream, new Date((range[0] + range[1])/2), new String[] {"AA5"}, 1_000_001);

        IdentityKey[] ids = stream.listEntities();

        long[] times = new long[ids.length];
        for (int i = 0; i < times.length; i++) {

            TickCursor cursor = stream.select(Long.MIN_VALUE, null, null,
                    new IdentityKey[] { ids[i] });
            cursor.next();
            times[i] = cursor.getMessage().getTimeStampMs();
            if (i > 0)
                assertEquals(times[i - 1], times[i]);
            cursor.close();
        }

        for (int i = 0; i < times.length; i++)
            System.out.println(ids[i] + " = " + times[i]);
    }

    @Ignore
    public void         testReverse() {
        String name = "testReverse";
        DXTickStream stream = getTickDb().createStream(name,
                StreamOptions.fixedType(StreamScope.DURABLE, name, null, 0,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

        loadData(stream, null, new String[] {"MSFT", "ORCL"}, 1_000_001);

        try (TickCursor cursor = stream.select(Long.MAX_VALUE, new SelectionOptions(true, false, true))) {
            int count = 0;
            while (cursor.next())
                count++;

            assertEquals(10_000_001, count);
        }
    }

    private void loadData(DXTickStream stream, Date time, String[] symbols, int count) {
        GregorianCalendar c = new GregorianCalendar(2010, 0, 1);
        if (time != null)
            c.setTime(time);

        BarsGenerator g = new BarsGenerator(c, (int)BarMessage.BAR_MINUTE, count, symbols);

        TickLoader loader = stream.createLoader();
        while (g.next())
            loader.send(g.getMessage());

        loader.close();

        System.out.println("Loaded range:" + GMT.formatDateTimeMillis(time) + ": " + g.getMessage().getTimeString());
    }

    @Test
    public void testRewrite() {
        DXTickStream stream = getTickDb().createStream("testRewrite",
                StreamOptions.fixedType(StreamScope.DURABLE, "testRewrite", null, 0,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));

        for (int i = 0; i < 20; i++)
            loadData(stream, null, new String[] {"MSFT", "ORCL"}, 1_000_001);

        getServerDb().close();
        getServerDb().open(false);
        long[] range = getTickDb().getStream("testRewrite").getTimeRange();
        assertTrue(range != null);
    }

    @Test
    public void testRewrite1() throws InterruptedException {
        DXTickStream stream = getTickDb().createStream("testRewrite1",
                StreamOptions.fixedType(StreamScope.DURABLE, "testRewrite1", null, 0,
                        StreamConfigurationHelper.mkUniversalBarMessageDescriptor()));


        loadData(stream, null, new String[] {"MSFT", "ORCL"}, 10_000_001);

        long[] range = stream.getTimeRange();

        stream.truncate(Long.MIN_VALUE, new ConstantIdentityKey("MSFT"));
        stream.truncate((range[0] + range[1])/2, new ConstantIdentityKey("ORCL"));

        loadData(stream, new Date(range[1]), new String[] {"ORCL"}, 10_000_001);

        try (TickCursor cursor = stream.select(Long.MIN_VALUE, null)) {
            int count = 0;
            while (cursor.next())
                count++;
            System.out.println("Number of messages:" + count);
        }

        getServerDb().close();
        getServerDb().open(false);
        range = getTickDb().getStream("testRewrite1").getTimeRange();
        assertTrue(range != null);
    }
}
