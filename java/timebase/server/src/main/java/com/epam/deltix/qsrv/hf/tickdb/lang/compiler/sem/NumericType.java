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
package com.epam.deltix.qsrv.hf.tickdb.lang.compiler.sem;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QValue;
import com.epam.deltix.util.jcg.JExpr;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.epam.deltix.qsrv.hf.tickdb.lang.compiler.cg.QCGHelpers.CTXT;

public enum NumericType {

    Float64(double.class, 0, CTXT.staticVarRef(FloatDataType.class, "IEEE64_NULL"), false,
            nullable -> new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, nullable),
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toDouble", value);
                } else {
                    return value.cast(double.class);
                }
            },
            value -> CTXT.staticCall(Double.class, "isNaN", value.cast(double.class))
    ),
    Float32(float.class, 1, CTXT.staticVarRef(FloatDataType.class, "IEEE32_NULL"),false,
            nullable -> new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, nullable),
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toDouble", value).cast(float.class);
                } else {
                    return value.cast(float.class);
                }
            },
            value -> CTXT.staticCall(Float.class, "isNaN", value.cast(float.class))
    ),
    Timestamp(long.class, 3, CTXT.staticVarRef(DateTimeDataType.class, "NULL"), false,
            DateTimeDataType::new,
            (value, sourceType) -> value.cast(long.class),
            value -> CTXT.binExpr(value.cast(long.class), "==", CTXT.staticVarRef(DateTimeDataType.class, "NULL"))
    ),
    Int64(long.class, 4, CTXT.staticVarRef(IntegerDataType.class, "INT64_NULL"),false,
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT64, nullable),
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toLong", value);
                } else {
                    return value.cast(long.class);
                }
            },
            value -> CTXT.binExpr(value.cast(long.class), "==", CTXT.staticVarRef(IntegerDataType.class, "INT64_NULL"))
    ),
    Int32(int.class, 5, CTXT.staticVarRef(IntegerDataType.class, "INT32_NULL"),false,
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT32, nullable),
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toInt", value);
                } else {
                    return value.cast(int.class);
                }
            },
            value -> CTXT.binExpr(value.cast(int.class), "==", CTXT.staticVarRef(IntegerDataType.class, "INT32_NULL"))
    ),
    Int16(short.class, 7, CTXT.staticVarRef(IntegerDataType.class, "INT16_NULL"),false,
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT16, nullable),
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toInt", value).cast(short.class);
                } else {
                    return value.cast(short.class);
                }
            },
            value -> CTXT.binExpr(value.cast(short.class), "==", CTXT.staticVarRef(IntegerDataType.class, "INT16_NULL"))
    ),
    Int8(byte.class, 8, CTXT.staticVarRef(IntegerDataType.class, "INT8_NULL"),false,
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT8, nullable),
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toInt", value).cast(byte.class);
                } else {
                    return value.cast(byte.class);
                }
            },
            value -> CTXT.binExpr(value.cast(byte.class), "==", CTXT.staticVarRef(IntegerDataType.class, "INT8_NULL"))
    ),
    Char(char.class, 6, CTXT.staticVarRef(CharDataType.class, "NULL"),false,
            CharDataType::new,
            (value, sourceType) -> {
                if (sourceType.isDecimal64()) {
                    return CTXT.staticCall(Decimal64Utils.class, "toInt", value).cast(char.class);
                } else {
                    return value.cast(char.class);
                }
            },
            value -> CTXT.binExpr(value.cast(byte.class), "==", CTXT.staticVarRef(IntegerDataType.class, "INT8_NULL"))
    ),
    Decimal64(long.class, 2, CTXT.staticVarRef(Decimal64Utils.class, "NULL"),true,
            nullable -> new FloatDataType(FloatDataType.ENCODING_DECIMAL64, nullable),
            (value, sourceType) -> {
                switch (sourceType) {
                    case Int8:
                    case Int16:
                    case Int32:
                        return CTXT.staticCall(Decimal64Utils.class, "fromInt", value);
                    case Int64:
                        return CTXT.staticCall(Decimal64Utils.class, "fromLong", value);
                    case Float32:
                    case Float64:
                        return CTXT.staticCall(Decimal64Utils.class, "fromDouble", value);
                    default:
                        throw new UnsupportedOperationException();
                }
            },
            value -> CTXT.binExpr(value.cast(long.class), "==", CTXT.staticVarRef(Decimal64Utils.class, "NULL"))
    );

    private final Class<?> clazz;
    private final int priority;
    private final JExpr nullValue;
    private final boolean isDecimal64;
    private final DataType nullable;
    private final DataType notNullable;
    private final ArrayDataType nullableArray;
    private final ArrayDataType nullableArrayNullableElement;
    private final ArrayDataType array;
    private final ArrayDataType arrayNullableElement;
    private final BiFunction<JExpr, NumericType, JExpr> extractor;
    private final Function<JExpr, JExpr> nullChecker;

    NumericType(Class<?> clazz, int priority, JExpr nullValue, boolean isDecimal64,
                Function<Boolean, DataType> creator,
                BiFunction<JExpr, NumericType, JExpr> extractor,
                Function<JExpr, JExpr> nullChecker) {
        this.clazz = clazz;
        this.priority = priority;
        this.nullValue = nullValue;
        this.isDecimal64 = isDecimal64;
        this.nullable = creator.apply(true);
        this.notNullable = creator.apply(false);
        this.nullableArray = new ArrayDataType(true, notNullable);
        this.array = new ArrayDataType(false, notNullable);
        this.nullableArrayNullableElement = new ArrayDataType(true, nullable);
        this.arrayNullableElement = new ArrayDataType(false, notNullable);
        this.extractor = extractor;
        this.nullChecker = nullChecker;
    }

    public int getPriority() {
        return priority;
    }

    public DataType getNullable() {
        return nullable;
    }

    public DataType getNotNullable() {
        return notNullable;
    }

    public DataType getType(boolean isNullable) {
        return isNullable ? nullable : notNullable;
    }

    public DataType getArrayType(boolean isNullable, boolean isElementNullable) {
        if (isNullable && isElementNullable) {
            return nullableArrayNullableElement;
        } else if (isNullable) {
            return nullableArray;
        } else if (isElementNullable) {
            return arrayNullableElement;
        } else {
            return array;
        }
    }

    public JExpr read(QValue value, NumericType sourceType) {
        return read(value.read(), sourceType);
    }

    public JExpr read(JExpr value, NumericType sourceType) {
        return sourceType == this ? value : extractor.apply(value, sourceType);
    }

    public JExpr castFrom(QValue value, NumericType valueType) {
        return castFrom(value.read(), valueType);
    }

    public JExpr castFrom(JExpr value, NumericType valueType) {
        return castFrom(value, valueType, nullValue);
    }

    public JExpr castFrom(QValue value, NumericType valueType, JExpr defaultValue) {
        return castFrom(value.read(), valueType, defaultValue);
    }

    public JExpr castFrom(JExpr value, NumericType valueType, JExpr defaultValue) {
        return CTXT.condExpr(
            valueType.checkNull(value),
            defaultValue,
            read(value, valueType)
        );
    }

    public JExpr checkNull(JExpr value) {
        return nullChecker.apply(value);
    }

    public JExpr checkNull(QValue value) {
        return checkNull(value.read());
    }

    public JExpr cast(JExpr expr) {
        return expr.cast(clazz);
    }

    public JExpr nullExpression() {
        return nullValue;
    }

    public boolean isDecimal64() {
        return isDecimal64;
    }

    public static NumericType forType(DataType dataType) {
        if (dataType instanceof IntegerDataType) {
            int size = ((IntegerDataType) dataType).getNativeTypeSize();
            switch (size) {
                case 1:
                    return Int8;
                case 2:
                    return Int16;
                case 4:
                    return Int32;
                case 8:
                    return Int64;
            }
        } else if (dataType instanceof FloatDataType) {
            if (((FloatDataType) dataType).isDecimal64()) {
                return Decimal64;
            } else if (((FloatDataType) dataType).isFloat()) {
                return Float32;
            } else {
                return Float64;
            }
        } else if (dataType instanceof DateTimeDataType) {
            return Timestamp;
        } else if (dataType instanceof CharDataType) {
            return Char;
        } else if (dataType instanceof ArrayDataType) {
            return forType(((ArrayDataType) dataType).getElementDataType());
        }
        return null;
    }

    public static NumericType resultType(@Nullable NumericType type1, @Nullable NumericType type2) {
        if (type1 == null || type2 == null)
            return null;
        NumericType result = type1.getPriority() < type2.getPriority() ? type1 : type2;
        return result.getPriority() > Int32.getPriority() ? Int32: result;
    }

    public static DataType resultType(DataType type1, DataType type2) {
        boolean isResultNullable = StandardTypes.isResultNullable(type1, type2);
        boolean returnArray = false;
        if (type1 instanceof ArrayDataType) {
            returnArray = true;
            type1 = ((ArrayDataType) type1).getElementDataType();
        }
        if (type2 instanceof ArrayDataType) {
            returnArray = true;
            type2 = ((ArrayDataType) type2).getElementDataType();
        }
        boolean isElementNullable = StandardTypes.isResultNullable(type1, type2);
        NumericType result = resultType(forType(type1), forType(type2));
        return returnArray ? result.getArrayType(isResultNullable, isElementNullable) : result.getType(isResultNullable);
    }

    public static boolean isNumericType(DataType type) {
        return type instanceof IntegerDataType || type instanceof FloatDataType;
    }

    public static boolean isNumericArrayType(DataType type) {
        return type instanceof ArrayDataType && isNumericType(((ArrayDataType) type).getElementDataType());
    }

    public static boolean isNumericOrNumericArray(DataType type) {
        return isNumericType(type) || isNumericArrayType(type);
    }

    public static boolean isCompatibleWithoutConversion(NumericType source, NumericType target) {
        if (source == Decimal64 && target != Decimal64 || source != Decimal64 && target == Decimal64) {
            return false;
        }
        return source.getPriority() >= target.getPriority();
    }

    public static int computeDistance(NumericType source, NumericType target) {
        return source.getPriority() - target.getPriority();
    }

    public static boolean isCompatibleWithoutConversion(DataType source, DataType target) {
        return isCompatibleWithoutConversion(forType(source), forType(target));
    }

    public static int computeDistance(DataType source, DataType target) {
        return computeDistance(forType(source), forType(target));
    }

    public static boolean isInteger(DataType dataType) {
        return dataType instanceof IntegerDataType ||
                (dataType instanceof ArrayDataType && (((ArrayDataType) dataType).getElementDataType()) instanceof IntegerDataType);
    }
}