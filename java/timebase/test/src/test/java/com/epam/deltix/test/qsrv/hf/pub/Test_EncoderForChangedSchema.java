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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.schema.StreamMetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.schema.SchemaAnalyzer;
import com.epam.deltix.qsrv.hf.tickdb.schema.MetaDataChange;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.TickDBShell;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.io.Home;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;

import java.io.File;

/**
 * Tests unbound encoders upon adding to RecordClassDescriptor nullable fields (with no data) 
 */
public class Test_EncoderForChangedSchema  {
    private static File DIR = new File(Home.get() + "\\testdata\\qsrv\\hf\\tickdb");
    private static String STREAM_NAME = "trade";

    private CodecFactory    factory;
    private DXTickDB        db;
    private DXTickStream    stream;

    @Before
    public void setup() throws Throwable {
        db = TickDBFactory.create(TDBRunner.getTemporaryLocation());
        db.open(false);

        loadTrades(db, 0);

        stream = db.getStream(STREAM_NAME);
    }

    @After
    public void tearDown() {
        db.close();
        db.delete();
    }

    @Test
    public void test3DecimalComp() throws Exception {
        setUpComp();
        test3Decimal();
    }

    @Test
    public void test3DecimalIntp() throws Exception {
        setUpIntp();
        test3Decimal();
    }

    private static final DataField[] DECIMAL_FIELDS = {
        new NonStaticDataField("f1", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
        new NonStaticDataField("f2", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
        new NonStaticDataField("f3", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true))
    };

    private void test3Decimal() throws Exception {
        changeSchema(DECIMAL_FIELDS);

        final UnboundDecoder decoder = factory.createFixedUnboundDecoder(stream.getFixedType());
        testScroll(decoder);
        testNextFieldAndIsNull(decoder, DECIMAL_FIELDS.length);
    }


    @Test
    public void testAllTypesComp() throws Exception {
        setUpComp();
        testAllTypes();
    }

    @Test
    public void testAllTypesIntp() throws Exception {
        setUpIntp();
        testAllTypes();
    }

    static final EnumClassDescriptor    cdKind =
        new EnumClassDescriptor (
            "Kind Descriptor",
            "My Enum class def",
            "BIG", "SMALL", "BEAUTIFUL"
        );

    private static final DataField[] ALL_TYPES_FIELDS = {
        new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
        new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
        new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
        new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
        new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
        new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
        new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
        new NonStaticDataField("mEnum", null, new EnumDataType(true, cdKind)),
        new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
        new NonStaticDataField("mBoolean", null, new BooleanDataType(true), true, null),
        new NonStaticDataField("mChar", null, new CharDataType(true)),
        new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
        new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

        new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true), true, null),
        new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
        new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
        new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
        new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true), true, null)
    };

    private void testAllTypes() throws Exception {
        changeSchema(ALL_TYPES_FIELDS);

        final UnboundDecoder decoder = factory.createFixedUnboundDecoder(stream.getFixedType());
        testScroll(decoder);
        testNextFieldAndIsNull(decoder, ALL_TYPES_FIELDS.length);
    }

    private static void loadTrades(DXTickDB db, int factor) throws Throwable {
        if (!Boolean.getBoolean("quiet"))
            System.out.println("distribution_factor=" + factor);

        DXTickStream trades = db.createStream(STREAM_NAME, STREAM_NAME, null, factor);
        StreamConfigurationHelper.setTrade(trades, null, null);
        TickDBShell.loadMessageFile(new File(DIR, "trade.qsmsg"), trades);

//        String[] args = {
//                "-db", path,
//                "-exec", "open",
//                "-exec", "mkstream", STREAM_NAME, factor,
//                "-exec", "cfgstream", STREAM_NAME,
//                "-exec", "set", "src", DIR + "\\trade.qsmsg",
//                "-exec", "import",
//                "-exec", "close",
//                "-exit"
//        };
//        TickDBShell.main(args);
    }

    private void changeSchema(DataField[] addFields) throws InterruptedException {
        final RecordClassSet oldSchema = new RecordClassSet();
        final RecordClassDescriptor oldRcd = stream.getFixedType();
        oldSchema.addContentClasses(oldRcd);

        final DataField[] oldFields = oldRcd.getFields();
        final DataField[] newFields = new DataField[oldFields.length + addFields.length];
        System.arraycopy(oldFields, 0, newFields, 0, oldFields.length);
        System.arraycopy(addFields, 0, newFields, oldFields.length, addFields.length);
        final RecordClassDescriptor newRcd = new RecordClassDescriptor(oldRcd.getName(), null, false, oldRcd.getParent(), newFields);

        final RecordClassSet newSchema = new RecordClassSet();
        newSchema.addContentClasses(newRcd);

        // change schema
        final StreamMetaDataChange change = SchemaAnalyzer.DEFAULT.getChanges(
            oldSchema, MetaDataChange.ContentType.Fixed,
            newSchema, MetaDataChange.ContentType.Fixed);

        stream.execute(new SchemaChangeTask(change));

        BackgroundProcessInfo process;
        while ((process = stream.getBackgroundProcess()) != null && !process.isFinished())
            Thread.sleep(100);
/*
        do {
            final BackgroundProcessInfo process = stream.getBackgroundProcess();
            if (process == null || process.isFinished())
                break;


            Thread.sleep(100);
        } while (true);
*/
    }

    private void testScroll(UnboundDecoder decoder) {
        final MemoryDataInput in = new MemoryDataInput();
        TickCursor cur = null;
        try {
            cur = stream.select(0, new SelectionOptions(true, false));
            final StringBuilder sb = new StringBuilder();

            while (cur.next()) {
                final RawMessage raw = (RawMessage) cur.getMessage();
                raw.setUpMemoryDataInput(in);

                sb.append(raw.type.getName());
                sb.append(",");
                sb.append(raw.getSymbol());
                sb.append(",");
                sb.append(GMT.formatDateTimeMillis(raw.getTimeStampMs()));

                decoder.beginRead(in);
                while (decoder.nextField()) {
                    NonStaticFieldInfo df = decoder.getField();
                    sb.append(",");
                    sb.append(df.getName());
                    sb.append(":");
                    try {
                        sb.append(decoder.getString());
                    } catch (NullValueException e) {
                        sb.append("<null>");
                    }
                }
                //System.out.println(sb.toString());
                sb.setLength(0);
            }

        } finally {
            if (cur != null)
                cur.close();
        }
    }

    private void testNextFieldAndIsNull(UnboundDecoder decoder, int newFields) {
        final MemoryDataInput in = new MemoryDataInput();
        TickCursor cur = null;
        try {
            cur = stream.select(0, new SelectionOptions(true, false));
            while (cur.next()) {
                final RawMessage raw = (RawMessage) cur.getMessage();
                raw.setUpMemoryDataInput(in);

                decoder.beginRead(in);
                while (decoder.nextField()) {
                }
                Assert.assertTrue(in.getPosition() == in.getLength());


                raw.setUpMemoryDataInput(in);
                decoder.beginRead(in);
                for (int i = 0; decoder.nextField(); i++) {
                    Assert.assertTrue(decoder.getField().toString(), i >= decoder.getClassInfo().getNonStaticFields().length - newFields == decoder.isNull());
                }
                Assert.assertTrue(in.getPosition() == in.getLength());
            }

        } finally {
            if (cur != null)
                cur.close();
        }
    }

    private void setUpComp() {
        factory = CodecFactory.COMPILED;
    }

    private void setUpIntp() {
        factory = CodecFactory.INTERPRETED;
    }
}
