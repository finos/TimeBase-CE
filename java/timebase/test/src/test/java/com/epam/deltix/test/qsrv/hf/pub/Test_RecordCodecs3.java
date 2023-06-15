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

import com.epam.deltix.qsrv.hf.pub.TypeLoader;
import com.epam.deltix.qsrv.hf.pub.codec.MetaDataBindException;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
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
 * Date: Mar 14, 2011
 *
 * @author BazylevD
 */
@Category(JUnitCategories.TickDBCodecs.class)
public class Test_RecordCodecs3 extends Test_RecordCodecsBase {

    @Test @Ignore("#9674")
    public void testCustomizedComp() throws Exception {
        setUpComp();
        testCustomized();
    }

    @Test @Ignore("#9674")
    public void testCustomizedIntp() throws Exception {
        setUpIntp();
        testCustomized();
    }

    public static class MyClass extends InstrumentMessage {
        public int i1;
        public long l2;

        @Override
        public String toString() {
            return i1 + "," + l2;
        }
    }

    public static class MyClassEx extends MyClass {
        public String s3;

        @Override
        public String toString() {
            return super.toString() + "," + s3;
        }
    }

    private void testCustomized() throws Exception {
        final RecordClassDescriptor rcd = (RecordClassDescriptor)Introspector.introspectSingleClass(MyClass.class);
        final RecordClassDescriptor rcdEx = new RecordClassDescriptor(
                "MyClassEx",
                null,
                false,
                rcd,
                new NonStaticDataField("s3", null, new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, false))
        );


        final MyClass inMsg = new MyClass();
        inMsg.i1 = 5;
        inMsg.l2 = 13241342343L;
        MemoryDataOutput out = boundEncode(inMsg, rcd);

        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        Object outMsg = boundDecode(null, rcd, in);
        Assert.assertEquals(inMsg.toString(), outMsg.toString());

        final MyClassEx inMsgEx = new MyClassEx();
        inMsgEx.i1 = 5;
        inMsgEx.l2 = 13241342343L;
        inMsgEx.s3 = "Hi Nicolia!";


        out = boundEncode(inMsg, new TypeLoader() {
            @Override
            public Class<?> load(ClassDescriptor cd) throws ClassNotFoundException {
                if (cd.getName().equals("MyClassEx"))
                    return MyClass.class;
                else
                    return TYPE_LOADER.load(cd);
            }
        },
                rcdEx
        );

        in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        Object outMsgEx = boundDecode(null, rcdEx, in);
        Assert.assertEquals(inMsgEx.toString(), outMsgEx.toString());
    }

    @Test
    public void testCharUnboundComp() throws Exception {
        setUpComp();
        testCharUnbound();
    }

    @Test
    public void testCharUnboundIntp() throws Exception {
        setUpIntp();
        testCharUnbound();
    }

    public static class MyClassChar extends InstrumentMessage {
        public int i1;
        public char ch2;
        public double d3;

        @Override
        public String toString() {
            return i1 + "," + ch2 + "," + d3;
        }
    }

    private void testCharUnbound() throws Exception {
        final RecordClassDescriptor rcd = (RecordClassDescriptor)Introspector.introspectSingleClass(MyClassChar.class);
        final UnboundDecoder udec = factory.createFixedUnboundDecoder(rcd);

        final List<Object> values = new ArrayList<Object>(3);

        values.add('X');
        values.add(134.345);
        values.add(25);

        MemoryDataOutput out = unboundEncode(values, rcd);
        final MemoryDataInput in = new MemoryDataInput(out);
        List<Object> values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        final MyClassChar msg = new MyClassChar();
        msg.i1 = 25;
        msg.ch2 = 'X';
        msg.d3 = 134.345;
        out = boundEncode(msg, rcd);
        in.setBytes(out);
        values2 = unboundDecode(in, udec);
        assertValuesEquals(values, values2, rcd);

        // check all supported getters
        in.setBytes(out);
        udec.beginRead(in);
        udec.nextField();

        assertEquals ("getChar", 'X', udec.getChar());
        assertEquals ("getInt", 'X', (char)udec.getInt());
        assertEquals ("getLong", 'X', (char)udec.getLong());
        assertEquals ("getString", "X", udec.getString());
        // check tailing double
        udec.nextField();
        assertEquals ("getDouble", 134.345, udec.getDouble(), 0.0001);

        udec.nextField(); //
        assertEquals("getInt", 25, udec.getInt());
    }

    @Test
    public void testBoxedTypesComp() throws Exception {
        setUpComp();
        testBoxedTypes();
    }

    @Test
    public void testBoxedTypesIntp() throws Exception {
        setUpIntp();
        testBoxedTypes();
    }

    public static class MyClassBoxed extends InstrumentMessage {
        public Integer i1;
        public Character ch2;
        public Double d3;

        @Override
        public String toString() {
            return i1 + "," + ch2 + "," + d3;
        }
    }

    public static class MyClassBoxedPrivate extends InstrumentMessage {
        private Integer i1;
        private Character ch2;
        private Double d3;

        public Character getCh2 () {
            return ch2;
        }

        public void setCh2 (Character ch2) {
            this.ch2 = ch2;
        }

        public Double getD3 () {
            return d3;
        }

        public void setD3 (Double d3) {
            this.d3 = d3;
        }

        public Integer getI1 () {
            return i1;
        }

        public void setI1 (Integer i1) {
            this.i1 = i1;
        }

        @Override
        public String toString() {
            return i1 + "," + ch2 + "," + d3;
        }
    }

    private void testBoxedTypes() throws Exception {
        final RecordClassDescriptor rcd = new RecordClassDescriptor(
                MyClassBoxed.class.getName(),
                null,
                false,
                null,
                new NonStaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                new NonStaticDataField("ch2", null, new CharDataType(true)),
                new NonStaticDataField("d3", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
        );


        final MyClassBoxed inMsg = new MyClassBoxed();
        inMsg.i1 = 5;
        inMsg.ch2 = 'Z';
        inMsg.d3 = 123.34;
        final MemoryDataOutput out;
        try {
            boundEncode(inMsg, rcd);
        } catch (MetaDataBindException e) {
            final Throwable inner = e.getCause();
            if(inner == null || !inner.toString().equals("java.lang.UnsupportedOperationException: Boxed types are not supported due to performance degradation: java.lang.Integer"))
                throw e;
        }
    }

    public static class MsgByte2Bool {
        public byte b1;
        @SchemaType(
                isNullable = false
        )
        public byte b2;

        public String toString() {
            return Integer.toHexString(b1) + "," + Integer.toHexString(b2);
        }
    }

    private static final RecordClassDescriptor cdMsgClassByte2Bool =
        new RecordClassDescriptor(
            MsgByte2Bool.class.getName(),
            null,
            false,
            null,
            new NonStaticDataField("b1", null, new BooleanDataType(true)),
            new NonStaticDataField("b2", null, new BooleanDataType(false))
        );

    @Test
    public void testByte2BoolComp() throws Exception {
        setUpComp();
        testByte2Bool();
    }

    @Test
    public void testByte2BoolIntp() throws Exception {
        setUpIntp();
        testByte2Bool();
    }

    private void testByte2Bool() throws Exception {

        MsgByte2Bool msg = new MsgByte2Bool();
        msg.b1 = 0;
        msg.b2 = 0;

        MemoryDataOutput out = boundEncode(msg, cdMsgClassByte2Bool);
        MemoryDataInput in = new MemoryDataInput(out);
        final MsgByte2Bool msg2 = new MsgByte2Bool();
        boundDecode(msg2, cdMsgClassByte2Bool, in);

        assertEquals(msg.toString(), msg2.toString());

        msg.b1 = 1;
        msg.b2 = 1;
        out = boundEncode(msg, cdMsgClassByte2Bool);
        in = new MemoryDataInput(out);
        boundDecode(msg2, cdMsgClassByte2Bool, in);
        assertEquals(msg.toString(), msg2.toString());

        msg.b1 = BooleanDataType.NULL;
        msg.b2 = 1;
        out = boundEncode(msg, cdMsgClassByte2Bool);
        in = new MemoryDataInput(out);
        boundDecode(msg2, cdMsgClassByte2Bool, in);
        assertEquals(msg.toString(), msg2.toString());
    }

    @Test
    public void testBoxedTypesPrivateComp () throws Exception {
        setUpComp();
        testBoxedTypesPrivate();
    }


    @Test
    public void testBoxedTypesPrivateIntpo () throws Exception {
        setUpIntp();
        testBoxedTypesPrivate();
    }

    private void testBoxedTypesPrivate() throws Exception {
        final RecordClassDescriptor rcd = new RecordClassDescriptor(
                MyClassBoxed.class.getName(),
                null,
                false,
                null,
                new NonStaticDataField("i1", null, new IntegerDataType(IntegerDataType.ENCODING_INT32, true)),
                new NonStaticDataField("ch2", null, new CharDataType(true)),
                new NonStaticDataField("d3", null, new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, true))
        );


        final MyClassBoxedPrivate inMsg = new MyClassBoxedPrivate();
        inMsg.i1 = 5;
        inMsg.ch2 = 'Z';
        inMsg.d3 = 123.34;
        final MemoryDataOutput out;
        try {
            boundEncode(inMsg, rcd);
        } catch (MetaDataBindException e) {
            final Throwable inner = e.getCause();
            if(inner == null || !inner.toString().equals("java.lang.UnsupportedOperationException: Boxed types are not supported due to performance degradation: java.lang.Integer"))
                throw e;
        }
    }

}