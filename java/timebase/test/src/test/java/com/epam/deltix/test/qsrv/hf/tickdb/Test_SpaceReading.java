package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.containers.CharSequenceUtils;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.server.ServerRunner;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;

import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingError;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingErrorListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.util.JUnitCategories;
import org.jetbrains.annotations.NotNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Alexei Osipov
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_SpaceReading {
    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new ServerRunner(false, true);
        runner.startup();
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.setCleanup(true);
        runner.shutdown();
        runner = null;
    }


    @Test
    public void testSpaceReadingAndPurging() throws Exception {
        String name = "testSpacesStream1";

        StreamOptions options = new StreamOptions(StreamScope.DURABLE, name, null, 0);
        options.version = "5.0";
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        final DXTickStream stream = runner.getTickDb().createStream(name, options);

        Assert.assertArrayEquals(new String[]{""}, stream.listSpaces());

        String space1 = "spaceOne";
        writeBars(space1, stream, 100);
        assertEquals(asSet("", space1), asSet(stream.listSpaces()));

        String space2 = "space2_tWo";
        writeBars(space2, stream, 200);
        assertEquals(asSet("", space1, space2), asSet(stream.listSpaces()));

        int count1 = countMessages(stream, space1);
        int count2 = countMessages(stream, space2);
        assertEquals(100, count1);
        assertEquals(200, count2);

        Assert.assertArrayEquals(new IdentityKey[]
                {
                        new ConstantIdentityKey("MSFT"),
                        new ConstantIdentityKey("IBM")
                }, stream.listEntities(space1));

        Assert.assertArrayEquals(new IdentityKey[]
                {
                        new ConstantIdentityKey("MSFT"),
                        new ConstantIdentityKey("IBM")
                }, stream.listEntities(space2));


        Assert.assertNotNull(stream.getTimeRange(space1));
        Assert.assertNotNull(stream.getTimeRange(space2));
        Assert.assertNull(stream.getTimeRange(""));

        for (String space : stream.listSpaces()) {
            stream.purge(Long.MAX_VALUE, space);
        }

        stream.delete();
    }

    @NotNull
    private HashSet<String> asSet(String... spaces) {
        return new HashSet<>(Arrays.asList(spaces));
    }

    private void writeBars(String space, DXTickStream stream, int count) {
        LoadingOptions o1 = new LoadingOptions();
        o1.space = space;
        o1.writeMode = LoadingOptions.WriteMode.APPEND;
        try (TickLoader loader = stream.createLoader(o1)) {

            final int[] errors = new int[1];
            final LoadingErrorListener listener = new LoadingErrorListener() {
                public void onError(LoadingError e) {
                    errors[0]++;
                    System.out.println(e);
                }
            };
            loader.addEventListener(listener);

            TDBRunner.BarsGenerator gn =
                    new TDBRunner.BarsGenerator(null, (int) BarMessage.BAR_MINUTE, count, "MSFT", "IBM");

            while (gn.next())
                loader.send(gn.getMessage());
            loader.close();
            assertEquals(0, errors[0]);
        }
    }

    private int countMessages(DXTickStream stream, String space) {
        SelectionOptions opts = new SelectionOptions();
        opts.space = space;

        int count = 0;
        try (TickCursor cursor = stream.select(Long.MIN_VALUE, opts)) {
            while (cursor.next())
                count++;
        }
        return count;
    }

    @Test
    public void testSpaceSaveAndRestore() {
        DXTickDB db = runner.getTickDb();
        //db.open(false);


        StreamOptions options = new StreamOptions(StreamScope.DURABLE, null, null, 0);
        options.version = "5.0";
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        DXTickStream stream = db.createStream("testSpacesStream2", options);

        /*
        PDStream pdStream = new PDStream(db, "test_stream", options);
        pdStream.init(db, Files.createTempDirectory("timbase-test-pdsctream").toFile());
        pdStream.format();

        pdStream.open(false);
         */

        LoadingOptions lo1 = new LoadingOptions();
        lo1.space = "abc_";
        TickLoader loader = stream.createLoader(lo1);
        loader.close();

        LoadingOptions lo2 = new LoadingOptions();
        lo2.space = "XYZ";
        TickLoader loader2 = stream.createLoader(lo2);
        loader2.close();

        assertEquals(asSet("", "abc_", "XYZ"), asSet(stream.listSpaces()));

        db.close();

        db.open(false);

        assertEquals(asSet("", "abc_", "XYZ"), asSet(stream.listSpaces()));
    }

    @Test
    public void testSpaceDetectionForConvertedStreams() throws Throwable {
        DXTickDB db = runner.getTickDb();

        StreamOptions options = new StreamOptions(StreamScope.DURABLE, null, null, 0);
        options.version = "5.0";
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        String streamKey = "testSpacesStream3";
        DXTickStream stream = db.createStream(streamKey, options);

        stream.enableVersioning();

        String s1 = "abc";
        String s2 = "XYZ";
        String s3 = "n_";

        writeBars(s1, stream, 100);
        writeBars(s2, stream, 100);
        writeBars(s3, stream, 100);
        Set<String> spaceSet = asSet("", s1, s2, s3);
        assertEquals(spaceSet, asSet(stream.listSpaces()));

        RecordClassDescriptor d2 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "",
                null,
                FloatDataType.ENCODING_FIXED_DOUBLE,
                FloatDataType.ENCODING_FIXED_DOUBLE);

        covertStream(stream, d2);

        assertEquals(spaceSet, asSet(stream.listSpaces()));

        // Close and reopen TB to ensure that space data was reloaded so logic that restores space name from location works fine
        db.close();
        db.open(false);
        stream = db.getStream(streamKey);

        assertEquals(spaceSet, asSet(stream.listSpaces()));


        writeBars(s1, stream, 100);
        writeBars(s2, stream, 100);
        writeBars(s3, stream, 100);

        assertEquals(spaceSet, asSet(stream.listSpaces()));

        RecordClassDescriptor d3 = StreamConfigurationHelper.mkBarMessageDescriptor(null,
                "",
                null,
                FloatDataType.ENCODING_SCALE_AUTO,
                FloatDataType.ENCODING_SCALE_AUTO);

        covertStream(stream, d3);

        assertEquals(spaceSet, asSet(stream.listSpaces()));

        // Close and reopen TB to ensure that space data was reloaded so logic that restores space name from location works fine
        db.close();
        db.open(false);
        stream = db.getStream(streamKey);

        assertEquals(spaceSet, asSet(stream.listSpaces()));

        //rawCompareStreams(source, target, null, null);
    }

    private static void covertStream(DXTickStream stream, RecordClassDescriptor rcd) throws Throwable {
        RecordClassSet in = new RecordClassSet();
        in.addContentClasses(stream.getFixedType());
        RecordClassSet out = new RecordClassSet();
        out.addContentClasses(rcd);

        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges
                (in, MetaDataChange.ContentType.Fixed, out, MetaDataChange.ContentType.Fixed);
        stream.execute(new SchemaChangeTask(change, false));
    }

    /**
     * Ensures that messages that returned by a cursor are ordered by space name.
     */
    @Test
    public void testCursorReading() {
        DXTickDB db = runner.getTickDb();

        StreamOptions options = new StreamOptions(StreamScope.DURABLE, null, null, 0);
        options.version = "5.0";
        options.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());

        String streamKey = "testSpacesStream4";
        DXTickStream stream = db.createStream(streamKey, options);


        generateMessagesForSpaces(stream, 8);

        ensureStrictMessageOrder(stream, false);
        ensureStrictMessageOrder(stream, true);
    }

    /**
     * Generates a set of spaces for a stream.
     * Each space will have fixed number of messages written at the same timestamp.
     */
    private void generateMessagesForSpaces(DXTickStream stream, int spaceCount) {
        assert spaceCount < 26;

        List<String> spaces = new ArrayList<>();
        List<TickLoader> loaders = new ArrayList<>();
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < spaceCount; i++) {
            String space = "" + (char)('a' + i);
            spaces.add(space);

            LoadingOptions lo = new LoadingOptions();
            lo.space = space;
            lo.writeMode = LoadingOptions.WriteMode.APPEND;
            TickLoader loader = stream.createLoader(lo);
            loaders.add(loader);
            indexes.add(i);
        }


        BarMessage message = new BarMessage();
        message.setHigh(1);
        message.setOpen(1);
        message.setClose(1);
        message.setLow(1);
        message.setVolume(1);
        message.setCurrencyCode((short)840);

        long timestamp = 0;
        for (int i = 0; i < 10_000; i++) {
            timestamp += 1;
            for (int j = 0; j < 1; j++) {
                Collections.shuffle(indexes);
                for (Integer index : indexes) {
                    TickLoader loader = loaders.get(index);
                    String space = spaces.get(index);

                    message.setTimeStampMs(timestamp);
                    message.setSymbol(space);

                    loader.send(message);
                }
            }
        }

        for (TickLoader loader : loaders) {
            loader.close();
        }
    }

    private void ensureStrictMessageOrder(DXTickStream stream, boolean reversed) {
        int count = 0;

        int direction = reversed ? -1 : 1;
        SelectionOptions options = new SelectionOptions();
        options.reversed = reversed;

        try (TickCursor cursor = stream.select(reversed ? Long.MAX_VALUE : Long.MIN_VALUE, options)) {
            long prevTimestamp = Long.MIN_VALUE;
            StringBuilder prevSymbol = new StringBuilder();
            while (cursor.next()) {
                InstrumentMessage msg = cursor.getMessage();
                long msgNanoTime = msg.getNanoTime();
                if (prevTimestamp != msgNanoTime) {
                    prevTimestamp = msgNanoTime;
                    prevSymbol.setLength(0);
                    prevSymbol.append(msg.getSymbol());
                } else {
                    CharSequence symbol = msg.getSymbol();
                    int compare = CharSequenceUtils.compare(symbol, prevSymbol, false) * direction;
                    Assert.assertTrue(compare >= 0);
                    if (compare != 0) {
                        prevSymbol.setLength(0);
                        prevSymbol.append(symbol);
                    }
                }
                count++;
            }
        }
        Assert.assertTrue(count > 0);
    }
}
