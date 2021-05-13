package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.test.messages.AggressorSide;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.test.qsrv.hf.tickdb.testframework.TestEnum;
import com.epam.deltix.timebase.messages.SchemaElement;
import com.epam.deltix.timebase.messages.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * User: BazylevD
 * Date: Apr 29, 2009
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecs2 extends Test_RecordCodecsBase {
    private static final String EXCEPTION_ETALON = "java.lang.IllegalArgumentException: Static value is out of range. Field mInt2 value 2147483648";
    private static final String EXCEPTION_ETALON2 = "java.lang.IllegalArgumentException: provided value exceeded Integer boundaries: 2147483648";
    private static final double EPSILON = 0.00001;

    public static class MsgClassInt {
        public int mInt;
        public int mInt2;

        public String toString() {
            return String.valueOf(mInt) + "," + mInt2;
        }
    }

    private static final RecordClassDescriptor cdMsgClassIntOk =
        new RecordClassDescriptor(
            MsgClassInt.class.getName(),
            "MsgClassIntOk static",
            false,
            null,                       
            new StaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false), 1),
            new StaticDataField("mInt2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), null)
        );

    @Test @Ignore("int binding was canceled")
    public void testStaticIntFieldsComp() throws Exception {
        setUpComp();
        testStaticIntFields();
    }

    @Test @Ignore("int binding was canceled")
    public void testStaticIntFieldsIntp() throws Exception {
        setUpIntp();
        testStaticIntFields();
    }

    private void testStaticIntFields() throws Exception {
        MsgClassInt msg = new MsgClassInt();
        msg.mInt = 1;
        msg.mInt2 = IntegerDataType.INT32_NULL;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassIntOk);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassInt msg2 = new MsgClassInt();
        boundDecode(msg2, cdMsgClassIntOk, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    private static final RecordClassDescriptor cdMsgClassIntBad =
        new RecordClassDescriptor(
            MsgClassInt.class.getName(),
            "MsgClassIntBad static",
            false,
            null,
            new StaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), null),
            new StaticDataField("mInt2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), (long) Integer.MAX_VALUE + 1)
        );

    @Test @Ignore("int binding was canceled")
    public void testStaticIntFieldsBadComp() throws Exception {
        setUpComp();
        testStaticIntFieldsBad();
    }

    @Test @Ignore("int binding was canceled")
    public void testStaticIntFieldsBadIntp() throws Exception {
        setUpIntp();
        testStaticIntFieldsBad();
    }

    private void testStaticIntFieldsBad() throws Exception {
        MsgClassInt msg = new MsgClassInt();
        msg.mInt = IntegerDataType.INT32_NULL;
        msg.mInt2 = 0;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassIntBad);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassInt msg2 = new MsgClassInt();
        try {
            boundDecode(msg2, cdMsgClassIntBad, in);
            Assert.fail("java.lang.IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals(EXCEPTION_ETALON, e.toString());
        } catch (RuntimeException e) {
            if (e.getCause() != null)
                assertEquals(EXCEPTION_ETALON2, e.getCause().toString());
            else
                throw e;
        }
    }

    public static class MsgClassFwdString {
        public String mString;
        public String mString2;
        public float mFloat;
        public String mString3;
        public float mFloat2;
        public String mString4;

        public String toString() {
            return mString + "," + mString2 + "," + mFloat + "," + mString3 + "," + mFloat2 + "," + mString4;
        }
    }

    @Test
    public void testSkipNonNullableComp() throws Exception {
        setUpComp();
        testSkipNonNullable();
    }

    @Test
    public void testSkipNonNullableIntp() throws Exception {
        setUpIntp();
        testSkipNonNullable();
    }

    private static final RecordClassDescriptor cdMsgClassNonNullable =
        new RecordClassDescriptor(
            "My MsgClassNonNullable Descriptor",
            "MsgClassNonNullable",
            false,
            null,
            new NonStaticDataField("mDouble1", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mDouble2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mDouble3", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mScale", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, false)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
            new NonStaticDataField("mBoolean1", null, new BooleanDataType(false)),
            new NonStaticDataField("mBoolean2", null, new BooleanDataType(false)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true))
        );

    private void testSkipNonNullable() throws Exception {
        final List<Object> values = new ArrayList<Object>(8);
        values.add(1.11);
        values.add(2.22);
        values.add(3.33);
        values.add(400.44);
        values.add("Hi Nicolia!");
        values.add(true);
        values.add(false);
        values.add(0xCCAADDEE); // check value

        MemoryDataOutput out = unboundEncode(values, cdMsgClassNonNullable);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClassNonNullable);
        udec.beginRead(in);
        // skip 3 double, 2 var-Size and 1 boolean field
        for (int i = 0; i < 6; i++)
            udec.nextField();

        udec.nextField();
        assertEquals(false, udec.isNull());
        assertEquals(Boolean.FALSE, readField(udec));
        udec.nextField();
        assertEquals(0xCCAADDEE, ((Number) readField(udec)).intValue());
    }


    @Test
    public void testSkipBaseFieldComp() throws Exception {
        setUpComp();
        testSkipBaseField();
    }

    @Test
    public void testSkipBaseFieldIntp() throws Exception {
        setUpIntp();
        testSkipBaseField();
    }

    private static final RecordClassDescriptor cdMsgClassRelativeEnc =
        new RecordClassDescriptor(
            "My RelativeEnc Descriptor",
            "MsgClassRelativeEnc",
            false,
            null,
            new NonStaticDataField("close", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("open", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true), "close")
        );

    private void testSkipBaseField() throws Exception {
        final List<Object> values = new ArrayList<Object>(5);
        values.add(1.11);
        values.add(1.22);

        MemoryDataOutput out = unboundEncode(values, cdMsgClassRelativeEnc);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());

        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClassRelativeEnc);
        udec.beginRead(in);
        // skip base field
        udec.nextField();
        //readField(udec);

        udec.nextField();
        assertEquals(1.22, ((Number) readField(udec)).doubleValue(), EPSILON);
    }

    @Test
    public void testUnboundEnumComp() throws Exception {
        setUpComp();
        testUnboundEnum();
    }

    @Test
    public void testUnboundEnumIntp() throws Exception {
        setUpIntp();
        testUnboundEnum();
    }

    private static final EnumClassDescriptor cdKind =
        new EnumClassDescriptor(
            "Kind Descriptor",
            "Kind Enum",
            "BIG", "SMALL", "BEAUTIFUL"
        );

    private static final RecordClassDescriptor cdMsgClassWithEnum =
        new RecordClassDescriptor(
            "My WithEnum Descriptor",
            "MsgClassWithEnum",
            false,
            null,
            new StaticDataField("staticEnum", null, new EnumDataType(true, cdKind), "SMALL"),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdKind))
        );

    private void testUnboundEnum() throws Exception {
        final List<Object> values = new ArrayList<Object>(5);
        //values.add("SMALL");
        values.add("BEAUTIFUL");

        MemoryDataOutput out = unboundEncode(values, cdMsgClassWithEnum);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, cdMsgClassWithEnum);
        assertValuesEquals(values, values2, cdMsgClassWithEnum);
    }

    @Test
    public void testNextFieldComp() throws Exception {
        setUpComp();
        testNextField();
    }

    @Test
    public void testNextFieldIntp() throws Exception {
        setUpIntp();
        testNextField();
    }

    private static final RecordClassDescriptor cdMsgClass2Doubles =
        new RecordClassDescriptor(
            "My 2Doubles Descriptor",
            "MsgClass2Doubles",
            false,
            null,
            new NonStaticDataField("close", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("open", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
        );

    private void testNextField() throws Exception {
        final double value = 5.55;
        FixedUnboundEncoder uenc = factory.createFixedUnboundEncoder(cdMsgClass2Doubles);
        MemoryDataOutput out = new MemoryDataOutput();

        for (int i = 0; i < 2; i++) {
            uenc.beginWrite(out);

            int idx = 0;
            while (uenc.nextField()) {
                if (idx == 0)
                    uenc.writeDouble(value);
                idx++;
            }
            assertEquals(2, idx);
        }

        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClass2Doubles);

        for (int i = 0; i < 2; i++) {
            udec.beginRead(in);

            int idx = 0;
            while (udec.nextField()) {
                if (idx == 0)
                    assertEquals(value, udec.getDouble(), EPSILON);
                idx++;
            }
            assertEquals(2, idx);
        }
    }

    public static class MsgClassAlphanumeric {
        public int int1;
        public int int2;
        public long long1;
        public long long2;
        public String string1;
        public String string2;
        public CharSequence cs1;
        public CharSequence cs2;

        public String toString() {
            return long1 + "," + long2 + "," + string1 + "," + string2 + "," + cs1 + "," + cs2;
        }
    }

    private static final DataField[] ALPHANUMERIC_NULLABLE = {
                    //new NonStaticDataField("int1", null, new StringDataType(StringDataType.getEncodingAlphanumeric(5), true, false), true, null),
                    //new NonStaticDataField("int2", null, new StringDataType(StringDataType.getEncodingAlphanumeric(5), true, false)),
                    new NonStaticDataField("long1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), true, null),
                    new NonStaticDataField("long2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false)),
                    new NonStaticDataField("string1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), true, null),
                    new NonStaticDataField("string2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false)),
                    new NonStaticDataField("cs1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), true, null),
                    new NonStaticDataField("cs2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false))
            };

    private static final RecordClassDescriptor cdMsgClassAlphanumeric =
        new RecordClassDescriptor(
            MsgClassAlphanumeric.class.getName(),
            "MsgClassAlphanumeric",
            false,
            null,
            ALPHANUMERIC_NULLABLE
        );

    private static final DataField[] ALPHANUMERIC_NOT_NULLABLE = {
                    //new NonStaticDataField("int1", null, new StringDataType(StringDataType.getEncodingAlphanumeric(5), true, false), true, null),
                    //new NonStaticDataField("int2", null, new StringDataType(StringDataType.getEncodingAlphanumeric(5), false, false), true, null),
                    new NonStaticDataField("long1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false)),
                    new NonStaticDataField("long2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), false, false)),
                    new NonStaticDataField("string1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), true, null),
                    new NonStaticDataField("string2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), false, false), true, null),
                    new NonStaticDataField("cs1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), true, null),
                    new NonStaticDataField("cs2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), false, false), true, null)
            };

    private static final RecordClassDescriptor cdMsgClassAlphanumericNonNullable =
        new RecordClassDescriptor(
            MsgClassAlphanumeric.class.getName(),
            "MsgClassAlphanumericNonNullable",
            false,
            null,
            ALPHANUMERIC_NOT_NULLABLE
        );

    public static class MsgClassAlphanumericEx {
        public byte b1;
    }

    private static final RecordClassDescriptor cdMsgClassAlphanumericEx =
        new RecordClassDescriptor(
            MsgClassAlphanumericEx.class.getName(),
            "MsgClassAlphanumericEx string",
            false,
            null,
            new NonStaticDataField("b1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(5), true, false))
        );


    @Test
    public void testAlphanumericBoundComp() throws Exception {
        setUpComp();
        System.err.println("Provoking Velocity errors:");  // ;-)
//        SEVERE: Method setterCode threw exception for reference $util in template deltix/qsrv/hf/metadata/encoder.vtl at  [2,18]
//        SEVERE: Method setterCode threw exception for reference $helper in template deltix/qsrv/hf/metadata/encoder.vtl at  [2,1]
        testAlphanumericBound();
    }

    @Test
    public void testAlphanumericBoundIntp() throws Exception {
        setUpIntp();
        testAlphanumericBound();
    }

    private final static int INT_STRING = 0x94dda220; // INT1
    private final static long LONG_STRING = 0xab2fba74524d4556L; // LONG123456

    private void testAlphanumericBound() throws Exception {
        // test allowed nulls
        MsgClassAlphanumeric msg = new MsgClassAlphanumeric();
        msg.int1 = INT_STRING;
        msg.int2 = IntegerDataType.INT32_NULL;
        msg.long1 = LONG_STRING;
        msg.long2 = IntegerDataType.INT64_NULL;
        msg.string1 = "JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF";
        msg.string2 = null;
        msg.cs1 = "JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF";
        msg.cs2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassAlphanumeric);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAlphanumeric msg2 = new MsgClassAlphanumeric();
        boundDecode(msg2, cdMsgClassAlphanumeric, in);
        assertEquals(msg.toString(), msg2.toString());

        // on read
        boolean assertsEnabled = false;
        assert assertsEnabled = true;  // Intentional side-effect!!!

        // test not allowed nulls (on write)
/*
        try {
            boundEncode(msg, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'int2' field is not nullable", e.toString());
        }

        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgClassAlphanumeric);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgClassAlphanumericNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                assertEquals("java.lang.AssertionError: 'int2' field is not nullable", e.toString());
            }
        }
*/

        msg.int2 = 0;
        try {
            boundEncode(msg, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'long2' field is not nullable", e.toString());
        }
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgClassAlphanumeric);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgClassAlphanumericNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                assertEquals("java.lang.AssertionError: 'long2' field is not nullable", e.toString());
            } catch (IllegalStateException e) {
                // TODO: is this policy Ok?
                assertEquals("java.lang.IllegalStateException: cannot write null to not nullable field", e.toString());
            }
        }

        msg.long2 = 0;
        try {
            boundEncode(msg, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'string2' field is not nullable", e.toString());
        }
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgClassAlphanumeric);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgClassAlphanumericNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                assertEquals("java.lang.AssertionError: 'string2' field is not nullable", e.toString());
            } catch (IllegalStateException e) {
                assertEquals("java.lang.IllegalStateException: cannot write null to not nullable field", e.toString());
            }

        }

        // test wrong base type

        try {
            boundEncode(msg, cdMsgClassAlphanumericEx);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: Type byte is not supported.", e.toString());
        } catch (RuntimeException e) {
            final Throwable t = e.getCause();
            Assert.assertTrue(t != null);
            if (t instanceof IllegalStateException) {
                assertEquals("java.lang.IllegalStateException: unexpected bound type byte", t.toString());
            } else if (t instanceof UnsupportedOperationException) {
                assertEquals("java.lang.UnsupportedOperationException: unexpected bound type byte", t.toString());
            } else {
                assertEquals("java.lang.IllegalArgumentException: VARCHAR cannot be bound to byte field", t.toString());
            }
        }


    }

    public static class MsgClassAlphanumericPrivate {
        private int int1;
        private int int2;
        private long long1;
        private long long2;
        private String string1;
        private String string2;
        private CharSequence cs1;
        private CharSequence cs2;

        public String toString() {
            return /*int1 + "," + int2 + "," + */long1 + "," + long2 + "," + string1 + "," + string2 + ","
                    + cs1 + "," + cs2;
        }

        public int getInt1 () {
            return int1;
        }

        public void setInt1 (int int1) {
            this.int1 = int1;
        }

        public int getInt2 () {
            return int2;
        }

        public void setInt2 (int int2) {
            this.int2 = int2;
        }

        public long getLong1 () {
            return long1;
        }

        public void setLong1 (long long1) {
            this.long1 = long1;
        }

        public long getLong2 () {
            return long2;
        }

        public void setLong2 (long long2) {
            this.long2 = long2;
        }

        public String getString1 () {
            return string1;
        }

        public void setString1 (String string1) {
            this.string1 = string1;
        }

        public String getString2 () {
            return string2;
        }

        public void setString2 (String string2) {
            this.string2 = string2;
        }

        public CharSequence getCs1 () {
            return cs1;
        }

        public void setCs1 (CharSequence cs1) {
            this.cs1 = cs1;
        }

        public CharSequence getCs2 () {
            return cs2;
        }

        public void setCs2 (CharSequence cs2) {
            this.cs2 = cs2;
        }
    }

    private static final RecordClassDescriptor cdMsgClassAlphanumericPrivate =
        new RecordClassDescriptor(
            MsgClassAlphanumericPrivate.class.getName(),
            "MsgClassAlphanumericPrivate",
            false,
            null,
            ALPHANUMERIC_NULLABLE
        );

    private static final RecordClassDescriptor cdMsgClassAlphanumericPrivateNonNullable =
        new RecordClassDescriptor(
            MsgClassAlphanumericPrivate.class.getName(),
            "MsgClassAlphanumericPrivateNonNullable",
            false,
            null,
            ALPHANUMERIC_NOT_NULLABLE
        );


    @Test
    public void testAlphanumericPrivateBoundComp() throws Exception {
        setUpComp();
        testAlphanumericPrivateBound();
    }

    @Test
    public void testAlphanumericPrivateBoundIntp() throws Exception {
        setUpIntp();
        testAlphanumericBound();
    }

    private void testAlphanumericPrivateBound() throws Exception {
        // test allowed nulls
        MsgClassAlphanumericPrivate msg = new MsgClassAlphanumericPrivate();
        msg.int1 = INT_STRING;
        msg.int2 = IntegerDataType.INT32_NULL;
        msg.long1 = LONG_STRING;
        msg.long2 = IntegerDataType.INT64_NULL;
        msg.string1 = "JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF";
        msg.string2 = null;
        msg.cs1 = "JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF";
        msg.cs2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassAlphanumericPrivate);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAlphanumericPrivate msg2 = new MsgClassAlphanumericPrivate();
        boundDecode(msg2, cdMsgClassAlphanumericPrivate, in);
        assertEquals(msg.toString(), msg2.toString());

        // on read
        boolean assertsEnabled = false;
        assert assertsEnabled = true;  // Intentional side-effect!!!

        // test not allowed nulls (on write)
/*
        try {
            boundEncode(msg, cdMsgClassAlphanumericPrivateNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'int2' field is not nullable", e.toString());
        }

        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgClassAlphanumericPrivate);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgClassAlphanumericPrivateNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                assertEquals("java.lang.AssertionError: 'int2' field is not nullable", e.toString());
            }
        }

        msg.int2 = 0;
*/
        try {
            boundEncode(msg, cdMsgClassAlphanumericPrivateNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'long2' field is not nullable", e.toString());
        }
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgClassAlphanumericPrivate);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgClassAlphanumericPrivateNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                assertEquals("java.lang.AssertionError: 'long2' field is not nullable", e.toString());
            } catch (IllegalStateException e) {
                assertEquals("java.lang.IllegalStateException: cannot write null to not nullable field", e.toString());
            }
        }

        msg.long2 = 0;
        try {
            boundEncode(msg, cdMsgClassAlphanumericPrivateNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'string2' field is not nullable", e.toString());
        }
        if (assertsEnabled) {
            try {
                out = boundEncode(msg, cdMsgClassAlphanumericPrivate);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());
                boundDecode(msg2, cdMsgClassAlphanumericPrivateNonNullable, in);
                Assert.fail("AssertionError was not thrown");
            } catch (AssertionError e) {
                assertEquals("java.lang.AssertionError: 'string2' field is not nullable", e.toString());
            } catch (IllegalStateException e) {
                assertEquals("java.lang.IllegalStateException: cannot write null to not nullable field", e.toString());
            }
        }
    }

    @Test
    public void testAlphanumericUnboundComp() throws Exception {
        setUpComp();
        testAlphanumericUnbound();
    }

    @Test
    public void testAlphanumericUnboundIntp() throws Exception {
        setUpIntp();
        testAlphanumericUnbound();
    }

    private void testAlphanumericUnbound() throws Exception {
        // test allowed nulls
        final List<Object> values = new ArrayList<Object>(6);
        //values.add("INT1");
        //values.add(null);
        values.add("LONG1");
        values.add(null);
        values.add("JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF");
        values.add(null);
        values.add("JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF");
        values.add(null);

        MemoryDataOutput out = unboundEncode(values, cdMsgClassAlphanumeric);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, cdMsgClassAlphanumeric);
        assertValuesEquals(values, values2, cdMsgClassAlphanumeric);

        // test not allowed nulls (on write)
/*
        try {
            unboundEncode(values, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'int2' field is not nullable", e.toString());
        }

        // on read
        try {
            out = unboundEncode(values, cdMsgClassAlphanumeric);
            in = new MemoryDataInput(out);
            in.reset(out.getPosition());
            unboundDecode(in, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("java.lang.IllegalStateException: 'int2' field is not nullable", e.toString());
        }
        values.set(1, "");
*/
        try {
            unboundEncode(values, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'long2' field is not nullable", e.toString());
        }
        try {
            out = unboundEncode(values, cdMsgClassAlphanumeric);
            in = new MemoryDataInput(out);
            in.reset(out.getPosition());
            unboundDecode(in, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("java.lang.IllegalStateException: 'long2' field is not nullable", e.toString());
        }

        values.set(1, "");
        try {
            unboundEncode(values, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalArgumentException was not thrown");
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: 'string2' field is not nullable", e.toString());
        }
        try {
            out = unboundEncode(values, cdMsgClassAlphanumeric);
            in = new MemoryDataInput(out);
            in.reset(out.getPosition());
            unboundDecode(in, cdMsgClassAlphanumericNonNullable);
            Assert.fail("IllegalStateException was not thrown");
        } catch (IllegalStateException e) {
            assertEquals("java.lang.IllegalStateException: 'string2' field is not nullable", e.toString());
        }
    }

    @Test
    public void testAlphanumericMixComp() throws Exception {
        setUpComp();
        testAlphanumericMix();
    }

    @Test
    public void testAlphanumericMixIntp() throws Exception {
        setUpIntp();
        testAlphanumericMix();
    }

    private void testAlphanumericMix() throws Exception {
        // test allowed nulls
        final List<Object> values = new ArrayList<Object>(6);
        //values.add("INT1");
        //values.add(null);
        values.add("LONG1");
        values.add(null);
        values.add("JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF");
        values.add(null);
        values.add("JUST VERY LONG MESSAGE 129  DWFEQG ERQ GWERGREGR EGREGERGERGGRE G R GERGREGER GRE G ERG REGREG REGREG REGREGREGREG RE FWE FWE FWEFWEFWEFWE FWEFWEFEW FWE FWEFEWFEWFEWFEW FEW FWE FEWFEWFEWFEWFEWFE WFEWFEWFEWFEWFEWFWEFE E FWEFWEFEWFEWFEWFEWFEWFEWFEWFEWFEWFWEFWEFEWFWEFEWFEWFEW FEWF");
        values.add(null);

        MemoryDataOutput out = unboundEncode(values, cdMsgClassAlphanumeric);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());

        final MsgClassAlphanumeric msg2 = new MsgClassAlphanumeric();
        boundDecode(msg2, cdMsgClassAlphanumeric, in);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            final String s = (String) values.get(i);
            switch (i) {
/*
                case 0:
                case 1: {
                    if (s == null)
                        sb.append(IntegerDataType.INT32_NULL).append(',');
                    else {
                        final AlphanumericCodec codec = new AlphanumericCodec(5);
                        sb.append((int) (codec.encodeToLong(s) >>> 32)).append(',');
                    }
                    break;
                }
*/
                case 0:
                case 1: {
                    if (s == null)
                        sb.append(IntegerDataType.INT64_NULL).append(',');
                    else {
                        final AlphanumericCodec codec = new AlphanumericCodec(10);
                        sb.append(codec.encodeToLong(s)).append(',');
                    }
                    break;
                }
                default:
                    sb.append(s).append(',');
            }
        }
        sb.deleteCharAt(sb.length() - 1);

        assertEquals(sb.toString(), msg2.toString());
    }

    @Test
    public void testAlphanumericStaticComp() throws Exception {
        setUpComp();
        testAlphanumericStatic();
    }

    @Test
    public void testAlphanumericStaticIntp() throws Exception {
        setUpIntp();
        testAlphanumericStatic();
    }

    //private final static String INT1_VALUE = "UN345";
    private final static String LONG1_VALUE = "ALPHA67890";
    private final static String STRING1_VALUE = "ALPHA67890 VERY VERY LONG";


    private static final RecordClassDescriptor cdMsgClassAlphanumericStatic =
        new RecordClassDescriptor(
            MsgClassAlphanumeric.class.getName(),
            "MsgClassAlphanumericStatic",
            false,
            null,
            // binding Alphanumeric to int is not supported
            //new StaticDataField("int1", null, new StringDataType(StringDataType.getEncodingAlphanumeric(5), true, false), INT1_VALUE),
            //new StaticDataField("int2", null, new StringDataType(StringDataType.getEncodingAlphanumeric(5), true, false), null),
            new StaticDataField("long1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), LONG1_VALUE),
            new StaticDataField("long2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), null),
            new StaticDataField("string1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), STRING1_VALUE),
            new StaticDataField("string2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), null),
            new StaticDataField("cs1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), STRING1_VALUE),
            new StaticDataField("cs2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), null)
        );

    private void testAlphanumericStatic() throws Exception {
        final MsgClassAlphanumeric msg = new MsgClassAlphanumeric();
        msg.int1 = 0; //INT1_VALUE;
        msg.int2 = 0; // IntegerDataType.INT32_NULL;
        msg.long1 = ExchangeCodec.codeToLong(LONG1_VALUE);
        msg.long2 = IntegerDataType.INT64_NULL;
        msg.string1 = STRING1_VALUE;
        msg.string2 = null;
        msg.cs1 = STRING1_VALUE;
        msg.cs2 = null;

        final MemoryDataOutput out = boundEncode(msg, cdMsgClassAlphanumericStatic);
        final MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAlphanumeric msg2 = new MsgClassAlphanumeric();
        boundDecode(msg2, cdMsgClassAlphanumericStatic, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    public static class MsgClassAllInt {
        public byte b;
        public short s;
        public int i;
        public long i48;
        public long l;

        public String toString() {
            return String.format("%d,%d,%d,%d,%d", 0xFF & b, s, i, i48, l);
        }
    }

    private static final RecordClassDescriptor cdMsgClassAllInt =
        new RecordClassDescriptor(
            MsgClassAllInt.class.getName(),
            "MsgClassAllInt",
            false,
            null,
            new NonStaticDataField("b", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true, -1, 2)),
            new NonStaticDataField("s", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true, -1, 2)),
            new NonStaticDataField("i", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true, -1, 2)),
            new NonStaticDataField("i48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true, -1, 2)),
            new NonStaticDataField("l", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true, -1, 2))
        );

    /*
     * test that nullValue is encoded ok, when min boundary is set 
     */

    @Test
    public void testNullForRestrictedIntegerComp() throws Exception {
        setUpComp();
        testNullForRestrictedInteger();
    }

    @Test
    public void testNullForRestrictedIntegerIntp() throws Exception {
        setUpIntp();
        testNullForRestrictedInteger();
    }

    private void testNullForRestrictedInteger() throws Exception {
        // test unbound 
        final List<Object> values = new ArrayList<Object>(5);
        for (int i = 0; i < 5; i++) {
            values.add(IntegerDataType.NULLS[i]);
        }

        MemoryDataOutput out = unboundEncode(values, cdMsgClassAllInt);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, cdMsgClassAllInt);
        assertValuesEquals(values, values2, cdMsgClassAllInt);

        MsgClassAllInt msg = new MsgClassAllInt();
        int idx = 0;
        msg.b = (byte) IntegerDataType.NULLS[idx++];
        msg.s = (short) IntegerDataType.NULLS[idx++];
        msg.i = (int) IntegerDataType.NULLS[idx++];
        msg.i48 = IntegerDataType.NULLS[idx++];
        msg.l = IntegerDataType.NULLS[idx];

        // test bound
        out = boundEncode(msg, cdMsgClassAllInt);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAllInt msg2 = new MsgClassAllInt();
        boundDecode(msg2, cdMsgClassAllInt, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    // private enum nullable. bounded case 

    @Test
    public void testPrivateEnumNullableComp() throws Exception {
        setUpComp();
        testPrivateEnumNullable();
    }

    @Test
    public void testPrivateEnumNullableIntp() throws Exception {
        setUpIntp();
        testPrivateEnumNullable();
    }

    public static class MsgClassPrivateEnum extends InstrumentMessage {
        public AggressorSide enum1;
        public AggressorSide enum2;

        @Override
        public String toString() {
            return enum1 + "," + enum2;
        }
    }

    private void testPrivateEnumNullable() throws Exception {
        final MsgClassPrivateEnum msg = new MsgClassPrivateEnum();
        msg.enum1 = null;
        msg.enum1 = AggressorSide.BUY;

        final RecordClassDescriptor rcd = getRCD(MsgClassPrivateEnum.class);

        final MemoryDataOutput out = boundEncode(msg, rcd);
        final MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassPrivateEnum msg2 = new MsgClassPrivateEnum();
        boundDecode(msg2, rcd, in);
        Assert.assertEquals(msg.toString(), msg2.toString());
    }

    // check that a dummy field is correctly skipped in codecs  

    @Test
    public void testDummyFieldComp() throws Exception {
        setUpComp();
        testDummyField();
    }

    @Test
    public void testDummyFieldIntp() throws Exception {
        setUpIntp();
        testDummyField();
    }

    private static final RecordClassDescriptor cdMarketMessage =
        new RecordClassDescriptor(
            MarketMessage.class.getName(),
            "MarketMessage converted from 4.2",
            false,
            null,

            new NonStaticDataField("dummy_exchangeCode", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("currencyCode", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true))
        );

    private static final RecordClassDescriptor cdBarMessage =
        StreamConfigurationHelper.mkBarMessageDescriptor(cdMarketMessage, null, null,
            FloatDataType.ENCODING_FIXED_DOUBLE, FloatDataType.ENCODING_FIXED_DOUBLE);

    private void testDummyField() throws Exception {
        final BarMessage msg = new BarMessage();
        msg.setSymbol("AAPL");
        msg.setTimeStampMs(1265748300000L); // 2010-02-09 20:45:00.0 GMT
        //msg.barSize = 1000;
        msg.setClose(1.1);
        msg.setOpen(2.2);
        msg.setHigh(3.3);
        msg.setLow(4.4);
        msg.setVolume(1201);

        final MemoryDataOutput out = boundEncode(msg, cdBarMessage);
        final MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final BarMessage msg2 = new BarMessage();
        boundDecode(msg2, cdBarMessage, in);

        // set fields not handled by codec
        msg2.setSymbol(msg.getSymbol());
        msg2.setTimeStampMs(msg.getTimeStampMs());
        Assert.assertEquals(msg.toString(), msg2.toString());
    }

    @Test
    public void testEmptyUnboundComp() throws Exception {
        setUpComp();
        testEmptyUnbound();
    }

    @Test
    public void testEmptyUnboundIntp() throws Exception {
        setUpIntp();
        testEmptyUnbound();
    }

    private static final RecordClassDescriptor cdMsgClassEmpty =
        new RecordClassDescriptor(
            "My ClassEmpty Descriptor",
            "My message class def",
            false,
            null,
            new StaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), 123),
            new StaticDataField("d2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true), 54.23)
        );

    private void testEmptyUnbound() throws Exception {
        final List<Object> values = new ArrayList<Object>();

        MemoryDataOutput out = unboundEncode(values, cdMsgClassEmpty);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, cdMsgClassEmpty);
        assertValuesEquals(values, values2, cdMsgClassEmpty);
    }

    @Test
    public void testRelativeFloatComp() throws Exception {
        setUpComp();
        testRelativeFloatBound();
        testRelativeDoubleBound();
        testRelativeFloatUnBound();
        testRelativeDoubleUnBound();
    }

    @Test
    public void testRelativeFloatIntp() throws Exception {
        setUpIntp();
        testRelativeFloatBound();
        testRelativeDoubleBound();
        testRelativeFloatUnBound();
        testRelativeDoubleUnBound();
    }

    public static class RelativeDoubleClass extends InstrumentMessage {
        @SchemaType(
                encoding = "DECIMAL",
                dataType = SchemaDataType.FLOAT
        )
        public double close;
        @RelativeTo("close")
        @SchemaType(
                encoding = "DECIMAL",
                dataType = SchemaDataType.FLOAT
        )
        public double open;
        @RelativeTo("close")
        @SchemaType(
                encoding = "DECIMAL",
                dataType = SchemaDataType.FLOAT
        )
        public double high;
        @RelativeTo("close")
        @SchemaType(
                encoding = "DECIMAL",
                dataType = SchemaDataType.FLOAT
        )
        public double low;
        @SchemaType(
                encoding = "DECIMAL",
                dataType = SchemaDataType.FLOAT
        )
        public double volume;

        public String toString() {
            return close + "," + open + "," + high + "," + low + "," + volume;
        }
    }

    public static class RelativeFloatClass extends InstrumentMessage {
        protected float close;
        @RelativeTo("close")
        protected float open;
        @RelativeTo("close")
        protected float high;
        @RelativeTo("close")
        protected float low;
        protected float volume;

        @SchemaElement
        public float getClose() {
            return close;
        }

        public void setClose(float close) {
            this.close = close;
        }

        @SchemaElement
        @RelativeTo("close")
        public float getOpen() {
            return open;
        }

        public void setOpen(float open) {
            this.open = open;
        }

        @SchemaElement
        @RelativeTo("close")
        public float getHigh() {
            return high;
        }

        public void setHigh(float high) {
            this.high = high;
        }

        @SchemaElement
        @RelativeTo("close")
        public float getLow() {
            return low;
        }

        public void setLow(float low) {
            this.low = low;
        }

        @SchemaElement
        public float getVolume() {
            return volume;
        }

        public void setVolume(float volume) {
            this.volume = volume;
        }

        public String toString() {
            return close + "," + open + "," + high + "," + low + "," + volume;
        }
    }

    private void testRelativeDoubleBound() throws Exception {
        RecordClassDescriptor rcd = getRCD(RelativeDoubleClass.class);
        FixedBoundEncoder encoder = factory.createFixedBoundEncoder(TYPE_LOADER, rcd);
        BoundDecoder decoder = factory.createFixedBoundDecoder(TYPE_LOADER, rcd);
        RelativeDoubleClass o = new RelativeDoubleClass();

        o.close = 124.54;
        o.open = 124.95;
        o.high = 125.32;
        o.low = 123.47;
        o.volume = 10323207;
        testRcdBound(o, encoder, decoder);

        o.close = Double.NaN;
        o.open = Double.NaN;
        o.high = Double.NaN;
        o.low = Double.NaN;
        o.volume = Double.NaN;
        testRcdBound(o, encoder, decoder);

        o.close = Double.NaN;
        o.open = 124.95;
        o.high = Double.NaN;
        o.low = 123.47;
        o.volume = Double.NaN;
        testRcdBound(o, encoder, decoder);

        o.close = 124.54;
        o.open = 124.95;
        o.high = Double.NaN;
        o.low = Double.NaN;
        o.volume = Double.NaN;
        testRcdBound(o, encoder, decoder);
    }

    private void testRelativeFloatBound() throws Exception {
        RecordClassDescriptor rcd = getRCD(RelativeFloatClass.class);
        FixedBoundEncoder encoder = factory.createFixedBoundEncoder(TYPE_LOADER, rcd);
        BoundDecoder decoder = factory.createFixedBoundDecoder(TYPE_LOADER, rcd);
        RelativeFloatClass o = new RelativeFloatClass();

        o.close = 124.54f;
        o.open = 124.95f;
        o.high = 125.32f;
        o.low = 123.47f;
        o.volume = 10323207;
        testRcdBound(o, encoder, decoder);

        o.close = Float.NaN;
        o.open = Float.NaN;
        o.high = Float.NaN;
        o.low = Float.NaN;
        o.volume = Float.NaN;
        testRcdBound(o, encoder, decoder);

        o.close = Float.NaN;
        o.open = 124.95f;
        o.high = Float.NaN;
        o.low = 123.47f;
        o.volume = Float.NaN;
        testRcdBound(o, encoder, decoder);

        o.close = 124.54f;
        o.open = 124.95f;
        o.high = Float.NaN;
        o.low = Float.NaN;
        o.volume = Float.NaN;
        testRcdBound(o, encoder, decoder);
    }

    private void testRelativeDoubleUnBound() throws Exception {
        RecordClassDescriptor rcd = getRCD(RelativeDoubleClass.class);
        FixedUnboundEncoder encoder = factory.createFixedUnboundEncoder(rcd);
        UnboundDecoder decoder = factory.createFixedUnboundDecoder(rcd);

        ArrayList<Object> values = new ArrayList<Object>(5);
        values.add(124.54);
        values.add(124.95);
        values.add(125.32);
        values.add(123.47);
        values.add(10323207);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(Double.NaN);
        values.add(Double.NaN);
        values.add(Double.NaN);
        values.add(Double.NaN);
        values.add(Double.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(null);
        values.add(null);
        values.add(null);
        values.add(null);
        values.add(null);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(124.54);
        values.add(124.95);
        values.add(Double.NaN);
        values.add(Double.NaN);
        values.add(Double.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(Double.NaN);
        values.add(124.95);
        values.add(Double.NaN);
        values.add(123.47);
        values.add(Double.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(124.54);
        values.add(124.95);
        values.add(Double.NaN);
        values.add(Double.NaN);
        values.add(Double.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(null);
        values.add(124.95);
        values.add(null);
        values.add(123.47);
        values.add(null);
        testRcdUnbound(values, encoder, decoder);
    }

    private void testRelativeFloatUnBound() throws Exception {
        RecordClassDescriptor rcd = getRCD(RelativeFloatClass.class);
        FixedUnboundEncoder encoder = factory.createFixedUnboundEncoder(rcd);
        UnboundDecoder decoder = factory.createFixedUnboundDecoder(rcd);

        ArrayList<Object> values = new ArrayList<Object>(5);
        values.add(124.54f);
        values.add(124.95f);
        values.add(125.32f);
        values.add(123.47f);
        values.add(10323207);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(Float.NaN);
        values.add(Float.NaN);
        values.add(Float.NaN);
        values.add(Float.NaN);
        values.add(Float.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(null);
        values.add(null);
        values.add(null);
        values.add(null);
        values.add(null);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(124.54f);
        values.add(124.95f);
        values.add(Float.NaN);
        values.add(Float.NaN);
        values.add(Float.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(Float.NaN);
        values.add(124.95f);
        values.add(Float.NaN);
        values.add(123.47f);
        values.add(Float.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(124.54f);
        values.add(124.95f);
        values.add(Float.NaN);
        values.add(Float.NaN);
        values.add(Float.NaN);
        testRcdUnbound(values, encoder, decoder);

        values.clear();
        values.add(null);
        values.add(124.95f);
        values.add(null);
        values.add(123.47f);
        values.add(null);
        testRcdUnbound(values, encoder, decoder);
    }

    // check that UnboundDecoder.getString throws NullValueException, when null-value is read

    @Test
    public void testGetString4NullComp() throws Exception {
        setUpComp();
        testGetString4Null();
    }

    @Test
    public void testGetString4NullIntp() throws Exception {
        setUpIntp();
        testGetString4Null();
    }

    public static class AllTypesClass extends InstrumentMessage {
        public boolean bool1;
        public char char2;

        public byte byte3;
        public short short4;
        public int int5;
        @SchemaType(
                encoding = "INT48",
                dataType = SchemaDataType.INTEGER
        )
        public long int48;
        public long long7;

        @SchemaType(
                encoding = "PUINT30",
                dataType = SchemaDataType.INTEGER
        )
        public int pint30;
        @SchemaType(
                encoding = "PUINT61",
                dataType = SchemaDataType.INTEGER
        )
        public long pint61;
        @SchemaType(
                encoding = "PINTERVAL",
                dataType = SchemaDataType.INTEGER
        )
        public int pinterval;

        public float f11;
        public double d12;
        @SchemaType(
                encoding = "DECIMAL",
                dataType = SchemaDataType.FLOAT
        )
        public double d14;

        @SchemaType(
                dataType = SchemaDataType.TIME_OF_DAY
        )
        public int timeOfDay;
        @SchemaType(
                dataType = SchemaDataType.TIMESTAMP
        )
        public long dateTime;
        public AggressorSide enum17;
        public String s18;
    }

    private void testGetString4Null() throws Exception {
        RecordClassDescriptor rcd = getRCD(AllTypesClass.class);

        final List<Object> values = new ArrayList<Object>(18);
        // boolean field is not nullable
        values.add(true);
        for (int i = 1; i < 17; i++) {
            values.add(null);

        }

        MemoryDataOutput out = unboundEncode(values, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final List<Object> values2 = unboundDecode(in, rcd);
        assertValuesEquals(values, values2, rcd);

        in.setBytes(out);
        UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);
        udec.beginRead(in);
        udec.nextField();
        for (int i = 1; i < values.size(); i++) {
            udec.nextField();

            try {
                udec.getString();
                Assert.fail("NullValueException was not thrown for " + udec.getField().getName());
            } catch (NullValueException e) {
            }
        }
    }

    // check that enum is read correct after changing ordinals of enum elements

    @Test
    public void testEnumOrdinalsChangeComp() throws Exception {
        setUpComp();
        testEnumOrdinalsChange();
    }

    @Test
    public void testEnumOrdinalsChangeIntp() throws Exception {
        setUpIntp();
        testEnumOrdinalsChange();
    }

    public enum CustomEnum {
        value1,
        value2,
        value3
    }

    public static class CustomEnumClass extends InstrumentMessage {
        public CustomEnum enum1;
        public CustomEnum enum2;
        public CustomEnum enum3;

        @Override
        public String toString() {
            return enum1 + "," + enum2 + "," + enum3;
        }
    }

    public enum CustomEnumModified {
        value3,
        value2,
        value1,
        value4
    }

    public static class CustomEnumModifiedClass extends InstrumentMessage {
        public CustomEnumModified enum1;
        public CustomEnumModified enum2;
        public CustomEnumModified enum3;

        @Override
        public String toString() {
            return enum1 + "," + enum2 + "," + enum3;
        }
    }

    private void testEnumOrdinalsChange() throws Exception {
        // 1. test changed enum on decoding
        final RecordClassDescriptor rcd = getRCD(CustomEnumClass.class);

        final CustomEnumClass msg = new CustomEnumClass();
        msg.enum1 = CustomEnum.value1;
        msg.enum2 = CustomEnum.value2;
        msg.enum3 = CustomEnum.value3;

        MemoryDataOutput out = boundEncode(msg, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final CustomEnumModifiedClass msg2 = new CustomEnumModifiedClass();
        //final CustomEnumClass msg2 = new CustomEnumClass();

        final TypeLoader typeLoader = new TypeLoader() {
            @Override
            public Class<?> load(ClassDescriptor cd) throws ClassNotFoundException {
                return cd.getName().equals(ClassDescriptor.getClassNameWithAssembly(CustomEnumClass.class)) ?
                    CustomEnumModifiedClass.class : null;
            }
        };
        boundDecode(msg2, typeLoader, rcd, in);
        assertEquals(msg.toString(), msg2.toString());

        // 2. test changed enum on encoding
        msg2.enum1 = CustomEnumModified.value1;
        msg2.enum2 = CustomEnumModified.value2;
        msg2.enum3 = CustomEnumModified.value3;

        out = boundEncode(msg2, typeLoader, rcd);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg, rcd, in);
        assertEquals(msg2.toString(), msg.toString());
    }

    /*
     * test that all possible field types are skipped ok by bound encoder and decoder 
     */

    @Test
    public void testSkipBoundComp() throws Exception {
        setUpComp();
        testSkipBound();
    }

    @Test
    public void testSkipBoundIntp() throws Exception {
        setUpIntp();
        testSkipBound();
    }

    public static class SkipBoundClass extends InstrumentMessage {
        public String s1;
        private String s2;

        @Override
        public String toString() {
            return s1 + "," + s2;
        }

        public String getS2 () {
            return s2;
        }

        public void setS2 (String s2) {
            this.s2 = s2;
        }
    }

    private static final RecordClassDescriptor cdMsgSkipBoundClass =
        new RecordClassDescriptor(
            SkipBoundClass.class.getName(),
            "MsgSkipBoundClass",
            false,
            null,
            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),

//            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdKind)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoolean", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true, -30000, null)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true, null, 0x7AAAAAAA)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true, null, 2000000000)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true, null, 0x1CCCAAAA1CCCBBBBL)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true, null, 1E38)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true, -1E302, null)),
            new NonStaticDataField("mDouble2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true, -1E302, null), "mDouble"),

            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true)),
            new NonStaticDataField("mAnString", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false)),

            new NonStaticDataField("s2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true))
        );

    private void testSkipBound() throws Exception {
        final FixedBoundEncoder encoder = factory.createFixedBoundEncoder(TYPE_LOADER, cdMsgSkipBoundClass);
        final BoundDecoder decoder = factory.createFixedBoundDecoder(TYPE_LOADER, cdMsgSkipBoundClass);
        final SkipBoundClass o = new SkipBoundClass();

        o.s1 = "Hi first!";
        o.s2 = "Hi last!";
        testRcdBound(o, encoder, decoder);
    }

    /*
    * test that a message with null-value fields in the tail is encoded to a correctly truncated record
    */

    @Test
    public void testTailTruncateByNullsComp() throws Exception {
        setUpComp();
        testTailTruncateByNullsPublic();
        testTailTruncateByNullsPrivate();
    }

    @Test
    public void testTailTruncateByNullsIntp() throws Exception {
        setUpIntp();
        testTailTruncateByNullsPublic();
        testTailTruncateByNullsPrivate();
    }


    private static final EnumClassDescriptor cdEnum = new EnumClassDescriptor(TestEnum.class);

    static final RecordClassDescriptor cdMsgClassAllPublic =
        new RecordClassDescriptor(
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoolByte", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    static final RecordClassDescriptor cdMsgClassAllPublicWithBool =
        new RecordClassDescriptor(
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoolByte", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    static final RecordClassDescriptor cdMsgClassAllPublicWithNotNull =
        new RecordClassDescriptor(
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    static final RecordClassDescriptor cdMsgClassAllPublicS1 =
        new RecordClassDescriptor(
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false))
        );


    static final RecordClassDescriptor cdMsgClassAllPublicToBool =
        new RecordClassDescriptor(
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoolByte", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mBoolean", null, new BooleanDataType(false))
        );

    static final RecordClassDescriptor cdMsgClassAllPublicToNotNull =
        new RecordClassDescriptor(
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false))
        );

    private void testTailTruncateByNullsPublic() {
        MsgClassAllPublic msg = new MsgClassAllPublic();
        msg.setValues();

        // check normal values (nullable)
        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllPublic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        //System.out.println("1# " + out.getPosition());
        MsgClassAllPublic msg2 = new MsgClassAllPublic();
        boundDecode(msg2, cdMsgClassAllPublic, in);
        assertEquals(msg.toString2(), msg2.toString2());

        // encode parts of MsgClassAllPublic to measure necessary space
        out = boundEncode(msg, cdMsgClassAllPublicS1);
        final int spaceS1 = out.getPosition();
        msg.setNulls();
        out = boundEncode(msg, cdMsgClassAllPublicToBool);
        final int spaceToBool = out.getPosition();

        // check null values (nullable)
        msg.setNulls();
        out = boundEncode(msg, cdMsgClassAllPublic);
        //System.out.println("2# " + spaceS1 + " " + out.getPosition());
        assertEquals("incorrect truncation", spaceS1, out.getPosition());
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPublic, in);
        assertEquals(msg2.toString2(), msg.toString2());

        // check bool case
        out = boundEncode(msg, cdMsgClassAllPublicWithBool);
        //System.out.println("3# " + spaceToBool + " " + out.getPosition());
        assertEquals("incorrect truncation", spaceToBool, out.getPosition());
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPublicWithBool, in);
        assertEquals(msg2.toString(), msg.toString());

        // check not-null case
        msg.mPIneterval = 60000;
        out = boundEncode(msg, cdMsgClassAllPublicToNotNull);
        final int spaceToNotNull = out.getPosition();

        out = boundEncode(msg, cdMsgClassAllPublicWithNotNull);
        //System.out.println("4# " + spaceToNotNull + " " + out.getPosition());
        assertEquals("incorrect truncation", spaceToNotNull, out.getPosition());
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPublicWithNotNull, in);
        assertEquals(msg2.toString(), msg.toString());
    }

    static final RecordClassDescriptor cdMsgClassAllPrivate =
        new RecordClassDescriptor(
            MsgClassAllPrivate.class.getName(),
            "MsgClassAllPrivate nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoolByte", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    static final RecordClassDescriptor cdMsgClassAllPrivateWithBool =
        new RecordClassDescriptor(
            MsgClassAllPrivate.class.getName(),
            "MsgClassAllPrivate nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoolByte", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    static final RecordClassDescriptor cdMsgClassAllPrivateWithNotNull =
        new RecordClassDescriptor(
            MsgClassAllPrivate.class.getName(),
            "MsgClassAllPublic nullable",
            false,
            null,

            new NonStaticDataField("s1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(true, cdEnum)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mChar", null, new CharDataType(true)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(true)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true)),

            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true)),
            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    private void testTailTruncateByNullsPrivate() {
        MsgClassAllPublic msgP = new MsgClassAllPublic();
        msgP.setValues();

        // encode parts of MsgClassAllPublic to measure necessary space
        MemoryDataOutput out = boundEncode(msgP, cdMsgClassAllPublicS1);
        final int spaceS1 = out.getPosition();
        msgP.setNulls();
        out = boundEncode(msgP, cdMsgClassAllPublicToBool);
        final int spaceToBool = out.getPosition();

        MsgClassAllPrivate msg = new MsgClassAllPrivate();
        msg.setValues();

        // check normal values (nullable)
        out = boundEncode(msg, cdMsgClassAllPrivate);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        //System.out.println("1# " + out.getPosition());
        MsgClassAllPrivate msg2 = new MsgClassAllPrivate();
        boundDecode(msg2, cdMsgClassAllPrivate, in);
        assertEquals(msg.toString2(), msg2.toString2());

        // check null values (nullable)
        msg.setNulls();
        out = boundEncode(msg, cdMsgClassAllPrivate);
        //System.out.println("2# " + spaceS1 + " " + out.getPosition());
        assertEquals("incorrect truncation", spaceS1, out.getPosition());
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPrivate, in);
        assertEquals(msg2.toString2(), msg.toString2());

        // check bool case
        out = boundEncode(msg, cdMsgClassAllPrivateWithBool);
        //System.out.println("3# " + spaceToBool + " " + out.getPosition());
        assertEquals("incorrect truncation", spaceToBool, out.getPosition());
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPrivateWithBool, in);
        assertEquals(msg2.toString(), msg.toString());

        // check not-null case
        msgP.mPIneterval = 60000;
        out = boundEncode(msgP, cdMsgClassAllPublicToNotNull);
        final int spaceToNotNull = out.getPosition();

        msg.mPIneterval = 60000;
        out = boundEncode(msg, cdMsgClassAllPrivateWithNotNull);
        //System.out.println("4# " + spaceToNotNull + " " + out.getPosition());
        assertEquals("incorrect truncation", spaceToNotNull, out.getPosition());
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPrivateWithNotNull, in);
        assertEquals(msg2.toString(), msg.toString());
    }

    private static void testRcdUnbound(List<Object> values, FixedUnboundEncoder encoder, UnboundDecoder decoder) {
        MemoryDataOutput out = unboundEncode(values, encoder);
        MemoryDataInput in = new MemoryDataInput(out);
        final List<Object> values2 = unboundDecode(in, decoder);
        assertValuesEquals(values, values2, encoder.getClassInfo().getDescriptor());
    }
}
