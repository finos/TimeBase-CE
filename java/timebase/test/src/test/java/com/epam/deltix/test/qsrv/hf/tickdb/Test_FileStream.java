package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.schema.Test_SchemaConverter;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.concurrent.UnavailableResourceException;
import com.epam.deltix.util.lang.ExceptionHandler;
import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.stream.MessageWriter2;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.io.Home;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

@Category(TickDBFast.class)
public class Test_FileStream {

    private static TDBRunner runner;
    private static String FILE_STREAM_NAME = "fileStream";
    private static String FILE_V0 = "testdata/tickdb/misc/dailyBars.qsmsg.gz";
    private static String FILE_V1 = "testdata/qsrv/hf/tickdb/bars.gz";

    private final IdentityKey[] keys =
            new IdentityKey[] { new ConstantIdentityKey("ORCL") };

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(true, true);
        runner.startup();

        DXTickStream stream = TickDBCreator.createBarsStream(runner.getServerDb());
        createFileStream(runner.getServerDb(), stream);
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    private static DXTickStream createFileStream(DXTickDB db, DXTickStream source) throws Exception {

        File tempFile = File.createTempFile("data", null);
        tempFile.deleteOnExit();
        MessageWriter2 writer = MessageWriter2.create(tempFile, source.getPeriodicity().getInterval(), null, source.getFixedType());

        TickCursor cursor = null;
        try {
            cursor = source.select(0, new SelectionOptions(true, false));
            while (cursor.next())
                writer.send(cursor.getMessage());
        } finally {
            Util.close(cursor);
            Util.close(writer);
        }

        int count = db.listStreams().length;
        DXTickStream fileStream = db.createFileStream(FILE_STREAM_NAME, tempFile.getPath());
        DXTickStream[] streams = db.listStreams();
        assertEquals(count + 1, streams.length);
        return fileStream;
    }

    @Test
    public void test() throws Exception {

        DXTickDB db = runner.getTickDb();
        if (!db.isOpen())
            db.open(true);
        DXTickStream stream = db.getStream("bars");
        DXTickStream fileStream = db.getStream(FILE_STREAM_NAME);

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;

        try {
            cursor1 = stream.select(0, new SelectionOptions(false, false), null, keys);
            cursor2 = fileStream.select(0, new SelectionOptions(false, false), null, keys);

            while (true) {
                if (cursor1.next() && cursor2.next())
                    Test_SchemaConverter.checkEquals(cursor1.getMessage(), cursor2.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }

        runner.close();
        runner.open(false);

        db = runner.getTickDb();
        assertTrue(db.getStream(FILE_STREAM_NAME) != null);
        runner.close();
    }

    @Test
    public void             test2() throws Exception {
        DXTickDB db = runner.getTickDb();
        if (!db.isOpen())
            db.open(true);

        DXTickStream stream = db.getStream("bars");
        DXTickStream fileStream = db.getStream(FILE_STREAM_NAME);

        TickCursor cursor1 = null;
        TickCursor cursor2 = null;
        try {
            cursor1 = stream.select(0, new SelectionOptions(false, false));

            cursor2 = fileStream.select(0, new SelectionOptions(false, false));

            while (true) {
                if (cursor1.next() && cursor2.next())
                    Test_SchemaConverter.checkEquals(cursor1.getMessage(), cursor2.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }

        int count = db.listStreams().length;
        runner.close();
        runner.open(false);

        db = runner.getTickDb();
        assertEquals(count, db.listStreams().length);
        db.close();
    }

    @Test
    public void             test1() throws Exception {
        DXTickDB db = runner.getTickDb();
        if (!db.isOpen())
            db.open(true);
        DXTickStream stream = db.getStream("bars");
        DXTickStream fileStream = db.getStream(FILE_STREAM_NAME);

        long[] range = stream.getTimeRange();
        TickCursor cursor1 = null;
        TickCursor cursor2 = null;

        try {
            long time = (range[1] - range[0]) / 2;
            
            cursor1 = stream.select(time, new SelectionOptions(false, false), null, keys);
            cursor2 = fileStream.select(time, new SelectionOptions(false, false), null, keys);

            while (true) {
                if (cursor1.next() && cursor2.next())
                    Test_SchemaConverter.checkEquals(cursor1.getMessage(), cursor2.getMessage());
                else
                    break;
            }
        } finally {
            Util.close(cursor1);
            Util.close(cursor2);
        }

        int count = db.listStreams().length;

        runner.close();
        runner.open(false);

        db = runner.getTickDb();
        assertEquals(count, db.listStreams().length);
        runner.close();
    }

    @Test
    public void Test3() throws Exception {

        DXTickDB db = runner.getServerDb();
        if (!db.isOpen())
            db.open(true);

        File dataFile = Home.getFile(FILE_V1);
        DXTickStream fileStream = db.createFileStream("Test3", dataFile.getPath());

        TickCursor cursor = fileStream.select(Long.MIN_VALUE,
                new SelectionOptions(false, true), null, new IdentityKey[0]);

        cursor.setAvailabilityListener( new Runnable () {
                public void     run () {
                }
            });

        long time = System.currentTimeMillis();
        try {
            cursor.next();

            assert false : "Exception not thrown";
        } catch (UnavailableResourceException ex) {
            // only valid case
        } finally {
            Util.close(cursor);
        }

        long spent = System.currentTimeMillis() - time;
        assertTrue("Reading time " + String.valueOf(spent) + " should be <= 1 ms", spent <= 1);
    }

    @Test
    public void Test4() throws Exception {

        DXTickDB db = runner.getServerDb();
        if (!db.isOpen())
            db.open(true);

        File dataFile = Home.getFile(FILE_V1);
        DXTickStream fileStream = db.createFileStream("Test4", dataFile.getPath());

        TickCursor cursor = fileStream.select(0, new SelectionOptions(true, true));
        assertTrue(cursor.next());
        cursor.clearAllEntities();

        cursor.setAvailabilityListener( new Runnable () {
            public void     run () {
            }
        });

        long time = System.currentTimeMillis();
        try {
            cursor.next();

            assert false : "Exception not thrown";
        } catch (UnavailableResourceException ex) {
            // only valid case
        } finally {
            Util.close(cursor);
        }

        long spent = System.currentTimeMillis() - time;
        assertTrue("Reading time " + String.valueOf(spent) + " should be <= 1 ms", spent <= 1);
    }

    @Test
    public void Test5() throws Exception {

        DXTickDB db = runner.getServerDb();
        if (!db.isOpen())
            db.open(true);

        File dataFile = Home.getFile(FILE_V1);
        DXTickStream fileStream = db.createFileStream("Test5", dataFile.getPath());

        TickCursor cursor = fileStream.createCursor(new SelectionOptions(false, false));
        assertTrue(!cursor.next());
    }

    @Test
    public void Test_dataFile0() throws Exception {
        Test_dataFile(FILE_V0);
    }

    @Test
    public void Test_dataFile1() throws Exception {
        Test_dataFile(FILE_V1);
    }

    public void Test_dataFile(String path) throws Exception {
        DXTickDB db = runner.getServerDb();
        File dataFile = Home.getFile(path);
        DXTickStream fileStream = db.createFileStream(dataFile.getName(), dataFile.getPath());

        TickCursor cursor = null;
        try {
            SelectionOptions options = new SelectionOptions(false, false);
            options.typeLoader = new TypeLoader() {

                public Class load(ClassDescriptor cd, String javaClassName,  ClassLoader loader, ExceptionHandler handler) throws ClassNotFoundException {
                    try {
                        return loader.loadClass(javaClassName);
                    } catch (ClassNotFoundException e) {

                        if (handler != null)
                            handler.handle(e);

                        // for record class look up among parents
                        if (cd instanceof RecordClassDescriptor) {
                            final ClassDescriptor parent = ((RecordClassDescriptor) cd).getParent();
                            if (parent == null)
                                return InstrumentMessage.class;
                            else
                                return load(parent);
                        } else
                            throw e;
                    }
                }

                @Override
                public Class load(ClassDescriptor cd) throws ClassNotFoundException {
                    String name = cd.getName();
                    if (name.startsWith("deltix.qsrv.hf.pub"))
                        name = name.replace("deltix.qsrv.hf.pub", "deltix.timebase.api.messages");


                    return load(cd, name, getClass().getClassLoader(), null);
                }
            };
            cursor = fileStream.select(Long.MIN_VALUE, options);
            assertTrue(cursor.next());
            InstrumentMessage msg = cursor.getMessage();
            assertTrue(msg instanceof MarketMessage);

            long time = msg.getTimeStampMs();

            cursor.reset(Long.MIN_VALUE);
            assertTrue(cursor.next());
            assertEquals(time, msg.getTimeStampMs());
        } finally {
            Util.close(cursor);
        }

        fileStream.delete();
    }
}