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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sx;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem.DataTypeHelper;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.QRT;
import com.epam.deltix.qsrv.hf.tickdb.lang.runtime.selectors.containers.*;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.jcg.JExpr;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public class CompiledArrayConstant<V, L extends List<V>, T extends BaseInstanceArray<?, L>> extends CompiledExpression<ArrayDataType> {

    protected final Class<?> cls;
    protected final Class<?> elementCls;
    protected final JExpr[] elements;
    protected final L list;
    protected final Function<V, String> toString;

    public CompiledArrayConstant(ArrayDataType type, Class<T> listClass, Class<?> elementCls, JExpr[] elements, L list,
                                 Function<V, String> toString) {
        super(type);
        this.cls = listClass;
        this.elementCls = elementCls;
        this.elements = elements;
        this.list = list;
        this.toString = toString;
    }

    public CompiledArrayConstant(ArrayDataType type, Class<T> listClass, Class<?> elementCls, JExpr[] elements, L list) {
        this(type, listClass, elementCls, elements, list, Objects::toString);
    }

    public Class<?> getCls() {
        return cls;
    }

    public Class<?> getElementCls() {
        return elementCls;
    }

    public JExpr[] getElements() {
        return elements;
    }

    public L getList() {
        return list;
    }

    @Override
    public void print(StringBuilder out) {
        if (elements.length == 0) {
            out.append("[]");
        } else {
            out.append('[').append(toString.apply(list.get(0)));
            for (int i = 1; i < list.size(); i++) {
                out.append(',').append(toString.apply(list.get(i)));
            }
            out.append(']');
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CompiledArrayConstant))
            return false;
        CompiledArrayConstant<?, ?, ?> o = (CompiledArrayConstant<?, ?, ?>) obj;
        return cls.equals(o.cls) && elementCls.equals(o.elementCls) && Arrays.equals(elements, o.elements);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = result * 31 + DataTypeHelper.hashcode(type);
        result = result * 31 + cls.hashCode();
        result = result * 31 + elementCls.hashCode();
        result = result * 31 + list.hashCode();
        return result;
    }

    public static JExpr[] createBoolean(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(BooleanDataType.class, "NULL") :
                    CTXT.intLiteral(QRT.bpos(constants[i].getBoolean())).cast(byte.class);
        }
        return values;
    }

    public static JExpr[] createByte(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(IntegerDataType.class, "INT8_NULL") :
                    CTXT.intLiteral(constants[i].getByte()).cast(byte.class);
        }
        return values;
    }

    public static JExpr[] createShort(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(IntegerDataType.class, "INT16_NULL") :
                    CTXT.intLiteral(constants[i].getShort()).cast(short.class);
        }
        return values;
    }

    public static JExpr[] createInteger(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(IntegerDataType.class, "INT32_NULL") :
                    CTXT.intLiteral(constants[i].getInteger());
        }
        return values;
    }

    public static JExpr[] createLong(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(IntegerDataType.class, "INT64_NULL") :
                    CTXT.longLiteral(constants[i].getLong());
        }
        return values;
    }

    public static JExpr[] createDecimal(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(Decimal64Utils.class, "NULL") :
                    CTXT.longLiteral(constants[i].getDecimalLong());
        }
        return values;
    }

    public static JExpr[] createDouble(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.staticVarRef(Double.class, "NaN") :
                    CTXT.doubleLiteral(constants[i].getDouble());
        }
        return values;
    }

    public static JExpr[] createString(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.nullLiteral() :
                    CTXT.stringLiteral(constants[i].getString());
        }
        return values;
    }

    public static JExpr[] createChar(CompiledConstant[] constants) {
        JExpr[] values = new JExpr[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? CTXT.nullLiteral() :
                    CTXT.charLiteral(constants[i].getChar());
        }
        return values;
    }

    public static ByteArrayList createBooleanArray(CompiledConstant[] constants) {
        byte[] values = new byte[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? BooleanDataType.NULL : QRT.bpos(constants[i].getBoolean());
        }
        return new ByteArrayList(values);
    }

    public static ByteArrayList createByteArray(CompiledConstant[] constants) {
        byte[] values = new byte[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? IntegerDataType.INT8_NULL : constants[i].getByte();
        }
        return new ByteArrayList(values);
    }

    public static ShortArrayList createShortArray(CompiledConstant[] constants) {
        short[] values = new short[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? IntegerDataType.INT16_NULL : constants[i].getShort();
        }
        return new ShortArrayList(values);
    }

    public static IntegerArrayList createIntegerArray(CompiledConstant[] constants) {
        int[] values = new int[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? IntegerDataType.INT32_NULL : constants[i].getInteger();
        }
        return new IntegerArrayList(values);
    }

    public static LongArrayList createLongArray(CompiledConstant[] constants) {
        long[] values = new long[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? IntegerDataType.INT64_NULL : constants[i].getLong();
        }
        return new LongArrayList(values);
    }

    @Decimal
    public static LongArrayList createDecimalLongArray(CompiledConstant[] constants) {
        @Decimal long[] values = new long[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? Decimal64Utils.NULL : constants[i].getDecimalLong();
        }
        return new LongArrayList(values);
    }

    public static DoubleArrayList createDoubleArray(CompiledConstant[] constants) {
        double[] values = new double[constants.length];
        for (int i = 0; i < constants.length; i++) {
            values[i] = constants[i].getValue() == null ? FloatDataType.IEEE64_NULL : constants[i].getDouble();
        }
        return new DoubleArrayList(values);
    }

    public static ObjectArrayList<CharSequence> createStringArray(CompiledConstant[] constants) {
        ObjectArrayList<CharSequence> values = new ObjectArrayList<>(constants.length);
        for (int i = 0; i < constants.length; i++) {
            values.add(constants[i] == null ? null : constants[i].getString());
        }
        return values;
    }

    public static CharacterArrayList createCharArray(CompiledConstant[] constants) {
        CharacterArrayList values = new CharacterArrayList(constants.length);
        for (int i = 0; i < constants.length; i++) {
            values.add(constants[i] == null ? CharDataType.NULL : constants[i].getChar());
        }
        return values;
    }

    public static CompiledArrayConstant<Byte, ByteArrayList, ByteInstanceArray> createBooleanArrayConstant(
            CompiledConstant[] constants, boolean isElementNullable
    ) {
        return new CompiledArrayConstant<>(TimebaseTypes.BOOLEAN_CONTAINER.getArrayType(false, isElementNullable),
                ByteInstanceArray.class, byte.class, createBoolean(constants), createBooleanArray(constants));
    }

    public static CompiledArrayConstant<Byte, ByteArrayList, ByteInstanceArray> createByteArrayConstant(CompiledConstant[] constants,
                                                                                                        boolean isElementNullable) {
        return new CompiledArrayConstant<>(TimebaseTypes.INT8_CONTAINER.getArrayType(false, isElementNullable),
                ByteInstanceArray.class, byte.class, createByte(constants), createByteArray(constants));
    }

    public static CompiledArrayConstant<Short, ShortArrayList, ShortInstanceArray> createShortArrayConstant(CompiledConstant[] constants,
                                                                                                            boolean isElementNullable) {
        return new CompiledArrayConstant<>(TimebaseTypes.INT16_CONTAINER.getArrayType(false, isElementNullable),
                ShortInstanceArray.class, short.class, createShort(constants), createShortArray(constants));
    }

    public static CompiledArrayConstant<Integer, IntegerArrayList, IntegerInstanceArray> createIntegerArrayConstant(CompiledConstant[] constants,
                                                                                                                    boolean isElementNullable) {
        return new CompiledArrayConstant<>(TimebaseTypes.INT32_CONTAINER.getArrayType(false, isElementNullable),
                IntegerInstanceArray.class, int.class, createInteger(constants), createIntegerArray(constants));
    }

    public static CompiledArrayConstant<Integer, IntegerArrayList, IntegerInstanceArray> createIntegerArrayConstant(IntegerArrayList list) {
        return new CompiledArrayConstant<>(TimebaseTypes.INT32_CONTAINER.getArrayType(false, false),
                IntegerInstanceArray.class, int.class, toExpressions(list), list);
    }

    public static CompiledArrayConstant<Long, LongArrayList, LongInstanceArray> createLongArrayConstant(CompiledConstant[] constants,
                                                                                                        boolean isElementNullable) {
        return new CompiledArrayConstant<>(TimebaseTypes.INT64_CONTAINER.getArrayType(false, isElementNullable),
                LongInstanceArray.class, long.class, createLong(constants), createLongArray(constants));
    }

    public static CompiledArrayConstant<Long, LongArrayList, LongInstanceArray> createLongArrayConstant(LongArrayList list) {
        return new CompiledArrayConstant<>(TimebaseTypes.INT64_CONTAINER.getArrayType(false, false),
                LongInstanceArray.class, long.class, toExpressions(list), list);
    }

    public static CompiledArrayConstant<Long, LongArrayList, LongInstanceArray> createDecimalArrayConstant(CompiledConstant[] constants,
                                                                                                           boolean isElementNullable) {
        return new CompiledArrayConstant<>(TimebaseTypes.DECIMAL64_CONTAINER.getArrayType(false, isElementNullable),
                LongInstanceArray.class, long.class, createDecimal(constants), createDecimalLongArray(constants),
                Decimal64Utils::toString);
    }

    public static CompiledArrayConstant<Long, LongArrayList, LongInstanceArray> createDecimalArrayConstant(@Decimal LongArrayList list) {
        return new CompiledArrayConstant<>(TimebaseTypes.DECIMAL64_CONTAINER.getArrayType(false, false),
                LongInstanceArray.class, long.class, toExpressions(list), list, Decimal64Utils::toString);
    }

    public static CompiledArrayConstant<Double, DoubleArrayList, DoubleInstanceArray> createDoubleArrayConstant(CompiledConstant[] constants,
                                                                                                                boolean isElementNullable) {
        return new CompiledArrayConstant<>(TimebaseTypes.FLOAT64_CONTAINER.getArrayType(false, isElementNullable),
                DoubleInstanceArray.class, double.class, createDouble(constants), createDoubleArray(constants));
    }

    public static CompiledArrayConstant<Double, DoubleArrayList, DoubleInstanceArray> createDoubleArrayConstant(DoubleArrayList list) {
        return new CompiledArrayConstant<>(TimebaseTypes.FLOAT64_CONTAINER.getArrayType(false, false),
                DoubleInstanceArray.class, double.class, toExpressions(list), list);
    }

    public static CompiledArrayConstant<?, ?, ?> createFloatArrayConstant(boolean isDecimal, CompiledConstant[] constants,
                                                                          boolean isNullable) {
        return isDecimal ? createDecimalArrayConstant(constants, isNullable) : createDoubleArrayConstant(constants, isNullable);
    }

    public static CompiledArrayConstant<?, ?, ?> createIntegerArrayConstant(int size, CompiledConstant[] constants,
                                                                            boolean isElementNullable) {
        switch (size) {
            case 1:
                return createByteArrayConstant(constants, isElementNullable);
            case 2:
                return createShortArrayConstant(constants, isElementNullable);
            case 4:
                return createIntegerArrayConstant(constants, isElementNullable);
            case 8:
                return createLongArrayConstant(constants, isElementNullable);
            default:
                throw new IllegalArgumentException("Illegal native size " + size + " for IntegerDataType");
        }
    }

    public static CompiledArrayConstant<CharSequence, ObjectArrayList<CharSequence>, CharSequenceInstanceArray> createStringArrayConstant(
            CompiledConstant[] constants, boolean isElementNullable
    ) {
        return new CompiledArrayConstant<>(TimebaseTypes.UTF8_CONTAINER.getArrayType(false, isElementNullable),
                CharSequenceInstanceArray.class, CharSequence.class, createString(constants), createStringArray(constants));
    }

    public static CompiledArrayConstant<Character, CharacterArrayList, CharacterInstanceArray> createCharArrayConstant(
            CompiledConstant[] constants, boolean isElementNullable
    ) {
        return new CompiledArrayConstant<>(TimebaseTypes.CHAR_CONTAINER.getArrayType(false, isElementNullable),
                CharacterInstanceArray.class, char.class, createChar(constants), createCharArray(constants));
    }

    public static CompiledArrayConstant<Long, LongArrayList, LongInstanceArray> createDateArrayConstant(
            CompiledConstant[] constants, boolean isElementNullable
    ) {
        return new CompiledArrayConstant<>(TimebaseTypes.DATE_TIME_CONTAINER.getArrayType(false, isElementNullable),
                LongInstanceArray.class, long.class, createLong(constants), createLongArray(constants));
    }

    public static CompiledArrayConstant<Long, LongArrayList, LongInstanceArray> createDateArrayConstant(LongArrayList list) {
        return new CompiledArrayConstant<>(TimebaseTypes.DATE_TIME_CONTAINER.getArrayType(false, false),
                LongInstanceArray.class, long.class, toExpressions(list), list);
    }

    private static JExpr[] toExpressions(DoubleArrayList list) {
        JExpr[] result = new JExpr[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = CTXT.doubleLiteral(list.getDouble(i));
        }
        return result;
    }

    private static JExpr[] toExpressions(LongArrayList list) {
        JExpr[] result = new JExpr[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = CTXT.longLiteral(list.getLong(i));
        }
        return result;
    }

    private static JExpr[] toExpressions(IntegerArrayList list) {
        JExpr[] result = new JExpr[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = CTXT.intLiteral(list.getInteger(i));
        }
        return result;
    }


}