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
import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.TypeLoaderImpl;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.GMT;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecs {

    public static final double  PRICE_PRECISION = 0.0005;
    public static final int     PSEUDO_STATIC_VALUE = 2465;
    private static final double EPSILON = 0.000001;

    public static enum Kind {
        BIG,
        SMALL,
        BEAUTIFUL
    };

    public static class MsgClass1 {
        public double               close;
        public int                  size;
        public int                  pseudoStatic;
        public String               name;
        public Kind                 kind;
        
        @Override
        public String               toString () {
            return (
                close + "," + size + "," + pseudoStatic +
                "," + name + "," + kind
            );
        }
    }

    public static class MsgClass2 extends MsgClass1 {
        public String               description;
        public boolean              isNice;
        public double               open;

        @Override
        public String               toString () {
            return (
                    super.toString() + "," +
                            (description != null ? description.substring(0, 20) : "null") + "...," + isNice + "," + open
            );
        }
    }

    static {
        System.setProperty("codec.codegen", "1");
    }

    static final TypeLoader CL = TypeLoaderImpl.DEFAULT_INSTANCE;

    static final EnumClassDescriptor    cdKind =
        new EnumClassDescriptor (
            Kind.class.getName (),
            "Kind enum",
            "BIG", "SMALL", "BEAUTIFUL"
        );

    static final RecordClassDescriptor   cdMsgClass1 =
        new RecordClassDescriptor (
            MsgClass1.class.getName (),
            "MsgClass1",
            false,
            null,
            new NonStaticDataField("close", "Price Field", new FloatDataType(FloatDataType.getEncodingScaled(4), true)),
            new NonStaticDataField ("size", "Size Field", new IntegerDataType (IntegerDataType.ENCODING_INT16, false)),
            new StaticDataField ("pseudoStatic", "Test Statics", new IntegerDataType (IntegerDataType.ENCODING_PUINT30, false), PSEUDO_STATIC_VALUE),
            new NonStaticDataField ("name", "Test Name", new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
            new NonStaticDataField ("kind", "Test Enum", new EnumDataType (true, cdKind))
        );

    static final RecordClassDescriptor   cdMsgClass2 =
        new RecordClassDescriptor (
            MsgClass2.class.getName (),
            "MsgClass2",
            false,
            cdMsgClass1,
            new NonStaticDataField ("description", "Test Description", new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true)),
            new NonStaticDataField ("isNice", "Test boolean", new BooleanDataType (false)),
            new NonStaticDataField ("open", "Test RelativeTo", new FloatDataType (FloatDataType.getEncodingScaled(4), false), "close")
        );

    private CodecFactory                factory;

    private Object      boundDecode (
        MemoryDataInput             in,
        RecordClassDescriptor       cd
    )
    {
        BoundDecoder                bdec =
            factory.createFixedBoundDecoder (CL, cd);

        return (bdec.decode (in));
    }

    private Object      boundRoundTrip (Object inMsg, RecordClassDescriptor cd) {
        FixedBoundEncoder           benc =
            factory.createFixedBoundEncoder (CL, cd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        benc.encode (inMsg, out);
       
        MemoryDataInput             in = new MemoryDataInput (out);

        Object                      outMsg = boundDecode (in, cd);

        return (outMsg);
    }

    private MemoryDataOutput unboundEncode (MsgClass2 inMsg) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (cdMsgClass2);

        MemoryDataOutput            out = new MemoryDataOutput ();

        uenc.beginWrite (out);

        while (uenc.nextField ()) {
            String              name = uenc.getField ().getName ();

            if (name.equals ("close"))
                uenc.writeDouble (inMsg.close);
            else if (name.equals ("size"))
                uenc.writeInt (inMsg.size);
            else if (name.equals ("name"))
                uenc.writeString (inMsg.name);
            else if (name.equals ("kind"))
                uenc.writeString (inMsg.kind == null ? null : inMsg.kind.name ());
            else if (name.equals ("description"))
                uenc.writeString (inMsg.description);
            else if (name.equals ("isNice"))
                uenc.writeBoolean (inMsg.isNice);
            else if (name.equals ("open"))
                uenc.writeDouble (inMsg.open);
            else
                throw new RuntimeException ("Unrecognized field: " + name);
        }

        return (out);
    }

    private void        testUnboundRoundTrip (MsgClass2 inMsg) {
        MemoryDataOutput            out = unboundEncode (inMsg);

        UnboundDecoder              udec =
            factory.createFixedUnboundDecoder (cdMsgClass2);
        
        MemoryDataInput             in = new MemoryDataInput (out);
        MemoryDataInput             in2 = new MemoryDataInput (out);

        assertEquals (0, udec.compareAll (in, in2));

        in.seek (0);
        
        udec.beginRead (in);

        while (udec.nextField ()) {
            String              name = udec.getField ().getName ();

            if (name.equals ("close"))
                try {
                    assertEquals(inMsg.close, udec.getDouble(), PRICE_PRECISION);
                } catch (NullValueException e) {
                    assertTrue(Double.isNaN(inMsg.close));
                }
            else if (name.equals ("size"))
                assertEquals (inMsg.size, udec.getInt ());
            else if (name.equals ("name"))
                assertEquals (inMsg.name, udec.getString ());
            else if (name.equals ("kind"))
                try {
                    assertEquals(inMsg.kind == null ? null : inMsg.kind.name(), udec.getString());
                } catch (Exception e) {
                    assertTrue(inMsg.kind == null);
                }
            else if (name.equals ("description"))
                try {
                    assertEquals(inMsg.description, udec.getString());
                } catch (Exception e) {
                    assertTrue(inMsg.description == null);
                }
            else if (name.equals ("isNice"))
                assertEquals (inMsg.isNice, udec.getBoolean ());
            else if (name.equals ("open"))
                assertEquals (inMsg.open, udec.getDouble (), PRICE_PRECISION);
            else
                throw new RuntimeException ("Unrecognized field: " + name);
        }
    }

    private void        testBoundRoundTrip (MsgClass2 inMsg) {
        MsgClass2   outMsg = (MsgClass2) boundRoundTrip (inMsg, cdMsgClass2);

        checkRoundTrip (inMsg, outMsg);
    }

    private void        testMixedRoundTrip (MsgClass2 inMsg) {
        MemoryDataOutput            out = unboundEncode (inMsg);
        MemoryDataInput             in = new MemoryDataInput (out);
        MsgClass2                   outMsg = (MsgClass2) boundDecode (in, cdMsgClass2);

        checkRoundTrip (inMsg, outMsg);
    }

    private void        checkRoundTrip (MsgClass2 inMsg, MsgClass2 outMsg) {
        assertEquals (inMsg.toString (), outMsg.toString ());

        assertEquals (PSEUDO_STATIC_VALUE, outMsg.pseudoStatic);
        assertEquals (inMsg.close, outMsg.close, PRICE_PRECISION);
        assertEquals (inMsg.size, outMsg.size);
        assertEquals (inMsg.name, outMsg.name);
        assertEquals (inMsg.kind, outMsg.kind);
        assertEquals (inMsg.description, outMsg.description);
        assertEquals (inMsg.isNice, outMsg.isNice);
        assertEquals (inMsg.open, outMsg.open, PRICE_PRECISION);
    }

    private void        testThreeWay (MsgClass2 msg1) {
        testBoundRoundTrip (msg1);
        testUnboundRoundTrip (msg1);
        testMixedRoundTrip (msg1);
    }
    
    @Test
    public void         testBoundUserDefinedComp () throws Exception {
        setUpComp ();
        boundUserDefined ();
    }

    @Test
    public void         testBoundUserDefinedIntp () throws Exception {
        setUpIntp ();
        boundUserDefined ();
    }

    private void         boundUserDefined () throws Exception {
        MsgClass2                   msg1 = new MsgClass2 ();
        
        msg1.close = 36.7819;
        msg1.size = 31687;
        msg1.name = "Gene's favorite message";
        msg1.kind = Kind.BEAUTIFUL;
        msg1.pseudoStatic = PSEUDO_STATIC_VALUE;
        msg1.description = "This is\na multi-\nline description";
        msg1.open = 36.9872;
        msg1.isNice = true;
        
        if (!Boolean.getBoolean ("quiet"))
            System.out.println (msg1);
        
        testThreeWay (msg1);
        
        // test relative to NaN
        msg1.close = Double.NaN;    
        // test null string and enum
        msg1.description = null;
        msg1.kind = null;
        
        testThreeWay (msg1);
    }

    public static class MsgClass3 {
        public int                  unlimited;
        public int                  leftLimit;
        public int                  rightLimit;
        public int                  limited;

        public double               unlimited2;
        public double               leftLimit2;
        public double               rightLimit2;
        public double               limited2;

        public long                 hugeLimit;
        public double               hugeLimit2;

        @Override
        public String               toString () {
            return (
                    unlimited + "," + leftLimit + "," + rightLimit + "," + limited + "," +
                            unlimited2 + "," + leftLimit2 + "," + rightLimit2 + "," + limited2 +
                            "," + hugeLimit + "," + hugeLimit2
            );
        }
    }

    static final RecordClassDescriptor   cdMsgClass3 =
        new RecordClassDescriptor (
            MsgClass3.class.getName (),
            "MsgClass3",
            false,
            null,
            new NonStaticDataField("unlimited", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false)),
            new NonStaticDataField("leftLimit", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, 500, null)),
            new NonStaticDataField("rightLimit", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, null, 1500)),
            new NonStaticDataField("limited", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, 500, 1500)),

            new NonStaticDataField("unlimited2", null, new FloatDataType(FloatDataType.getEncodingScaled(8), false)),
            new NonStaticDataField("leftLimit2", null, new FloatDataType(FloatDataType.getEncodingScaled(8), false, 500.5, null)),
            new NonStaticDataField("rightLimit2", null, new FloatDataType(FloatDataType.getEncodingScaled(8), false, null, 1500.05), "leftLimit2"),
            new NonStaticDataField("limited2", null, new FloatDataType(FloatDataType.getEncodingScaled(8), false, 500.5, 1500.05)),

            new NonStaticDataField("hugeLimit", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false, null, Long.MAX_VALUE - 1)),
            new NonStaticDataField("hugeLimit2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, null, Double.MAX_VALUE - 1E292))
        );

    private MemoryDataOutput unboundEncode (MsgClass3 inMsg) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (cdMsgClass3);

        MemoryDataOutput            out = new MemoryDataOutput ();

        uenc.beginWrite (out);

        while (uenc.nextField ()) {
            String              name = uenc.getField ().getName ();

            if (name.equals("unlimited"))
                uenc.writeInt(inMsg.unlimited);
            else if (name.equals("leftLimit"))
                uenc.writeInt(inMsg.leftLimit);
            else if (name.equals("rightLimit"))
                uenc.writeInt(inMsg.rightLimit);
            else if (name.equals("limited"))
                uenc.writeInt(inMsg.limited);
            else if (name.equals("unlimited2"))
                uenc.writeDouble(inMsg.unlimited2);
            else if (name.equals("leftLimit2"))
                uenc.writeDouble(inMsg.leftLimit2);
            else if (name.equals("rightLimit2"))
                uenc.writeDouble(inMsg.rightLimit2);
            else if (name.equals("limited2"))
                uenc.writeDouble(inMsg.limited2);
            else if (name.equals("hugeLimit"))
                uenc.writeLong(inMsg.hugeLimit);
            else if (name.equals("hugeLimit2"))
                uenc.writeDouble(inMsg.hugeLimit2);
            else
                throw new RuntimeException("Unrecognized field: " + name);
        }

        return (out);
    }

    private void        testUnboundRoundTrip (MsgClass3 inMsg) {
        MemoryDataOutput            out = unboundEncode (inMsg);

        UnboundDecoder              udec =
            factory.createFixedUnboundDecoder (cdMsgClass3);

        MemoryDataInput             in = new MemoryDataInput (out);

        MemoryDataInput             in2 = new MemoryDataInput (out);

        assertEquals (0, udec.compareAll (in, in2));

        in.seek (0);

        udec.beginRead (in);

        while (udec.nextField ()) {
            String              name = udec.getField ().getName ();

            if (name.equals ("unlimited"))
                assertEquals (inMsg.unlimited, udec.getInt());
            else if (name.equals("leftLimit"))
                assertEquals (inMsg.leftLimit, udec.getInt());
            else if (name.equals("rightLimit"))
                assertEquals (inMsg.rightLimit, udec.getInt());
            else if (name.equals("limited"))
                assertEquals (inMsg.limited, udec.getInt());
            else if (name.equals ("unlimited2"))
                assertEquals (inMsg.unlimited2, udec.getDouble (), PRICE_PRECISION);
            else if (name.equals("leftLimit2"))
                assertEquals (inMsg.leftLimit2, udec.getDouble (), PRICE_PRECISION);
            else if (name.equals("rightLimit2"))
                assertEquals (inMsg.rightLimit2, udec.getDouble (), PRICE_PRECISION);
            else if (name.equals("limited2"))
                assertEquals (inMsg.limited2, udec.getDouble (), PRICE_PRECISION);
            else if (name.equals("hugeLimit"))
                assertEquals(inMsg.hugeLimit, udec.getLong ());
            else if (name.equals("hugeLimit2"))
                assertEquals(inMsg.hugeLimit2, udec.getDouble (), PRICE_PRECISION);
            else
                throw new RuntimeException ("Unrecognized field: " + name);
        }
    }

    private void        testMixedRoundTrip (MsgClass3 inMsg) {
        MemoryDataOutput            out = unboundEncode (inMsg);
        MemoryDataInput             in = new MemoryDataInput (out);

        MsgClass3                   outMsg = (MsgClass3) boundDecode (in, cdMsgClass3);

        checkRoundTrip (inMsg, outMsg);
    }

    private void        checkRoundTrip (MsgClass3 inMsg, MsgClass3 outMsg) {
        assertEquals (inMsg.toString (), outMsg.toString ());
    }

    private void        setUpComp () {
        factory = CodecFactory.newCompiledCachingFactory ();
    }

    private void        setUpIntp () {
        factory = CodecFactory.newInterpretingCachingFactory ();
    }

    @Before
    public void         setUp () {
        factory = null;
    }

    @After
    public void         tearDown () {
        factory = null;
    }

    @Test
    public void         testLimitedTypesComp () throws Exception {
        setUpComp ();
        testLimitedTypes ();
    }

    @Test
    public void         testLimitedTypesIntp () throws Exception {
        setUpIntp ();
        testLimitedTypes ();
    }

    private void         testLimitedTypes () throws Exception {
        MsgClass3                   msg = new MsgClass3 ();
        msg.unlimited = 2500000;
        msg.leftLimit = 500;
        msg.rightLimit = 1500;
        msg.limited = 1001;
        msg.unlimited2 = 2500000.05;
        msg.leftLimit2 = 500.55;
        msg.rightLimit2 = 1500.05;
        msg.limited2 = 1001.05;
        msg.hugeLimit = 0x7FFFFFFFFFFL;
        msg.hugeLimit2 = 1001.13E123;

        if (!Boolean.getBoolean ("quiet"))
            System.out.println (msg);

        // testBoundRoundTrip (msg);
        MsgClass3   outMsg = (MsgClass3) boundRoundTrip (msg, cdMsgClass3);
        checkRoundTrip(msg, outMsg);

        testUnboundRoundTrip (msg);
        testMixedRoundTrip (msg);
    }

    public static class MsgClass4 {
        public byte                 mByte;
        public short                mShort;
        public int                  mInt;
        public long                 mInt48;
        public long                 mLong;

        public float                mFloat;
        public double               mDouble;

        @Override
        public String toString() {
            return mByte + " " + mShort + " " + mInt + " " + mInt48 + " " + mLong + " " + mFloat + " " + mDouble;
        }
    }

    static final RecordClassDescriptor   cdMsgClass4 =
        new RecordClassDescriptor (
            MsgClass4.class.getName (),
            "MsgClass4",
            false,
            null,
            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, false)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false))
        );

    private MemoryDataOutput unboundEncode (List<Object> values, RecordClassDescriptor rcd) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (rcd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        while (uenc.nextField ()) {
            String              name = uenc.getField ().getName ();

            if (name.equals("mByte"))
                uenc.writeInt(((Number)values.get(idx++)).intValue());
            else if (name.equals("mShort"))
                uenc.writeInt(((Number)values.get(idx++)).intValue());
            else if (name.equals("mInt"))
                uenc.writeLong(((Number)values.get(idx++)).longValue());
            else if (name.equals("mInt48"))
                uenc.writeLong(((Number)values.get(idx++)).longValue());
            else if (name.equals("mLong"))
                uenc.writeLong(((Number)values.get(idx++)).longValue());
            else if (name.equals("mFloat"))
                uenc.writeDouble(((Number)values.get(idx++)).doubleValue());
            else if (name.equals("mDouble"))
                uenc.writeDouble(((Number)values.get(idx++)).doubleValue());
            else
                throw new RuntimeException("Unrecognized field: " + name);
        }

        return (out);
    }

    private MemoryDataOutput unboundEncode3 (List<Object> values, RecordClassDescriptor rcd) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (rcd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        while (uenc.nextField ()) {
            String              name = uenc.getField ().getName ();

            final Object value = values.get(idx++);
            if (value instanceof Number) {
                if (value instanceof Double)
                    uenc.writeDouble((Double) value);
                else if (value instanceof Float)
                    uenc.writeDouble((Float) value);
                else if (value instanceof Long)
                    uenc.writeLong(((Number) value).longValue());
                else
                    uenc.writeInt(((Number) value).intValue());
            } else if (value instanceof Boolean)
                uenc.writeBoolean((Boolean) value);
            else if (value instanceof String)
                uenc.writeString((String) value);
            else if (value instanceof Enum || value instanceof Character)
                uenc.writeString(value.toString());
            else
                throw new RuntimeException("Unrecognized field: " + name + " " + value.getClass());
        }

        return (out);
    }

    private MemoryDataOutput unboundEncodeAsString (List<Object> values, RecordClassDescriptor rcd) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (rcd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        while (uenc.nextField()) {
            uenc.writeString(values.get(idx++).toString());
        }

        return (out);
    }

    @Test
    public void         testNaturalLimitsComp () throws Exception {
        setUpComp ();
        testNaturalLimits ();
    }

    @Test
    public void         testNaturalLimitsIntp () throws Exception {
        setUpIntp ();
        testNaturalLimits ();
    }

    private void         testNaturalLimits () throws Exception {
        List<Object> values = new ArrayList<Object>(7);
        //values.add((byte)0);
        values.add(Short.MAX_VALUE);
        values.add((short)0);
        values.add((int)0);
        values.add((long)0);
        values.add((long)0);
        values.add((float)0.0);
        values.add((double)0.0);

        try {
            unboundEncode(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + Short.MAX_VALUE, e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            // TODO: quick fix for FixedUnboundEncoderImpl.checkLimit
            if (!"java.lang.IllegalArgumentException: 32767".equals(e.toString()))
                assertEquals("java.lang.NumberFormatException: Value out of range: " + Short.MAX_VALUE, e.toString());
        }

        values.set(0, (byte)0);
        values.set(1, Integer.MAX_VALUE);
        try {
            unboundEncode(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + Integer.MAX_VALUE, e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.NumberFormatException: Value out of range: " + Integer.MAX_VALUE, e.toString());
        }

        values.set(1, (short)0);
        values.set(2, Long.MAX_VALUE);
        try {
            unboundEncode(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + Long.MAX_VALUE, e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            //assertEquals("java.lang.NumberFormatException: Value out of range. Value:\"" + Long.MAX_VALUE + "\" Radix:10", e.toString());
            //assertEquals("java.lang.NumberFormatException: For input string: \"" + Long.MAX_VALUE + "\"", e.toString());
            assertEquals("java.lang.NumberFormatException: Integer (4-byte) too large: " + Long.MAX_VALUE, e.toString());
        }

        values.set(2, (int)0);
        values.set(3, Long.MAX_VALUE - 1);
        try {
            unboundEncode(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(3), e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(3), e.toString());
        }

        values.set(3, (long)0);
        values.set(5, Double.MAX_VALUE - 1E299);
        try {
            unboundEncode(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(5), e.toString());
        }
        try {
            //1.7976931338623157E40
            values.set(5, "17976931338623157000000000000000000000000");
            unboundEncodeAsString(values, cdMsgClass4);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals(e.toString().equals("java.lang.IllegalArgumentException: Infinity") ? "java.lang.IllegalArgumentException: Infinity" : "java.lang.IllegalArgumentException: " + values.get(5), e.toString());
        }

    }

    static final RecordClassDescriptor   cdMsgClass5 =
        new RecordClassDescriptor (
            MsgClass4.class.getName (),
            "MsgClass5",
            false,
            null,
            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false, null, 100)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false, -30000, null)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, null, 2000000000)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false, null, 2000000000000L)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false, null, 1E38)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, -1E302, null))
        );

    @Test
    public void         testUserLimitsUnboundComp () throws Exception {
        setUpComp ();
        testUserLimitsUnbound ();
    }

    @Test
    public void         testUserLimitsUnboundIntp () throws Exception {
        setUpIntp ();
        testUserLimitsUnbound ();
    }

    private void         testUserLimitsUnbound () throws Exception {
        List<Object> values = new ArrayList<Object>(6);
        values.add((byte)101);
        values.add((short) 0);
        values.add((int) 0);
        values.add((long) 0);
        values.add((float) 0.0);
        values.add((double) 0.0);
        try {
            unboundEncode(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(0), e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(0), e.toString());
        }

        values.set(0, (byte)0);
        values.set(1, (short) -30001);
        try {
            unboundEncode(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(1), e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(1), e.toString());
        }

        values.set(1, (short)0);
        values.set(2, (int) 2000000001);
        try {
            unboundEncode(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(2), e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(2), e.toString());
        }

        values.set(2, (int) 0);
        values.set(3, (long) 2000000000001L);
        try {
            unboundEncode(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(3), e.toString());
        }
        try {
            unboundEncodeAsString(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(3), e.toString());
        }

        values.set(3, (long) 0);
        values.set(4, (float) 1.5E38);
        try {
            unboundEncode(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + ((Number)values.get(4)).doubleValue(), e.toString());
        }
        try {
            // 1.5E38
            values.set(4, "150000000000000000000000000000000000000");
            unboundEncodeAsString(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(4), e.toString());
        }

        values.set(4, (float) 0);
        values.set(5, (double) -1.2E302);
        try {
            unboundEncode(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(5), e.toString());
        }
        try {
            // -1.2E302
            values.set(5, "-120000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000");
            unboundEncodeAsString(values, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + values.get(5), e.toString());
        }

    }

    private MemoryDataOutput boundEncode(Object inMsg, RecordClassDescriptor cd) {
        FixedBoundEncoder benc =
                factory.createFixedBoundEncoder(CL, cd);

        MemoryDataOutput out = new MemoryDataOutput();
        benc.encode(inMsg, out);
        return out;
    }

    private void boundDecode(Object inMsg, RecordClassDescriptor cd, MemoryDataInput in) {
        FixedExternalDecoder  bdec =
                factory.createFixedExternalDecoder(CL, cd);

        bdec.setStaticFields(inMsg);
        bdec.decode(in, inMsg);
    }

    @Test
    public void         testUserLimitsBoundedComp () throws Exception {
        setUpComp ();
        testUserLimitsBounded ();
    }

    @Test
    public void         testUserLimitsBoundedIntp () throws Exception {
        setUpIntp ();
        testUserLimitsBounded ();
    }

    private void         testUserLimitsBounded () throws Exception {
        MsgClass4                   msg = new MsgClass4 ();
        msg.mByte = 101;
        msg.mShort = 0;
        msg.mInt = 0;
        msg.mLong = 0L;
        msg.mFloat = 0.0f;
        msg.mDouble = 0.0;
        try {
            boundEncode(msg, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + msg.getClass().getField("mByte") + " == " + msg.mByte, e.toString());
        }

        msg.mByte = 0;
        msg.mShort = -30001;
        try {
            boundEncode(msg, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + msg.getClass().getField("mShort") + " == " + msg.mShort, e.toString());
        }

        msg.mShort = 0;
        msg.mInt = 2000000001;
        try {
            boundEncode(msg, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + msg.getClass().getField("mInt") + " == " + msg.mInt, e.toString());
        }

        msg.mInt = 0;
        msg.mLong = 2000000000001L;
        try {
            boundEncode(msg, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + msg.getClass().getField("mLong") + " == " + msg.mLong, e.toString());
        }

        msg.mLong = 0L;
        msg.mFloat = 1.5E38f;
        try {
            boundEncode(msg, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + msg.getClass().getField("mFloat") + " == " + msg.mFloat, e.toString());
        }

        msg.mFloat = 0.0f;
        msg.mDouble = -1.2E302;
        try {
            boundEncode(msg, cdMsgClass5);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals("java.lang.IllegalArgumentException: " + msg.getClass().getField("mDouble") + " == " + msg.mDouble, e.toString());
        }
    }

    static final RecordClassDescriptor   cdMsgClass6 =
        new RecordClassDescriptor (
            "MsgClass6",
            "MsgClass6 title",
            false,
            null,
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
        );

    private void writeField(String name, Object value, FixedUnboundEncoder uenc) {
        if (name.equals("mByte"))
            uenc.writeInt(((Number) value).intValue());
        else if (name.equals("mShort"))
            uenc.writeInt(((Number) value).intValue());
        else if (name.equals("mInt"))
            uenc.writeLong(((Number) value).longValue());
        else if (name.equals("mInt48"))
            uenc.writeLong(((Number) value).longValue());
        else if (name.equals("mLong"))
            uenc.writeLong(((Number) value).longValue());
        else if (name.equals("mFloat"))
            uenc.writeDouble(((Number) value).doubleValue());
        else if (name.equals("mDouble"))
            uenc.writeDouble(((Number) value).doubleValue());
        else if (name.equals("mEnum"))
            uenc.writeString(value.toString());
        else if (name.equals("mString") || name.equals("mCharSequence"))
            uenc.writeString(value.toString());
        else if (name.equals("mBoolean"))
            uenc.writeBoolean((Boolean) value);
        else if (name.equals("mBoolByte")) {
            if (value == null)
                uenc.writeNull();
            else
                uenc.writeBoolean((Boolean) value);
        }
        else if (name.equals("mChar"))
            uenc.writeString(value.toString());
        else if (name.equals("mDateTime"))
            uenc.writeLong((Long) value);
        else if (name.equals("mTimeOfDay"))
            uenc.writeInt((Integer) value);
        else if (name.equals("mPUINT30"))
            uenc.writeInt((Integer) value);
        else if (name.equals("mPUINT61"))
            uenc.writeLong((Long) value);
        else if (name.equals("mPIneterval"))
            uenc.writeInt((Integer) value);
        else if (name.equals("mSCALE_AUTO"))
            uenc.writeDouble(((Number) value).doubleValue());
        else if (name.equals("mSCALE4"))
            uenc.writeDouble(((Number) value).doubleValue());
        else
            throw new RuntimeException("Unrecognized field: " + name);
    }

    private MemoryDataOutput unboundEncode2 (List<Object> values, RecordClassDescriptor rcd) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (rcd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        while (uenc.nextField ()) {
            String              name = uenc.getField ().getName ();

            final Object value = values.get(idx++);
            if (value == null)
                uenc.writeNull();
            else
                writeField(name, value, uenc);
        }

        return (out);
    }

    private MemoryDataOutput unboundEncode4 (List<Object> values, RecordClassDescriptor rcd) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (rcd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        // find max not-null value
        int max = -1;
        for (int i = values.size() - 1; i >= 0; i--) {
            if (values.get(i) != null) {
                max = i;
                break;
            }
        }

        for (int i = 0; i <= max && uenc.nextField(); i++) {
            String              name = uenc.getField ().getName ();

            final Object value = values.get(idx++);
            if (value != null)
                writeField(name, value, uenc);
        }

        return (out);
    }

    private void readField(String name, Object value, UnboundDecoder udec) {
        if (name.equals("mByte")) {
            int actual = udec.getInt();
            int expected = ((Byte) value).intValue();
            assertEquals(expected, actual);
        } else if (name.equals("mShort")) {
            int actual = udec.getInt();
            int expected = ((Short) value).intValue();
            assertEquals(expected, actual);
        } else if (name.equals("mInt")) {
            int actual = udec.getInt();
            int expected = (Integer) value;
            assertEquals(expected, actual);
        } else if (name.equals("mInt48")) {
            long actual = udec.getLong();
            long expected = (Long) value;
            assertEquals(expected, actual);
        } else if (name.equals("mLong")) {
            long actual = udec.getLong();
            long expected = (Long) value;
            assertEquals(expected, actual);
        } else if (name.equals("mFloat")) {
            float actual = udec.getFloat();
            float expected = value != null ? (Float) value : Float.NaN;
            assertEquals(expected, actual, (float) EPSILON);
        } else if (name.equals("mDouble")) {
            double actual = udec.getDouble();
            double expected = value != null ? (Double) value : Double.NaN;
            assertEquals(expected, actual, EPSILON);
        } else if (name.equals("mEnum")) {
            String v = udec.getString();
            // TODO: what is about bind enums?
            assertEquals(value != null ? value.toString() : null, v);
        } else if (name.equals("mString") || name.equals("mCharSequence")) {
            String v = udec.getString();
            assertEquals(value, v);
        } else if (name.equals("mBoolean")) {
            boolean v = udec.getBoolean();
            assertEquals(value, v);
        } else if (name.equals("mBoolByte")) {
            boolean v = udec.getBoolean();
            assertEquals(value, v);
        } else if (name.equals("mChar")) {
            String v = udec.getString();
            assertEquals(value.toString(), v);
        } else if (name.equals("mDateTime")) {
            long v = udec.getLong();
            assertEquals(value, v);
        } else if (name.equals("mTimeOfDay")) {
            int v = udec.getInt();
            assertEquals(value, v);
        } else if (name.equals("mPUINT30")) {
            int v = udec.getInt();
            assertEquals(value, v);
        } else if (name.equals("mPUINT61")) {
            long v = udec.getLong();
            assertEquals(value, v);
        } else if (name.equals("mPIneterval")) {
            int v = udec.getInt();
            assertEquals(value, v);
        } else if (name.equals("mSCALE_AUTO")) {
            double actual = udec.getDouble();
            double expected = (Double) value;
            assertEquals(expected, actual, EPSILON);
        } else if (name.equals("mSCALE4")) {
            double actual = udec.getDouble();
            double expected = (Double) value;
            assertEquals(expected, actual, EPSILON);
        } else
            throw new RuntimeException("Unrecognized field: " + name);
    }

    private MemoryDataOutput unboundEncodeAsString2 (List<Object> values, RecordClassDescriptor rcd) {
        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (rcd);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        while (uenc.nextField()) {
            Object value = values.get(idx++);
            if (uenc.getField().getName().equals("mDateTime") && value != null && "0".equals(value.toString()))
                value = "1970-01-01 00:00:00.0";
            uenc.writeString(value != null ? value.toString() : null);
        }

        return (out);
    }

    private void        testUnboundRoundTrip2 (List<Object> values, RecordClassDescriptor rcd1, RecordClassDescriptor rcd2, boolean asString, boolean handleNull) {
        MemoryDataOutput out = asString ?
                unboundEncodeAsString2(values, rcd1) :
                unboundEncode2(values, rcd1);

        UnboundDecoder              udec =
                factory.createFixedUnboundDecoder(rcd2 != null ? rcd2 : rcd1);

        MemoryDataInput             in = new MemoryDataInput (out);

        in.seek (0);
        int idx = 0;
        udec.beginRead (in);

        while (udec.nextField ()) {
            String              name = udec.getField ().getName ();

            final Object value = values.get(idx++);
            if (handleNull && value == null)
                assertEquals(name, true, udec.isNull());
            else
                readField(name, value, udec);
        }
    }

    private void        testUnboundRoundTrip2Set (List<Object> values, RecordClassDescriptor rcd) {
        try {
            testUnboundRoundTrip2(values, rcd, null, false, true);
            testUnboundRoundTrip2(values, rcd, null, false, false);
            assertTrue("NullValueException was not thrown", false);
        } catch (NullValueException e) {
            assertEquals(NullValueException.class.getName() + ": NULL", e.toString());
        }

        try {
            testUnboundRoundTrip2(values, rcd, null, true, true);
            testUnboundRoundTrip2(values, rcd, null, true, false);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (NullValueException e) {
            assertEquals(NullValueException.class.getName() + ": NULL", e.toString());
        }
    }

    @Test
    public void         testNullValueUnboundedComp () throws Exception {
        setUpComp ();
        testNullValueUnbounded ();
    }

    @Test
    public void         testNullValueUnboundedIntp () throws Exception {
        setUpIntp ();
        testNullValueUnbounded ();
    }

    private void         testNullValueUnbounded () throws Exception {
        List<Object> values = new ArrayList<Object>(18);
        //values.add((byte)0);
        values.add(null);
        values.add((short) 0);
        values.add((int) 0);
        values.add((long) 0);
        values.add((long) 0);        
        values.add((float) 0.0);
        values.add((double) 0.0);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add((long) 0);
        values.add((int) 0);

        values.add((int) 0); // 13
        values.add((long) 0);
        values.add((int) 1);
        values.add((double) 0.0);
        values.add((double) 0.0);

        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(0, (byte)0);
        values.set(1, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(1, (short)0);
        values.set(2, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(2, (int)0);
        values.set(3, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(3, (long)0);
        values.set(4, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(4, (long)0);
        values.set(5, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(5, (float)0);
        values.set(6, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(6, (double)0);
        values.set(7, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(7, Kind.BIG);
        values.set(8, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(8, "Hi Nikolia!");
        values.set(9, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(9, Boolean.TRUE);
        values.set(10, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(10, 'C');
        values.set(11, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(11, (long)0);
        values.set(12, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(12, (int)0);
        values.set(13, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(13, (int)0);
        values.set(14, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(14, (long)0);
        values.set(15, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(15, (int)1);
        values.set(16, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);

        values.set(16, (double)0);
        values.set(17, null);
        testUnboundRoundTrip2Set(values, cdMsgClass6);
    }

    @Test
    public void         testPositionTrackingComp () throws Exception {
        setUpComp ();
        testPositionTracking ();
    }

    @Test
    public void         testPositionTrackingIntp () throws Exception {
        setUpIntp ();
        testPositionTracking ();
    }

    private void         testPositionTracking () throws Exception {
        List<Object> values = new ArrayList<Object>(18);
        values.add((byte)0);
        values.add((short) 0);
        values.add((int) 0);
        values.add((long) 0);
        values.add((long) 0);
        values.add((float) 0.0);
        values.add((double) 0.0);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add((long) 0);
        values.add((int) 0);

        values.add((int) 1); // TODO: mPUINT30
        values.add((long) 1); // mPUINT61
        values.add((int) 1);
        values.add((double) 0.0);
        values.add((double) 0.0);

        FixedUnboundEncoder         uenc =
            factory.createFixedUnboundEncoder (cdMsgClass6);

        MemoryDataOutput            out = new MemoryDataOutput ();

        int idx = 0;
        uenc.beginWrite (out);

        int position;
        while (uenc.nextField ()) {
            String              name = uenc.getField ().getName ();

            uenc.writeNull();
            position = out.getPosition();
            uenc.writeNull();
            uenc.writeNull();
            assertEquals(position, out.getPosition());

            final Object value = values.get(idx++);
            writeField(name, value, uenc);
            position = out.getPosition();

            for (int i = 0; i < 3; i++)
                writeField(name, value, uenc);

            assertEquals(position, out.getPosition());
        }

        UnboundDecoder              udec =
            factory.createFixedUnboundDecoder (cdMsgClass6);
        MemoryDataInput             in = new MemoryDataInput (out);

        in.seek (0);
        idx = 0;
        udec.beginRead (in);

        while (udec.nextField ()) {
            String              name = udec.getField ().getName ();

            final Object value = values.get(idx++);
            udec.isNull();
            position = out.getPosition();
            udec.isNull();
            udec.isNull();
            assertEquals(position, out.getPosition());

            readField(name, value, udec);
            assertEquals(position, out.getPosition());
            readField(name, value, udec);
            readField(name, value, udec);
            assertEquals(position, out.getPosition());
        }
    }

    static final RecordClassDescriptor   cdMsgClass4Nullable =
        new RecordClassDescriptor (
            MsgClass4.class.getName (),
            "MsgClass4Nullable",
            false,
            null,
            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
        );

    @Test
    public void         testNullCheckBoundedComp () throws Exception {
        setUpComp ();
        testNullCheckBounded ();
    }

    @Test
    public void         testNullCheckBoundedIntp () throws Exception {
        setUpIntp ();
        testNullCheckBounded ();
    }

    private void         testNullCheckBounded () throws Exception {
        MsgClassAllPublic msg = new MsgClassAllPublic(); 
        msg.mString = "IBM";
        msg.mCharSequence = "MSFT";
        msg.mEnum = Kind.SMALL;
        msg.mByte = 1;
        msg.mShort = 2;
        msg.mInt = 3;
        msg.mInt48 = 4;
        msg.mLong = 5;
        msg.mFloat = 63545.34f;
        msg.mDouble = 76456577.76;

        msg.mBoolean = true;
        msg.mBoolByte = BooleanDataType.TRUE;
        msg.mChar = 'C';
        msg.mDateTime = 1235746625319L;
        msg.mTimeOfDay= 56841000;

        msg.mPUINT30= 0x1CCCAAAA;
        msg.mPUINT61= 0x1CCCAAAA1CCCAAAAL;
        msg.mPIneterval = 60000;
        msg.mSCALE_AUTO = 1.52;
        msg.mSCALE4 = 1.53;

        // check normal values (nullable)
        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllPublic);

        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        MsgClassAllPublic msg2 = new MsgClassAllPublic();
        boundDecode(msg2, cdMsgClassAllPublic, in);
        assertEquals(msg.toString(), msg2.toString());


        // check null values (nullable)
        MsgClassAllPublic msg3 = new MsgClassAllPublic();
        msg3.setNulls();
        out = boundEncode(msg3, cdMsgClassAllPublic);

        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPublic, in);
        assertEquals(msg2.toString(), msg3.toString());

        final String[] fields = {
                "mEnum",
                "mString",
                "mCharSequence",
                //"mBoolean",
                "mBoolByte",
                "mChar",
                "mDateTime",
                "mTimeOfDay",
                "mByte",
                "mShort",
                "mInt",
                "mInt48",
                "mLong",
                "mFloat",
                "mDouble",
                //"mDouble2",
                "mPUINT30",
                "mPUINT61",
                "mPIneterval",
                "mSCALE_AUTO",
                "mSCALE4"
        };

        // check null values (not nullable)
        // encoder
        for (String fieldName : fields) {
            try {
                boundEncode(msg3, cdMsgClassAllPublicNotNull);
                assertTrue("IllegalArgumentException was not thrown " + fieldName, false);
            } catch (IllegalArgumentException e) {
                assertEquals("java.lang.IllegalArgumentException: '" + fieldName + "' field is not nullable", e.toString());
            }
            copyField(msg, msg3, fieldName);
        }

        // decoder
        msg3.setNulls();

        boolean assertsEnabled = false;
        assert assertsEnabled = true;  // Intentional side-effect!!!
        if(assertsEnabled) {
            for (String fieldName : fields) {
                out = boundEncode(msg3, cdMsgClassAllPublic);
                in = new MemoryDataInput(out);
                in.reset(out.getPosition());

                try {
                    boundDecode(msg2, cdMsgClassAllPublicNotNull, in);
                    assertTrue("AssertionError was not thrown", false);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    assertEquals(String.format("cannot write null to not nullable field '%s'", fieldName), e.getMessage());
                } catch (AssertionError e) {
                    assertEquals("java.lang.AssertionError: '" + fieldName + "' field is not nullable", e.toString());
                }
                copyField(msg, msg3, fieldName);
            }
        }

        // check normal values ((not nullable)
        msg.mByte = (byte) (0x8F); // substitute negative value under IKVM
        msg.mShort = (short)0x1FFEE;
        msg.mInt = 0x7AAAAAAA;
        msg.mLong = 0x1CCCAAAA1CCCBBBBL;
        msg.mFloat = 1.32f;
        msg.mDouble = 1.68;

        msg.mEnum = Kind.BIG;
        msg.mString = "Hi Nicolia!";
        msg.mCharSequence = "Hi Sania!";
        msg.mBoolean = true;
        msg.mChar = 'C';
        msg.mDateTime = 0x1CCCAAAA1CCCAAAAL;
        msg.mTimeOfDay= 0x1CCCAAAA;

        msg.mPUINT30= 0x1CCCAAAA;
        msg.mPUINT61= 0x1CCCAAAA1CCCAAAAL;
        msg.mPIneterval = 60000;
        msg.mSCALE_AUTO = 1.52;
        msg.mSCALE4 = 1.53;
        out = boundEncode(msg, cdMsgClassAllPublicNotNull);

        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        msg2 = new MsgClassAllPublic();
        boundDecode(msg2, cdMsgClassAllPublicNotNull, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    private void        testUnboundRoundTrip3Set (List<Object> values, RecordClassDescriptor rcd1, RecordClassDescriptor rcd2, String exception) {
        try {
            testUnboundRoundTrip2(values, rcd1, rcd2, false, false);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals(exception, e.toString());
        }

        try {
            testUnboundRoundTrip2(values, rcd1, rcd2, true, false);
            assertTrue("IllegalArgumentException was not thrown", false);
        } catch (IllegalArgumentException e) {
            assertEquals(exception, e.toString());
        }
    }

    static final RecordClassDescriptor   cdMsgClass6NotNull =
        new RecordClassDescriptor (
            "My Msg6 NotNull",
            "MsgClass6NotNull",
            false,
            null,
            new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
            new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false)),
            new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false)),
            new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, false)),
            new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
            new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false)),
            new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false)),
            new NonStaticDataField("mEnum", null, new EnumDataType(false, cdKind)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
            new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
            new NonStaticDataField("mChar", null, new CharDataType(false)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(false)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(false)),

            new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
            new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
            new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
            new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, false)),
            new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), false))
        );

    @Test
    public void         testNullCheckUnboundedComp () throws Exception {
        setUpComp ();
        testNullCheckUnbounded ();
    }

    @Test
    public void         testNullCheckUnboundedIntp () throws Exception {
        setUpIntp ();
        testNullCheckUnbounded ();
    }

    private void         testNullCheckUnbounded () throws Exception {
        final List<Object> notNullValues = new ArrayList<Object>(19);
        notNullValues.add(Kind.SMALL);
        notNullValues.add("IBM");
        notNullValues.add("MSFT");
        notNullValues.add(true);
        notNullValues.add(true);
        notNullValues.add('C');
        notNullValues.add((long) 1235746625319L);
        notNullValues.add((int)56841000);

        notNullValues.add((byte)1);
        notNullValues.add((short)2);
        notNullValues.add((int) 3);
        notNullValues.add((long) 4);
        notNullValues.add((long) 5);
        notNullValues.add((float) 63545.34f);
        notNullValues.add((double) 7645657.76);

        notNullValues.add((int) 0x1CCCAAAA);
        notNullValues.add((long)0x1CCCAAAA1CCCAAAAL);
        notNullValues.add((int) 60000);
        notNullValues.add((double) 1.52);
        notNullValues.add((double) 1.53);

        final List<Object> nullValues = new ArrayList<Object>(19);
        nullValues.add(null);
        nullValues.add(null);
        nullValues.add(null);
        nullValues.add(true);
        nullValues.add(null);
        nullValues.add(null);
        nullValues.add(DateTimeDataType.NULL);
        nullValues.add(TimeOfDayDataType.NULL);

        nullValues.add(IntegerDataType.INT8_NULL);
        nullValues.add(IntegerDataType.INT16_NULL);
        nullValues.add(IntegerDataType.INT32_NULL);
        nullValues.add(IntegerDataType.INT48_NULL);
        nullValues.add(IntegerDataType.INT64_NULL);
        nullValues.add(FloatDataType.IEEE32_NULL);
        nullValues.add(FloatDataType.IEEE64_NULL);

        nullValues.add(IntegerDataType.PUINT30_NULL);
        nullValues.add(IntegerDataType.PUINT61_NULL);
        nullValues.add(IntegerDataType.PINTERVAL_NULL);
        nullValues.add(FloatDataType.DECIMAL_NULL);
        nullValues.add(FloatDataType.DECIMAL_NULL);

        final List<Object> values = new ArrayList<Object>(nullValues);
        final DataField[] fields = cdMsgClassAllPublicNotNull.getFields();
        for (int i = 0; i < nullValues.size(); i++) {
            final String fieldName = fields[i].getName();
            if (!fieldName.equals("mBoolean")) {
                final String exception = "java.lang.IllegalArgumentException: '" + fieldName + "' field is not nullable";
                try {
                    testUnboundRoundTrip2(values, cdMsgClassAllPublicNotNull, null, false, false);
                    assertTrue("IllegalArgumentException was not thrown", false);
                } catch (IllegalArgumentException e) {
                    assertEquals(exception, e.toString());
                }
                try {
                    testUnboundRoundTrip3Set(values, cdMsgClassAllPublic, cdMsgClassAllPublicNotNull, null);
                    assertTrue("IllegalStateException was not thrown", false);
                } catch (IllegalStateException e) {
                    assertEquals("java.lang.IllegalStateException: '" + fieldName + "' field is not nullable", e.toString());
                }
            }
            values.set(i, notNullValues.get(i));
        }

        // change notNullValues for getString/setString functions
        notNullValues.set(6, GMT.formatDateTimeMillis((Long)notNullValues.get(6)));
        nullValues.set(6, null);
        notNullValues.set(7, TimeOfDayDataType.staticFormat ((Integer) notNullValues.get(7)));
        nullValues.set(7, null);
        values.clear();
        values.addAll(nullValues);

        for (int i = 0; i < nullValues.size(); i++) {
            final String fieldName = fields[i].getName();
            if (!fieldName.equals("mBoolean")) {
                final String exception = "java.lang.IllegalArgumentException: '" + fieldName + "' field is not nullable";
                try {
                    testUnboundRoundTrip2(values, cdMsgClassAllPublicNotNull, null, true, false);
                    assertTrue("IllegalArgumentException was not thrown", false);
                } catch (IllegalArgumentException e) {
                    assertEquals(exception, e.toString());
                }
            }
            values.set(i, notNullValues.get(i));
        }
    }

    @Test
    public void         testCompareComp () throws Exception {
        setUpComp ();
        testCompare ();
    }

    @Test
    public void         testCompareIntp () throws Exception {
        setUpIntp ();
        testCompare ();
    }

    private void         testCompare () throws Exception {
        List<Object> values = new ArrayList<Object>(18);
        values.add((byte) 0);
        values.add((short) 0);
        values.add((int) 0);
        values.add((long) 0);
        values.add((long) 0);
        values.add((float) 0.0);
        values.add((double) 0.0);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add((long) 0);
        values.add((int) 0);

        values.add((int) 0);
        values.add((long) 0);
        values.add((int) 1);
        values.add((double) 0.0);
        values.add((double) 0.0);

        MemoryDataOutput out = unboundEncode2(values, cdMsgClass6);

        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClass6);

        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        MemoryDataInput in2 = new MemoryDataInput(out);
        in2.reset(out.getPosition());
        assertEquals(0, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(0, udec.comparePrimaryKeys(in, in2));


        List<Object> valuesNull = new ArrayList<Object>(18);
        valuesNull.addAll(values);
        for (int i = 0; i < 18; i++) {
            valuesNull.set(i, null);
            if (i > 0)
                valuesNull.set(i - 1, values.get(i - 1));

            out = unboundEncode2(valuesNull, cdMsgClass6);
            in = new MemoryDataInput(out);
            in.reset(out.getPosition());
            in2 = new MemoryDataInput(out);
            in2.reset(out.getPosition());
            assertEquals(0, udec.compareAll(in, in2));
            in.seek(0);
            in2.seek(0);
            assertEquals(0, udec.comparePrimaryKeys(in, in2));
        }

        out = unboundEncode2(values, cdMsgClass6);
        MemoryDataOutput out2 = unboundEncode2(valuesNull, cdMsgClass6);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        in2 = new MemoryDataInput(out2);
        in2.reset(out.getPosition());
        assertEquals(1, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(-1, udec.compareAll(in2, in));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.comparePrimaryKeys(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(-1, udec.comparePrimaryKeys(in2, in));

        valuesNull.set(17, (double)0.01);
        out2 = unboundEncode2(valuesNull, cdMsgClass6);
        in2 = new MemoryDataInput(out2);
        in.seek(0);
        assertEquals(-1, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.compareAll(in2, in));
        in.seek(0);
        in2.seek(0);
        assertEquals(-1, udec.comparePrimaryKeys(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.comparePrimaryKeys(in2, in));

        valuesNull.set(17, values.get(17));
        valuesNull.set(0, (byte)115);
        out2 = unboundEncode2(valuesNull, cdMsgClass6);
        in2 = new MemoryDataInput(out2);
        in.seek(0);
        assertEquals(-1, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.compareAll(in2, in));
        in.seek(0);
        in2.seek(0);
        assertEquals(0, udec.comparePrimaryKeys(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(0, udec.comparePrimaryKeys(in2, in));
    }

    @Test
    public void         testCompareTruncatedComp () throws Exception {
        setUpComp ();
        testCompareTruncated ();
    }

    @Test
    public void         testCompareTruncatedIntp () throws Exception {
        setUpIntp ();
        testCompareTruncated ();
    }

    private void         testCompareTruncated () throws Exception {
        List<Object> values = new ArrayList<Object>(18);
        values.add((byte) 0);
        values.add((short) 0);
        values.add((int) 0);
        values.add((long) 0);
        values.add((long) 0);
        values.add((float) 0.0);
        values.add((double) 0.0);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add((long) 0);
        values.add((int) 0);

        values.add((int) 0);
        values.add((long) 0);
        values.add((int) 1);
        values.add((double) 0.0);
        values.add((double) 0.0);

        MemoryDataOutput out = unboundEncode2(values, cdMsgClass6);
        MemoryDataOutput out2 = unboundEncode4(values, cdMsgClass6);

        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClass6);

        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        MemoryDataInput in2 = new MemoryDataInput(out2);
        in2.reset(out2.getPosition());
        assertEquals(0, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(0, udec.comparePrimaryKeys(in, in2));


        List<Object> valuesNull = new ArrayList<Object>(18);
        valuesNull.addAll(values);
        for (int i = 17; i >=0; i--) {
            valuesNull.set(i, null);

            out = unboundEncode2(valuesNull, cdMsgClass6);
            out2 = unboundEncode4(valuesNull, cdMsgClass6);
            in = new MemoryDataInput(out);
            in.reset(out.getPosition());
            in2 = new MemoryDataInput(out2);
            in2.reset(out2.getPosition());
            assertEquals(0, udec.compareAll(in, in2));
            in.seek(0);
            in2.seek(0);
            assertEquals(0, udec.comparePrimaryKeys(in, in2));
        }

        out = unboundEncode2(values, cdMsgClass6);
        out2 = unboundEncode4(valuesNull, cdMsgClass6);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        in2 = new MemoryDataInput(out2);
        in2.reset(out2.getPosition());
        assertEquals(1, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(-1, udec.compareAll(in2, in));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.comparePrimaryKeys(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(-1, udec.comparePrimaryKeys(in2, in));

        valuesNull.clear();
        valuesNull.addAll(values);
        valuesNull.set(17, (double)0.01);
        out2 = unboundEncode4(valuesNull, cdMsgClass6);
        in2 = new MemoryDataInput(out2);
        in.seek(0);
        assertEquals(-1, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.compareAll(in2, in));
        in.seek(0);
        in2.seek(0);
        assertEquals(-1, udec.comparePrimaryKeys(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.comparePrimaryKeys(in2, in));

        valuesNull.set(17, values.get(17));
        valuesNull.set(0, (byte)115);
        out2 = unboundEncode4(valuesNull, cdMsgClass6);
        in2 = new MemoryDataInput(out2);
        in.seek(0);
        assertEquals(-1, udec.compareAll(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(1, udec.compareAll(in2, in));
        in.seek(0);
        in2.seek(0);
        assertEquals(0, udec.comparePrimaryKeys(in, in2));
        in.seek(0);
        in2.seek(0);
        assertEquals(0, udec.comparePrimaryKeys(in2, in));
    }

    @Test
    @Ignore("#4347") // Due to http://fw.orientsoft.by/bugzilla2/show_bug.cgi?id=4347
    public void         testGetString () throws Exception {
        List<Object> values = new ArrayList<Object>(18);
        values.add((byte) 0);
        values.add((short) 0);
        values.add((int) 0);
        values.add((long) 0);
        values.add((long) 0);
        values.add((float) 0.0);
        values.add((double) 1.5E40);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add((long) 0);
        values.add((int) 0);

        values.add((int) 0);
        values.add((long) 0);
        values.add((int) 1);
        values.add((double) 0.0);
        values.add((double) 0.0);

        // test only decoding (encoding has a bug)
        MemoryDataOutput out = unboundEncode2(values, cdMsgClass6);
        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClass6);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        udec.beginRead(in);
        // skip 6 fields
        for (int i = 0; i < 7; i++)
            udec.nextField();
        assertEquals(StringUtils.toDecimalString(((Number) values.get(6)).doubleValue()), udec.getString());

        // test full only encoding/decoding chain
        values.set(6, "150000000000000000000000000000000000000");
        out = unboundEncodeAsString2(values, cdMsgClass6);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        udec.beginRead(in);
        // skip 6 fields
        for (int i = 0; i < 7; i++)
            udec.nextField();
        assertEquals(values.get(6), udec.getString());
    }

    @Test
    public void         testSkipFieldsComp () throws Exception {
        setUpComp ();
        testSkipFields ();
    }

    @Test
    public void         testSkipFieldsIntp () throws Exception {
        setUpIntp ();
        testSkipFields ();
    }

    private void         testSkipFields () throws Exception {
        List<Object> values = new ArrayList<Object>(18);
        values.add((byte) 1);
        values.add((short) 2);
        values.add((int) 4);
        values.add((long) 8);
        values.add((long) 15);
        values.add((float) 3456.13);
        values.add((double) 1.5000034);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add("2009-02-27 14:57:05.319");
        values.add("15:47:21");

        values.add((int) 3423525);
        values.add((long) 23546547576765L);
        values.add((int) 87978561);
        values.add((double) 2342342.3413);
        values.add((double) 6878667.7884);

        MemoryDataOutput out = unboundEncodeAsString2(values, cdMsgClass6);
        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClass6);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        for (int i = 0; i < values.size(); i++) {
            in.reset(in.getLength());
            udec.beginRead(in);
            for (int j = 0; j <= i; j++)
                udec.nextField();
            assertEquals(values.get(i).toString(), udec.getString());
        }
    }

    public static class MsgClassAllPrivate {
        private Kind                mEnum;
        private String              mString;
        private CharSequence        mCharSequence;
        private boolean             mBoolean;
        private byte                mBoolByte;
        private char                mChar;
        private long                mDateTime;
        private int                 mTimeOfDay;

        private byte                mByte;
        private short               mShort;
        private int                 mInt;
        private long                mInt48;
        private long                mLong;
        private float               mFloat;
        private double              mDouble;
        private double              mDouble2;

        private int                 mPUINT30;
        private long                mPUINT61;
        private int                 mPIneterval;
        private double              mSCALE_AUTO;
        private double              mSCALE4;

        public String toString() {
            return mEnum + " " + mString + " " + mCharSequence + " " + mBoolean + " " + mBoolByte + " " + mChar + " " + mDateTime + " "
                + mTimeOfDay + " " + mByte + " " + mShort + " " + mInt + " " + mInt48 + " " + mLong + " " + mFloat + " "
                + mDouble + " " + mDouble2 + " " + mPUINT30 + " " + mPUINT61 + " " + mPIneterval + " " + mSCALE_AUTO + " "
                + mSCALE4;
        }

        public Kind getmEnum () {
            return mEnum;
        }

        public void setmEnum (Kind mEnum) {
            this.mEnum = mEnum;
        }

        public String getmString () {
            return mString;
        }

        public void setmString (String mString) {
            this.mString = mString;
        }

        public CharSequence getmCharSequence () {
            return mCharSequence;
        }

        public void setmCharSequence (CharSequence mCharSequence) {
            this.mCharSequence = mCharSequence;
        }

        public boolean ismBoolean () {
            return mBoolean;
        }

        public void setmBoolean (boolean mBoolean) {
            this.mBoolean = mBoolean;
        }

        public byte getmBoolByte () {
            return mBoolByte;
        }

        public void setmBoolByte (byte mBoolByte) {
            this.mBoolByte = mBoolByte;
        }

        public char getmChar () {
            return mChar;
        }

        public void setmChar (char mChar) {
            this.mChar = mChar;
        }

        public long getmDateTime () {
            return mDateTime;
        }

        public void setmDateTime (long mDateTime) {
            this.mDateTime = mDateTime;
        }

        public int getmTimeOfDay () {
            return mTimeOfDay;
        }

        public void setmTimeOfDay (int mTimeOfDay) {
            this.mTimeOfDay = mTimeOfDay;
        }

        public byte getmByte () {
            return mByte;
        }

        public void setmByte (byte mByte) {
            this.mByte = mByte;
        }

        public short getmShort () {
            return mShort;
        }

        public void setmShort (short mShort) {
            this.mShort = mShort;
        }

        public int getmInt () {
            return mInt;
        }

        public void setmInt (int mInt) {
            this.mInt = mInt;
        }

        public long getmInt48 () {
            return mInt48;
        }

        public void setmInt48 (long mInt48) {
            this.mInt48 = mInt48;
        }

        public long getmLong () {
            return mLong;
        }

        public void setmLong (long mLong) {
            this.mLong = mLong;
        }

        public float getmFloat () {
            return mFloat;
        }

        public void setmFloat (float mFloat) {
            this.mFloat = mFloat;
        }

        public double getmDouble () {
            return mDouble;
        }

        public void setmDouble (double mDouble) {
            this.mDouble = mDouble;
        }

        public double getmDouble2 () {
            return mDouble2;
        }

        public void setmDouble2 (double mDouble2) {
            this.mDouble2 = mDouble2;
        }

        public int getmPUINT30 () {
            return mPUINT30;
        }

        public void setmPUINT30 (int mPUINT30) {
            this.mPUINT30 = mPUINT30;
        }

        public long getmPUINT61 () {
            return mPUINT61;
        }

        public void setmPUINT61 (long mPUINT61) {
            this.mPUINT61 = mPUINT61;
        }

        public int getmPIneterval () {
            return mPIneterval;
        }

        public void setmPIneterval (int mPIneterval) {
            this.mPIneterval = mPIneterval;
        }

        public double getmSCALE_AUTO () {
            return mSCALE_AUTO;
        }

        public void setmSCALE_AUTO (double mSCALE_AUTO) {
            this.mSCALE_AUTO = mSCALE_AUTO;
        }

        public double getmSCALE4 () {
            return mSCALE4;
        }

        public void setmSCALE4 (double mSCALE4) {
            this.mSCALE4 = mSCALE4;
        }
    }

    public static class MsgClassAllPublic {
        public Kind             mEnum;
        public String           mString;
        public CharSequence     mCharSequence;
        public boolean          mBoolean;
        public byte             mBoolByte;
        public char             mChar;
        public long             mDateTime;
        public int              mTimeOfDay;

        public byte             mByte;
        public short            mShort;
        public int              mInt;
        public long             mInt48;
        public long             mLong;
        public float            mFloat;
        public double           mDouble;
        public double           mDouble2;

        public int              mPUINT30;
        public long             mPUINT61;
        public int              mPIneterval;
        public double           mSCALE_AUTO;
        public double           mSCALE4;

        public String toString() {
            return mEnum + " " + mString + " " + mCharSequence + " " + mBoolean + " " + mBoolByte + " " + mChar + " " + mDateTime + " "
                + mTimeOfDay + " " + mByte + " " + mShort + " " + mInt + " " + mInt48 + " " + mLong + " " + mFloat + " "
                + mDouble + " " + mDouble2 + " " + mPUINT30 + " " + mPUINT61 + " " + mPIneterval + " " + mSCALE_AUTO + " "
                + mSCALE4;
        }

        // Set all fields to null values (except mBoolean and mDouble2)
        private void setNulls() {
            mEnum = null;
            mString = null;
            mCharSequence = null;
            //mBoolean = true;
            mBoolByte = BooleanDataType.NULL;
            mChar = CharDataType.NULL;
            mDateTime = DateTimeDataType.NULL;
            mTimeOfDay = TimeOfDayDataType.NULL;

            mByte = IntegerDataType.INT8_NULL;
            mShort = IntegerDataType.INT16_NULL;
            mInt = IntegerDataType.INT32_NULL;
            mInt48 = IntegerDataType.INT48_NULL;
            mLong = IntegerDataType.INT64_NULL;
            mFloat = FloatDataType.IEEE32_NULL;
            mDouble = FloatDataType.IEEE64_NULL;
            //mDouble2 = FloatDataType.IEEE64_NULL;
            mPUINT30 = IntegerDataType.PUINT30_NULL;
            mPUINT61 = IntegerDataType.PUINT61_NULL;
            mPIneterval = IntegerDataType.PINTERVAL_NULL;
            mSCALE_AUTO = FloatDataType.DECIMAL_NULL;
            mSCALE4 = FloatDataType.DECIMAL_NULL;
        }
    }

    private static final DataField[] ALL_NOT_NULLABLE_RANGE_FIELDS = {
                    new NonStaticDataField("mEnum", null, new EnumDataType(false, cdKind)),
                    new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
                    new NonStaticDataField("mChar", null, new CharDataType(false)),
                    new NonStaticDataField("mDateTime", null, new DateTimeDataType(false)),
                    new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(false)),

                    new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false, null, 0x7F)),
                    new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false, -30000, null)),
                    new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, null, 0x7AAAAAAA)),
                    new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, false, null, 2000000000)),
                    new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false, null, 0x1CCCAAAA1CCCBBBBL)),
                    new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false, null, 1E38)),
                    new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, -1E302, null)),

                    new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
                    new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
                    new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
                    new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, false)),
                    new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), false))
            };

    static final RecordClassDescriptor   cdMsgClassAllPrivate =
        new RecordClassDescriptor (
            MsgClassAllPrivate.class.getName (),
            "MsgClassAllPrivate",
            false,
            null,
            ALL_NOT_NULLABLE_RANGE_FIELDS
        );

    private static final DataField[] ALL_NOT_NULLABLE_RANGE_REL_FIELDS = {
                    new NonStaticDataField("mEnum", null, new EnumDataType(false, cdKind)),
                    new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
                    new NonStaticDataField("mChar", null, new CharDataType(false)),
                    new NonStaticDataField("mDateTime", null, new DateTimeDataType(false)),
                    new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(false)),

                    new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false, null, 0x7F)),
                    new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false, -30000, null)),
                    new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, null, 0x7AAAAAAA)),
                    new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, false, null, 2000000000)),
                    new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false, null, 0x1CCCAAAA1CCCBBBBL)),
                    new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false, null, 1E38)),
                    new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, -1E302, null)),
                    new NonStaticDataField("mDouble2", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, -1E302, null), "mDouble"),

                    new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
                    new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
                    new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
                    new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, false)),
                    new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), false))
            };

    static final RecordClassDescriptor cdMsgClassAllPrivateRelative =
            new RecordClassDescriptor(
                    MsgClassAllPrivate.class.getName(),
                    "MsgClassAllPrivateRelative",
                    false,
                    null,
                    ALL_NOT_NULLABLE_RANGE_REL_FIELDS
            );

    @Test
    public void         testPrivateFieldsComp () throws Exception {
        setUpComp ();
        testPrivateFields ();
    }

    @Test
    public void         testPrivateFieldsIntp () throws Exception {
        setUpIntp ();
        testPrivateFields ();
    }

    private void         testPrivateFields () throws Exception {
        MsgClassAllPrivate msg = new MsgClassAllPrivate();
        msg.mEnum = Kind.BIG;
        msg.mString = "Hi Nicolia!";
        msg.mCharSequence = "Hi Sania!";
        msg.mBoolean = true;
        msg.mChar = 'C';
        msg.mDateTime = 0x1CCCAAAA1CCCAAAAL;
        msg.mTimeOfDay= 0x1CCCAAAA;

        msg.mByte = (byte)0x7F;
        msg.mShort = (short)0x1FFEE;
        msg.mInt = 0x7AAAAAAA;
        msg.mInt48 = 0x70AAAAA;
        msg.mLong = 0x1CCCAAAA1CCCBBBBL;
        msg.mFloat = 1.32f;
        msg.mDouble = 1.68;

        msg.mPUINT30= 0x1CCCAAAA;
        msg.mPUINT61= 0x1CCCAAAA1CCCAAAAL;
        msg.mPIneterval = 60000;
        msg.mSCALE_AUTO = 1.52;
        msg.mSCALE4 = 1.53;

        // simple encoding
        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllPrivate);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAllPrivate msg2 = new MsgClassAllPrivate();
        boundDecode(msg2, cdMsgClassAllPrivate, in);
        assertEquals(msg.toString(), msg2.toString());

        // relative encoding
        msg.mDouble2 = 70450577.06;
        out = boundEncode(msg, cdMsgClassAllPrivateRelative);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        boundDecode(msg2, cdMsgClassAllPrivateRelative, in);
        assertEquals(msg.toString(), msg2.toString());

    }

    private static final DataField[] ALL_NOT_NULLABLE_STATIC_FIELDS = {
                    new StaticDataField("mEnum", null, new EnumDataType(true, cdKind), "SMALL"),
                    new StaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), "IBM"),
                    new StaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), "MSFT"),
                    new StaticDataField("mBoolean", null, new BooleanDataType(true), true),
                    new StaticDataField("mChar", null, new CharDataType(true), 'C'),

                    new StaticDataField("mDateTime", null, new DateTimeDataType(true), "2009-02-27 14:57:05.319"),
                    new StaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true), "15:47:21"),
                    new StaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false), 1),
                    new StaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false), 2),
                    new StaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false), 3),
                    new StaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, false), 4),
                    new StaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false), 5),
                    new StaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false), 63545.34f),
                    new StaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false), 76456577.76),

                    new StaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true), 0x1CCCAAAA),
                    new StaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true), 0x1CCCAAAA1CCCAAAAL),
                    new StaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true), 60000),
                    new StaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true), 1.52),
                    new StaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true), 1.53)
            };

    static final RecordClassDescriptor   cdMsgClassAllPrivateStatic =
        new RecordClassDescriptor (
            MsgClassAllPrivate.class.getName(),
            "MsgClassAllPrivateStatic",
            false,
            null,
            ALL_NOT_NULLABLE_STATIC_FIELDS
        );

    private static final DataField[] ALL_NULLABLE_FIELDS = {
                    new NonStaticDataField("mEnum", null, new EnumDataType(true, cdKind)),
                    new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
                    new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
                    new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
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
            };

    static final RecordClassDescriptor cdMsgClassAllPublic =
            new RecordClassDescriptor(
                    MsgClassAllPublic.class.getName(),
                    "MsgClassAllPublic nullable",
                    false,
                    null,
                    ALL_NULLABLE_FIELDS
            );

    private static final DataField[] ALL_NOT_NULLABLE_FIELDS = {
                    new NonStaticDataField("mEnum", null, new EnumDataType(false, cdKind)),
                    new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
                    new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
                    new NonStaticDataField("mBoolByte", null, new BooleanDataType(false)),
                    new NonStaticDataField("mChar", null, new CharDataType(false)),

                    new NonStaticDataField("mDateTime", null, new DateTimeDataType(false)),
                    new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(false)),
                    new NonStaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
                    new NonStaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false)),
                    new NonStaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false)),
                    new NonStaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, false)),
                    new NonStaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                    new NonStaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, false)),
                    new NonStaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false)),
                    new NonStaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
                    new NonStaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
                    new NonStaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
                    new NonStaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, false)),
                    new NonStaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), false))
            };

    static final RecordClassDescriptor cdMsgClassAllPublicNotNull =
            new RecordClassDescriptor(
                    MsgClassAllPublic.class.getName(),
                    "MsgClassAllPublicNotNull",
                    false,
                    null,
                    ALL_NOT_NULLABLE_FIELDS
            );

    static final RecordClassDescriptor   cdMsgClassAllPublicStatic =
        new RecordClassDescriptor (
            MsgClassAllPublic.class.getName(),
            "MsgClassAllPublicStatic",
            false,
            null,
            ALL_NOT_NULLABLE_STATIC_FIELDS
        );

    @Test
    public void         testStaticPrivateFieldsComp () throws Exception {
        setUpComp ();
        testStaticPrivateFields ();
    }

    @Test
    public void         testStaticPrivateFieldsIntp () throws Exception {
        setUpIntp ();
        testStaticPrivateFields ();
    }

    private void         testStaticPrivateFields () throws Exception {
        MsgClassAllPrivate msg = new MsgClassAllPrivate();
        msg.mString = "IBM";
        msg.mCharSequence = "MSFT";
        msg.mEnum = Kind.SMALL;
        msg.mByte = 1;
        msg.mShort = 2;
        msg.mInt = 3;
        msg.mInt48 = 4;
        msg.mLong = 5;
        msg.mFloat = 63545.34f;
        msg.mDouble = 76456577.76;

        msg.mBoolean = true;
        msg.mChar = 'C';
        msg.mDateTime = 1235746625319L;
        msg.mTimeOfDay= 56841000;

        msg.mPUINT30= 0x1CCCAAAA;
        msg.mPUINT61= 0x1CCCAAAA1CCCAAAAL;
        msg.mPIneterval = 60000;
        msg.mSCALE_AUTO = 1.52;
        msg.mSCALE4 = 1.53;


        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllPrivateStatic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAllPrivate msg2 = new MsgClassAllPrivate();
        boundDecode(msg2, cdMsgClassAllPrivateStatic, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    @Test
    public void         testStaticPublicFieldsComp () throws Exception {
        setUpComp ();
        testStaticPublicFields ();
    }

    @Test
    public void         testStaticPublicFieldsIntp () throws Exception {
        setUpIntp ();
        testStaticPublicFields ();
    }

    private void         testStaticPublicFields () throws Exception {
        MsgClassAllPublic msg = new MsgClassAllPublic();
        msg.mString = "IBM";
        msg.mCharSequence = "MSFT";
        msg.mEnum = Kind.SMALL;
        msg.mByte = 1;
        msg.mShort = 2;
        msg.mInt = 3;
        msg.mInt48 = 4;
        msg.mLong = 5;
        msg.mFloat = 63545.34f;
        msg.mDouble = 76456577.76;

        msg.mBoolean = true;
        msg.mChar = 'C';
        msg.mDateTime = 1235746625319L;
        msg.mTimeOfDay= 56841000;

        msg.mPUINT30= 0x1CCCAAAA;
        msg.mPUINT61= 0x1CCCAAAA1CCCAAAAL;
        msg.mPIneterval = 60000;
        msg.mSCALE_AUTO = 1.52;
        msg.mSCALE4 = 1.53;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllPublicStatic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgClassAllPublic msg2 = new MsgClassAllPublic();
        boundDecode(msg2, cdMsgClassAllPublicStatic, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    static final RecordClassDescriptor   cdMsgClassUnconvesional =
        new RecordClassDescriptor (
            "My UnknownClass Descriptor",
            "MsgClassUnconvesional",
            false,
            null,
            new StaticDataField("smEnum@", null, new EnumDataType(false, cdKind), "SMALL"),
            new StaticDataField("smString\\", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), "IBM"),
            new StaticDataField("smBoole  an", null, new BooleanDataType(false), true),
            new StaticDataField("smChar##", null, new CharDataType(false), 'C'),

            new StaticDataField("s~mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false), 1),
            new StaticDataField("s@mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false), 2),
            new StaticDataField("smIn t*", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false), 3),

            new NonStaticDataField("mEnum@", null, new EnumDataType(false, cdKind)),
            new NonStaticDataField("mString\\", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
            new NonStaticDataField("mBoole  an", null, new BooleanDataType(false)),
            new NonStaticDataField("mChar##", null, new CharDataType(false)),

            new NonStaticDataField("~mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false, null, 100)),
            new NonStaticDataField("@mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false, -30000, null)),
            new NonStaticDataField("mIn t*", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, false, null, 0x7AAAAAAA)),
            new NonStaticDataField("mDou ble?", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, false, -1E302, null)),

            new NonStaticDataField("mPUIN T61 &", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
            new NonStaticDataField("mSCALE_A UTO #", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, false)),
            new NonStaticDataField("mSCAL E4 !", null, new FloatDataType(FloatDataType.getEncodingScaled(4), false))
        );

    static final RecordClassDescriptor   cdMsgClassUnconvesionalNullable =
        new RecordClassDescriptor (
            "My UnknownClass Descriptor",
            "MsgClassUnconvesionalNullable",
            false,
            null,
            new StaticDataField("smEnum@", null, new EnumDataType(true, cdKind), "SMALL"),
            new StaticDataField("smString\\", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), "IBM"),
            new StaticDataField("smBoole  an", null, new BooleanDataType(true), true),
            new StaticDataField("smChar##", null, new CharDataType(true), 'C'),

            new StaticDataField("s~mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true), 1),
            new StaticDataField("s@mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true), 2),
            new StaticDataField("smIn t*", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), 3),

            new NonStaticDataField("mEnum@", null, new EnumDataType(true, cdKind)),
            new NonStaticDataField("mString\\", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
            new NonStaticDataField("mBoole  an", null, new BooleanDataType(true)),
            new NonStaticDataField("mChar##", null, new CharDataType(true)),

            new NonStaticDataField("~mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true, null, 100)),
            new NonStaticDataField("@mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true, -30000, null)),
            new NonStaticDataField("mIn t*", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true, null, 0x7AAAAAAA)),
            new NonStaticDataField("mDou ble?", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true, -1E302, null)),

            new NonStaticDataField("mPUIN T61 &", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
            new NonStaticDataField("mSCALE_A UTO #", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true)),
            new NonStaticDataField("mSCAL E4 !", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true))
        );

    @Test
    public void         testUnconventionalNamesComp () throws Exception {
        setUpComp ();
        testUnconventionalNames ();
    }

    @Test
    public void         testUnconventionalNamesIntp () throws Exception {
        setUpIntp ();
        testUnconventionalNames ();
    }

    private void         testUnconventionalNames () throws Exception {
        List<Object> values = new ArrayList<Object>(11);
        values.add(Kind.BIG);
        values.add("Hi Nikolia!");
        values.add(Boolean.TRUE);
        values.add('C');
        values.add((byte)1);
        values.add((short) 2);
        values.add((int) 3);
        values.add((double) 4.5);
        values.add((long) 3554564546563L);
        values.add((double) 32324.15344);
        values.add((double) 367624.3478);

        MemoryDataOutput out = unboundEncode3(values, cdMsgClassUnconvesional);
        UnboundDecoder udec = factory.createFixedUnboundDecoder(cdMsgClassUnconvesional);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        udec.beginRead(in);
        for (int i = 0; i < values.size(); i++) {
            udec.nextField();
            assertEquals(values.get(i).toString(), udec.getString());
        }

        out = unboundEncodeAsString(values, cdMsgClassUnconvesional);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        udec.beginRead(in);
        for (int i = 0; i < values.size(); i++) {
            udec.nextField();
            assertEquals(values.get(i).toString(), udec.getString());
        }

        out = unboundEncode3(values, cdMsgClassUnconvesionalNullable);
        udec = factory.createFixedUnboundDecoder(cdMsgClassUnconvesionalNullable);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        udec.beginRead(in);
        for (int i = 0; i < values.size(); i++) {
            udec.nextField();
            assertEquals(values.get(i).toString(), udec.getString());
        }

        out = unboundEncodeAsString(values, cdMsgClassUnconvesionalNullable);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        udec.beginRead(in);
        for (int i = 0; i < values.size(); i++) {
            udec.nextField();
            assertEquals(values.get(i).toString(), udec.getString());
        }
    }

    public static class MsgPrivateClass {
        private Kind                mEnum;
        private String              mString;
        private boolean             mBoolean;
        private char                mChar;
        private long                mDateTime;
        private int                 mTimeOfDay;

        public Kind                mEnum2;
        public String              mString2;
        public boolean             mBoolean2;
        public char                mChar2;
        public long                mDateTime2;
        public int                 mTimeOfDay2;

        public String toString() {
            return mEnum + " " + mString + " " + mBoolean + " " + mChar + " " + mDateTime + " " + mTimeOfDay + " " +
                    mEnum2 + " " + mString2 + " " + mBoolean2 + " " + mChar2 + " " + mDateTime2 + " " + mTimeOfDay2;
        }

        public Kind getmEnum () {
            return mEnum;
        }

        public void setmEnum (Kind mEnum) {
            this.mEnum = mEnum;
        }

        public String getmString () {
            return mString;
        }

        public void setmString (String mString) {
            this.mString = mString;
        }

        public boolean ismBoolean () {
            return mBoolean;
        }

        public void setmBoolean (boolean mBoolean) {
            this.mBoolean = mBoolean;
        }

        public char getmChar () {
            return mChar;
        }

        public void setmChar (char mChar) {
            this.mChar = mChar;
        }

        public long getmDateTime () {
            return mDateTime;
        }

        public void setmDateTime (long mDateTime) {
            this.mDateTime = mDateTime;
        }

        public int getmTimeOfDay () {
            return mTimeOfDay;
        }

        public void setmTimeOfDay (int mTimeOfDay) {
            this.mTimeOfDay = mTimeOfDay;
        }
    }

    static final RecordClassDescriptor   cdMsgPrivateClass =
        new RecordClassDescriptor (
            MsgPrivateClass.class.getName (),
            "MsgPrivateClass",
            false,
            null,
            new NonStaticDataField("mEnum", null, new EnumDataType(false, cdKind)),
            new NonStaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
            new NonStaticDataField("mBoolean", null, new BooleanDataType(false)),
            new NonStaticDataField("mChar", null, new CharDataType(false)),
            new NonStaticDataField("mDateTime", null, new DateTimeDataType(false)),
            new NonStaticDataField("mTimeOfDay", null, new TimeOfDayDataType(false)),

            new NonStaticDataField("mEnum2", null, new EnumDataType(false, cdKind)),
            new NonStaticDataField("mString2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false)),
            new NonStaticDataField("mBoolean2", null, new BooleanDataType(false)),
            new NonStaticDataField("mChar2", null, new CharDataType(false)),
            new NonStaticDataField("mDateTime2", null, new DateTimeDataType(false)),
            new NonStaticDataField("mTimeOfDay2", null, new TimeOfDayDataType(false))
    );

    @Test
    public void         testPrivateClassComp () throws Exception {
        setUpComp ();
        testPrivateClass ();
    }

    @Test
    public void         testPrivateClassIntp () throws Exception {
        setUpIntp ();
        testPrivateClass ();
    }

    private void         testPrivateClass () throws Exception {
        MsgPrivateClass msg = new MsgPrivateClass();
        msg.mEnum = Kind.BIG;
        msg.mString = "Hi Nicolia!";
        msg.mBoolean = true;
        msg.mChar = 'C';
        msg.mDateTime = 0x1CCCAAAA1CCCAAAAL;
        msg.mTimeOfDay = 0x1CCCAAAA;

        msg.mEnum2 = Kind.BIG;
        msg.mString2 = "Hi Nicolia!";
        msg.mBoolean2 = true;
        msg.mChar2 = 'C';
        msg.mDateTime2 = 0x1CCCAAAA1CCCAAAAL;
        msg.mTimeOfDay2 = 0x1CCCAAAA;

        // simple encoding: external
        MemoryDataOutput out = boundEncode(msg, cdMsgPrivateClass);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgPrivateClass msg2 = new MsgPrivateClass();
        boundDecode(msg2, cdMsgPrivateClass, in);
        assertEquals(msg.toString(), msg2.toString());

        // simple encoding
        out = boundEncode(msg, cdMsgPrivateClass);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgPrivateClass msg3 = (MsgPrivateClass)boundDecode(in, cdMsgPrivateClass);
        assertEquals(msg.toString(), msg3.toString());
    }

    static final RecordClassDescriptor cdMsgPrivateClassStatic =
            new RecordClassDescriptor(
                    MsgPrivateClass.class.getName(),
                    "MsgPrivateClassStatic",
                    false,
                    null,
                    new StaticDataField("mEnum", null, new EnumDataType(true, cdKind), "SMALL"),
                    new StaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), "IBM"),
                    new StaticDataField("mBoolean", null, new BooleanDataType(true), true),
                    new StaticDataField("mChar", null, new CharDataType(true), 'C'),
                    new StaticDataField("mDateTime", null, new DateTimeDataType(true), "2009-02-27 14:57:05.319"),
                    new StaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true), "15:47:21"),

                    new StaticDataField("mEnum2", null, new EnumDataType(true, cdKind), "SMALL"),
                    new StaticDataField("mString2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), "IBM"),
                    new StaticDataField("mBoolean2", null, new BooleanDataType(true), true),
                    new StaticDataField("mChar2", null, new CharDataType(true), 'C'),
                    new StaticDataField("mDateTime2", null, new DateTimeDataType(true), "2009-02-27 14:57:05.319"),
                    new StaticDataField("mTimeOfDay2", null, new TimeOfDayDataType(true), "15:47:21")
            );

    @Test
    public void         testStaticPrivateClassComp () throws Exception {
        setUpComp ();
        testStaticPrivateClass ();
    }

    @Test
    public void         testStaticPrivateClassIntp () throws Exception {
        setUpIntp ();
        testStaticPrivateClass ();
    }

    private void         testStaticPrivateClass () throws Exception {
        MsgPrivateClass msg = new MsgPrivateClass();
        msg.mEnum = Kind.SMALL;
        msg.mString = "IBM";
        msg.mBoolean = true;
        msg.mChar = 'C';
        msg.mDateTime = 1235746625319L;
        msg.mTimeOfDay = 56841000;

        msg.mEnum2 = Kind.SMALL;
        msg.mString2 = "IBM";
        msg.mBoolean2 = true;
        msg.mChar2 = 'C';
        msg.mDateTime2 = 1235746625319L;
        msg.mTimeOfDay2 = 56841000;

        MemoryDataOutput out = boundEncode(msg, cdMsgPrivateClassStatic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        final MsgPrivateClass msg2 = new MsgPrivateClass();
        boundDecode(msg2, cdMsgPrivateClassStatic, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    private static void copyField(Object src, Object dst, String fieldName) {
        try {
            final Field field = src.getClass().getField(fieldName);
            field.set(dst, field.get(src));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void         testStaticNullsComp () throws Exception {
        setUpComp();
        testStaticNulls();
    }

    @Test
    public void         testStaticNullsIntp () throws Exception {
        setUpIntp();
        testStaticNulls();
    }

    private static final DataField[] ALL_NULLABLE_STATIC_FIELDS = {
                    new StaticDataField("mEnum", null, new EnumDataType(true, cdKind), null),
                    new StaticDataField("mString", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), null),
                    new StaticDataField("mCharSequence", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), null),
                    new StaticDataField("mBoolByte", null, new BooleanDataType(true), null),
                    new StaticDataField("mChar", null, new CharDataType(true), null),

                    new StaticDataField("mDateTime", null, new DateTimeDataType(true), null),
                    new StaticDataField("mTimeOfDay", null, new TimeOfDayDataType(true), null),
                    new StaticDataField("mByte", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true), null),
                    new StaticDataField("mShort", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true), null),
                    new StaticDataField("mInt", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), null),
                    new StaticDataField("mInt48", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true), null),
                    new StaticDataField("mLong", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), null),
                    new StaticDataField("mFloat", null, new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, true), null),
                    new StaticDataField("mDouble", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true), null),

                    new StaticDataField("mPUINT30", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true), null),
                    new StaticDataField("mPUINT61", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true), null),
                    new StaticDataField("mPIneterval", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true), null),
                    new StaticDataField("mSCALE_AUTO", null, new FloatDataType(FloatDataType.ENCODING_SCALE_AUTO, true), null),
                    new StaticDataField("mSCALE4", null, new FloatDataType(FloatDataType.getEncodingScaled(4), true), null)
            };

    private static final String ETALON =
            "null null null false -1 \u0000 -9223372036854775808 -1 -128 -32768 -2147483648 -140737488355328 -9223372036854775808 NaN NaN 0.0 2147483647 9223372036854775807 0 NaN NaN";

    private void         testStaticNulls () throws Exception {

        final RecordClassDescriptor cdMsgClass =
                new RecordClassDescriptor(
                        MsgClassAllPublic.class.getName(),
                        "MsgClassAllPublic",
                        false,
                        null,
                        ALL_NULLABLE_STATIC_FIELDS
                );

        final BoundExternalDecoder dec = factory.createFixedExternalDecoder(CL, cdMsgClass);
        MsgClassAllPublic msg = new MsgClassAllPublic();
        dec.setStaticFields(msg);
        assertEquals(ETALON, msg.toString());
    }
}
