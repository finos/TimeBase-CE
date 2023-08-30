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
package com.epam.deltix.test.qsrv.hf.tickdb.shell;

import com.epam.deltix.data.stream.ConsumableMessageSource;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.stream.Protocol;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.testframework.TBLightweight;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.replication.*;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.tool.StreamComparer;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.service.StreamTruncatedMessage;
import com.epam.deltix.timebase.messages.service.MetaDataChangeMessage;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Interval;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.time.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_Replicator {


    private static String PRIMARY_HOME = Home.getPath("temp", "replicator1", "tickdb");
    private static String SECONDARY_HOME = Home.getPath("temp", "replicator2", "tickdb");

    @BeforeClass
    public static void before() {
        if (!new File(PRIMARY_HOME).exists())
            assertTrue(new File(PRIMARY_HOME).mkdirs());

        if (!new File(SECONDARY_HOME).exists())
            assertTrue(new File(SECONDARY_HOME).mkdirs());
    }

    @Before
    public void beforeTest() {
        assertTrue("Could not cleanup dir: " + PRIMARY_HOME, IOUtil.clearDir(new File(PRIMARY_HOME)));
        assertTrue("Could not cleanup dir: " + SECONDARY_HOME, IOUtil.clearDir(new File(SECONDARY_HOME)));
    }

    @After
    public void afterTest() {
        assertTrue("Could not cleanup dir: " + PRIMARY_HOME, IOUtil.clearDir(new File(PRIMARY_HOME)));
        assertTrue("Could not cleanup dir: " + SECONDARY_HOME, IOUtil.clearDir(new File(SECONDARY_HOME)));
    }

    @Test
    public void testTruncateLocal() throws Throwable {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        testBackupTruncate(PRIMARY_HOME);
    }

    @Test
    public void testVersions() throws IOException, InterruptedException {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        TickDBShell shell = new TickDBShell();
        shell.setConfirm(false);

        String[] lines = new String[] {
                "set db " + PRIMARY_HOME,
                "open",
                "set bkpath " + Home.getPath("testdata", "tickdb", "misc", "backup"),
                "set reload allow",
                "set srcstream " + TickDBCreator.BARS_STREAM_KEY,
                "restore restored",
                "close", ""
        };
        Reader rd = new StringReader(StringUtils.join("\r\n", lines));
        shell.runScript("", rd, false, true);
    }

    @Test
    public void testTruncateMode() throws Throwable {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        testBackupTruncate1(PRIMARY_HOME);
    }

    public void         checkVersions(String path) {
        try (DXTickDB db = TickDBFactory.create(path)) {
            db.open(true);
            checkVersions(db);
        }
    }

    public void         checkVersions(DXTickDB db) {
        DXTickStream[] streams = db.listStreams();

        for (DXTickStream stream : streams) {
            //System.out.println(stream.getKey() + ":" + stream.getDataVersion());

            SelectionOptions options = new SelectionOptions(false, false);
            options.versionTracking = true;

            long version = -1;
            try (TickCursor cursor = stream.select(Long.MIN_VALUE, options, null, new IdentityKey[] { new ConstantIdentityKey("") })) {
                while (cursor.next()) {
                    InstrumentMessage msg = cursor.getMessage();
                    if (msg instanceof StreamTruncatedMessage)
                        version = ((StreamTruncatedMessage)msg).getVersion();
                    else if (msg instanceof MetaDataChangeMessage)
                        version = ((MetaDataChangeMessage)msg).getVersion();
                }
            }

            assertEquals(version, stream.getDataVersion());
        }
    }

    @Test
    public void testTruncateRemote() throws Throwable {
        ServerRunner runner = new ServerRunner();
        try {
            runner.startup();
            TickDBCreator.createBarsStream(runner.getTickDb(), TickDBCreator.BARS_STREAM_KEY);
            testBackupTruncate("dxtick://localhost:" + runner.getPort());
        } finally {
            runner.shutdown();
        }

        checkVersions(runner.getLocation());
    }

    private void        loadTestStream(DXTickStream stream) {

        TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                new GregorianCalendar(), 100, 6000,
                "MSFT", "AAPL", "ORCL", "IBM");

        try (TickLoader loader = stream.createLoader()) {
            while (gn.next()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    break;
                }
                loader.send(gn.getMessage());
            }
        }
    }


    public void testTransientReplicate() throws Throwable {
        ServerRunner runner = new ServerRunner();
        runner.startup();

        RecordClassDescriptor classDescriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null, "DECIMAL(4)", "DECIMAL(0)"
        );

        final DXTickStream source = runner.getTickDb().createStream("transient",
                StreamOptions.fixedType(StreamScope.TRANSIENT, "transient", null, 0, classDescriptor));

        DXTickStream replica = runner.getTickDb().createStream("replica",
                StreamOptions.fixedType(StreamScope.DURABLE, "replica", null, 0, classDescriptor));

        Thread loader = new Thread() {
            @Override
            public void run() {
                loadTestStream(source);
            }
        };
        try {
            String timebase = "dxtick://localhost:" + runner.getPort();

            TickDBShell shell = new TickDBShell();
            shell.setConfirm(false);

            String[] lines = {
                    "set db " + timebase,
                    "open",
                    "set longendtime " + (System.currentTimeMillis() + 60 * 1000),
                    "set srcdb " + timebase,
                    "set srcstream transient",
                    "set cpmode live",
                    "replicate replica",
                    "close"
            };

            Reader rd = new StringReader(StringUtils.join("\r\n", lines));
            loader.start();
            shell.runScript("", rd, false, true);

            assert replica.getTimeRange() != null;

        } finally {
            loader.join();
            runner.shutdown();
        }
    }

    public void testBackupTruncate(String timebase) throws Throwable {

        testBackupRestore(timebase, true, ReloadMode.truncate);

        try (DXTickDB db = TickDBFactory.createFromUrl(timebase)) {
            db.open(false);
            DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);
//          long[] range = stream.getTimeRange();
//          long time = (range[0] + range[1]) / 2;

            long time = 1108501140000L;

            System.out.println("Truncating stream to " + time);
            stream.truncate(time);
        }

        testBackupRestore(timebase, false, ReloadMode.truncate);
    }

    public void testBackupTruncate1(String timebase) throws Throwable {
        try (DXTickDB db = TickDBFactory.createFromUrl(timebase)) {
            db.open(false);
            DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);
            stream.enableVersioning();

//        long[] range = stream.getTimeRange();
//        long time = (range[0] + range[1]) / 2;

            long time = 1108501140000L;

            System.out.println("Truncating stream to " + time);
            stream.truncate(time);
        }

        File temp = Home.getFile("temp/backup");
        assertTrue(IOUtil.clearDir(temp));

        testBackupRestore(timebase, false, ReloadMode.prohibit);
    }

    @Test
    public void testReplicateTruncate() throws Throwable {

        ServerRunner runner = new ServerRunner();
        try {
            runner.startup();

            DXTickStream bars = TickDBCreator.createBarsStream(runner.getTickDb(), TickDBCreator.BARS_STREAM_KEY);
            bars.enableVersioning();
            long time = bars.getTimeRange()[0] + 4000 * 60;
            bars.truncate(time);

            String secondary = SECONDARY_HOME;
            DXTickDB tdb = TickDBFactory.createFromUrl (secondary);
            tdb.format();
            tdb.close();

            testReplicate("dxtick://localhost:" + runner.getPort(), secondary);
        } finally {
            runner.shutdown();
        }

        checkVersions(runner.getLocation());
    }

    @Test
    public void testReplicateTruncate1() throws Throwable {
        String primary = TDBRunner.getTemporaryLocation();

        TickDBCreator.createTickDB(primary, true).close();

        ServerRunner runner = new ServerRunner();
        try {
            runner.startup();
            String secondary = "dxtick://localhost:" + runner.getPort();

            for (int i = 0; i < 2; i++) {

                testReplicate(primary, secondary);

                try (DXTickDB db = TickDBFactory.createFromUrl(primary)) {
                    db.open(false);
                    DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);

                    long time = 1108501140000L;

                    System.out.println("Truncating stream to " + time);
                    stream.truncate(time);
                }

                testReplicate(primary, secondary);
            }
        } finally {
            runner.shutdown();
        }

        checkVersions(primary);
    }

    @Test
    public void testReplicateTruncate2() throws Throwable {

        ServerRunner runner = new ServerRunner(true, PRIMARY_HOME);
        ServerRunner runner1 = new ServerRunner(true, SECONDARY_HOME);

        try {
            runner.startup();
            TickDBCreator.createBarsStream(runner.getTickDb(), TickDBCreator.BARS_STREAM_KEY);
            String primary = "dxtick://localhost:" + runner.getPort();

            runner1.startup();
            String secondary = "dxtick://localhost:" + runner1.getPort();

            for (int i = 0; i < 2; i++) {

                testReplicate(primary, secondary);

                DXTickStream stream = runner.getTickDb().getStream(TickDBCreator.BARS_STREAM_KEY);
                long time = 1108501140000L;

                System.out.println("Truncating stream to " + time);
                stream.truncate(time);

                testReplicate(primary, secondary);
            }
        } finally {
            runner.shutdown();
            runner1.shutdown();
        }

        checkVersions(runner.getLocation());
    }

//    @Test
//    public void testReplicate() throws Throwable {
//
//        TDBRunner runner = new TDBRunner(true);
//        runner.startup();
//        TickDBCreator.createBarsStream(runner.getTickDb(), TickDBCreator.BARS_STREAM_KEY);
//
//        String secondary = Home.getPath("temp/qstest/tickdb1");
//
//        DXTickDB tdb = TickDBFactory.createFromUrl (secondary);
//        tdb.format();
//        tdb.close();
//
//        testReplicate("dxtick://localhost:" + runner.getServer().getLocalPort(), secondary);
//
//        runner.shutdown();
//    }

    @Test
    public void testReplicateDelete() throws Throwable {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        String secondary = SECONDARY_HOME;

        DXTickDB tdb = TickDBFactory.createFromUrl (secondary);
        tdb.format();
        tdb.close();

        testReplicate(PRIMARY_HOME, secondary);

        DXTickDB db = TickDBFactory.createFromUrl(PRIMARY_HOME);
        db.open(false);
        DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);

        stream.clear(new ConstantIdentityKey("MSFT"));
        db.close();

        testReplicate(PRIMARY_HOME, secondary);

        checkVersions(PRIMARY_HOME);
    }

    @Test
    public void testReplicateAppend() throws Throwable {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        String secondary = SECONDARY_HOME;

        DXTickDB tdb = TickDBFactory.createFromUrl (secondary);
        tdb.format();
        tdb.close();

        testReplicate(PRIMARY_HOME, secondary);

        DXTickDB db = TickDBFactory.createFromUrl(PRIMARY_HOME);
        db.open(false);
        DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);

        LoadingOptions options = new LoadingOptions();
        options.writeMode = LoadingOptions.WriteMode.APPEND;
        TickLoader loader = stream.createLoader(options);
        BarMessage msg = new BarMessage();
        msg.setSymbol("ZZZ");
        msg.setTimeStampMs(TimeKeeper.currentTime);
        msg.setOpen(29.76);
        msg.setHigh(msg.getOpen());
        msg.setLow(msg.getOpen());
        msg.setClose(msg.getOpen());
        loader.send(msg);

        msg.setTimeStampMs(msg.getTimeStampMs() + 1);
        msg.setOpen(21.76);
        msg.setHigh(msg.getOpen());
        msg.setLow(msg.getOpen());
        msg.setClose(msg.getOpen());
        msg.setSymbol("ZZ");
        loader.send(msg);

        msg.setTimeStampMs(msg.getTimeStampMs() + 1);
        msg.setOpen(25.76);
        msg.setHigh(msg.getOpen());
        msg.setLow(msg.getOpen());
        msg.setClose(msg.getOpen());
        msg.setSymbol("ORCL");
        loader.send(msg);
        loader.close();

        db.close();

        testReplicate(PRIMARY_HOME, secondary);

        checkVersions(PRIMARY_HOME);
    }

    @Test
    public void testCustomRange() throws IOException, InterruptedException {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        String source = PRIMARY_HOME;
        String target = SECONDARY_HOME;

        DXTickDB db = TickDBFactory.createFromUrl (target);
        db.format();
        db.close();

        TickDBShell shell = new TickDBShell();
        shell.setConfirm(false);

        String stream = TickDBCreator.BARS_STREAM_KEY;

        String[] lines = new String[] {
                "set db " + target,
                "open",
                //"set reload allow",
                "set srcdb " + source,
                "set srcstream " + stream,
                "set timeoffset 1D",
                "replicate " + stream,
                "set timeoffset 2D",
                "replicate " + stream,
                "close", ""
        };
        Reader rd = new StringReader(StringUtils.join("\r\n", lines));
        shell.runScript("", rd, false, true);

        try (
                DXTickDB primary = TickDBFactory.createFromUrl (source);
                DXTickDB secondary = TickDBFactory.createFromUrl (target)
        ) {
            primary.open(false);
            long[] initial = primary.getStream(stream).getTimeRange();

            secondary.open(false);
            long[] range = secondary.getStream(stream).getTimeRange();

            // last time should be the same
            assertEquals(range[1], initial[1]);

            assertEquals("Target range " + Interval.create(range[1] - range[0], TimeUnit.MILLISECOND).toString() + " < 2D",
                    range[1] - range[0], Interval.create(2, TimeUnit.DAY).toMilliseconds());

        }
    }

    @Test
    public void testReplicate2Existing() throws Throwable {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        StreamOptions options =
            StreamOptions.fixedType(StreamScope.DURABLE, "replica", null, 0, StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        try (DXTickDB secondary = TickDBFactory.createFromUrl(SECONDARY_HOME)) {
            secondary.format();

            DXTickStream stream = secondary.createStream(options.name, options);
            StreamConfigurationHelper.setBar(stream, "", null, Interval.MINUTE, "DECIMAL(4)", "DECIMAL(0)");
        }

        testReplicate(PRIMARY_HOME, SECONDARY_HOME, new ConstantIdentityKey("ORCL"));

        checkVersions(PRIMARY_HOME);
    }

    @Test
    public void testReplicateAppend1() throws Throwable {
        TickDBCreator.createTickDB(PRIMARY_HOME, true).close();

        String secondary = SECONDARY_HOME;

        try (DXTickDB tdb = TickDBFactory.createFromUrl (secondary)) {
            tdb.format();
        }

        testReplicate(PRIMARY_HOME, secondary, new ConstantIdentityKey("ORCL"));

        try (DXTickDB db = TickDBFactory.createFromUrl(PRIMARY_HOME)) {
            db.open(false);
            DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);

            LoadingOptions options = new LoadingOptions();
            options.writeMode = LoadingOptions.WriteMode.APPEND;
            try (TickLoader loader = stream.createLoader(options)) {
                BarMessage msg = new BarMessage();
                msg.setSymbol("ORCL");
                msg.setTimeStampMs(TimeKeeper.currentTime);


                msg.setOpen(29.76);
                msg.setHigh(msg.getOpen());
                msg.setLow(msg.getOpen());
                msg.setClose(msg.getOpen());
                msg.setSymbol("ZZ");


                loader.send(msg);
                msg.setTimeStampMs(msg.getTimeStampMs() + 1);
                loader.send(msg);
                msg.setTimeStampMs(msg.getTimeStampMs() + 1);
                loader.send(msg);
            }
        }

        testReplicate(PRIMARY_HOME, secondary,
                new ConstantIdentityKey("ORCL"));

        checkVersions(PRIMARY_HOME);
    }

    public void testBackupRestore(String timebase, boolean format, ReloadMode mode) throws Throwable {

        TickDBShell shell = new TickDBShell();
        shell.setConfirm(false);

        String[] lines = {
                "set db " + timebase,
                "open",
                "set bkpath " + Home.getPath("temp", "backup"),
                "set reload " + mode,
                "set stream " + TickDBCreator.BARS_STREAM_KEY,
                "backup" + (format ? " format" : ""),
                "close",
        };
        Reader rd = new StringReader(StringUtils.join("\r\n", lines));
        shell.runScript("", rd, false, true);

        lines = new String[] {
                "set db " + timebase,
                "open",
                "set bkpath " + Home.getPath("temp", "backup"),
                "set reload " + mode,
                "set srcstream " + TickDBCreator.BARS_STREAM_KEY,
                "restore restored",
                "close", ""
        };
        rd = new StringReader(StringUtils.join("\r\n", lines));
        shell.runScript ("", rd, false, true);

        try (DXTickDB db = TickDBFactory.createFromUrl(timebase)) {
            db.open(false);
            compareStreams(db.getStream(TickDBCreator.BARS_STREAM_KEY), db.getStream("restored"));
        }
    }

    public void testReplicate(String source, String target, IdentityKey... ids) throws Throwable {

        TickDBShell shell = new TickDBShell();
        shell.setConfirm(false);

        ArrayList<String> lines = new ArrayList<String>(Arrays.asList(
                "set db " + target,
                "open"
        ));

        for (IdentityKey id : ids)
            lines.add("symbols add " + id.getSymbol());

        lines.addAll(Arrays.asList(
                "set srcdb " + source,
                "set srcstream " + TickDBCreator.BARS_STREAM_KEY,
                "set reload allow",
                "replicate replica",
                "close"));

        Reader rd = new StringReader(StringUtils.join("\r\n", lines.toArray(new String[lines.size()])));
        shell.runScript ("", rd, false, true);

        DXTickDB db1 = TickDBFactory.createFromUrl(source);
        DXTickDB db2 = TickDBFactory.createFromUrl(target);

        try {
            db1.open(false);
            db2.open(false);

            DXTickStream replica = db2.getStream("replica");
            DXTickStream stream = db1.getStream(TickDBCreator.BARS_STREAM_KEY);
            compareStreams(stream, replica, ids);

        } finally {
            Util.close(db1);
            Util.close(db2);
        }
    }

    @Test
    public void         convert() throws Throwable {

        try (DXTickDB db = TickDBCreator.createTickDB(PRIMARY_HOME, true)) {
            DXTickStream stream = db.getStream(TickDBCreator.BARS_STREAM_KEY);
            stream.enableVersioning();

            final StreamStorage from = new StreamStorage(db, TickDBCreator.BARS_STREAM_KEY);
            final StreamStorage to = new StreamStorage(db, "convert");

            ReplicationOptions options = new ReplicationOptions();

            new StreamReplicator().replicate(from, to, options);

            compareStreams(from.getSource(), to.getSource());

            RecordClassDescriptor rcd = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                    "",
                    0,
                    FloatDataType.ENCODING_FIXED_DOUBLE,
                    FloatDataType.ENCODING_FIXED_DOUBLE);

            convert(from.getSource(), rcd);

            new StreamReplicator().replicate(from, to, options);

            compareStreams(from.getSource(), to.getSource());
        }

        checkVersions(PRIMARY_HOME);

        try (DXTickDB db = TickDBFactory.openFromUrl(PRIMARY_HOME, true)) {
            final StreamStorage from = new StreamStorage(db, TickDBCreator.BARS_STREAM_KEY);
            final StreamStorage to = new StreamStorage(db, "convert");

            compareStreams(from.getSource(), to.getSource());
        }
    }

//    @Test
//    public void         testDF1() throws Throwable {
//
//        final File file1 = Home.getFile("testdata", "uhf", "messageFiles", "securities5.qsmsg.gz");
//        final File file2 = Home.getFile("testdata", "tickdb", "misc", "securities5_120303.qsmsg.gz");
//
//        ServerRunner runner = new ServerRunner(true, true);
//        try {
//            runner.startup();
//
//            DXTickDB db = runner.getTickDb();
//
//            StreamOptions   options =
//                    StreamOptions.polymorphic(
//                            StreamScope.DURABLE, "securities", null, 1,
//                            StreamConfigurationHelper.mkSecurityMetaInfoDescriptors()
//                    );
//
//            DXTickStream stream = db.createStream("securities", options);
//            importFile(file1, stream);
//
//            final StreamStorage from = new StreamStorage(db, "securities");
//            final StreamStorage to = new StreamStorage(db, "test");
//
//            ReplicationOptions rOptions = new ReplicationOptions();
//            //rOptions.entities = new IdentityKey[] {stream.listEntities()[0]};
//            rOptions.entities = new IdentityKey[] {new ConstantIdentityKey(InstrumentType.EQUITY, "AAPL")};
//            new StreamReplicator().replicate(from, to, rOptions);
//            compareStreams(from.getSource(), to.getSource(), rOptions.entities);
//
//            importFile(file2, stream);
//
//            rOptions = new ReplicationOptions();
//            new StreamReplicator().replicate(from, to, rOptions);
//            compareStreams(from.getSource(), to.getSource(), stream.listEntities());
//        } finally {
//            runner.shutdown();
//        }
//
//    }
    //@Test
    public void testReplicateReconnect() throws InterruptedException, IOException {
        //start target TimeBase
        int portTrg = 8023;
        String workFolderTrg = Home.getPath("temp/tbRepTarget");
        IOUtil.mkDirIfNeeded(new File(workFolderTrg));
        String tbFolderTrg = workFolderTrg + "/tickdb";
        final TBLightweight.TBProcess tbTarget = new TBLightweight.TBProcess(workFolderTrg, tbFolderTrg, portTrg);
        tbTarget.start();

        //start sourceTimeBase
        int portSrc = 8024;
        String workFolderSrc = Home.getPath("temp/tbRepSource");
        IOUtil.mkDirIfNeeded(new File(workFolderSrc));
        String tbFolderSrc = workFolderSrc + "/tickdb";
        final TBLightweight.TBProcess tbSource = new TBLightweight.TBProcess(workFolderSrc, tbFolderSrc, portSrc);
        tbSource.start();

        Thread.sleep(1000);

        try (TickDBClient targetClient = (TickDBClient) TickDBFactory.open("localhost", portTrg, false);
             TickDBClient sourceClient = (TickDBClient) TickDBFactory.open("localhost", portSrc, false);) {
            RecordClassDescriptor classDescriptor = StreamConfigurationHelper.mkBarMessageDescriptor(
                null, "", null, "DECIMAL(4)", "DECIMAL(0)"
            );

            final DXTickStream targetStream = targetClient.createStream("stream_trg",
                StreamOptions.fixedType(StreamScope.DURABLE, "stream_trg", null, 0, classDescriptor));
            final DXTickStream sourceStream = sourceClient.createStream("stream_src",
                StreamOptions.fixedType(StreamScope.DURABLE, "stream_src", null, 0, classDescriptor));

            Thread loaderThread = new Thread() {
                @Override
                public void run() {
                    TDBRunner.BarsGenerator gn = new TDBRunner.BarsGenerator(
                        new GregorianCalendar(), 100, 60,
                        "MSFT");

                    TickLoader loader = sourceStream.createLoader(new LoadingOptions(false));
                    //generate 10 messages
                    //then wait restart source
                    //then generate another 10
                    int num = 0;
                    while (gn.next()) {
                        /**/
                        if (++num == 10) {
                            try {
                                //wait until source restart and then recreate loader
                                Thread.sleep(20000);
                            } catch (InterruptedException e) { }
                            loader.close();
                            loader = sourceStream.createLoader(new LoadingOptions(LoadingOptions.WriteMode.APPEND));
                        }
                        /**/

                        System.out.println("Msg " + num + " = " + gn.getMessage());
                        loader.send(gn.getMessage());
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) { }
                    }
                    loader.close();
                }
            };

            Thread replicateThread = new Thread() {
                @Override
                public void run() {
                    int recAttempts = 100;

                    ReplicationOptions options = new ReplicationOptions();
                    options.live = true;
                    options.retries = recAttempts;
                    options.flush = 1;

                    StreamStorage from = new StreamStorage(sourceClient, "stream_src");
                    StreamStorage to = new StreamStorage(targetClient, "stream_trg");
                    StreamReplicator replicator = new StreamReplicator();

                    try {
                        replicator.replicate(from, to, options);
                    } catch (Throwable t) {
                        //ignore
                    }
                }
            };

            Thread sourceKillerThread = new Thread() {
                @Override
                public void run() {
                    try {
                        tbSource.stop();
                        Thread.sleep(5000);
                        tbSource.restart();
                    } catch (IOException|InterruptedException e) {
                        System.out.println("Error restart process.");
                        throw new UncheckedIOException(e);
                    }
                }
            };

            Thread targetKillerThread = new Thread() {
                @Override
                public void run() {
                    try {
                        tbTarget.stop();
                        Thread.sleep(5000);
                        tbTarget.restart();
                    } catch (IOException|InterruptedException e) {
                        System.out.println("Error restart process.");
                        throw new UncheckedIOException(e);
                    }
                }
            };

            loaderThread.start();
            replicateThread.start();

            //wait a little to work, then restart source
            Thread.sleep(10000);
            sourceKillerThread.start();
            sourceKillerThread.join();

            //wait a little, then restart target
            Thread.sleep(20000);
            targetKillerThread.start();
            targetKillerThread.join();

            //wait a little until replication finish and interrupt
            loaderThread.join();
            Thread.sleep(2000);
            replicateThread.interrupt();
            replicateThread.join();

            compareStreams(sourceStream, targetStream);
            System.out.println("Source and target streams are equal");
        } finally {
            tbSource.stop();
            tbTarget.stop();
        }
    }

    private void importFile(File file, DXTickStream stream) {

        ConsumableMessageSource<InstrumentMessage> reader = null;
        TickLoader loader = stream.createLoader();

        try {
            reader = Protocol.openReader(file, TypeLoaderImpl.DEFAULT_INSTANCE);
            while (reader.next())
                loader.send(reader.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Util.close(loader);
            Util.close(reader);
        }
    }

    public void compareStreams(DXTickStream source, DXTickStream target, IdentityKey... ids) {

        IdentityKey[] entities = ids.length == 0 ? source.listEntities() : ids;

//        TickCursor cursor1 = null;
//        TickCursor cursor2 = null;
//        try {
//
//            SelectionOptions options = new SelectionOptions(false, false);
//            options.ordered = true;
//
//
//            cursor1 = source.select(Long.MIN_VALUE, options, null, ids);
//            cursor2 = target.select(Long.MIN_VALUE, options, null, ids);
//
//            while (cursor1.next())
//                System.out.println("1st: " + cursor1.getMessage());
//
//            System.out.println("==================");
//
//            while (cursor2.next())
//                System.out.println("2nd: " + cursor2.getMessage());
//
//            while (true) {
//                if (cursor1.next())
//                    assertTrue("Target cursor has no message, but source has " + cursor1.getMessage(), cursor2.next());
//                else
//                    break;
//
//                System.out.println("1st: " + cursor1.getMessage() + ", 2nd: " + cursor2.getMessage());
//                //Test_SchemaConverter.checkEquals(cursor1.getMessage(), cursor2.getMessage());
//            }
//        } finally {
//            Util.close(cursor1);
//            Util.close(cursor2);
//        }

        StreamComparer c = StreamComparer.create(StreamComparer.ComparerType.Unordered);
        assertTrue(c.compare(source, target, entities));

    }

    public static void convert(DXTickStream source, RecordClassDescriptor rcd) throws InterruptedException {

        RecordClassSet in = new RecordClassSet ();
        in.addContentClasses(source.getFixedType());
        RecordClassSet out = new RecordClassSet ();
        out.addContentClasses(rcd);

        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges
                (in, MetaDataChange.ContentType.Fixed,  out, MetaDataChange.ContentType.Fixed);
        SchemaChangeTask task = new SchemaChangeTask(change);
        task.background = false;
        source.execute(task);
    }
}