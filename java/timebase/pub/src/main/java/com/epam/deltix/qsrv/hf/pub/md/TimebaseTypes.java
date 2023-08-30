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
package com.epam.deltix.qsrv.hf.pub.md;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.util.annotations.Alphanumeric;
import com.epam.deltix.util.annotations.Bool;
import com.epam.deltix.util.annotations.TimeOfDay;
import com.epam.deltix.util.annotations.TimestampMs;

import java.util.HashMap;
import java.util.function.Function;

public final class TimebaseTypes {

    // NULLs
    // Integer
    public static final byte INT8_NULL = IntegerDataType.INT8_NULL;
    public static final short INT16_NULL = IntegerDataType.INT16_NULL;
    public static final int INT32_NULL = IntegerDataType.INT32_NULL;
    public static final long INT64_NULL = IntegerDataType.INT64_NULL;
    // Float
    public static final float FLOAT32_NULL = FloatDataType.IEEE32_NULL;
    public static final double FLOAT64_NULL = FloatDataType.IEEE64_NULL;
    @Decimal
    public static final long DECIMAL64_NULL = Decimal64Utils.NULL;

    public static final char CHAR_NULL = CharDataType.NULL;
    @TimestampMs
    public static final long DATETIME_NULL = DateTimeDataType.NULL;
    @TimeOfDay
    public static final int TIMEOFDAY_NULL = TimeOfDayDataType.NULL;
    @Bool
    public static final byte BOOLEAN_NULL = BooleanDataType.NULL;
    @Alphanumeric
    public static final long ALPHANUMERIC_NULL = VarcharDataType.ALPHANUMERIC_NULL;

    public static final TypesContainer<IntegerDataType> INT8_CONTAINER = new TypesContainer<>("INT8",
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT8, nullable));
    public static final TypesContainer<IntegerDataType> INT16_CONTAINER = new TypesContainer<>("INT16",
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT16, nullable));
    public static final TypesContainer<IntegerDataType> INT32_CONTAINER = new TypesContainer<>("INT32",
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT32, nullable));
    public static final TypesContainer<IntegerDataType> INT64_CONTAINER = new TypesContainer<>("INT64",
            nullable -> new IntegerDataType(IntegerDataType.ENCODING_INT64, nullable));
    public static final TypesContainer<FloatDataType> FLOAT32_CONTAINER = new TypesContainer<>("FLOAT32",
            nullable -> new FloatDataType(FloatDataType.ENCODING_FIXED_FLOAT, nullable));
    public static final TypesContainer<FloatDataType> FLOAT64_CONTAINER = new TypesContainer<>("FLOAT64",
            nullable -> new FloatDataType(FloatDataType.ENCODING_FIXED_DOUBLE, nullable));
    public static final TypesContainer<FloatDataType> DECIMAL64_CONTAINER = new TypesContainer<>("DECIMAL",
            nullable -> new FloatDataType(FloatDataType.ENCODING_DECIMAL64, nullable));
    public static final TypesContainer<BooleanDataType> BOOLEAN_CONTAINER = new TypesContainer<>("BOOLEAN", BooleanDataType::new);
    public static final TypesContainer<VarcharDataType> ALPHANUMERIC10_CONTAINER = new TypesContainer<>("ALPHANUMERIC",
            nullable -> new VarcharDataType(VarcharDataType.ENCODING_ALPHANUMERIC + "(10)", nullable, false));
    public static final TypesContainer<VarcharDataType> UTF8_CONTAINER = new TypesContainer<>("VARCHAR",
            nullable -> new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, nullable, true));
    public static final TypesContainer<CharDataType> CHAR_CONTAINER = new TypesContainer<>("CHAR", CharDataType::new);
    public static final TypesContainer<DateTimeDataType> DATE_TIME_CONTAINER = new TypesContainer<>("TIMESTAMP", DateTimeDataType::new);
    public static final TypesContainer<TimeOfDayDataType> TIME_OF_DAY_CONTAINER = new TypesContainer<>("TIMEOFDAY", TimeOfDayDataType::new);
    public static final TypesContainer<ClassDataType> OBJECT_CONTAINER = new TypesContainer<>("OBJECT", ClassDataType::new);

    private static final TypesContainer<?>[] CONTAINERS = new TypesContainer[] {
            INT8_CONTAINER, INT16_CONTAINER, INT32_CONTAINER, INT64_CONTAINER,
            FLOAT32_CONTAINER, FLOAT64_CONTAINER, DECIMAL64_CONTAINER,
            BOOLEAN_CONTAINER,
            ALPHANUMERIC10_CONTAINER, UTF8_CONTAINER, CHAR_CONTAINER,
            DATE_TIME_CONTAINER, TIME_OF_DAY_CONTAINER,
            OBJECT_CONTAINER
    };

    private static final ArrayDataType[] ARRAY_NULLABLE_TYPES = new ArrayDataType[] {
            INT8_CONTAINER.getArrayType(true, true),
            INT16_CONTAINER.getArrayType(true, true),
            INT32_CONTAINER.getArrayType(true, true),
            INT64_CONTAINER.getArrayType(true, true),
            FLOAT32_CONTAINER.getArrayType(true, true),
            FLOAT64_CONTAINER.getArrayType(true, true),
            DECIMAL64_CONTAINER.getArrayType(true, true),
            BOOLEAN_CONTAINER.getArrayType(true, true),
            ALPHANUMERIC10_CONTAINER.getArrayType(true, true),
            UTF8_CONTAINER.getArrayType(true, true),
            CHAR_CONTAINER.getArrayType(true, true),
            DATE_TIME_CONTAINER.getArrayType(true, true),
            TIME_OF_DAY_CONTAINER.getArrayType(true, true),
            OBJECT_CONTAINER.getArrayType(true, true)
    };

    public static final HashMap<String, DataType> TYPES_MAP = new HashMap<>();

    static {
        for (TypesContainer<?> container : CONTAINERS) {
            TYPES_MAP.putAll(container.types);
        }
    }

    public static String toSimpleName(DataType type) {
        String s = type.getBaseName();

        if (type.isNullable())
            s += "?";

        return (s);
    }

    public static class TypesContainer<T extends DataType> {

        private final String name;
        private final String nullableName;
        private final String arrayName;
        private final String nullableArrayName;
        private final String arrayNameNullable;
        private final String nullableArrayNameNullable;
        private final T cleanType;
        private final T nullableType;
        private final ArrayDataType arrayType;
        private final ArrayDataType nullableArrayType;
        private final ArrayDataType arrayTypeNullable;
        private final ArrayDataType nullableArrayTypeNullable;

        private final HashMap<String, DataType> types = new HashMap<>();

        public TypesContainer(String name, Function<Boolean, T> creator) {
            this.cleanType = creator.apply(false);
            this.nullableType = creator.apply(true);
            this.arrayType = new ArrayDataType(false, cleanType);
            this.arrayTypeNullable = new ArrayDataType(false, nullableType);
            this.nullableArrayType = new ArrayDataType(true, cleanType);
            this.nullableArrayTypeNullable = new ArrayDataType(true, nullableType);
            this.name = name;
            this.nullableName = name + "?";
            this.arrayName = "ARRAY(" + name + ")";
            this.arrayNameNullable = "ARRAY(" + nullableName + ")";
            this.nullableArrayName = arrayName + "?";
            this.nullableArrayNameNullable = arrayNameNullable + "?";
            types.put(name, cleanType);
            types.put(nullableName, nullableType);
            types.put(arrayName, arrayType);
            types.put(arrayNameNullable, arrayTypeNullable);
            types.put(nullableArrayName, nullableArrayType);
            types.put(nullableArrayNameNullable, nullableArrayTypeNullable);
        }

        public DataType parse(String name) {
            return types.get(name.toLowerCase());
        }

        public ArrayDataType getArrayType(boolean isNullable, boolean isElementNullable) {
            if (isNullable && isElementNullable) {
                return nullableArrayTypeNullable;
            } else if (isNullable) {
                return nullableArrayType;
            } else if (isElementNullable) {
                return arrayTypeNullable;
            } else {
                return arrayType;
            }
        }

        public T getType(boolean nullable) {
            return nullable ? nullableType: cleanType;
        }

        public String getName() {
            return name;
        }
    }

    public static ArrayDataType getBooleanArrayType(boolean isNullable, boolean isElementNullable) {
        return BOOLEAN_CONTAINER.getArrayType(isNullable, isElementNullable);
    }

    public static BooleanDataType getBooleanType(boolean isNullable) {
        return BOOLEAN_CONTAINER.getType(isNullable);
    }

    public static boolean isResultNullable(DataType type1, DataType type2) {
        return type1.isNullable() || type2.isNullable();
    }

    public static boolean isBooleanOrBooleanArray(DataType type) {
        return type instanceof BooleanDataType ||
                (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof  BooleanDataType);
    }

    public static boolean isVarcharOrVarcharArray(DataType type) {
        return type instanceof VarcharDataType ||
                (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof VarcharDataType);
    }

    public static boolean isEnumOrEnumArray(DataType type) {
        return type instanceof EnumDataType || (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof EnumDataType);
    }

    public static boolean isTimeOfDayOrTimeOfDayArray(DataType type) {
        return type instanceof TimeOfDayDataType ||
                (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof TimeOfDayDataType);
    }

    public static boolean isDateTimeOrDateTimeArray(DataType type) {
        return type instanceof DateTimeDataType ||
                (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof DateTimeDataType);
    }

    public static boolean isCharOrCharArray(DataType type) {
        return type instanceof CharDataType ||
                (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof CharDataType);
    }

    public static boolean isObjectOrObjectArray(DataType type) {
        return type instanceof ClassDataType ||
                (type instanceof ArrayDataType && ((ArrayDataType) type).getElementDataType() instanceof ClassDataType);
    }

    public static EnumClassDescriptor extractEnumClassDescriptor(DataType dataType) {
        if (dataType instanceof EnumDataType) {
            return ((EnumDataType) dataType).getDescriptor();
        } else if (dataType instanceof ArrayDataType) {
            return ((EnumDataType) ((ArrayDataType) dataType).getElementDataType()).getDescriptor();
        }
        throw new UnsupportedOperationException();
    }

    public static IntegerDataType getIntegerDataType(int nativeSize, boolean isNullable) {
        switch (nativeSize) {
            case 1:
                return INT8_CONTAINER.getType(isNullable);
            case 2:
                return INT16_CONTAINER.getType(isNullable);
            case 4:
                return INT32_CONTAINER.getType(isNullable);
            case 8:
                return INT64_CONTAINER.getType(isNullable);
            default:
                throw new IllegalArgumentException("Unsupported native size " + nativeSize + " for IntegerDataType");
        }
    }

    public static ArrayDataType getIntegerArrayDataType(int nativeSize, boolean isNullable, boolean isElementNullable) {
        switch (nativeSize) {
            case 1:
                return INT8_CONTAINER.getArrayType(isNullable, isElementNullable);
            case 2:
                return INT16_CONTAINER.getArrayType(isNullable, isElementNullable);
            case 4:
                return INT32_CONTAINER.getArrayType(isNullable, isElementNullable);
            case 8:
                return INT64_CONTAINER.getArrayType(isNullable, isElementNullable);
            default:
                throw new IllegalArgumentException("Unsupported native size " + nativeSize + " for IntegerDataType");
        }
    }

    public static ArrayDataType[] getAllArrayNullableTypes() {
        return ARRAY_NULLABLE_TYPES;
    }

    public static boolean isNull(double v) {
        return Double.isNaN(v);
    }

    public static boolean isNull(float v) {
        return Float.isNaN(v);
    }

    public static boolean isNull(byte v) {
        return v == INT8_NULL;
    }

    public static boolean isNull(short v) {
        return v == INT16_NULL;
    }

    public static boolean isNull(int v) {
        return v == INT32_NULL;
    }

    public static boolean isNull(long v) {
        return v == INT64_NULL;
    }

    public static boolean isNull(Object o) {
        return o == null;
    }

    public static boolean isDecimalNull(@Decimal long v) {
        return v == DECIMAL64_NULL;
    }

    public static boolean isDateTimeNull(@TimestampMs long v) {
        return v == DATETIME_NULL;
    }

    public static boolean isTimeOfDayNull(@TimeOfDay int v) {
        return v == TIMEOFDAY_NULL;
    }

    public static boolean isBooleanNull(@Bool byte v) {
        return v == BOOLEAN_NULL;
    }
    
    public static DataType copy(DataType dataType, boolean nullable) {
        if (dataType instanceof IntegerDataType) {
            return copy((IntegerDataType) dataType, nullable);
        } else if (dataType instanceof FloatDataType) {
            return copy((FloatDataType) dataType, nullable);
        } else if (dataType instanceof BooleanDataType) {
            return copy((BooleanDataType) dataType, nullable);
        } else if (dataType instanceof CharDataType) {
            return copy((CharDataType) dataType, nullable);
        } else if (dataType instanceof VarcharDataType) {
            return copy((VarcharDataType) dataType, nullable);
        } else if (dataType instanceof DateTimeDataType) {
            return copy((DateTimeDataType) dataType, nullable);
        } else if (dataType instanceof TimeOfDayDataType) {
            return copy((TimeOfDayDataType) dataType, nullable);
        } else if (dataType instanceof BinaryDataType) {
            return copy((BinaryDataType) dataType, nullable);
        } else if (dataType instanceof EnumDataType) {
            return copy((EnumDataType) dataType, nullable);
        } else if (dataType instanceof ClassDataType) {
            return copy((ClassDataType) dataType, nullable);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public static IntegerDataType copy(IntegerDataType dataType, boolean nullable) {
        return new IntegerDataType(dataType.getEncoding(), nullable, dataType.getMin(), dataType.getMax());
    }

    public static FloatDataType copy(FloatDataType dataType, boolean nullable) {
        return new FloatDataType(dataType.getEncoding(), nullable, dataType.getMin(), dataType.getMax());
    }

    public static BooleanDataType copy(BooleanDataType dataType, boolean nullable) {
        return new BooleanDataType(nullable);
    }
    
    public static CharDataType copy(CharDataType dataType, boolean nullable) {
        return new CharDataType(nullable);
    }
    
    public static VarcharDataType copy(VarcharDataType dataType, boolean nullable) {
        return new VarcharDataType(dataType.getEncoding(), nullable, dataType.isMultiLine());
    }
    
    public static DateTimeDataType copy(DateTimeDataType dataType, boolean nullable) {
        return new DateTimeDataType(nullable);
    }
    
    public static TimeOfDayDataType copy(TimeOfDayDataType dataType, boolean nullable) {
        return new TimeOfDayDataType(nullable);
    }
    
    public static BinaryDataType copy(BinaryDataType dataType, boolean nullable) {
        return new BinaryDataType(nullable, dataType.getMaxSize(), dataType.getCompressionLevel());
    }
    
    public static EnumDataType copy(EnumDataType dataType, boolean nullable) {
        return new EnumDataType(nullable, dataType.getDescriptor());
    }
    
    public static ClassDataType copy(ClassDataType dataType, boolean nullable) {
        return new ClassDataType(nullable, dataType.getDescriptors());
    }
    
    public static ArrayDataType copy(ArrayDataType dataType, boolean nullable, boolean elementNullable) {
        return new ArrayDataType(nullable, copy(dataType.getElementDataType(), elementNullable));
    }
    
}