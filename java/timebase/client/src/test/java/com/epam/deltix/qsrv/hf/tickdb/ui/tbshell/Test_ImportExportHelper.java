package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.FixedUnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Ignore
public class Test_ImportExportHelper {

    private static DXTickDB db = null;

    private static final DataField f1 = new NonStaticDataField(
            "f1",
            "f1",
            new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true),
            null
    );

    private static final DataField f2 = new NonStaticDataField(
            "f2",
            "f2",
            new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true),
            null
    );

    private static final DataField f3 = new NonStaticDataField(
            "f3",
            "f3",
            new IntegerDataType(IntegerDataType.ENCODING_INT32, true),
            null
    );

    private static final DataField f4 = new NonStaticDataField(
            "f4",
            "f4",
            new BooleanDataType(true),
            null
    );

    private static final RecordClassDescriptor CLASS1 = new RecordClassDescriptor(
            "TestMessageImporterClass",
            "Class for testing ImportExportHelper",
            false,
            null,
            f1, f2
    );

    private static final RecordClassDescriptor CLASS2 = new RecordClassDescriptor(
            "TestMessageImporterClass",
            "Class for testing ImportExportHelper",
            false,
            null,
            f2, f3
    );

    private static final RecordClassDescriptor CLASS3 = new RecordClassDescriptor(
            "TestMessageImporterClass",
            "Class for testing ImportExportHelper",
            false,
            null,
            f1, f3, f4
    );

    private static final String STREAM1_KEY = "testMessageImporter.stream1";
    private static final String STREAM2_KEY = "testMessageImporter.stream2";
    private static final String STREAM3_KEY = "testMessageImporter.stream3";

    private static DXTickStream STREAM1 = null;
    private static DXTickStream STREAM2 = null;
    private static DXTickStream STREAM3 = null;

    private static Path FILE1 = null;

    private static final String FILE1_NAME = "file.qsmsg.gz";

    @BeforeClass
    public static void beforeClass() {
        try {
            FILE1 = Files.createTempDirectory("test_MessageImporter");
            db = TickDBFactory.createFromUrl("dxtick://localhost:8011");
            db.open(false);
        } catch (IOException exc) {
            throw new RuntimeException(exc);
        }
        createAndExportStreams(db);
    }

    @AfterClass
    public static void afterClass() {
        if (db != null) {
            if (STREAM1 != null) {
                STREAM1.delete();
            }
            if (STREAM2 != null) {
                STREAM2.delete();
            }
            if (STREAM3 != null) {
                STREAM3.delete();
            }
            db.close();
        }
    }

    @Test
    public void testIsCompatible() {
        assertTrue(ImportExportHelper.isCompatible(new RecordClassDescriptor[] {CLASS1},
                new RecordClassDescriptor[]{CLASS2}));
        assertTrue(ImportExportHelper.isCompatible(new RecordClassDescriptor[] {CLASS1},
                new RecordClassDescriptor[]{CLASS3}));
        assertTrue(ImportExportHelper.isCompatible(new RecordClassDescriptor[] {CLASS2},
                new RecordClassDescriptor[]{CLASS3}));
    }

    @Test
    public void testFilterMessageFile1() throws IOException, FieldNotFoundException {
        STREAM3 = db.createStream(
                STREAM3_KEY,
                STREAM3_KEY,
                "test-stream",
                0
        );
        STREAM3.setFixedType(CLASS3);
        ImportExportHelper.filterMessageFile(FILE1.resolve(FILE1_NAME).toFile(), STREAM3);
        TickCursor cursor = null;
        try {
            cursor = STREAM3.select(Long.MIN_VALUE,
                    new SelectionOptions(true,false));
            RawMessageHelper helper = new RawMessageHelper();
            while (cursor.next()) {
                RawMessage message = (RawMessage) cursor.getMessage();
                String f1 = (String) helper.getValue(message, "f1");
                Integer f3 = (Integer) helper.getValue(message, "f3");
                Boolean f4 = (Boolean) helper.getValue(message, "f4");
                assertTrue((f1 != null && f1.compareTo("Hello!") == 0 ||
                        f3 != null && f3 % 42 == 0) && f4 == null);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }


    private static void createAndExportStreams(DXTickDB db) {
        STREAM1 = db.createStream(
                STREAM1_KEY,
                STREAM1_KEY,
                "test-stream",
                0
        );
        STREAM1.setFixedType(CLASS1);
        STREAM2 = db.createStream(
                STREAM2_KEY,
                STREAM2_KEY,
                "test-stream",
                0
        );
        STREAM2.setFixedType(CLASS2);
        loadStream1();
        loadStream2();
        try {
            ImportExportHelper.writeStreamsToFile(
                    FILE1.resolve(FILE1_NAME).toFile(),
                    db,
                    new DXTickStream[]{STREAM1, STREAM2},
                    null);
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private static void loadStream1() {
        RawMessage message = new RawMessage(CLASS1);
        LoadingOptions options = new LoadingOptions(true);
        TickLoader loader = STREAM1.createLoader(options);

        MemoryDataOutput dataOutput = new MemoryDataOutput();
        FixedUnboundEncoder encoder = CodecFactory.COMPILED.createFixedUnboundEncoder(CLASS1);
        try {
            int n = 100;
            for (int i = 0; i < n; i++) {
                message.setTimeStampMs(System.currentTimeMillis() - (n - i) * 1000);

                message.setSymbol("YDPL");

                dataOutput.reset();
                encoder.beginWrite(dataOutput);

                encoder.nextField();
                encoder.writeString("Hello!");

                encoder.nextField();
                encoder.writeDouble(i * 0.042);

                if (encoder.nextField())
                    throw new RuntimeException("unexpected field: " + encoder.getField().toString());

                message.setBytes(dataOutput, 0);

                loader.send(message);
            }
        } finally {
            loader.close();
        }
    }

    private static void loadStream2() {
        RawMessage message = new RawMessage(CLASS2);
        LoadingOptions options = new LoadingOptions(true);
        TickLoader loader = STREAM2.createLoader(options);

        MemoryDataOutput dataOutput = new MemoryDataOutput();
        FixedUnboundEncoder encoder = CodecFactory.COMPILED.createFixedUnboundEncoder(CLASS2);
        try {
            int n = 100;
            for (int i = 0; i < n; i++) {
                message.setTimeStampMs(System.currentTimeMillis() - (n - i) * 1000);

                message.setSymbol("YDPL");

                dataOutput.reset();
                encoder.beginWrite(dataOutput);

                encoder.nextField();
                encoder.writeDouble(i * 0.042);

                encoder.nextField();
                encoder.writeInt(42 * i);

                if (encoder.nextField())
                    throw new RuntimeException("unexpected field: " + encoder.getField().toString());

                message.setBytes(dataOutput, 0);

                loader.send(message);
            }
        } finally {
            loader.close();
        }
    }

}