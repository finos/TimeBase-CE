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
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.progress.ConsoleProgressIndicator;
import com.epam.deltix.util.time.TimeKeeper;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_RenameInstruments {

    private static TDBRunner runner;
    private static DXTickDB db;
    private final static boolean REMOTE = true;

    @BeforeClass
    public static void init() throws Throwable {
        runner = new TDBRunner(REMOTE, true, Home.getPath("temp/renameInstruments/timebase"), new TomcatServer());
        runner.startup();
        db = runner.getTickDb();
    }

    @AfterClass
    public static void uninit() throws Exception {
        runner.shutdown();
    }

    private void waitAndDrawProgressBar(DXTickStream stream) {
        ConsoleProgressIndicator cpi = new ConsoleProgressIndicator();
        cpi.setTotalWork(1.0);
        boolean complete = false;
        while (!complete) {
            BackgroundProcessInfo process = stream.getBackgroundProcess();
            complete = (process != null && process.isFinished());
            cpi.setWorkDone(process == null ? 0 : process.progress);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
    }

    @Test
    public void testDFMaxRename() throws Exception {
        DXTickStream stream = makeSimpleStream(db, "streamDFMax", 0);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("AAA"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("EEE"),
            new ConstantIdentityKey("FFF"),
        };

        checkRenameInstrument(stream, from, to);
    }

    @Test
    public void testDFMaxRenameAndSelect() throws Throwable {
        DXTickStream stream = makeSimpleStream(db, "streamDFMax3", 0);
        writeTestData1(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("AAA"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
        };

        checkRenameInstrument(stream, from, to);

        System.out.println("--------------------------------------");
        writeTestData2(stream);
        printEntities(stream);
        assert printStreamContent(stream) == 6;
    }

    @Test
    public void testDFMaxRenameForManyInstruments() throws Exception {
        DXTickStream stream = makeSimpleStream(db, "streamDFMaxManInstrument", 0);
        writeManyTestData(stream);

        List<IdentityKey> fromList = new ArrayList<>();
        for (int i = 0; i < 300; ++i) {
            fromList.add(new ConstantIdentityKey("AAA" + i));
        }
        for (int i = 0; i < 300; ++i) {
            fromList.add(new ConstantIdentityKey("BBB" + i));
        }

        List<IdentityKey> toList = new ArrayList<>();
        for (int i = 0; i < 600; ++i) {
            toList.add(new ConstantIdentityKey("KKK_" + i));
        }

        IdentityKey[] from = fromList.toArray(new IdentityKey[fromList.size()]);
        IdentityKey[] to = toList.toArray(new IdentityKey[toList.size()]);

        IdentityKey[] before = stream.listEntities();
        final long ts = TimeKeeper.currentTime;
        stream.renameInstruments(from, to);

        if (stream.getFormatVersion() < 5) {
            waitAndDrawProgressBar(stream);
        }
        System.out.println("total time (ms): " + (TimeKeeper.currentTime - ts));
        IdentityKey[] after = stream.listEntities();

        checkRenameInstrument(before, after, from, to);
    }

    @Test
    public void testDFNotMaxRename() throws Exception {
        DXTickStream stream = makeSimpleStream(db, "streamDFNotMax", 2);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("AAA"),
            new ConstantIdentityKey("CCC"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("EEE"),
            new ConstantIdentityKey("FFF"),
            new ConstantIdentityKey("GGG"),
        };

        checkRenameInstrument(stream, from, to);
    }

    //@Test
    public void testRenameWithTBShutdown() throws Throwable {
        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("AAA"),
            new ConstantIdentityKey("CCC"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("EEE"),
            new ConstantIdentityKey("FFF"),
            new ConstantIdentityKey("GGG"),
        };

        checkRenameWithShutdown(Home.getPath("temp/renameInstruments2/timebase"), "Stream121", from, to);
    }

    @Test
    public void testInstrumentExistsException() throws Throwable {
        DXTickStream stream = makeSimpleStream(db, "instrExc1", 0);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("AAA"),
            new ConstantIdentityKey("CCC"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("EEE"),
            new ConstantIdentityKey("DDD"),
            new ConstantIdentityKey("FFF"),
        };

        try {
            stream.renameInstruments(from, to);
            assert false;
        } catch (Exception e) {
        }
    }

    @Test
    public void testInstrumentDuplicateException() throws Throwable {
        DXTickStream stream = makeSimpleStream(db, "instrExc2", 0);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("AAA"),
            new ConstantIdentityKey("CCC"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("MMM"),
            new ConstantIdentityKey("DDD"),
            new ConstantIdentityKey("MMM"),
        };

        try {
            stream.renameInstruments(from, to);
            assert false;
        } catch (Exception e) {
        }
    }

    @Test
    public void testInstrumentNullException() throws Throwable {
        DXTickStream stream = makeSimpleStream(db, "instrExc3", 0);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("AAA"),
            new ConstantIdentityKey("CCC"),
        };

        try {
            stream.renameInstruments(from, null);
            assert false;
        } catch (Exception e) {
        }
    }

    @Test
    public void testInstrumentSizeMismatchException() throws Throwable {
        DXTickStream stream = makeSimpleStream(db, "instrExc4", 0);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("CCC"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("MMM"),
            new ConstantIdentityKey("MMM"),
        };

        try {
            stream.renameInstruments(from, to);
            assert false;
        } catch (Exception e) {
        }
    }

    @Test
    public void testInstrumentDuplicateRename() throws Throwable {
        DXTickStream stream = makeSimpleStream(db, "instrExc5", 0);
        writeTestData(stream);

        IdentityKey[] from = new IdentityKey[] {
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("BBB"),
            new ConstantIdentityKey("CCC"),
        };

        IdentityKey[] to = new IdentityKey[] {
            new ConstantIdentityKey("MMM"),
            new ConstantIdentityKey("MMM1"),
            new ConstantIdentityKey("MMM2"),
        };

        try {
            stream.renameInstruments(from, to);
            assert false;
        } catch (Exception e) {
        }
    }

    private void checkRenameInstrument(DXTickStream stream, IdentityKey[] from, IdentityKey[] to) {
        System.out.println("Stream: " + stream.getKey());

        System.out.println("Rename pattern:");
        for (int i = 0; i < from.length; ++i)
            System.out.println("\t" + from[i] + " -> " + to[i]);

        System.out.println("Before rename: ");
        printEntities(stream);
        printStreamContent(stream);

        IdentityKey[] before = stream.listEntities();
        stream.renameInstruments(from, to);
        if (stream.getFormatVersion() < 5) {
            waitAndDrawProgressBar(stream);
        }
        IdentityKey[] after = stream.listEntities();

        System.out.println("After rename");
        printEntities(stream);
        printStreamContent(stream);

        checkRenameInstrument(before, after, from, to);
    }

    private void checkRenameWithShutdown(String tdbPath, String streamName,
                                         IdentityKey[] from, IdentityKey[] to) throws Throwable {
        //create timebase and create stream
        IOUtil.removeRecursive(new File(tdbPath));
        TDBRunner tdbRunner = new TDBRunner(REMOTE, true, tdbPath, new TomcatServer());
        tdbRunner.startup();
        DXTickStream stream = makeSimpleStream(tdbRunner.getTickDb(), streamName, 2);
        writeTestData(stream);

        System.out.println("Stream: " + stream.getKey());
        System.out.println("Rename pattern:");
        for (int i = 0; i < from.length; ++i)
            System.out.println("\t" + from[i] + " -> " + to[i]);
        System.out.println("Before rename: ");
        printEntities(stream);
        printStreamContent(stream);

        IdentityKey[] before = stream.listEntities();
        stream.renameInstruments(from, to);
        if (stream.getFormatVersion() < 5) {
            waitAndDrawProgressBar(stream);
        }

        //restart timebase
        tdbRunner.shutdown();
        Thread.sleep(3000);

        tdbRunner = new TDBRunner(REMOTE, false, tdbPath, new TomcatServer());
        tdbRunner.startup();
        stream = tdbRunner.getTickDb().getStream(streamName);

        IdentityKey[] after = stream.listEntities();
        System.out.println("After rename");
        printEntities(stream);
        printStreamContent(stream);

        checkRenameInstrument(before, after, from, to);

        tdbRunner.shutdown();
    }

    private void checkRenameInstrument(IdentityKey[] before, IdentityKey[] after,
                                       IdentityKey[] from, IdentityKey[] to) {
        for (int i = 0; i < before.length; ++i) {
            for (int j = 0; j < from.length; ++j) {
                if (before[i].equals(from[j])) {
                    boolean found = false;
                    for (int k = 0; k < after.length; ++k) {
                        if (after[k].equals(to[j])) {
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                        assert false
                            : "Error rename. Can't find renamed value for " + from[j];
                }
            }
        }
    }

    private int printStreamContent(DXTickStream stream) {
        TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(false, false));
        cursor.reset(Long.MIN_VALUE);
        int count = 0;
        while (cursor.next()) {
            System.out.println("MSG: " + cursor.getMessage());
            ++count;
        }
        return count;
    }

    private void printEntities(DXTickStream stream) {
        IdentityKey[] entities = stream.listEntities();
        for (IdentityKey id : entities) {
            System.out.println(id);
        }
    }

    private static DXTickStream makeSimpleStream(DXTickDB db, String name, int df) {
        StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, name, "", df);
        streamOptions.setFixedType(StreamConfigurationHelper.mkUniversalTradeMessageDescriptor());
        return db.createStream(name, streamOptions);
    }

    private static void writeTestData(DXTickStream stream) {
        TickLoader loader = stream.createLoader(new LoadingOptions(false));

        TradeMessage msg = new TradeMessage();

        msg.setSymbol("AAA");
        msg.setTimeStampMs(1000);
        loader.send(msg);
        msg.setTimeStampMs(1001);
        loader.send(msg);
        msg.setTimeStampMs(1002);
        loader.send(msg);

        msg.setSymbol("BBB");
        msg.setTimeStampMs(1003);
        loader.send(msg);
        msg.setTimeStampMs(1004);
        loader.send(msg);
        msg.setTimeStampMs(1005);
        loader.send(msg);

        msg.setSymbol("CCC");
        msg.setTimeStampMs(1006);
        loader.send(msg);
        msg.setTimeStampMs(1007);
        loader.send(msg);
        msg.setTimeStampMs(1008);
        loader.send(msg);

        msg.setSymbol("DDD");
        msg.setTimeStampMs(1009);
        loader.send(msg);
        msg.setTimeStampMs(1010);
        loader.send(msg);
        msg.setTimeStampMs(1011);
        loader.send(msg);

        loader.close();
    }

    private static void writeTestData1(DXTickStream stream) {
        TickLoader loader = stream.createLoader(new LoadingOptions(false));

        TradeMessage msg = new TradeMessage();

        msg.setSymbol("AAA");
        msg.setTimeStampMs(1000);
        loader.send(msg);
        msg.setTimeStampMs(1001);
        loader.send(msg);
        msg.setTimeStampMs(1002);
        loader.send(msg);

        loader.close();
    }

    private static void writeTestData2(DXTickStream stream) {
        TickLoader loader = stream.createLoader(new LoadingOptions(false));

        TradeMessage msg = new TradeMessage();

        msg.setSymbol("AAA");
        msg.setTimeStampMs(2000);
        loader.send(msg);
        msg.setTimeStampMs(2001);
        loader.send(msg);
        msg.setTimeStampMs(2002);
        loader.send(msg);

        loader.close();
    }

    private static void writeManyTestData(DXTickStream stream) {
        TickLoader loader = stream.createLoader(new LoadingOptions(false));

        TradeMessage msg = new TradeMessage();

        String symbol = "AAA";
        long curTimestamp = 1000;

        for (int i = 0; i < 3000; ++i) {
            msg.setSymbol(symbol + i);
            for (int j = 0; j < 1000; ++j) {
                msg.setTimeStampMs(curTimestamp++);
                loader.send(msg);
            }
        }

        symbol = "BBB";
        for (int i = 0; i < 3000; ++i) {
            msg.setSymbol(symbol + i);
            for (int j = 0; j < 1000; ++j) {
                msg.setTimeStampMs(curTimestamp++);
                loader.send(msg);
            }
        }

        loader.close();
    }

}