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
package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.SimpleTypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.codec.StaticFieldLayout;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This test overlaps with existing ones. Its purpose is to ensure that our binding specification is supported
 * correctly.
 *
 * @author BazylevD
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecs4 extends Test_RecordCodecsBase {

    /*
     * StringDataType - UTF8 TODO: private case
     */
    public static class MsgClassStringPublic {
        public String string1;
        public String string2;
        public CharSequence cs1;
        public CharSequence cs2;

        public String toString() {
            return string1 + "," + string2 + ","
                    + cs1 + "," + cs2;
        }
    }

    private static final String STRING1 = "Hi Kolia!!!";
    private static final String STRING2 = "Hi Gene!!!";

    private static final RecordClassDescriptor cdMsgClassStringPublic =
            new RecordClassDescriptor(
                    MsgClassStringPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("string1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), true, null),
                    new NonStaticDataField("string2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false)),
                    new NonStaticDataField("cs1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), true, null),
                    new NonStaticDataField("cs2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false))
            );


    private static final RecordClassDescriptor cdMsgClassStringPublicStatic =
            new RecordClassDescriptor(
                    MsgClassStringPublic.class.getName(),
                    null,
                    false,
                    null,
                    new StaticDataField("string1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), STRING1),
                    new StaticDataField("string2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), null),
                    new StaticDataField("cs1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, false), STRING2),
                    new StaticDataField("cs2", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), null)
            );

    @Test
    public void testStringBoundComp() throws Exception {
        setUpComp();
        testStringBound();
    }

    @Test
    public void testStringBoundIntp() throws Exception {
        setUpIntp();
        testStringBound();
    }

    private void testStringBound() throws Exception {
        MsgClassStringPublic msg = new MsgClassStringPublic();
        msg.string1 = STRING1;
        msg.string2 = null;
        msg.cs1 = STRING2;
        msg.cs2 = null;
        testStringBound(msg);

        msg.string1 = STRING1;
        msg.string2 = null;
        msg.cs1 = new StringBuilder(STRING2);
        msg.cs2 = null;
        testStringBound(msg);
    }

    private void testStringBound(MsgClassStringPublic msg) throws Exception {
        msg.string1 = STRING1;
        msg.string2 = null;
        msg.cs1 = STRING2;
        msg.cs2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassStringPublic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassStringPublic msg2 = new MsgClassStringPublic();
            boundDecode(msg2, cdMsgClassStringPublic, in);
            Assert.assertEquals("public nonstatic", msg.toString(), msg2.toString());
        }

        out = boundEncode(msg, cdMsgClassStringPublicStatic);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassStringPublic msg2 = new MsgClassStringPublic();
            boundDecode(msg2, cdMsgClassStringPublicStatic, in);
            Assert.assertEquals("public static", msg.toString(), msg2.toString());
        }
    }

    /*
     * VarcharDataType - ALPHANUMERIC(10) TODO: private case
     */
    public static class MsgClassAlphanumericPublic {
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

    private final static long LONG_STRING = 0xab2fba74524d4556L; // LONG123456
    private static final String ALPHANUMERIC_4LONG = "LONG123456";
    private static final String ALPHANUMERIC1 = "ABCDE 1234_#$%";
    private static final String ALPHANUMERIC2 = "ZUYXW 098-@+";

    private static final RecordClassDescriptor cdMsgClassAlphanumeric =
            new RecordClassDescriptor(
                    MsgClassAlphanumericPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("long1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), true, null),
                    new NonStaticDataField("long2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false)),
                    new NonStaticDataField("string1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), true, null),
                    new NonStaticDataField("string2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false)),
                    new NonStaticDataField("cs1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), true, null),
                    new NonStaticDataField("cs2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false))
            );

    private static final RecordClassDescriptor cdMsgClassAlphanumericStatic =
            new RecordClassDescriptor(
                    MsgClassAlphanumericPublic.class.getName(),
                    null,
                    false,
                    null,
                    new StaticDataField("long1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), ALPHANUMERIC_4LONG),
                    new StaticDataField("long2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(10), true, false), null),
                    new StaticDataField("string1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), ALPHANUMERIC1),
                    new StaticDataField("string2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), null),
                    new StaticDataField("cs1", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), ALPHANUMERIC2),
                    new StaticDataField("cs2", null, new VarcharDataType(VarcharDataType.getEncodingAlphanumeric(278), true, false), null)
            );

    @Test
    public void testAlphanumericBoundComp() throws Exception {
        setUpComp();
        testAlphanumericBound();
    }

    @Test
    public void testAlphanumericBoundIntp() throws Exception {
        setUpIntp();
        testAlphanumericBound();
    }

    private void testAlphanumericBound() throws Exception {
        MsgClassAlphanumericPublic msg = new MsgClassAlphanumericPublic();
        msg.long1 = LONG_STRING;
        msg.long2 = IntegerDataType.INT64_NULL;
        msg.string1 = ALPHANUMERIC1;
        msg.string2 = null;
        msg.cs1 = ALPHANUMERIC2;
        msg.cs2 = null;
        testAlphanumericBound(msg);

        msg.long1 = LONG_STRING;
        msg.long2 = IntegerDataType.INT64_NULL;
        msg.string1 = ALPHANUMERIC1;
        msg.string2 = null;
        msg.cs1 = new StringBuilder(ALPHANUMERIC2);
        msg.cs2 = null;
        testAlphanumericBound(msg);
    }

    private void testAlphanumericBound(MsgClassAlphanumericPublic msg) throws Exception {
        MemoryDataOutput out = boundEncode(msg, cdMsgClassAlphanumeric);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassAlphanumericPublic msg2 = new MsgClassAlphanumericPublic();
            boundDecode(msg2, cdMsgClassAlphanumeric, in);
            Assert.assertEquals("public nonstatic", msg.toString(), msg2.toString());
        }

        out = boundEncode(msg, cdMsgClassAlphanumericStatic);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassAlphanumericPublic msg2 = new MsgClassAlphanumericPublic();
            boundDecode(msg2, cdMsgClassAlphanumericStatic, in);
            Assert.assertEquals("public static", msg.toString(), msg2.toString());
        }
    }

    public static class MsgClassAllIntPublic {
        public byte b1;
        public byte b2;
        public short s1;
        public short s2;
        public int i1;
        public int i2;
        public long i48_1;
        public long i48_2;
        public long l1;
        public long l2;
        public int pint30_1;
        public int pint30_2;
        public long pint61_1;
        public long pint61_2;
        public int piterval1;
        public int piterval2;

        public String toString() {
            return String.format("%x,%x,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", 0xFF & b1, 0xFF & b2, s1, s2, i1, i2, i48_1, i48_2, l1, l2,
                    pint30_1, pint30_2, pint61_1, pint61_2, piterval1, piterval2);
        }
    }

    public static class MsgClassAllIntPublicLong {
        public long b1;
        public long b2;
        public long s1;
        public long s2;
        public long i1;
        public long i2;
        public long i48_1;
        public long i48_2;
        public long l1;
        public long l2;
        public long pint30_1;
        public long pint30_2;
        public long pint61_1;
        public long pint61_2;
        public long piterval1;
        public long piterval2;

        public String toString() {
            return String.format("%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d,%d", b1, b2, s1, s2, i1, i2, i48_1, i48_2, l1, l2,
                    pint30_1, pint30_2, pint61_1, pint61_2, piterval1, piterval2);
        }
    }

    private static final RecordClassDescriptor cdMsgClassAllIntPublic =
            new RecordClassDescriptor(
                    MsgClassAllIntPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("b1", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
                    new NonStaticDataField("b2", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
                    new NonStaticDataField("s1", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false)),
                    new NonStaticDataField("s2", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
                    new NonStaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                    new NonStaticDataField("i2", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                    new NonStaticDataField("i48_1", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
                    new NonStaticDataField("i48_2", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
                    new NonStaticDataField("l1", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("l2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("pint30_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
                    new NonStaticDataField("pint30_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
                    new NonStaticDataField("pint61_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
                    new NonStaticDataField("pint61_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
                    new NonStaticDataField("piterval1", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
                    new NonStaticDataField("piterval2", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true))
            );

    private static final RecordClassDescriptor cdMsgClassAllIntPublicStatic =
            new RecordClassDescriptor(
                    MsgClassAllIntPublic.class.getName(),
                    null,
                    false,
                    null,
                    new StaticDataField("b1", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false), 0x55),
                    new StaticDataField("b2", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true), null),
                    new StaticDataField("s1", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false), -30001),
                    new StaticDataField("s2", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true), null),
                    new StaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), 2000000001),
                    new StaticDataField("i2", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), null),
                    new StaticDataField("i48_1", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true), 0x7FFFFFFFFF0AL),
                    new StaticDataField("i48_2", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true), null),
                    new StaticDataField("l1", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), 2000000000001L),
                    new StaticDataField("l2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), null),
                    new StaticDataField("pint30_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false), 0x3FFFFFFE),
                    new StaticDataField("pint30_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true), null),
                    new StaticDataField("pint61_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false), 0x1FFFFFFFFFFFFFFEL),
                    new StaticDataField("pint61_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true), null),
                    new StaticDataField("piterval1", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false), Integer.MAX_VALUE),
                    new StaticDataField("piterval2", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true), null)
            );

    private static final RecordClassDescriptor cdMsgClassAllIntPublicLong =
            new RecordClassDescriptor(
                    MsgClassAllIntPublicLong.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("b1", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
                    new NonStaticDataField("b2", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
                    new NonStaticDataField("s1", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false)),
                    new NonStaticDataField("s2", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true)),
                    new NonStaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                    new NonStaticDataField("i2", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                    new NonStaticDataField("i48_1", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
                    new NonStaticDataField("i48_2", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true)),
                    new NonStaticDataField("l1", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("l2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true)),
                    new NonStaticDataField("pint30_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
                    new NonStaticDataField("pint30_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true)),
                    new NonStaticDataField("pint61_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false)),
                    new NonStaticDataField("pint61_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true)),
                    new NonStaticDataField("piterval1", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false)),
                    new NonStaticDataField("piterval2", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true))
            );

    private static final RecordClassDescriptor cdMsgClassAllIntPublicStaticLong =
            new RecordClassDescriptor(
                    MsgClassAllIntPublicLong.class.getName(),
                    null,
                    false,
                    null,
                    new StaticDataField("b1", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false), 0x55),
                    new StaticDataField("b2", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true), null),
                    new StaticDataField("s1", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, false), -30001),
                    new StaticDataField("s2", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true), null),
                    new StaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), 2000000001),
                    new StaticDataField("i2", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true), null),
                    new StaticDataField("i48_1", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true), 0x7FFFFFFFFF0AL),
                    new StaticDataField("i48_2", null, new IntegerDataType(IntegerDataType.ENCODING_INT48, true), null),
                    new StaticDataField("l1", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), 2000000000001L),
                    new StaticDataField("l2", null, new IntegerDataType(IntegerDataType.ENCODING_INT64, true), null),
                    new StaticDataField("pint30_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false), 0x3FFFFFFE),
                    new StaticDataField("pint30_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true), null),
                    new StaticDataField("pint61_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, false), 0x1FFFFFFFFFFFFFFEL),
                    new StaticDataField("pint61_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT61, true), null),
                    new StaticDataField("piterval1", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, false), Integer.MAX_VALUE),
                    new StaticDataField("piterval2", null, new IntegerDataType(IntegerDataType.ENCODING_PINTERVAL, true), null)
            );

    @Test
    public void testAllIntBoundComp() throws Exception {
        setUpComp();
        testAllIntBound();
    }

    @Test
    public void testAllIntBoundIntp() throws Exception {
        setUpIntp();
        testAllIntBound();
    }

    private void testAllIntBound() throws Exception {
        final MsgClassAllIntPublic msg = new MsgClassAllIntPublic();
        msg.b1 = 0x55;
        msg.b2 = IntegerDataType.INT8_NULL;
        msg.s1 = -30001;
        msg.s2 = IntegerDataType.INT16_NULL;
        msg.i1 = 2000000001;
        msg.i2 = IntegerDataType.INT32_NULL;
        msg.i48_1 = 0x7FFFFFFFFF0AL;
        msg.i48_2 = IntegerDataType.INT48_NULL;
        msg.l1 = 2000000000001L;
        msg.l2 = IntegerDataType.INT64_NULL;
        msg.pint30_1 = 0x3FFFFFFE;
        msg.pint30_2 = IntegerDataType.PUINT30_NULL;
        msg.pint61_1 = 0x1FFFFFFFFFFFFFFEL;
        msg.pint61_2 = IntegerDataType.PUINT61_NULL;
        msg.piterval1 = Integer.MAX_VALUE;
        msg.piterval2 = IntegerDataType.PINTERVAL_NULL;

        testAllIntBound(msg);

        final MsgClassAllIntPublicLong msg2 = new MsgClassAllIntPublicLong();
        msg2.b1 = 0x55;
        msg2.b2 = IntegerDataType.INT8_NULL;
        msg2.s1 = -30001;
        msg2.s2 = IntegerDataType.INT16_NULL;
        msg2.i1 = 2000000001;
        msg2.i2 = IntegerDataType.INT32_NULL;
        msg2.i48_1 = 0x7FFFFFFFFF0AL;
        msg2.i48_2 = IntegerDataType.INT48_NULL;
        msg2.l1 = 2000000000001L;
        msg2.l2 = IntegerDataType.INT64_NULL;
        msg2.pint30_1 = 0x3FFFFFFE;
        msg2.pint30_2 = IntegerDataType.PUINT30_NULL;
        msg2.pint61_1 = 0x1FFFFFFFFFFFFFFEL;
        msg2.pint61_2 = IntegerDataType.PUINT61_NULL;
        msg2.piterval1 = Integer.MAX_VALUE;
        msg2.piterval2 = IntegerDataType.PINTERVAL_NULL;
        testAllIntBound(msg2);
    }

    private void testAllIntBound(MsgClassAllIntPublic msg) throws Exception {
        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllIntPublic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassAllIntPublic msg2 = new MsgClassAllIntPublic();
            boundDecode(msg2, cdMsgClassAllIntPublic, in);
            Assert.assertEquals("public nonstatic", msg.toString(), msg2.toString());
        }

        out = boundEncode(msg, cdMsgClassAllIntPublicStatic);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassAllIntPublic msg2 = new MsgClassAllIntPublic();
            boundDecode(msg2, cdMsgClassAllIntPublicStatic, in);
            Assert.assertEquals("public static", msg.toString(), msg2.toString());
        }
    }

    private void testAllIntBound(MsgClassAllIntPublicLong msg) throws Exception {
        MemoryDataOutput out = boundEncode(msg, cdMsgClassAllIntPublicLong);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassAllIntPublicLong msg2 = new MsgClassAllIntPublicLong();
            boundDecode(msg2, cdMsgClassAllIntPublicLong, in);
            Assert.assertEquals("public nonstatic", msg.toString(), msg2.toString());
        }

        out = boundEncode(msg, cdMsgClassAllIntPublicStaticLong);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassAllIntPublicLong msg2 = new MsgClassAllIntPublicLong();
            boundDecode(msg2, cdMsgClassAllIntPublicStaticLong, in);
            Assert.assertEquals("public static", msg.toString(), msg2.toString());
        }
    }

    public static class MsgClassTempPublic {
        public long b1;
        public long b2;
        public long pint30_1;
        public long pint30_2;

        public String toString() {
            return String.format("%d,%d,%d,%d", b1, b2, pint30_1, pint30_2);
        }
    }

    private static final RecordClassDescriptor cdMsgClassTempPublic =
            new RecordClassDescriptor(
                    MsgClassTempPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("b1", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false)),
                    new NonStaticDataField("b2", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true)),
                    new NonStaticDataField("pint30_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false)),
                    new NonStaticDataField("pint30_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true))
            );

    private static final RecordClassDescriptor cdMsgClassTempPublicStatic =
            new RecordClassDescriptor(
                    MsgClassTempPublic.class.getName(),
                    null,
                    false,
                    null,
                    new StaticDataField("b1", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, false), 0x55),
                    new StaticDataField("b2", null, new IntegerDataType(IntegerDataType.ENCODING_INT8, true), null),
                    new StaticDataField("pint30_1", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, false), 0x3FFFFFFE),
                    new StaticDataField("pint30_2", null, new IntegerDataType(IntegerDataType.ENCODING_PUINT30, true), null)
            );

    // TODO: what do I need it for?
    @Test
    public void testTempComp() throws Exception {
        setUpComp();
        testTempBound();
    }

    @Test
    public void testTempIntp() throws Exception {
        setUpIntp();
        testTempBound();
    }

    private void testTempBound() throws Exception {
        final MsgClassTempPublic msg = new MsgClassTempPublic ();
        msg.b1 = 0x55;
        msg.b2 = IntegerDataType.INT8_NULL;
        msg.pint30_1 = 0x3FFFFFFE;
        msg.pint30_2 = IntegerDataType.PUINT30_NULL;

        testTempBound(msg);
    }

    private void testTempBound(MsgClassTempPublic msg) throws Exception {
        MemoryDataOutput out = boundEncode(msg, cdMsgClassTempPublic);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassTempPublic msg2 = new MsgClassTempPublic();
            boundDecode(msg2, cdMsgClassTempPublic, in);
            Assert.assertEquals("public nonstatic", msg.toString(), msg2.toString());
        }

        out = boundEncode(msg, cdMsgClassTempPublicStatic);
        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        {
            final MsgClassTempPublic msg2 = new MsgClassTempPublic();
            boundDecode(msg2, cdMsgClassTempPublicStatic, in);
            Assert.assertEquals("public static", msg.toString(), msg2.toString());
        }
    }

    private static final EnumClassDescriptor ecdSampleEnum = new EnumClassDescriptor(null, null, false,
            new EnumValue("positive", 10),
            new EnumValue("big", 300),
            new EnumValue("negative", -2)
    );

    // This is hand-written part of Test_RecordCodecs5.testEarlyBindingStatic
    @Test
    public void testEarlyBindingStatic() throws NoSuchFieldException {

        // VARCHAR to long
        {
            // ok
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("l1", null, new VarcharDataType("ALPHANUMERIC(11)", true, false), "1234567890"));
            layout.bind(MsgClassAllIntPublic.class);
        }
        try {
            // bad char
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("l1", null, new VarcharDataType("ALPHANUMERIC(11)", true, false), "a45678901"));
            layout.bind(MsgClassAllIntPublic.class);
            fail("long to VARCHAR - bad char");
        } catch (java.lang.IllegalArgumentException e) {
            Assert.assertEquals(null, "java.lang.IllegalArgumentException: Character 'a' (0x61) in 'a45678901' is out of range", e.toString());
        }
        try {
            // bad length
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("l1", null, new VarcharDataType("ALPHANUMERIC(11)", true, false), "12345678901"));
            layout.bind(MsgClassAllIntPublic.class);
            fail("long to VARCHAR - bad length");
        } catch (java.lang.IllegalArgumentException e) {
            Assert.assertEquals(null, "java.lang.IllegalArgumentException: '12345678901' is longer then 10", e.toString());
        }

        // ENUM to byte
        {
            // ok
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("b1", null, new EnumDataType(true, ecdSampleEnum), "positive"));
            layout.bind(MsgClassAllIntPublic.class);
        }
        try {
            // too big
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("b1", null, new EnumDataType(true, ecdSampleEnum), "big"));
            layout.bind(MsgClassAllIntPublic.class);
            fail("byte to ENUM - too big");
        } catch (java.lang.IllegalArgumentException e) {
            Assert.assertEquals(null, "java.lang.IllegalArgumentException: Cannot store static value 300 to byte", e.toString());
        }

    }

    /*
     * Test that RCD pointing out to a missed class, is processed correctly (see #11925)
     */

    private static final RecordClassDescriptor cdMsgMissedClass =
            new RecordClassDescriptor(
                    "MissedClass",
                    null,
                    false,
                    null,
                    new NonStaticDataField("string1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null)
            );

    private static final RecordClassDescriptor cdMsgMarketMessage =
            new RecordClassDescriptor(
                    MarketMessage.class.getName(),
                    null,
                    true,
                    null,
                    new NonStaticDataField("originalTimestamp", null, new DateTimeDataType (true)),
                    new NonStaticDataField("currencyCode", null, new IntegerDataType(IntegerDataType.ENCODING_INT16, true))
            );

    private static final RecordClassDescriptor cdMsgMissedClassWithMM =
            new RecordClassDescriptor(
                    "MissedClass",
                    null,
                    false,
                    cdMsgMarketMessage,
                    new NonStaticDataField("string1", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false), true, null)
            );

    @Test
    public void testMissedClassComp() throws Exception {
        setUpComp();
        testMissedClass();
    }

    @Test
    public void testMissedClassIntp() throws Exception {
        setUpIntp();
        testMissedClass();
    }

    private void testMissedClass() throws Exception {
        factory.createFixedBoundEncoder(TYPE_LOADER, cdMsgMissedClass);
        factory.createFixedBoundDecoder(TYPE_LOADER, cdMsgMissedClass);

        factory.createFixedBoundEncoder(TYPE_LOADER, cdMsgMissedClassWithMM);
        factory.createFixedBoundDecoder(TYPE_LOADER, cdMsgMissedClassWithMM);
    }

    /*
     * Test early binding for BOOLEAN type
     */

    @Test
    public void testEarlyBinding() {
        {
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("mBoolean", null, new BooleanDataType(false), "true"));
            layout.bind(MsgClassAllPublic.class);
        }
        try {
            final StaticFieldLayout layout = new StaticFieldLayout(null, new StaticDataField("mBoolean", null, new BooleanDataType(true), null));
            layout.bind(MsgClassAllPublic.class);
            fail("mBoolean to static BOOLEAN-nullable");
        } catch (java.lang.IllegalArgumentException e) {
            org.junit.Assert.assertEquals(null, "java.lang.IllegalArgumentException: Nullable BOOLEAN cannot be bound to boolean field", (e).toString());
        }
        {
            final NonStaticFieldLayout layout = new NonStaticFieldLayout(new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField("mBoolean", null, new BooleanDataType(false)));
            layout.bind(MsgClassAllPublic.class);
        }
        try {
            final NonStaticFieldLayout layout = new NonStaticFieldLayout(new com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField("mBoolean", null, new BooleanDataType(true)));
            layout.bind(MsgClassAllPublic.class);
            fail("mBoolean to non-static BOOLEAN-nullable");
        } catch (java.lang.IllegalArgumentException e) {
            org.junit.Assert.assertEquals(null, "java.lang.IllegalArgumentException: Nullable BOOLEAN cannot be bound to boolean field", (e).toString());
        }
    }

    /*
     * Test that
     * 1. enum field with an absent value (in comparison with ECD) can be used in encoder (#11935)
     * 2. enum field with an absent value (in comparison with ECD) can be used in decoder and the decoder throws an exception,
     *    when an absent value is read from Timebase (#6854)
     * 3. enum field with an added value (in comparison with ECD) can be used in decoder
     * 4. enum field with an added value (in comparison with ECD) can be used in encoder and the encoder throws an exception,
     *    when an absent value is written to Timebase
     */

    public enum MyEnum {
        RED,
        YELLOW,
        GREEN
    }

    public static class MsgClassEnumPublic {
        public MyEnum e1;
        public MyEnum e2;

        @Override
        public String toString() {
            return e1 + "," + e2;
        }
    }

    static final EnumClassDescriptor ECD_MY_ENUM = new EnumClassDescriptor(
            "myEnum",
            null,
            "RED", "YELLOW", "GREEN"
            );

    private static final EnumClassDescriptor ecdMyEnumAdded = new EnumClassDescriptor(
            "myEnum",
            null,
            "RED", "YELLOW", "GREEN", "BLACK"
            );

    private static final EnumClassDescriptor ecdMyEnumMissed = new EnumClassDescriptor(
            "myEnum",
            null,
            "RED", "YELLOW"
            );

    static final EnumClassDescriptor ECD_MY_BITMASK = new EnumClassDescriptor(
            "myEnum",
            null,
            true,
            "RED", "YELLOW", "GREEN"
    );

    private static final RecordClassDescriptor cdMsgClassEnumPublic =
            new RecordClassDescriptor(
                    MsgClassEnumPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(true, ECD_MY_ENUM))
            );

    private static final RecordClassDescriptor cdMsgClassEnumPublicAdded =
            new RecordClassDescriptor(
                    MsgClassEnumPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(true, ecdMyEnumAdded))
            );

    private static final RecordClassDescriptor cdMsgClassEnumPublicMissed =
            new RecordClassDescriptor(
                    MsgClassEnumPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(true, ecdMyEnumMissed))
            );

    @Test
    public void testEnumBoundChangedComp() throws Exception {
        setUpComp();
        testEnumBoundChangedPublic();
        testEnumBoundChangedPrivate();
    }

    @Test
    public void testEnumBoundChangedIntp() throws Exception {
        setUpIntp();
        testEnumBoundChangedPublic();
        testEnumBoundChangedPrivate();
    }



    private void testEnumBoundChangedPublic() {
        final MsgClassEnumPublic msg = new MsgClassEnumPublic();
        msg.e1 = Test_RecordCodecs4.MyEnum.YELLOW;
        msg.e2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassEnumPublic);
        MemoryDataInput in = new MemoryDataInput(out);
        MsgClassEnumPublic msg2 = new MsgClassEnumPublic();
        boundDecode(msg2, cdMsgClassEnumPublic, in);
        Assert.assertEquals("public 0.", msg.toString(), msg2.toString());

        // 1.
        out = boundEncode(msg, cdMsgClassEnumPublicAdded);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPublic();
        boundDecode(msg2, cdMsgClassEnumPublic, in);
        Assert.assertEquals("public 1.", msg.toString(), msg2.toString());

        // 2.
        out = boundEncode(msg, cdMsgClassEnumPublic);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPublic();
        try {
            boundDecode(msg2, cdMsgClassEnumPublicAdded, in);
            fail("cannot decoding enum schema into smaller class!");
//            Assert.assertEquals("public 2.", msg.toString(), msg2.toString());
        } catch (IllegalArgumentException ex) {
            assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", ex.getMessage());
        } catch (RuntimeException ex) {
            if (ex.getCause() != null)
                assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", ex.getCause().getMessage());
            else
                throw ex;
        }
        // 2.a
        final ArrayList<Object> values = new ArrayList<Object>();
        values.add("BLACK");
        values.add(null);
        out = unboundEncode(values , cdMsgClassEnumPublicAdded);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPublic();
        try {
            boundDecode(msg2, cdMsgClassEnumPublicAdded, in);
            fail("public 2.a");
        } catch (IllegalArgumentException e) {
            assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", e.getMessage());
//            Assert.assertEquals("public 2.a", "java.lang.IllegalArgumentException: value is out of range 3", e.toString());
        } catch (RuntimeException e) {
            if (e.getCause() != null)
                assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", e.getCause().getMessage());
//                Assert.assertEquals("public 2.a", "java.lang.IllegalArgumentException: value is out of range 3", e.getCause().toString());
            else
                throw e;
        }


        // 3.
        out = boundEncode(msg, cdMsgClassEnumPublic);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPublic();
        boundDecode(msg2, cdMsgClassEnumPublicMissed, in);
        Assert.assertEquals("public 3.", msg.toString(), msg2.toString());

        // 4.
        out = boundEncode(msg, cdMsgClassEnumPublicMissed);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPublic();
        boundDecode(msg2, cdMsgClassEnumPublic, in);
        Assert.assertEquals("public 4.", msg.toString(), msg2.toString());

        // 4.a
        msg.e1 = Test_RecordCodecs4.MyEnum.GREEN;
        try {
            boundEncode(msg, cdMsgClassEnumPublicMissed);
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("public 4.a", "value is absent in schema: e1 == GREEN", e.getMessage());
        }
    }


    public static class MsgClassEnumPrivate {
        private MyEnum e1;
        private MyEnum e2;

        @Override
        public String toString() {
            return e1 + "," + e2;
        }

        public MyEnum getE1 () {
            return e1;
        }

        public void setE1 (MyEnum e1) {
            this.e1 = e1;
        }

        public MyEnum getE2 () {
            return e2;
        }

        public void setE2 (MyEnum e2) {
            this.e2 = e2;
        }
    }

    private static final RecordClassDescriptor cdMsgClassEnumPrivate =
            new RecordClassDescriptor(
                    MsgClassEnumPrivate.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(true, ECD_MY_ENUM))
            );

    private static final RecordClassDescriptor cdMsgClassEnumPrivateAdded =
            new RecordClassDescriptor(
                    MsgClassEnumPrivate.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(true, ecdMyEnumAdded))
            );

    private static final RecordClassDescriptor cdMsgClassEnumPrivateMissed =
            new RecordClassDescriptor(
                    MsgClassEnumPrivate.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(true, ecdMyEnumMissed))
            );

    private void testEnumBoundChangedPrivate() {
        final MsgClassEnumPrivate msg = new MsgClassEnumPrivate();
        msg.e1 = Test_RecordCodecs4.MyEnum.YELLOW;
        msg.e2 = null;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassEnumPrivate);
        MemoryDataInput in = new MemoryDataInput(out);
        MsgClassEnumPrivate msg2 = new MsgClassEnumPrivate();
        boundDecode(msg2, cdMsgClassEnumPrivate, in);
        Assert.assertEquals("public 0.", msg.toString(), msg2.toString());

        // 1.
        out = boundEncode(msg, cdMsgClassEnumPrivateAdded);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPrivate();
        boundDecode(msg2, cdMsgClassEnumPrivate, in);
        Assert.assertEquals("public 1.", msg.toString(), msg2.toString());

        // 2.
        out = boundEncode(msg, cdMsgClassEnumPrivate);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPrivate();
        try{
            boundDecode(msg2, cdMsgClassEnumPrivateAdded, in);
            fail("cannot decoding enum schema into smaller class!");
//            Assert.assertEquals("public 2.", msg.toString(), msg2.toString());
        } catch (IllegalArgumentException e) {
            assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", e.getMessage());
        } catch (RuntimeException ex) {
            if (ex.getCause() != null)
                assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", ex.getCause().getMessage());
            else
                throw ex;
        }

        // 2.a
        final ArrayList<Object> values = new ArrayList<Object>();
        values.add("BLACK");
        values.add(null);
        out = unboundEncode(values , cdMsgClassEnumPrivateAdded);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPrivate();
        try {
            boundDecode(msg2, cdMsgClassEnumPrivateAdded, in);
            fail("public 2.a");
        } catch (IllegalArgumentException e) {
            assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", e.getMessage());
//            Assert.assertEquals("public 2.a", "java.lang.IllegalArgumentException: value is out of range 3", e.toString());
        } catch (RuntimeException e) {
            if (e.getCause() != null)
                assertEquals("Class " + MyEnum.class.getName() + " must contains all schema values for decoding!", e.getCause().getMessage());
//                Assert.assertEquals("public 2.a", "java.lang.IllegalArgumentException: value is out of range 3", e.getCause().toString());
            else
                throw e;
        }


        // 3.
        out = boundEncode(msg, cdMsgClassEnumPrivate);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPrivate();
        boundDecode(msg2, cdMsgClassEnumPrivateMissed, in);
        Assert.assertEquals("public 3.", msg.toString(), msg2.toString());

        // 4.
        out = boundEncode(msg, cdMsgClassEnumPrivateMissed);
        in = new MemoryDataInput(out);
        msg2 = new MsgClassEnumPrivate();
        boundDecode(msg2, cdMsgClassEnumPrivate, in);
        Assert.assertEquals("public 4.", msg.toString(), msg2.toString());

        // 4.a
        msg.e1 = Test_RecordCodecs4.MyEnum.GREEN;
        try {
            boundEncode(msg, cdMsgClassEnumPrivateMissed);
        } catch (IllegalArgumentException e) {
            Assert.assertEquals("public 4.a", "value is absent in schema: e1 == GREEN", e.getMessage());
        }
    }

   /*
    * ENUM/BITMASK: test invalid values check
    * bounded to int/CharSequence/enum
    * unbound: setInt/setString
    */

    @Test
    public void testEnumViolationComp() throws Exception {
        setUpComp();
        testEnumViolationUnbound();
        testEnumViolationBound();

        testEnumViolationBound2();
    }

    @Test
    public void testEnumViolationIntp() throws Exception {
        setUpIntp();
        testEnumViolationUnbound();
        testEnumViolationBound();

        testEnumViolationBound2();
    }

    private static final RecordClassDescriptor cdMsgClassEnumTmp =
            new RecordClassDescriptor(
                    MsgClassEnumPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(false, ECD_MY_ENUM)),
                    new NonStaticDataField("e2", null, new EnumDataType(false, ECD_MY_BITMASK))
            );

    private static final Object[] ELEMENT_VALUES_GOOD2 = {
            1,
            3
    };

    private static final Object[] ELEMENT_VALUES_GOOD_STR2 = {
            "GREEN",
            "YELLOW|GREEN"
    };

    private static final Object[] ELEMENT_VALUES_BAD2 = {
            100,
            101
    };

    private static final Object[] ELEMENT_VALUES_BAD_STR2 = {
            "XXL",
            "XXL2"
    };

    private void testEnumViolationUnbound() throws Exception {
        final ArrayList<Object> values = new ArrayList<Object>();
        final DataField[] fields = cdMsgClassEnumTmp.getFields();

        for (int i = 0; i < fields.length; i++)
            values.add(null);

        // null-value
        for (int i = 0; i < fields.length; i++) {
            final String fieldName = fields[i].getName();
            try {
                unboundEncode(values, cdMsgClassEnumTmp);
                fail("null " + fieldName);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("null " + fieldName, String.format("'%s' field is not nullable", fieldName), e.getMessage());
            }
            values.set(i, ELEMENT_VALUES_GOOD2[i]);
        }

        // good
        testRcdUnbound(values, cdMsgClassEnumTmp);

        // good str
        for (int i = 0; i < fields.length; i++)
            values.set(i, ELEMENT_VALUES_GOOD_STR2[i]);
        testRcdUnbound(values, cdMsgClassEnumTmp);

        //  bad element
        for (int i = 0; i < fields.length; i++) {
            final String fieldName = fields[i].getName();
            values.set(i, ELEMENT_VALUES_BAD2[i]);
            try {
                unboundEncode(values, cdMsgClassEnumTmp);
                fail("bad " + fieldName);
            } catch (IllegalArgumentException e) {
                final String expected = cdMsgClassEnumTmp.getName() + "." + fieldName + " == " + ELEMENT_VALUES_BAD2[i];
                Assert.assertEquals("bad " + fieldName, expected, e.getMessage());
            }
            values.set(i, ELEMENT_VALUES_GOOD2[i]);
        }

        // bad element str
        for (int i = 0; i < fields.length; i++) {
            final String fieldName = fields[i].getName();
            values.set(i, ELEMENT_VALUES_BAD_STR2[i]);
            try {
                unboundEncode(values, cdMsgClassEnumTmp);
                fail("bad " + fieldName);
            } catch (IllegalArgumentException e) {
                final String expected = cdMsgClassEnumTmp.getName() + "." + fieldName + " == " + ELEMENT_VALUES_BAD_STR2[i];
                Assert.assertEquals("bad " + fieldName, expected, e.getMessage());
            }
            values.set(i, ELEMENT_VALUES_GOOD2[i]);
        }
    }

    public static class MsgClassEnumOtherBindingsPublic {
        public int e1;
        public CharSequence e2;
        public int f3;
        public CharSequence f4;

        @Override
        public String toString() {
            return e1 + "," + e2 + ","
                    + f3 + "," + f4;
        }

        void initNulls() {
            e1 = EnumDataType.NULL;
            e2 = null;
            f3 = EnumDataType.NULL;
            f4 = null;
        }
    }

    private static final RecordClassDescriptor cdMsgClassEnumOtherBindingsPublic =
            new RecordClassDescriptor(
                    MsgClassEnumOtherBindingsPublic.class.getName(),
                    null,
                    false,
                    null,
                    new NonStaticDataField("e1", null, new EnumDataType(false, ECD_MY_ENUM)),
                    new NonStaticDataField("e2", null, new EnumDataType(false, ECD_MY_ENUM)),
                    new NonStaticDataField("f3", null, new EnumDataType(false, ECD_MY_BITMASK)),
                    new NonStaticDataField("f4", null, new EnumDataType(false, ECD_MY_BITMASK))
            );

    public static class MsgClassEnumOtherBindingsPrivate {
        public int e1;
        public CharSequence e2;
        public int f3;
        public CharSequence f4;

        @Override
        public String toString() {
            return e1 + "," + e2 + ","
                    + f3 + "," + f4;
        }

        void initNulls() {
            e1 = EnumDataType.NULL;
            e2 = null;
            f3 = EnumDataType.NULL;
            f4 = null;
        }
    }

    private static final Object[] ENUM_VALUES_GOOD = {
            1,
            "YELLOW",
            3,
            "YELLOW|GREEN"
    };

    private static final Object[] ENUM_VALUES_BAD = {
            101,
            "BAD",
            103,
            "VERY BAD"
    };

    private void testEnumViolationBound() throws Exception {
        // public
        {
            final Field[] fields = MsgClassEnumOtherBindingsPublic.class.getDeclaredFields();
            final MsgClassEnumOtherBindingsPublic msg = new MsgClassEnumOtherBindingsPublic();
            msg.initNulls();

            // null-value
            for (int i = 0; i < fields.length; i++) {
                final String fieldName = fields[i].getName();
                try {
                    boundEncode(msg, cdMsgClassEnumOtherBindingsPublic);
                    fail("null " + fieldName);
                } catch (IllegalArgumentException e) {
                    Assert.assertEquals("null " + fieldName, String.format("'%s' field is not nullable", fieldName), e.getMessage());
                }

                final Field field = fields[i];
                if (field.getType().isPrimitive())
                    field.setInt(msg, (Integer) ENUM_VALUES_GOOD[i]);
                else
                    field.set(msg, ENUM_VALUES_GOOD[i]);
            }

            //  bad
            for (int i = 0; i < fields.length; i++) {
                final String fieldName = fields[i].getName();
                final Field field = fields[i];
                if (field.getType().isPrimitive())
                    field.setInt(msg, (Integer) ENUM_VALUES_BAD[i]);
                else
                    field.set(msg, ENUM_VALUES_BAD[i]);


                try {
                    boundEncode(msg, cdMsgClassEnumOtherBindingsPublic);
                    fail("bad " + fieldName);
                } catch (IllegalArgumentException e) {
                    // compiled and interpreted codecs throws different exceptions
                    final String expected = fieldName + " == " + ENUM_VALUES_BAD[i];
                    final String expected2 = fields[i] + " == " + ENUM_VALUES_BAD[i];
                    if (!expected.equals(e.getMessage()))
                        Assert.assertEquals("bad " + fieldName, expected2, e.getMessage());
                }

                if (field.getType().isPrimitive())
                    field.setInt(msg, (Integer) ENUM_VALUES_GOOD[i]);
                else
                    field.set(msg, ENUM_VALUES_GOOD[i]);
            }
        }

        // private
        {
            final SimpleTypeLoader typeLoader = new SimpleTypeLoader(cdMsgClassEnumOtherBindingsPublic.getName(), MsgClassEnumOtherBindingsPrivate.class);
            final Field[] fields = MsgClassEnumOtherBindingsPrivate.class.getDeclaredFields();
            final MsgClassEnumOtherBindingsPrivate msg = new MsgClassEnumOtherBindingsPrivate();
            msg.initNulls();

            // null-value
            for (int i = 0; i < fields.length; i++) {
                final String fieldName = fields[i].getName();
                try {
                    boundEncode(msg, typeLoader, cdMsgClassEnumOtherBindingsPublic);
                    fail("null " + fieldName);
                } catch (IllegalArgumentException e) {
                    Assert.assertEquals("null " + fieldName, String.format("'%s' field is not nullable", fieldName), e.getMessage());
                }

                final Field field = fields[i];
                if (field.getType().isPrimitive())
                    field.setInt(msg, (Integer) ENUM_VALUES_GOOD[i]);
                else
                    field.set(msg, ENUM_VALUES_GOOD[i]);
            }

            //  bad
            for (int i = 0; i < fields.length; i++) {
                final String fieldName = fields[i].getName();
                final Field field = fields[i];
                if (field.getType().isPrimitive())
                    field.setInt(msg, (Integer) ENUM_VALUES_BAD[i]);
                else
                    field.set(msg, ENUM_VALUES_BAD[i]);


                try {
                    boundEncode(msg, typeLoader, cdMsgClassEnumOtherBindingsPublic);
                    fail("bad " + fieldName);
                } catch (IllegalArgumentException e) {
                    // compiled and interpreted codecs throws different exceptions
                    final String expected = fieldName + " == " + ENUM_VALUES_BAD[i];
                    final String expected2 = fields[i] + " == " + ENUM_VALUES_BAD[i];
                    if (!expected.equals(e.getMessage()))
                        Assert.assertEquals("bad " + fieldName, expected2, e.getMessage());
                }

                if (field.getType().isPrimitive())
                    field.setInt(msg, (Integer) ENUM_VALUES_GOOD[i]);
                else
                    field.set(msg, ENUM_VALUES_GOOD[i]);
            }
        }
    }

    public enum MyEnumAdded {
        RED,
        YELLOW,
        GREEN,
        BLACK
    }

    public enum MyEnumAddedShuffle {
        YELLOW,
        BLACK,
        RED,
        GREEN,
    }

    public static class MsgClassEnumAddedPublic {
        public MyEnumAdded e1;
        private MyEnumAdded e2;
        // to pass "doesn't contain NOT NULL field" validation
        public int f3;
        public int f4;

        @Override
        public String toString() {
            return e1 + "," + e2;
        }

        public MyEnumAdded getE2 () {
            return e2;
        }

        public void setE2 (MyEnumAdded e2) {
            this.e2 = e2;
        }
    }

    public static class MsgClassEnumAddedShufflePublic {
        public MyEnumAddedShuffle e1;
        private MyEnumAddedShuffle e2;
        // to pass "doesn't contain NOT NULL field" validation
        public int f3;
        public int f4;

        @Override
        public String toString() {
            return e1 + "," + e2;
        }

        public MyEnumAddedShuffle getE2 () {
            return e2;
        }

        public void setE2 (MyEnumAddedShuffle e2) {
            this.e2 = e2;
        }
    }

    // check enum values absent in schema on encoding
    private void testEnumViolationBound2() throws Exception {
        {
            final SimpleTypeLoader typeLoader = new SimpleTypeLoader(cdMsgClassEnumOtherBindingsPublic.getName(), MsgClassEnumAddedPublic.class);
            final MsgClassEnumAddedPublic msg = new MsgClassEnumAddedPublic();
            msg.e1 = MyEnumAdded.RED;
            msg.e2 = MyEnumAdded.YELLOW;
            testRcdBound(cdMsgClassEnumOtherBindingsPublic, typeLoader, msg, null);

            // value in enum, but not in schema
            msg.e1 = MyEnumAdded.BLACK;
            try {
                boundEncode(msg, typeLoader, cdMsgClassEnumOtherBindingsPublic);
                fail("bad value e1 == " + msg.e1);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("bad value e1 ", "value is absent in schema: e1 == " + msg.e1, e.getMessage());
            }

            msg.e1 = MyEnumAdded.GREEN;
            msg.e2 = MyEnumAdded.BLACK;
            try {
                boundEncode(msg, typeLoader, cdMsgClassEnumOtherBindingsPublic);
                fail("bad value e2 == " + msg.e1);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("bad value e2 ", "value is absent in schema: e2 == " + msg.e2, e.getMessage());
            }
        }

        // Shuffled enum
        {
            final SimpleTypeLoader typeLoader = new SimpleTypeLoader(cdMsgClassEnumOtherBindingsPublic.getName(), MsgClassEnumAddedShufflePublic.class);
            final MsgClassEnumAddedShufflePublic msg = new MsgClassEnumAddedShufflePublic();
            msg.e1 = MyEnumAddedShuffle.RED;
            msg.e2 = MyEnumAddedShuffle.YELLOW;
            testRcdBound(cdMsgClassEnumOtherBindingsPublic, typeLoader, msg, null);

            // value in enum, but not in schema
            msg.e1 = MyEnumAddedShuffle.BLACK;
            try {
                boundEncode(msg, typeLoader, cdMsgClassEnumOtherBindingsPublic);
                fail("bad value e1 == " + msg.e1);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("bad value e1 ", "value is absent in schema: e1 == " + msg.e1, e.getMessage());
            }

            msg.e1 = MyEnumAddedShuffle.GREEN;
            msg.e2 = MyEnumAddedShuffle.BLACK;
            try {
                boundEncode(msg, typeLoader, cdMsgClassEnumOtherBindingsPublic);
                fail("bad value e2 == " + msg.e1);
            } catch (IllegalArgumentException e) {
                Assert.assertEquals("bad value e2 ", "value is absent in schema: e2 == " + msg.e2, e.getMessage());
            }
        }
    }




}