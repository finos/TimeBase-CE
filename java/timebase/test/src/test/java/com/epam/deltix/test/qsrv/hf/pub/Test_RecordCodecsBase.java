package com.epam.deltix.test.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.codec.ArrayTypeUtil;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.List;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import org.junit.Assert;

/**
 * User: BazylevD
 * Date: Dec 17, 2009
 */
public abstract class Test_RecordCodecsBase {
    protected CodecFactory factory = null;

    protected static final TypeLoader TYPE_LOADER = TypeLoaderImpl.DEFAULT_INSTANCE;
    protected static final double EPSILON = 0.00001;
    private final CodecFactory compiledFactory = CodecFactory.newCompiledCachingFactory();
    private final CodecFactory interpretedFactory = CodecFactory.newInterpretingCachingFactory();

    protected void setUpComp() {
        factory = compiledFactory;
    }

    protected void setUpIntp() {
        factory = interpretedFactory;
    }

    protected MemoryDataOutput boundEncode(Object inMsg, RecordClassDescriptor cd) {
        return boundEncode(inMsg, TYPE_LOADER, cd);
    }

    protected MemoryDataOutput boundEncode(Object inMsg, TypeLoader typeLoader, RecordClassDescriptor cd) {
        MemoryDataOutput out = new MemoryDataOutput();
        boundEncode(inMsg, typeLoader, cd, out);
        return out;
    }

    protected void boundEncode(Object inMsg, TypeLoader typeLoader, RecordClassDescriptor cd, MemoryDataOutput out) {
        FixedBoundEncoder benc = factory.createFixedBoundEncoder(typeLoader, cd);
        benc.encode(inMsg, out);
    }

    protected Object boundDecode(Object inMsg, RecordClassDescriptor cd, MemoryDataInput in) {
        return boundDecode(inMsg, TYPE_LOADER, cd, in);
    }

    protected Object boundDecode(Object inMsg, TypeLoader typeLoader, RecordClassDescriptor cd, MemoryDataInput in) {
        if (inMsg == null) {
            BoundDecoder bdec = factory.createFixedBoundDecoder(typeLoader, cd);
            Object msg = bdec.decode(in);
            bdec.setStaticFields(msg);
            return msg;
        } else {
            FixedExternalDecoder bdec = factory.createFixedExternalDecoder(typeLoader, cd);
            bdec.setStaticFields(inMsg);
            bdec.decode(in, inMsg);
            return inMsg;
        }
    }

    protected static void testRcdBound(Object o, FixedBoundEncoder encoder, BoundDecoder decoder) {
        if (o != null) {
            MemoryDataOutput out0 = new MemoryDataOutput();
            encoder.encode(o, out0);
            MemoryDataInput in0 = new MemoryDataInput(out0);
            in0.reset(out0.getPosition());
            Object o2 = decoder.decode(in0);
            Assert.assertEquals(o.toString(), o2.toString());
        }
    }
    
    protected void testRcdBound(String text, Object o, RecordClassDescriptor cd) {
        if (o != null) {
            MemoryDataOutput out0 = boundEncode(o, cd);
            MemoryDataInput in0 = new MemoryDataInput(out0);
            in0.reset(out0.getPosition());
            Object o2 = boundDecode(null, cd, in0);
            Assert.assertEquals(text, o, o2);
        }
    }

    // TODO: refactor all testRcdBound overrides
    protected void testRcdBound(RecordClassDescriptor rcd, final Object inMsg, final Object outMsg) {
        testRcdBound(rcd, TYPE_LOADER, inMsg, outMsg);
    }

    protected void testRcdBound(RecordClassDescriptor rcd, TypeLoader typeLoader, final Object inMsg, final Object outMsg) {
        MemoryDataOutput out = boundEncode(inMsg, typeLoader, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        in.reset(out.getPosition());
        Object msg = boundDecode(outMsg, typeLoader, rcd, in);
        Assert.assertEquals(inMsg.toString(), msg.toString());
    }

    public static RecordClassDescriptor getRCD(Class<?> clazz) throws Introspector.IntrospectionException {
        return (RecordClassDescriptor) Introspector.introspectSingleClass(clazz);
    }

    protected void testRcdUnbound(List<Object> values, RecordClassDescriptor rcd) {
        MemoryDataOutput out = unboundEncode(values, rcd);
        MemoryDataInput in = new MemoryDataInput(out);
        final List<Object> values2  = unboundDecode(in, rcd);
        assertValuesEquals(values, values2, rcd);
    }

    protected MemoryDataOutput unboundEncode(List<Object> values, RecordClassDescriptor rcd) {
        return unboundEncode(values, factory.createFixedUnboundEncoder(rcd));
    }

    static MemoryDataOutput unboundEncode(List<Object> values, FixedUnboundEncoder uenc) {
        MemoryDataOutput out = new MemoryDataOutput();
        uenc.beginWrite(out);

        int idx = 0;
        while (uenc.nextField()) {
            final Object value = values.get(idx++);
            if (value == null)
                uenc.writeNull();
            else
                writeField(value, uenc);
        }

        uenc.endWrite();
        return (out);
    }

    private static void writeField(Object value, UnboundEncoder uenc) {
        final DataType type = uenc.getField().getType();
        writeField(value, type, uenc);
    }

    private static void writeField(Object value, DataType type, WritableValue uenc) {
        if (value == null)
            uenc.writeNull();
        else if (value instanceof CharSequence)
            uenc.writeString((CharSequence) value);
        else if (type instanceof IntegerDataType)
            uenc.writeLong(((Number) value).longValue());
        else if (type instanceof FloatDataType) {
            if (type.getEncoding().equals(FloatDataType.ENCODING_FIXED_FLOAT))
                uenc.writeFloat(((Number) value).floatValue());
            else
                uenc.writeDouble(((Number) value).doubleValue());
        }
        else if (type instanceof EnumDataType || type instanceof VarcharDataType || type instanceof CharDataType) {
            if (value instanceof Integer)
                uenc.writeLong((Integer) value);
            else if (value instanceof Character)
                uenc.writeChar((Character) value);
            else if (type instanceof EnumDataType && value.getClass().isEnum())
                uenc.writeLong(((Enum) value).ordinal());
            else
                throw new IllegalArgumentException(value.getClass().getName());
        }
        else if (type instanceof BooleanDataType) {
            if (value instanceof Boolean)
                uenc.writeBoolean((Boolean) value);
            else
                uenc.writeBoolean((Byte) value == BooleanDataType.TRUE);
        }
        else if (type instanceof DateTimeDataType)
            uenc.writeLong((Long) value);
        else if (type instanceof TimeOfDayDataType)
            uenc.writeInt((Integer) value);
        else if (type instanceof ArrayDataType)
            writeArray(value, (ArrayDataType) type, uenc);
        else if (type instanceof ClassDataType) {

            ArrayList values = (ArrayList) value;
            ClassDataType cType = (ClassDataType) type;

            RecordClassDescriptor rcd = null;
            int index = 0;

            if (values.get(0) instanceof RecordClassDescriptor) {
                rcd = (RecordClassDescriptor) values.get(0);
                index = 1;
            } else if (cType.isFixed()) {
                rcd = cType.getFixedDescriptor();
            }

            assert rcd != null;

            writeObject(values, uenc.getFieldEncoder(rcd), index);
        }
        else if (type instanceof BinaryDataType) {
            byte[] bin = (byte[]) value;
            uenc.writeBinary(bin, 0, bin.length);
        } else
            throw new RuntimeException("Unrecognized dataType: " + type);
    }

    @SuppressWarnings("unchecked")
    private static void writeArray(Object value, ArrayDataType type, WritableValue uenc) {
        final AbstractList a = (AbstractList) value;
        final int len = a.size();
        uenc.setArrayLength(len);
        final DataType underlineType = type.getElementDataType();

        for (int i = 0; i < len; i++) {
            final Object v = a.get(i);
            final WritableValue rv = uenc.nextWritableElement();
            if (v != null || type.getElementDataType().isNullable())
                writeField(v, underlineType, rv);
        }
    }

    protected static void writeObject(ArrayList values, UnboundEncoder encoder, int index) {
        final int len = values.size();

        for (int i = index; i < len && encoder.nextField(); i++) {
            Object v = values.get(i);
            if (v != null)
                writeField(v, encoder);
        }
    }

    protected List<Object> unboundDecode(MemoryDataInput in, RecordClassDescriptor rcd) {
        return unboundDecode(in, factory.createFixedUnboundDecoder(rcd));
    }

    protected static List<Object> unboundDecode(MemoryDataInput in, UnboundDecoder udec) {
        final List<Object> values = new ArrayList<Object>();
        udec.beginRead(in);
        while (udec.nextField()) {
            try {
                values.add(readField(udec));
            } catch (NullValueException e) {
                values.add(null);
            }
        }

        return (values);
    }

    protected static Object readField(UnboundDecoder udec) {
        final DataType type = udec.getField().getType();
        return readField(type, udec);
    }

    protected static Object readField(DataType type, ReadableValue udec) {
        try {
            if (type instanceof IntegerDataType) {
                if (((IntegerDataType) type).getNativeTypeSize() >= 6)
                    return udec.getLong();
                else
                    return udec.getInt();
            } else if (type instanceof FloatDataType)
                if (((FloatDataType) type).isFloat())
                    return udec.getFloat();
                else
                    return udec.getDouble();
            else if (type instanceof CharDataType)
                return udec.getChar();
            else if (type instanceof EnumDataType || type instanceof VarcharDataType)
                return udec.getString();
            else if (type instanceof BooleanDataType)
                return udec.getBoolean();
            else if (type instanceof DateTimeDataType)
                return udec.getLong();
            else if (type instanceof TimeOfDayDataType)
                return udec.getInt();
            else if (type instanceof ArrayDataType)
                return readArray((ArrayDataType) type, udec);
            else if (type instanceof ClassDataType)
                return readObject(udec, !((ClassDataType) type).isFixed());
            else if (type instanceof BinaryDataType) {
                try {
                    final int size = udec.getBinaryLength();
                    final byte[] bin = new byte[size];
                    udec.getBinary(0, size, bin, 0);
                    return bin;
                } catch (NullValueException e) {
                    return null;
                }
            } else
                throw new RuntimeException("Unrecognized dataType: " + type);
        } catch (NullValueException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Object readArray(ArrayDataType type, ReadableValue udec) throws NullValueException {
        final int len = udec.getArrayLength();
        final DataType underlineType = type.getElementDataType();
        final boolean isNullableBool = (underlineType instanceof BooleanDataType) && underlineType.isNullable();
        final boolean isByte = (underlineType instanceof IntegerDataType) && (((IntegerDataType) underlineType).getNativeTypeSize() == 1);
        final boolean isShort = (underlineType instanceof IntegerDataType) && (((IntegerDataType) underlineType).getNativeTypeSize() == 2);
        try {
            //final AbstractList a = (AbstractList) RecordLayout.getNativeType(type).newInstance();
            //setArrayLength(a, len);
            RecordLayout.getNativeType(type).newInstance();
            final AbstractList a = new ArrayList<Object>();
            for (int i = 0; i < len; i++) {
                a.add(null);
            }

            for (int i = 0; i < len; i++) {
                Object value;
                try {
                    final ReadableValue rv = udec.nextReadableElement();
                    value = readField(underlineType, rv);
                } catch (NullValueException e) {
                    value = null;
                }

                if (value == null && !isNullableBool) {
                    if (a instanceof ArrayList)
                        a.set(i, null);
                    else
                        return readArrayUncasted(i, a, underlineType, udec);
                } else {
                    if (isNullableBool) {
                        Boolean b = (Boolean) value;
                        a.set(i, (byte) (b == null ? -1 : b ? 1 : 0));
                    } else if (isByte) {
                        a.set(i, ((Integer) value).byteValue());
                    } else if (isShort) {
                        a.set(i, ((Integer) value).shortValue());
                    } else
                        a.set(i, value);
                }
            }
            return a;

        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readArrayUncasted(int idx, final AbstractList a, DataType underlineType , ReadableValue udec) {
        @SuppressWarnings("unchecked")
        final ArrayList<Object> r = new ArrayList<Object>(a);
        r.set(idx, null);
        for (int i = idx + 1; i < a.size(); i++) {
            try {
                final ReadableValue rv = udec.nextReadableElement();
                r.set(i, readField(underlineType, rv));
            } catch (NullValueException e) {
                r.set(i, null);
            }
        }

        return r;
    }

    protected static void setArrayLength(AbstractList a, int length) {
        try {
            Method m = a.getClass().getMethod("setSize", int.class);
            m.invoke(a, length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static void setArrayElement(DataType underlineType, AbstractList a, int idx, ReadableValue rv) {
        final boolean isNullableBool = (underlineType instanceof BooleanDataType) && underlineType.isNullable();
        final boolean isByte = (underlineType instanceof IntegerDataType) && (((IntegerDataType) underlineType).getNativeTypeSize() == 1);
        final boolean isShort = (underlineType instanceof IntegerDataType) && (((IntegerDataType) underlineType).getNativeTypeSize() == 2);

        Object value = readField(underlineType, rv);
        // special handling for null-value case
        if(value == null &&  !isNullableBool && !(a instanceof ArrayList)) {
            value = MdUtil.getNullValue(underlineType, ArrayTypeUtil.getUnderline(a.getClass()));
        }

        if (isNullableBool) {
            Boolean b = (Boolean) value;
            a.set(idx, (byte) (b == null ? -1 : b ? 1 : 0));
        } else if (isByte) {
            a.set(idx, ((Integer) value).byteValue());
        } else if (isShort) {
            a.set(idx, ((Integer) value).shortValue());
        } else
            a.set(idx, value);
    }

    protected static Object readObject(ReadableValue udec, boolean isPolymorphic) throws NullValueException {
        try {
            final UnboundDecoder decoder = udec.getFieldDecoder();
            final ArrayList<Object> values = new ArrayList<>();
            if (isPolymorphic)
                values.add(decoder.getClassInfo().getDescriptor());

            while (decoder.nextField()) {
                try {
                    values.add(readField(decoder));
                } catch (NullValueException e) {
                    values.add(null);
                }
            }
            return values;
        } catch (NullValueException e) {
            return null;
        }
    }

    protected static void assertValuesEquals(List<Object> expected, List<Object> actual, RecordClassDescriptor rcd) {
        assertEquals("sizes of lists are different", expected.size(), actual.size());

        final ArrayList<RecordClassDescriptor> plainRcdList = new ArrayList<RecordClassDescriptor>();
        do {
            plainRcdList.add(rcd);
        } while ((rcd = rcd.getParent()) != null);
        final ArrayList<DataField> fields = new ArrayList<DataField>();
        for (int i = plainRcdList.size() - 1; i >= 0; i--) {
            final DataField[] rcdFields = plainRcdList.get(i).getFields();
            for (DataField rcdField : rcdFields) {
                if (rcdField instanceof NonStaticDataField)
                    fields.add(rcdField);
            }
        }

        for (int i = 0; i < expected.size(); i++) {
            final Object expectedBoxed = expected.get(i);
            final DataType dataType = fields.get(i).getType();
            assertValuesEquals("element[" + i + "]: ", expectedBoxed, actual.get(i), dataType);
        }
    }

    public static void assertArraysEquals(String msg, List<Object> expected, List<Object> actual, DataType dataType) {
        assertEquals(msg + "sizes of lists are different", expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            final Object expectedBoxed = expected.get(i);
            assertValuesEquals(msg + " element[" + i + "]: ", expectedBoxed, actual.get(i), dataType);
        }
    }

    protected static void assertValuesEquals(String msg, Object expectedBoxed, Object actualBoxed, DataType dataType) {
        if(expectedBoxed == actualBoxed)
            return;

        if (dataType instanceof FloatDataType) {
            double expectedValue = expectedBoxed != null ? ((Number) expectedBoxed).doubleValue() : Double.NaN;
            if (Double.isNaN(expectedValue)) {
                assertNull(msg, actualBoxed);
            } else {
                Number actualValue = (Number) actualBoxed;
                assertNotNull(msg, actualValue);
                assertEquals(msg, expectedValue, actualValue.doubleValue(), EPSILON);
            }
        } else if (dataType instanceof IntegerDataType &&
                (expectedBoxed != null || actualBoxed != null)) {
            final long nullValue = ((IntegerDataType) dataType).getNullValue();
            if (expectedBoxed == null) {
                long actualValue = ((Number) actualBoxed).longValue();
                assertEquals(msg, null, actualValue == nullValue ? null : actualBoxed);
            } else if (actualBoxed == null) {
                long expectedValue = ((Number) expectedBoxed).longValue();
                assertEquals(msg, expectedValue == nullValue ? null : expectedBoxed, null);
            } else {
                final long expectedValue = ((Number) expectedBoxed).longValue();
                final long actualValue = ((Number) actualBoxed).longValue();
                assertEquals(msg, expectedValue, actualValue);
            }
        } else if (dataType instanceof BooleanDataType || dataType instanceof CharDataType) {
            if (expectedBoxed == null || actualBoxed == null) {
                final Class<?> clazz = (dataType instanceof BooleanDataType) ? boolean.class : char.class;
                final Object nullValue = MdUtil.getNullValue(dataType, clazz);
                if ((expectedBoxed == null && !actualBoxed.equals(nullValue)) ||
                        (actualBoxed == null && !expectedBoxed.equals(nullValue)))
                    assertEquals(msg, expectedBoxed, actualBoxed);
            } else if (dataType instanceof BooleanDataType &&
                    expectedBoxed instanceof Boolean && actualBoxed instanceof Byte)
                assertEquals(msg, expectedBoxed, (Byte) actualBoxed == BooleanDataType.TRUE);
            else
                assertEquals(msg, expectedBoxed, actualBoxed);
        } else if (dataType instanceof EnumDataType) {
            if (expectedBoxed != null && expectedBoxed instanceof Integer &&
                    actualBoxed != null && actualBoxed instanceof String) {
                final EnumClassDescriptor ecd = ((EnumDataType) dataType).descriptor;
                long lvActual;
                if (!ecd.isBitmask() || actualBoxed.toString().indexOf('|') == -1)
                    lvActual = ecd.stringToLong((CharSequence) actualBoxed);
                else {
                    lvActual = 0;
                    final String[] ss = actualBoxed.toString().split("\\|");
                    for (String s : ss) {
                        lvActual |= ecd.stringToLong(s);
                    }
                }
                assertEquals(msg, (long) (Integer) expectedBoxed, lvActual);
            } else
                assertEquals(msg, expectedBoxed, actualBoxed);
        } else if (dataType instanceof ArrayDataType) {
            if (expectedBoxed != null && actualBoxed != null && !expectedBoxed.equals(actualBoxed)) {
                @SuppressWarnings("unchecked")
                final List<Object> el = (List<Object>)expectedBoxed;
                @SuppressWarnings("unchecked")
                final List<Object> al = (List<Object>)actualBoxed;
                assertArraysEquals(msg, el, al, ((ArrayDataType) dataType).getElementDataType());
            } else
                assertEquals(msg, expectedBoxed, actualBoxed);
        } else if (dataType instanceof BinaryDataType)
            Assert.assertArrayEquals((byte[]) expectedBoxed, (byte[]) actualBoxed);
        else if (dataType instanceof ClassDataType) {
            if (expectedBoxed != null && actualBoxed != null && !expectedBoxed.equals(actualBoxed)) {
                @SuppressWarnings("unchecked")
                final Object[] el = ((List<Object>) expectedBoxed).toArray();
                @SuppressWarnings("unchecked")
                final Object[] al = ((List<Object>) actualBoxed).toArray();
                Assert.assertArrayEquals(msg, el, al);
            } else
                assertEquals(msg, expectedBoxed, actualBoxed);
        } else
            assertEquals(msg, expectedBoxed, actualBoxed);
    }
}
