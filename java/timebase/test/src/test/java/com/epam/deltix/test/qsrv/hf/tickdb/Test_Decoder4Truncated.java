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

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.util.ZIPUtil;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.RecordInfo;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.lang.ExceptionHandler;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;

import java.io.*;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ArrayList;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

import static org.junit.Assert.assertEquals;

/**
 * Test that bounded decoder sets just extended fields (having no message data) to <code>null</code> (bug #7075).
 *
 * @author BazylevD
 */
@Category(TickDBFast.class)
public class Test_Decoder4Truncated {
    //private static final File DIR = Home.getFile("testdata" );
    //private static final File ZIP = new File(DIR, "tickdb.trade-bars.zip");

    private DXTickDB db = null;

    @BeforeClass
    public static void setUpClass() throws IOException, InterruptedException {
        //System.out.println("setUpClass " + System.getProperty("use.codecs"));
        System.setProperty("use.codecs", "compiled");
    }

    @Before
    public void setUp() throws IOException, InterruptedException {
        try (InputStream stream = IOUtil.openResourceAsStream("com/epam/deltix/trade-bars.zip")) {
            String path = createDB(stream);
            db = TickDBFactory.create(path);
        }

        db.open(false);
    }

    @After
    public void tearDown() {
        if (db != null)
            db.close();
    }

    public static enum Kind {
        BIG,
        SMALL,
        BEAUTIFUL
    }

    private static final EnumClassDescriptor cdKind = new EnumClassDescriptor(Kind.class);

    private final static DataType[] DATA_TYPES = {
            new BinaryDataType(true, BinaryDataType.MIN_COMPRESSION),
            new BooleanDataType(true),
            new CharDataType(true),
            new DateTimeDataType(true),
            new EnumDataType(true, cdKind),

            new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true),
            new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true),
            new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true),
            new FloatDataType(FloatDataType.getEncodingScaled(2), true),

            new IntegerDataType(IntegerDataType.ENCODING_INT8, true),
            new IntegerDataType(IntegerDataType.ENCODING_INT16, true),
            new IntegerDataType(IntegerDataType.ENCODING_INT32, true),
            new IntegerDataType(IntegerDataType.ENCODING_INT48, true),
            new IntegerDataType(IntegerDataType.ENCODING_INT64, true),
            new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true),
            new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true),
            new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true),

            new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false),
            new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(5), true, false),
            // bind to long
            new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false),

            new TimeOfDayDataType(true),
    };

    /*
     * Test case, when extended fields added into existent message class 
     */
    @Test
    public void testInsideExtensionPublic() throws IOException, InterruptedException {
        test(true, false);
    }

    @Test
    public void testInsideExtensionPrivate() throws IOException, InterruptedException {
        test(true, true);
    }

    /*
     * Test case, when extended fields added into successor class  
     */
    @Test
    public void testOutsideExtensionPublic() throws IOException, InterruptedException {
        test(false, false);
    }

    @Test
    public void testOutsideExtensionPrivate() throws IOException, InterruptedException {
        test(false, true);
    }

    private void test(boolean isInsideClass, boolean usePrivate) throws IOException, InterruptedException {
        test(isInsideClass, usePrivate, "trade", "trade." + getSuffix() + ".txt");
        test(isInsideClass, usePrivate, "bars", "bars." + getSuffix() + ".txt");

        // also test streams having a dummy field
        test(isInsideClass, usePrivate, "trade.dummy", "trade." + getSuffix() + ".txt");
        test(isInsideClass, usePrivate, "bars.dummy", "bars." + getSuffix() + ".txt");
    }

    private void test(boolean isInsideClass, boolean usePrivate, String streamKey, String etalonFile) throws IOException, InterruptedException {
        final DXTickStream stream = db.getStream(streamKey);
        extendStreamMetadata(stream, isInsideClass);
        select(0, stream, etalonFile, usePrivate);
    }

    private static void extendStreamMetadata(DXTickStream stream, boolean isInsideClass) throws InterruptedException {
        RecordClassDescriptor rcd = stream.getFixedType();
        RecordClassSet in = new RecordClassSet();
        in.addContentClasses(rcd);

        final RecordClassDescriptor rcdExtended;
        if (isInsideClass) {
            DataField[] fields = rcd.getFields();
            final int len = fields.length;
            DataField[] fieldsExtended = new DataField[len + DATA_TYPES.length];
            System.arraycopy(fields, 0, fieldsExtended, 0, len);
            populateFields(fieldsExtended, len);
            rcdExtended = new RecordClassDescriptor(rcd.getName(),
                    rcd.getTitle(),
                    rcd.isAbstract(),
                    rcd.getParent(),
                    fieldsExtended);
        } else {
            DataField[] fieldsExtended = new DataField[DATA_TYPES.length];
            populateFields(fieldsExtended, 0);

            String name = (rcd.getName().equals(TradeMessage.class.getName()) || rcd.getName().equals(TradeExtended.class.getName())) ?
                    TradeExtended.class.getName() : BarExtended.class.getName();

            rcdExtended = new RecordClassDescriptor(
                    name,
                    null,
                    false,
                    rcd,
                    fieldsExtended);
        }

        RecordClassSet out = new RecordClassSet();
        out.addContentClasses(rcdExtended);

        StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges
                (in, MetaDataChange.ContentType.Fixed, out, MetaDataChange.ContentType.Fixed);
        SchemaChangeTask task = new SchemaChangeTask(change);
        task.background = false;
        stream.execute(task);
    }

    private static void populateFields(DataField[] fields, int offset) {
        for (int i = 0; i < DATA_TYPES.length; i++) {
            final DataType dataType = DATA_TYPES[i];
            // descriptive field name simplifies further troubleshooting
            final String encoding = dataType.getEncoding();
            String fieldName;
            if (dataType instanceof BooleanDataType)
                fieldName = "unbound_bool";
            else if (encoding == null)
                fieldName = dataType instanceof EnumDataType ? "enum" : dataType.getBaseName();
            else
                fieldName = (encoding.indexOf('(') != -1) ? encoding.substring(0, encoding.indexOf('(')) : encoding;

            fieldName = fieldName.toLowerCase() + '_' + (i + 1);
            //System.out.println(fieldName);
            fields[offset + i] = new NonStaticDataField(fieldName, null, dataType);
        }
    }

//    private static String createDB(String zipFileName) throws IOException, InterruptedException {
//        File folder = new File(TDBRunner.getTemporaryLocation());
//        //BasicIOUtil.deleteFileOrDir(folder);f
//        //folder.mkdirs();
//
//        FileInputStream is = new FileInputStream(zipFileName);
//        ZIPUtil.extractZipStream(is, folder);
//        is.close();
//
//        return folder.getAbsolutePath();
//    }

    private static String createDB(InputStream is) throws IOException, InterruptedException {
        File folder = new File(TDBRunner.getTemporaryLocation());
        ZIPUtil.extractZipStream(is, folder);
        is.close();

        return folder.getAbsolutePath();
    }

    private void select(long time, TickStream stream, String etalonFile, boolean usePrivate)
            throws IOException, InterruptedException
    {
        final SelectionOptions options = new SelectionOptions();
        options.typeLoader = getTypeLoader(usePrivate);
        final TickCursor cursor = stream.select(time, options);

        final StringWriter os = new StringWriter();
        final PrintWriter out = new PrintWriter(os);
        try {
            printCursor(cursor, out);
        } finally {
            cursor.close();
            out.close();
        }

        String[] lines;

        try (InputStream in = getClass().getResourceAsStream(etalonFile)) {
            Reader reader = new LineNumberReader (new InputStreamReader (in));
            lines = IOUtil.readLinesFromReader(reader);
        }

        String etalon = StringUtils.join(System.lineSeparator(), lines);
        assertEquals("Data log is not the same as etalon", etalon, os.toString());
    }

    private void printCursor(TickCursor cur, PrintWriter out) {
        if (cur == null)
            out.println("NO DATA");
        else {
            final ArrayList<InstrumentMessage> list = new ArrayList<InstrumentMessage>();
            while (cur.next())
                list.add(cur.getMessage().clone());

            final InstrumentMessage[] array = list.toArray(new InstrumentMessage[list.size()]);
            sort(array);

            for (int i = 0; i < array.length; i++) {
                if (i > 0)
                    out.append(System.lineSeparator());

                out.print(array[i]);
            }
        }
    }

    // taken from Test_TickDBLoader
    private static void sort(InstrumentMessage[] messages) {
        int idx1, idx2;
        idx1 = idx2 = 0;
        while (idx1 < messages.length) {
            long ts = messages[idx1].getTimeStampMs();
            for (int i = idx1; i < messages.length; i++) {
                InstrumentMessage message = messages[i];
                if (message.getTimeStampMs() != ts) {
                    idx2 = i;
                    break;
                }
            }
            if (idx2 - idx1 > 1)
                Arrays.sort(messages, idx1, idx2, new Comparator<InstrumentMessage>() {
                    public int compare(InstrumentMessage o1, InstrumentMessage o2) {
                        assert Util.compare(o1.getTimeStampMs(), o2.getTimeStampMs()) == 0;
                        return Util.compare(o1.getSymbol(), o2.getSymbol(), true);
                    }
                });
            idx1 = idx2;
            idx2 = messages.length;
        }
    }

    protected TypeLoader getTypeLoader(boolean usePrivate) {
        return new HashMapTypeLoader(Test_Decoder4Truncated.class.getClassLoader(), usePrivate);
    }

    protected String getSuffix() {
        return "Ex";
    }

    public static class TradeExtended extends TradeMessage {
        //public byte[] binary_1;
        public ByteArrayList binary_1;
        //public boolean boolean_2;
        public char char_3;
        public long timestamp_4;
        public Kind enum_5;
        public float ieee32_6;
        public double ieee64_7;
        public double decimal_8;
        public double decimal_9;
        public byte int8_10;
        public short int16_11;
        public int int32_12;
        public long int48_13;
        public long int64_14;
        public int puint30_15;
        public long puint61_16;
        public int pinterval_17;
        public String utf8_18;
        public String alphanumeric_19;
        public long alphanumeric_20;
        public int timeofday_21;

        @Override
        public TradeMessage copyFrom(RecordInfo template) {
            super.copyFrom(template);

            if (template instanceof TradeExtended) {
                TradeExtended ext = (TradeExtended)template;

                this.binary_1 = ext.binary_1;
                //msg.boolean_2 = this.boolean_2;
                this.char_3 = ext.char_3;
                this.timestamp_4 = ext.timestamp_4;
                this.enum_5 = ext.enum_5;
                this.ieee32_6 = ext.ieee32_6;
                this.ieee64_7 = ext.ieee64_7;
                this.decimal_8 = ext.decimal_8;
                this.decimal_9 = ext.decimal_9;
                this.int8_10 = ext.int8_10;
                this.int16_11 = ext.int16_11;
                this.int32_12 = ext.int32_12;
                this.int48_13 = ext.int48_13;
                this.int64_14 = ext.int64_14;
                this.puint30_15 = ext.puint30_15;
                this.puint61_16 = ext.puint61_16;
                this.pinterval_17 = ext.pinterval_17;
                this.utf8_18 = ext.utf8_18;
                this.alphanumeric_19 = ext.alphanumeric_19;
                this.alphanumeric_20 = ext.alphanumeric_20;
                this.timeofday_21 = ext.timeofday_21;
            }

            return this;
        }

        @Override
        protected TradeMessage createInstance() {
            return new TradeExtended();
        }

//        @Override
//        public TradeExtended copy(boolean deep) {
//            final TradeExtended msg = new TradeExtended();
//            msg.copyFrom(this);
//
//            msg.binary_1 = this.binary_1;
//            //msg.boolean_2 = this.boolean_2;
//            msg.char_3 = this.char_3;
//            msg.timestamp_4 = this.timestamp_4;
//            msg.enum_5 = this.enum_5;
//            msg.ieee32_6 = this.ieee32_6;
//            msg.ieee64_7 = this.ieee64_7;
//            msg.decimal_8 = this.decimal_8;
//            msg.decimal_9 = this.decimal_9;
//            msg.int8_10 = this.int8_10;
//            msg.int16_11 = this.int16_11;
//            msg.int32_12 = this.int32_12;
//            msg.int48_13 = this.int48_13;
//            msg.int64_14 = this.int64_14;
//            msg.puint30_15 = this.puint30_15;
//            msg.puint61_16 = this.puint61_16;
//            msg.pinterval_17 = this.pinterval_17;
//            msg.utf8_18 = this.utf8_18;
//            msg.alphanumeric_19 = this.alphanumeric_19;
//            msg.alphanumeric_20 = this.alphanumeric_20;
//            msg.timeofday_21 = this.timeofday_21;
//            return (msg);
//        }

        @Override
        public String toString() {
            return String.format("%1$s,[%2$s],%3$b,%4$d,%5$d,%6$s,%7$f,%8$f,%9$f,%10$f,%11$d,%12$d,%13$d,%14$d,%15$d,%16$d,%17$d,%18$d,%19$s,%20$s,%21$d,%22$d",
                    String.format(
                            "Trade,%s,%s,%.2f,%.2f,%s,%s,%s,%.2f,%s,%s,%s",
                            symbol,
                            getTimeString(),
                            price,
                            size,
                            condition,
                            aggressorSide,
                            -1, // old beginMatch
                            netPriceChange,
                            eventType != null ? eventType.name() : "",
                            ExchangeCodec.longToCode(exchangeId),
                            getCurrencyCode ()
                    ),
                    binary_1 != null ? Util.arraydump(binary_1.getInternalBuffer(), 0, Math.min(5, binary_1.size())) : "null",
                    false, //boolean_2,
                    (int) char_3,
                    timestamp_4,
                    enum_5,
                    ieee32_6,
                    ieee64_7,
                    decimal_8,
                    decimal_9,
                    int8_10,
                    int16_11,
                    int32_12,
                    int48_13,
                    int64_14,
                    puint30_15,
                    puint61_16,
                    pinterval_17,
                    utf8_18,
                    alphanumeric_19,
                    alphanumeric_20,
                    timeofday_21);
        }
    }

    public static class TradeExtendedPrivate extends TradeMessage {
        //private byte[] binary_1;
        public ByteArrayList binary_1;
        //private boolean boolean_2;
        private char char_3;
        private long timestamp_4;
        private Kind enum_5;
        private float ieee32_6;
        private double ieee64_7;
        private double decimal_8;
        private double decimal_9;
        private byte int8_10;
        private short int16_11;
        private int int32_12;
        private long int48_13;
        private long int64_14;
        private int puint30_15;
        private long puint61_16;
        private int pinterval_17;
        private String utf8_18;
        private String alphanumeric_19;
        private long alphanumeric_20;
        private int timeofday_21;

        @SchemaElement
        public String getAlphanumeric_19 () {
            return alphanumeric_19;
        }

        public void setAlphanumeric_19 (String alphanumeric_19) {
            this.alphanumeric_19 = alphanumeric_19;
        }

        @SchemaElement
        public long getAlphanumeric_20 () {
            return alphanumeric_20;
        }

        public void setAlphanumeric_20 (long alphanumeric_20) {
            this.alphanumeric_20 = alphanumeric_20;
        }

        @SchemaElement
        public char getChar_3 () {
            return char_3;
        }

        public void setChar_3 (char char_3) {
            this.char_3 = char_3;
        }

        @SchemaElement
        public double getDecimal_8 () {
            return decimal_8;
        }

        public void setDecimal_8 (double decimal_8) {
            this.decimal_8 = decimal_8;
        }

        @SchemaElement
        public double getDecimal_9 () {
            return decimal_9;
        }

        public void setDecimal_9 (double decimal_9) {
            this.decimal_9 = decimal_9;
        }

        @SchemaElement
        public Kind getEnum_5 () {
            return enum_5;
        }

        public void setEnum_5 (Kind enum_5) {
            this.enum_5 = enum_5;
        }

        @SchemaElement
        public float getIeee32_6 () {
            return ieee32_6;
        }

        public void setIeee32_6 (float ieee32_6) {
            this.ieee32_6 = ieee32_6;
        }

        @SchemaElement
        public double getIeee64_7 () {
            return ieee64_7;
        }

        public void setIeee64_7 (double ieee64_7) {
            this.ieee64_7 = ieee64_7;
        }

        @SchemaElement
        public short getInt16_11 () {
            return int16_11;
        }

        public void setInt16_11 (short int16_11) {
            this.int16_11 = int16_11;
        }

        @SchemaElement
        public int getInt32_12 () {
            return int32_12;
        }

        public void setInt32_12 (int int32_12) {
            this.int32_12 = int32_12;
        }

        @SchemaElement
        public long getInt48_13 () {
            return int48_13;
        }

        public void setInt48_13 (long int48_13) {
            this.int48_13 = int48_13;
        }

        @SchemaElement
        public long getInt64_14 () {
            return int64_14;
        }

        public void setInt64_14 (long int64_14) {
            this.int64_14 = int64_14;
        }

        @SchemaElement
        public byte getInt8_10 () {
            return int8_10;
        }

        public void setInt8_10 (byte int8_10) {
            this.int8_10 = int8_10;
        }

        @SchemaElement
        public int getPinterval_17 () {
            return pinterval_17;
        }

        public void setPinterval_17 (int pinterval_17) {
            this.pinterval_17 = pinterval_17;
        }

        @SchemaElement
        public int getPuint30_15 () {
            return puint30_15;
        }

        public void setPuint30_15 (int puint30_15) {
            this.puint30_15 = puint30_15;
        }

        @SchemaElement
        public long getPuint61_16 () {
            return puint61_16;
        }

        public void setPuint61_16 (long puint61_16) {
            this.puint61_16 = puint61_16;
        }

        @SchemaElement
        public int getTimeofday_21 () {
            return timeofday_21;
        }

        public void setTimeofday_21 (int timeofday_21) {
            this.timeofday_21 = timeofday_21;
        }

        @SchemaElement
        public long getTimestamp_4 () {
            return timestamp_4;
        }

        public void setTimestamp_4 (long timestamp_4) {
            this.timestamp_4 = timestamp_4;
        }

        @SchemaElement
        public String getUtf8_18 () {
            return utf8_18;
        }

        public void setUtf8_18 (String utf8_18) {
            this.utf8_18 = utf8_18;
        }

        @Override
        protected TradeMessage createInstance() {
            return new TradeExtendedPrivate();
        }

        @Override
        public TradeMessage copyFrom(RecordInfo template) {
            super.copyFrom(template);

            if (template instanceof TradeExtendedPrivate) {
                TradeExtendedPrivate ext = (TradeExtendedPrivate)template;

                this.binary_1 = ext.binary_1;
                //msg.boolean_2 = this.boolean_2;
                this.char_3 = ext.char_3;
                this.timestamp_4 = ext.timestamp_4;
                this.enum_5 = ext.enum_5;
                this.ieee32_6 = ext.ieee32_6;
                this.ieee64_7 = ext.ieee64_7;
                this.decimal_8 = ext.decimal_8;
                this.decimal_9 = ext.decimal_9;
                this.int8_10 = ext.int8_10;
                this.int16_11 = ext.int16_11;
                this.int32_12 = ext.int32_12;
                this.int48_13 = ext.int48_13;
                this.int64_14 = ext.int64_14;
                this.puint30_15 = ext.puint30_15;
                this.puint61_16 = ext.puint61_16;
                this.pinterval_17 = ext.pinterval_17;
                this.utf8_18 = ext.utf8_18;
                this.alphanumeric_19 = ext.alphanumeric_19;
                this.alphanumeric_20 = ext.alphanumeric_20;
                this.timeofday_21 = ext.timeofday_21;
            }

            return (this);
        }

//        @Override
//        public TradeExtendedPrivate copy(boolean deep) {
//
//            this.binary_1 = this.binary_1;
//            //msg.boolean_2 = this.boolean_2;
//            this.char_3 = this.char_3;
//            this.timestamp_4 = this.timestamp_4;
//            this.enum_5 = this.enum_5;
//            this.ieee32_6 = this.ieee32_6;
//            this.ieee64_7 = this.ieee64_7;
//            this.decimal_8 = this.decimal_8;
//            this.decimal_9 = this.decimal_9;
//            this.int8_10 = this.int8_10;
//            this.int16_11 = this.int16_11;
//            this.int32_12 = this.int32_12;
//            this.int48_13 = this.int48_13;
//            this.int64_14 = this.int64_14;
//            this.puint30_15 = this.puint30_15;
//            this.puint61_16 = this.puint61_16;
//            this.pinterval_17 = this.pinterval_17;
//            this.utf8_18 = this.utf8_18;
//            this.alphanumeric_19 = this.alphanumeric_19;
//            this.alphanumeric_20 = this.alphanumeric_20;
//            this.timeofday_21 = this.timeofday_21;
//            return (this);
//        }

        @Override
        public String toString() {
            return String.format("%1$s,[%2$s],%3$b,%4$d,%5$d,%6$s,%7$f,%8$f,%9$f,%10$f,%11$d,%12$d,%13$d,%14$d,%15$d,%16$d,%17$d,%18$d,%19$s,%20$s,%21$d,%22$d",
                    String.format(
                            "Trade,%s,%s,%.2f,%.2f,%s,%s,%s,%.2f,%s,%s,%s",
                            symbol,
                            getTimeString(),
                            price,
                            size,
                            condition,
                            aggressorSide,
                            -1, // old beginMatch
                            netPriceChange,
                            eventType != null ? eventType.name() : "",
                            ExchangeCodec.longToCode(exchangeId),
                            getCurrencyCode ()
                    ),
                    binary_1 != null ? Util.arraydump(binary_1.getInternalBuffer(), 0, Math.min(5, binary_1.size())) : "null",
                    false, //boolean_2,
                    (int) char_3,
                    timestamp_4,
                    enum_5,
                    ieee32_6,
                    ieee64_7,
                    decimal_8,
                    decimal_9,
                    int8_10,
                    int16_11,
                    int32_12,
                    int48_13,
                    int64_14,
                    puint30_15,
                    puint61_16,
                    pinterval_17,
                    utf8_18,
                    alphanumeric_19,
                    alphanumeric_20,
                    timeofday_21);
        }
    }

    public static class BarExtended extends BarMessage {
        //public byte[] binary_1;
        public ByteArrayList binary_1;
        //public boolean boolean_2;
        public char char_3;
        public long timestamp_4;
        public Kind enum_5;
        public float ieee32_6;
        public double ieee64_7;
        public double decimal_8;
        public double decimal_9;
        public byte int8_10;
        public short int16_11;
        public int int32_12;
        public long int48_13;
        public long int64_14;
        public int puint30_15;
        public long puint61_16;
        public int pinterval_17;
        public String utf8_18;
        public String alphanumeric_19;
        public long alphanumeric_20;
        public int timeofday_21;

        @Override
        protected BarMessage createInstance() {
            return new BarExtended();
        }

        @Override
        public BarMessage copyFrom(RecordInfo template) {
            super.copyFrom(template);

            if (template instanceof BarExtended) {
                BarExtended ext = (BarExtended) template;

                this.binary_1 = ext.binary_1;
                this.char_3 = ext.char_3;
                this.timestamp_4 = ext.timestamp_4;
                this.enum_5 = ext.enum_5;
                this.ieee32_6 = ext.ieee32_6;
                this.ieee64_7 = ext.ieee64_7;
                this.decimal_8 = ext.decimal_8;
                this.decimal_9 = ext.decimal_9;
                this.int8_10 = ext.int8_10;
                this.int16_11 = ext.int16_11;
                this.int32_12 = ext.int32_12;
                this.int48_13 = ext.int48_13;
                this.int64_14 = ext.int64_14;
                this.puint30_15 = ext.puint30_15;
                this.puint61_16 = ext.puint61_16;
                this.pinterval_17 = ext.pinterval_17;
                this.utf8_18 = ext.utf8_18;
                this.alphanumeric_19 = ext.alphanumeric_19;
                this.alphanumeric_20 = ext.alphanumeric_20;
                this.timeofday_21 = ext.timeofday_21;
            }
            return (this);
        }

//        @Override
//        public BarExtended copy(boolean deep) {
//            final BarExtended msg = new BarExtended();
//            msg.copyFrom(this);
//
//            msg.binary_1 = this.binary_1;
//            //msg.boolean_2 = this.boolean_2;
//            msg.char_3 = this.char_3;
//            msg.timestamp_4 = this.timestamp_4;
//            msg.enum_5 = this.enum_5;
//            msg.ieee32_6 = this.ieee32_6;
//            msg.ieee64_7 = this.ieee64_7;
//            msg.decimal_8 = this.decimal_8;
//            msg.decimal_9 = this.decimal_9;
//            msg.int8_10 = this.int8_10;
//            msg.int16_11 = this.int16_11;
//            msg.int32_12 = this.int32_12;
//            msg.int48_13 = this.int48_13;
//            msg.int64_14 = this.int64_14;
//            msg.puint30_15 = this.puint30_15;
//            msg.puint61_16 = this.puint61_16;
//            msg.pinterval_17 = this.pinterval_17;
//            msg.utf8_18 = this.utf8_18;
//            msg.alphanumeric_19 = this.alphanumeric_19;
//            msg.alphanumeric_20 = this.alphanumeric_20;
//            msg.timeofday_21 = this.timeofday_21;
//            return (msg);
//        }

        @Override
        public String toString() {
            return String.format("%1$s,[%2$s],%3$b,%4$d,%5$d,%6$s,%7$f,%8$f,%9$f,%10$f,%11$d,%12$d,%13$d,%14$d,%15$d,%16$d,%17$d,%18$d,%19$s,%20$s,%21$d,%22$d",
                    String.format (
                            "BAR,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%s",
                            symbol.toString (),
                            getTimeString(),
                            open,
                            high,
                            low,
                            close,
                            volume,
                            ExchangeCodec.longToCode(exchangeId),
                            getCurrencyCode ()
                    ),
                    binary_1 != null ? Util.arraydump(binary_1.getInternalBuffer(), 0, Math.min(5, binary_1.size())) : "null",
                    false, //boolean_2,
                    (int) char_3,
                    timestamp_4,
                    enum_5,
                    ieee32_6,
                    ieee64_7,
                    decimal_8,
                    decimal_9,
                    int8_10,
                    int16_11,
                    int32_12,
                    int48_13,
                    int64_14,
                    puint30_15,
                    puint61_16,
                    pinterval_17,
                    utf8_18,
                    alphanumeric_19,
                    alphanumeric_20,
                    timeofday_21);
        }
    }

    public static class BarExtendedPrivate extends BarMessage {
        //private byte[] binary_1;
        public ByteArrayList binary_1;
        //private boolean boolean_2;
        private char char_3;
        private long timestamp_4;
        private Kind enum_5;
        private float ieee32_6;
        private double ieee64_7;
        private double decimal_8;
        private double decimal_9;
        private byte int8_10;
        private short int16_11;
        private int int32_12;
        private long int48_13;
        private long int64_14;
        private int puint30_15;
        private long puint61_16;
        private int pinterval_17;
        private String utf8_18;
        private String alphanumeric_19;
        private long alphanumeric_20;
        private int timeofday_21;

        @SchemaElement
        public long getAlphanumeric_20 () {
            return alphanumeric_20;
        }

        public void setAlphanumeric_20 (long alphanumeric_20) {
            this.alphanumeric_20 = alphanumeric_20;
        }

        @SchemaElement
        public String getAlphanumeric_19 () {
            return alphanumeric_19;
        }

        public void setAlphanumeric_19 (String alphanumeric_19) {
            this.alphanumeric_19 = alphanumeric_19;
        }

        @SchemaElement
        public char getChar_3 () {
            return char_3;
        }

        public void setChar_3 (char char_3) {
            this.char_3 = char_3;
        }

        @SchemaElement
        public double getDecimal_8 () {
            return decimal_8;
        }

        public void setDecimal_8 (double decimal_8) {
            this.decimal_8 = decimal_8;
        }

        @SchemaElement
        public double getDecimal_9 () {
            return decimal_9;
        }

        public void setDecimal_9 (double decimal_9) {
            this.decimal_9 = decimal_9;
        }

        @SchemaElement
        public Kind getEnum_5 () {
            return enum_5;
        }

        public void setEnum_5 (Kind enum_5) {
            this.enum_5 = enum_5;
        }

        @SchemaElement
        public float getIeee32_6 () {
            return ieee32_6;
        }

        public void setIeee32_6 (float ieee32_6) {
            this.ieee32_6 = ieee32_6;
        }

        @SchemaElement
        public double getIeee64_7 () {
            return ieee64_7;
        }

        public void setIeee64_7 (double ieee64_7) {
            this.ieee64_7 = ieee64_7;
        }

        @SchemaElement
        public short getInt16_11 () {
            return int16_11;
        }

        public void setInt16_11 (short int16_11) {
            this.int16_11 = int16_11;
        }

        @SchemaElement
        public int getInt32_12 () {
            return int32_12;
        }

        public void setInt32_12 (int int32_12) {
            this.int32_12 = int32_12;
        }

        @SchemaElement
        public long getInt48_13 () {
            return int48_13;
        }

        public void setInt48_13 (long int48_13) {
            this.int48_13 = int48_13;
        }

        @SchemaElement
        public long getInt64_14 () {
            return int64_14;
        }

        public void setInt64_14 (long int64_14) {
            this.int64_14 = int64_14;
        }

        @SchemaElement
        public byte getInt8_10 () {
            return int8_10;
        }

        public void setInt8_10 (byte int8_10) {
            this.int8_10 = int8_10;
        }

        @SchemaElement
        public int getPinterval_17 () {
            return pinterval_17;
        }

        public void setPinterval_17 (int pinterval_17) {
            this.pinterval_17 = pinterval_17;
        }

        @SchemaElement
        public int getPuint30_15 () {
            return puint30_15;
        }

        public void setPuint30_15 (int puint30_15) {
            this.puint30_15 = puint30_15;
        }

        @SchemaElement
        public long getPuint61_16 () {
            return puint61_16;
        }

        public void setPuint61_16 (long puint61_16) {
            this.puint61_16 = puint61_16;
        }

        @SchemaElement
        public int getTimeofday_21 () {
            return timeofday_21;
        }

        public void setTimeofday_21 (int timeofday_21) {
            this.timeofday_21 = timeofday_21;
        }

        @SchemaElement
        public long getTimestamp_4 () {
            return timestamp_4;
        }

        public void setTimestamp_4 (long timestamp_4) {
            this.timestamp_4 = timestamp_4;
        }

        @SchemaElement
        public String getUtf8_18 () {
            return utf8_18;
        }

        public void setUtf8_18 (String utf8_18) {
            this.utf8_18 = utf8_18;
        }

        @Override
        protected BarMessage createInstance() {
            return new BarExtendedPrivate();
        }

        @Override
        public BarMessage copyFrom(RecordInfo template) {
            super.copyFrom(template);

            if (template instanceof BarExtendedPrivate) {
                BarExtendedPrivate ext = (BarExtendedPrivate)template;

                this.binary_1 = ext.binary_1;
                this.char_3 = ext.char_3;
                this.timestamp_4 = ext.timestamp_4;
                this.enum_5 = ext.enum_5;
                this.ieee32_6 = ext.ieee32_6;
                this.ieee64_7 = ext.ieee64_7;
                this.decimal_8 = ext.decimal_8;
                this.decimal_9 = ext.decimal_9;
                this.int8_10 = ext.int8_10;
                this.int16_11 = ext.int16_11;
                this.int32_12 = ext.int32_12;
                this.int48_13 = ext.int48_13;
                this.int64_14 = ext.int64_14;
                this.puint30_15 = ext.puint30_15;
                this.puint61_16 = ext.puint61_16;
                this.pinterval_17 = ext.pinterval_17;
                this.utf8_18 = ext.utf8_18;
                this.alphanumeric_19 = ext.alphanumeric_19;
                this.alphanumeric_20 = ext.alphanumeric_20;
                this.timeofday_21 = ext.timeofday_21;
            }

            return this;
        }

//        @Override
//        public BarExtendedPrivate copy(boolean deep) {
//            final BarExtendedPrivate msg = new BarExtendedPrivate();
//            msg.copyFrom(this);
//
//            msg.binary_1 = this.binary_1;
//            //msg.boolean_2 = this.boolean_2;
//            msg.char_3 = this.char_3;
//            msg.timestamp_4 = this.timestamp_4;
//            msg.enum_5 = this.enum_5;
//            msg.ieee32_6 = this.ieee32_6;
//            msg.ieee64_7 = this.ieee64_7;
//            msg.decimal_8 = this.decimal_8;
//            msg.decimal_9 = this.decimal_9;
//            msg.int8_10 = this.int8_10;
//            msg.int16_11 = this.int16_11;
//            msg.int32_12 = this.int32_12;
//            msg.int48_13 = this.int48_13;
//            msg.int64_14 = this.int64_14;
//            msg.puint30_15 = this.puint30_15;
//            msg.puint61_16 = this.puint61_16;
//            msg.pinterval_17 = this.pinterval_17;
//            msg.utf8_18 = this.utf8_18;
//            msg.alphanumeric_19 = this.alphanumeric_19;
//            msg.alphanumeric_20 = this.alphanumeric_20;
//            msg.timeofday_21 = this.timeofday_21;
//            return (msg);
//        }

        @Override
        public String toString() {
            return String.format("%1$s,[%2$s],%3$b,%4$d,%5$d,%6$s,%7$f,%8$f,%9$f,%10$f,%11$d,%12$d,%13$d,%14$d,%15$d,%16$d,%17$d,%18$d,%19$s,%20$s,%21$d,%22$d",
                    String.format (
                            "BAR,%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%s,%s",
                            symbol.toString (),
                            getTimeString(),
                            open,
                            high,
                            low,
                            close,
                            volume,
                            ExchangeCodec.longToCode(exchangeId),
                            getCurrencyCode ()
                    ),
                    binary_1 != null ? Util.arraydump(binary_1.getInternalBuffer(), 0, Math.min(5, binary_1.size())) : "null",
                    false, //boolean_2,
                    (int) char_3,
                    timestamp_4,
                    enum_5,
                    ieee32_6,
                    ieee64_7,
                    decimal_8,
                    decimal_9,
                    int8_10,
                    int16_11,
                    int32_12,
                    int48_13,
                    int64_14,
                    puint30_15,
                    puint61_16,
                    pinterval_17,
                    utf8_18,
                    alphanumeric_19,
                    alphanumeric_20,
                    timeofday_21);
        }
    }

    private static class HashMapTypeLoader extends TypeLoaderImpl {
        private final HashMap<String, Class> classCache = new HashMap<String, Class>();

        public HashMapTypeLoader(ClassLoader loader, boolean usePrivate) {
            super(loader);

            if (usePrivate) {
                classCache.put(TradeMessage.class.getName(), TradeExtendedPrivate.class);
                classCache.put(TradeExtended.class.getName(), TradeExtendedPrivate.class);
                classCache.put(BarMessage.class.getName(), BarExtendedPrivate.class);
                classCache.put(BarExtended.class.getName(), BarExtendedPrivate.class);
            } else {
                classCache.put(TradeMessage.class.getName(), TradeExtended.class);
                classCache.put(BarMessage.class.getName(), BarExtended.class);
            }
        }

        @Override
        public Class<?> load(ClassDescriptor cd, ExceptionHandler handler) throws ClassNotFoundException {
            String name = cd.getName();
            final Class clazz = classCache.get(name);
            return clazz != null ? clazz : super.load(cd, handler);
        }

        @Override
        public Class load(ClassDescriptor cd) throws ClassNotFoundException {
           return load(cd, null);
        }
    }
}