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
package com.epam.deltix.qsrv.hf.pub.codec.intp;

import com.epam.deltix.qsrv.hf.pub.md.BooleanDataType;
import com.epam.deltix.qsrv.hf.pub.md.CharDataType;
import com.epam.deltix.qsrv.hf.pub.md.EnumDataType;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.util.collections.generated.*;

import java.util.AbstractList;

/**
 * Code-generated at 12-Jul-2012 21:53:20
 *
 * @author BazylevD
 */
class ArraysAdapters {

    public abstract static class ArrayAdapter<T> {
        protected final FieldDecoder fieldDecoder;
        protected final FieldEncoder fieldEncoder;

        public ArrayAdapter(FieldDecoder fieldDecoder) {
            this.fieldDecoder = fieldDecoder;
            this.fieldEncoder = null;
        }

        public ArrayAdapter(FieldEncoder fieldEncoder) {
            this.fieldDecoder = null;
            this.fieldEncoder = fieldEncoder;
        }

        abstract void decode(int idx, DecodingContext ctxt);

        abstract void encode(AbstractList<T> array, int idx, EncodingContext ctxt);

        abstract AbstractList<T> initArray(int size, DecodingContext ctxt);

        abstract void setNullElement(int idx);

        protected long getAndAssert(DecodingContext ctxt) {
            final long v = fieldDecoder.getLong(ctxt);
            assert fieldDecoder.isNullable || v != EnumDataType.NULL : getNotNullableMsg();
            return v;
        }

        protected String getNotNullableMsg() {
            return String.format("'%s' field array element is not nullable", fieldDecoder.fieldInfo.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ArrayAdapter<T> createDecodeAdapter(Class<?> cls, FieldDecoder fieldDecoder, boolean isEnum, boolean isUTF8) {
        if (isEnum) {
            if (ByteArrayList.class == cls)
                return (ArrayAdapter<T>) new ByteEnumArrayAdapter(fieldDecoder);
            else if (ShortArrayList.class == cls)
                return (ArrayAdapter<T>) new ShortEnumArrayAdapter(fieldDecoder);
            else if (IntegerArrayList.class == cls)
                return (ArrayAdapter<T>) new IntegerEnumArrayAdapter(fieldDecoder);
            else if (LongArrayList.class == cls)
                return (ArrayAdapter<T>) new LongEnumArrayAdapter(fieldDecoder);
            else if (ObjectArrayList.class == cls)
                return (ArrayAdapter<T>) new ObjectEnumArrayAdapter(fieldDecoder);
            else
                throw new IllegalArgumentException("unexpected class for ENUM type: " + cls.getName());
        } else {
            if (BooleanArrayList.class == cls)
                return (ArrayAdapter<T>) new BooleanArrayAdapter(fieldDecoder);
            else if (CharacterArrayList.class == cls)
                return (ArrayAdapter<T>) new CharacterArrayAdapter(fieldDecoder);
            else if (ByteArrayList.class == cls)
                return (ArrayAdapter<T>) new ByteArrayAdapter(fieldDecoder);
            else if (ShortArrayList.class == cls)
                return (ArrayAdapter<T>) new ShortArrayAdapter(fieldDecoder);
            else if (IntegerArrayList.class == cls)
                return (ArrayAdapter<T>) new IntegerArrayAdapter(fieldDecoder);
            else if (LongArrayList.class == cls)
                return (ArrayAdapter<T>) new LongArrayAdapter(fieldDecoder);
            else if (FloatArrayList.class == cls)
                return (ArrayAdapter<T>) new FloatArrayAdapter(fieldDecoder);
            else if (DoubleArrayList.class == cls)
                return (ArrayAdapter<T>) new DoubleArrayAdapter(fieldDecoder);
            else if (ObjectArrayList.class == cls)
                return (ArrayAdapter<T>) (isUTF8 ? new CharSequenceArrayAdapter(fieldDecoder): new ObjectArrayAdapter(fieldDecoder));
            else
                throw new IllegalArgumentException("unexpected class: " + cls.getName());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> ArrayAdapter<T> createEncodeAdapter(Class<?> cls, FieldEncoder fieldEncoder, boolean isEnum, boolean isUTF8) {
        if (isEnum) {
            if (ByteArrayList.class == cls)
                return (ArrayAdapter<T>) new ByteEnumArrayAdapter(fieldEncoder);
            else if (ShortArrayList.class == cls)
                return (ArrayAdapter<T>) new ShortEnumArrayAdapter(fieldEncoder);
            else if (IntegerArrayList.class == cls)
                return (ArrayAdapter<T>) new IntegerEnumArrayAdapter(fieldEncoder);
            else if (LongArrayList.class == cls)
                return (ArrayAdapter<T>) new LongEnumArrayAdapter(fieldEncoder);
            else if (ObjectArrayList.class == cls)
                return (ArrayAdapter<T>) new ObjectEnumArrayAdapter(fieldEncoder);
            else
                throw new IllegalArgumentException("unexpected class for ENUM type: " + cls.getName());
        } else {
            if (BooleanArrayList.class == cls)
                return (ArrayAdapter<T>) new BooleanArrayAdapter(fieldEncoder);
            else if (CharacterArrayList.class == cls)
                return (ArrayAdapter<T>) new CharacterArrayAdapter(fieldEncoder);
            else if (ByteArrayList.class == cls)
                return (ArrayAdapter<T>) new ByteArrayAdapter(fieldEncoder);
            else if (ShortArrayList.class == cls)
                return (ArrayAdapter<T>) new ShortArrayAdapter(fieldEncoder);
            else if (IntegerArrayList.class == cls)
                return (ArrayAdapter<T>) new IntegerArrayAdapter(fieldEncoder);
            else if (LongArrayList.class == cls)
                return (ArrayAdapter<T>) new LongArrayAdapter(fieldEncoder);
            else if (FloatArrayList.class == cls)
                return (ArrayAdapter<T>) new FloatArrayAdapter(fieldEncoder);
            else if (DoubleArrayList.class == cls)
                return (ArrayAdapter<T>) new DoubleArrayAdapter(fieldEncoder);
            else if (ObjectArrayList.class == cls)
                return (ArrayAdapter<T>) (isUTF8 ? new CharSequenceArrayAdapter(fieldEncoder): new ObjectArrayAdapter(fieldEncoder));
            else
                throw new IllegalArgumentException("unexpected class: " + cls.getName());
        }
    }

    private static class BooleanArrayAdapter extends ArrayAdapter<Boolean> {
        private BooleanArrayList a;

        BooleanArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        BooleanArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final byte r = fieldDecoder.getByte(ctxt);
            if (r == BooleanDataType.NULL)
                throw new IllegalStateException(getNotNullableMsg());
            else
                a.set(idx, r == BooleanDataType.TRUE);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Boolean> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (BooleanArrayList) array;

            fieldEncoder.setBoolean(a.getBoolean(idx), ctxt);
        }

        @Override
        AbstractList<Boolean> initArray(int size, DecodingContext ctx) {
            a = (BooleanArrayList) ctx.manager.use(BooleanArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class CharacterArrayAdapter extends ArrayAdapter<Character> {
        private CharacterArrayList a;

        private CharacterArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private CharacterArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final char v = fieldDecoder.getChar(ctxt);
            assert fieldDecoder.isNullable || v != CharDataType.NULL : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Character> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (CharacterArrayList) array;

            fieldEncoder.setChar(a.getCharacter(idx), ctxt);
        }

        @Override
        CharacterArrayList initArray(int size, DecodingContext ctx) {
            a = (CharacterArrayList) ctx.manager.use(CharacterArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class ByteArrayAdapter extends ArrayAdapter<Byte> {
        private ByteArrayList a;

        private ByteArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private ByteArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final byte v = fieldDecoder.getByte(ctxt);
            assert fieldDecoder.isNullable || v != IntegerDataType.INT8_NULL : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        AbstractList<Byte> initArray(int size, DecodingContext ctx) {
            a = (ByteArrayList) ctx.manager.use(ByteArrayList.class, size);
            a.setSize(size);
            return a;
        }

        @Override
        void encode(AbstractList<Byte> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (ByteArrayList) array;

            final byte v = a.getByte(idx);
            fieldEncoder.setByte(v, ctxt);
        }
    }

    private static class ShortArrayAdapter extends ArrayAdapter<Short> {
        private ShortArrayList a;

        private ShortArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private ShortArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final short v = fieldDecoder.getShort(ctxt);
            assert fieldDecoder.isNullable || v != IntegerDataType.INT16_NULL : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        AbstractList<Short> initArray(int size, DecodingContext ctx) {
            a = (ShortArrayList) ctx.manager.use(ShortArrayList.class, size);
            a.setSize(size);
            return a;
        }

        @Override
        void encode(AbstractList<Short> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (ShortArrayList) array;

            final short v = a.getShort(idx);
            fieldEncoder.setShort(v, ctxt);
        }

    }

    private static class IntegerArrayAdapter extends ArrayAdapter<Integer> {
        private IntegerArrayList a;

        private IntegerArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private IntegerArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final int v = fieldDecoder.getInt(ctxt);
            assert fieldDecoder.isNullable || v != IntegerDataType.INT32_NULL : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Integer> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (IntegerArrayList) array;

            final int v = a.getInteger(idx);
            fieldEncoder.setInt(v, ctxt);
        }

        @Override
        AbstractList<Integer> initArray(int size, DecodingContext ctx) {
            a = (IntegerArrayList) ctx.manager.use(IntegerArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class LongArrayAdapter extends ArrayAdapter<Long> {
        private LongArrayList a;

        private LongArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private LongArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final long v = fieldDecoder.getLong(ctxt);
            assert fieldDecoder.isNullable || v != IntegerDataType.INT64_NULL : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        AbstractList<Long> initArray(int size, DecodingContext ctx) {
            a = (LongArrayList) ctx.manager.use(LongArrayList.class, size);
            a.setSize(size);
            return a;
        }

        @Override
        void encode(AbstractList<Long> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (LongArrayList) array;

            final long v = a.getLong(idx);
            fieldEncoder.setLong(v, ctxt);
        }
    }

    private static class FloatArrayAdapter extends ArrayAdapter<Float> {
        private FloatArrayList a;

        private FloatArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private FloatArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final float v = fieldDecoder.getFloat(ctxt);
            assert fieldDecoder.isNullable || !Float.isNaN(v) : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Float> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (FloatArrayList) array;

            final float v = a.getFloat(idx);
            fieldEncoder.setFloat(v, ctxt);
        }

        @Override
        AbstractList<Float> initArray(int size, DecodingContext ctx) {
            a = (FloatArrayList) ctx.manager.use(FloatArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class DoubleArrayAdapter extends ArrayAdapter<Double> {
        private DoubleArrayList a;

        private DoubleArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private DoubleArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            final double v = fieldDecoder.getDouble(ctxt);
            assert fieldDecoder.isNullable || !Double.isNaN(v) : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        AbstractList<Double> initArray(int size, DecodingContext ctx) {
            a = (DoubleArrayList) ctx.manager.use(DoubleArrayList.class, size);
            a.setSize(size);
            return a;
        }

        @Override
        void encode(AbstractList<Double> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (DoubleArrayList) array;

            final double v = a.getDouble(idx);
            fieldEncoder.setDouble(v, ctxt);
        }
    }

    private static class ByteEnumArrayAdapter extends ArrayAdapter<Byte> {
        private ByteArrayList a;

        private ByteEnumArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private ByteEnumArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            a.set(idx, (byte) getAndAssert(ctxt));
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        AbstractList<Byte> initArray(int size, DecodingContext ctx) {
            a = (ByteArrayList) ctx.manager.use(ByteArrayList.class, size);
            a.setSize(size);
            return a;
        }

        @Override
        void encode(AbstractList<Byte> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (ByteArrayList) array;

            final byte v = a.getByte(idx);
            fieldEncoder.setLong(v, ctxt);
        }
    }

    private static class ShortEnumArrayAdapter extends ArrayAdapter<Short> {
        private ShortArrayList a;

        private ShortEnumArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private ShortEnumArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            a.set(idx, (short) getAndAssert(ctxt));
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Short> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (ShortArrayList) array;

            final short v = a.getShort(idx);
            fieldEncoder.setLong(v, ctxt);
        }

        @Override
        AbstractList<Short> initArray(int size, DecodingContext ctx) {
            a = (ShortArrayList) ctx.manager.use(ShortArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class IntegerEnumArrayAdapter extends ArrayAdapter<Integer> {
        private IntegerArrayList a;

        private IntegerEnumArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private IntegerEnumArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            a.set(idx, (int) getAndAssert(ctxt));
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Integer> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (IntegerArrayList) array;

            final int v = a.getInteger(idx);
            fieldEncoder.setLong(v, ctxt);
        }

        @Override
        AbstractList<Integer> initArray(int size, DecodingContext ctx) {
            a = (IntegerArrayList) ctx.manager.use(IntegerArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class ObjectEnumArrayAdapter extends ArrayAdapter<Object> {
        private ObjectArrayList a;

        private ObjectEnumArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private ObjectEnumArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            Object value = ((EnumFieldDecoder) fieldDecoder).getValue(getAndAssert(ctxt));
            a.set(idx, value);
        }

        @Override
        void encode(AbstractList<Object> array, int idx, EncodingContext ctxt) {
            assert array != null;

            ((EnumFieldEncoder)fieldEncoder).encode((Enum) array.get(idx), ctxt);
        }

        @Override
        @SuppressWarnings("unchecked")
        public AbstractList<Object> initArray(int size, DecodingContext ctxt) {
            a = (ObjectArrayList) ctxt.manager.use(ObjectArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class LongEnumArrayAdapter extends ArrayAdapter<Long> {
        private LongArrayList a;

        private LongEnumArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private LongEnumArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        public void decode(int idx, DecodingContext ctxt) {
            a.set(idx, getAndAssert(ctxt));
        }

        @Override
        void setNullElement(int idx) {
            fieldDecoder.setNull(a.getInternalBuffer(), idx);
        }

        @Override
        void encode(AbstractList<Long> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (LongArrayList) array;

            final long v = a.getLong(idx);
            fieldEncoder.setLong(v, ctxt);
        }

        @Override
        AbstractList<Long> initArray(int size, DecodingContext ctx) {
            a = (LongArrayList) ctx.manager.use(LongArrayList.class, size);
            a.setSize(size);
            return a;
        }
    }

    private static class CharSequenceArrayAdapter extends ArrayAdapter<CharSequence> {

        private ObjectArrayList<CharSequence> a;

        public CharSequenceArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        public CharSequenceArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        void decode(int idx, DecodingContext ctxt) {
            assert fieldDecoder != null;
            final CharSequence v = ((StringFieldDecoder) fieldDecoder).getStringBuilder(ctxt);
            assert fieldDecoder.isNullable || v != null : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        void encode(AbstractList<CharSequence> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (ObjectArrayList<CharSequence>) array;

            assert fieldEncoder != null;
            fieldEncoder.setString(a.get(idx), ctxt);
        }

        @Override
        AbstractList<CharSequence> initArray(int size, DecodingContext ctxt) {
            a = ctxt.manager.useCharSequenceList();
            a.setSize(size);
            return a;
        }

        @Override
        void setNullElement(int idx) {
            a.set(idx, null);
        }
    }

    private static class ObjectArrayAdapter extends ArrayAdapter<Object> {
        private ObjectArrayList a;

        private ObjectArrayAdapter(FieldDecoder fieldDecoder) {
            super(fieldDecoder);
        }

        private ObjectArrayAdapter(FieldEncoder fieldEncoder) {
            super(fieldEncoder);
        }

        @Override
        @SuppressWarnings("unchecked")
        public void decode(int idx, DecodingContext ctxt) {
            final Object v = ((ClassFieldDecoder) fieldDecoder).readObject(ctxt);
            assert fieldDecoder.isNullable || v != null : getNotNullableMsg();
            a.set(idx, v);
        }

        @Override
        @SuppressWarnings("unchecked")
        void setNullElement(int idx) {
            a.set(idx, null);
        }

        @Override
        void encode(AbstractList<Object> array, int idx, EncodingContext ctxt) {
            assert array != null;
            if (a != array)
                a = (ObjectArrayList) array;

            ((ClassFieldEncoder) fieldEncoder).writeObject(a.get(idx), ctxt);
        }

        @Override
        public AbstractList<Object> initArray(int size, DecodingContext ctxt) {
            a = (ObjectArrayList) ctxt.manager.use(ObjectArrayList.class, size);
            a.setSize(size);
            return a;
        }

    }
}