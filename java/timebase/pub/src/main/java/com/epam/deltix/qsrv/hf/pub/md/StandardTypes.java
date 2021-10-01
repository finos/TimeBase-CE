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
package com.epam.deltix.qsrv.hf.pub.md;

import java.util.HashMap;
import java.util.function.Function;

/**
 *
 */
public class StandardTypes {
    public static final DataType CLEAN_BOOLEAN = new BooleanDataType (false);
    public static final DataType NULLABLE_BOOLEAN = new BooleanDataType (true);

    public static final DataType CLEAN_INTEGER = new IntegerDataType (IntegerDataType.ENCODING_INT64, false);
    public static final DataType NULLABLE_INTEGER = new IntegerDataType (IntegerDataType.ENCODING_INT64, true);

    public static final DataType CLEAN_FLOAT = new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, false);
    public static final DataType NULLABLE_FLOAT = new FloatDataType (FloatDataType.ENCODING_FIXED_DOUBLE, true);

    public static final DataType CLEAN_DECIMAL = new FloatDataType(FloatDataType.ENCODING_DECIMAL64, false);
    public static final DataType NULLABLE_DECIMAL = new FloatDataType(FloatDataType.ENCODING_DECIMAL64, true);

    public static final DataType CLEAN_VARCHAR = new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, false, true);
    public static final DataType NULLABLE_VARCHAR = new VarcharDataType(VarcharDataType.ENCODING_INLINE_VARSIZE, true, true);

    public static final DataType CLEAN_CHAR = new CharDataType (false);
    public static final DataType NULLABLE_CHAR = new CharDataType (true);

    public static final DataType CLEAN_TIMESTAMP = new DateTimeDataType (false);
    public static final DataType NULLABLE_TIMESTAMP = new DateTimeDataType (true);

    public static final DataType CLEAN_TIMEOFDAY = new TimeOfDayDataType (false);
    public static final DataType NULLABLE_TIMEOFDAY = new TimeOfDayDataType (true);

    public static final DataType CLEAN_BINARY = new BinaryDataType (false, 0);
    public static final DataType NULLABLE_BINARY = new BinaryDataType (true, 0);

    public static final DataType CLEAN_QUERY = new QueryDataType (false, null);
    public static final DataType NULLABLE_QUERY = new QueryDataType (true, null);

    public static final DataType ARR = new ArrayDataType(true, null);
    public static final DataType CLASS = new ClassDataType(true);

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

    private static final HashMap<String, DataType> TYPES_MAP = new HashMap<>();

    static {
        for (TypesContainer<?> container : CONTAINERS) {
            TYPES_MAP.putAll(container.types);
        }
    }

    public static final String []  PRIMITIVE_FIELD_TYPE_NAMES = {
            CLEAN_BOOLEAN.getBaseName (),
            CLEAN_INTEGER.getBaseName (),
            CLEAN_FLOAT.getBaseName (),
            CLEAN_DECIMAL.getBaseName(),
            CLEAN_VARCHAR.getBaseName (),
            CLEAN_CHAR.getBaseName (),
            CLEAN_TIMESTAMP.getBaseName (),
            CLEAN_TIMEOFDAY.getBaseName (),
            CLEAN_BINARY.getBaseName (),
    };

    public static String        toSimpleName (DataType type) {
        String  s = type.getBaseName ();

        if (type.isNullable ())
            s += "?";

        return (s);
    }

    public static DataType      forName (String name) {
        name = name.trim ();

        if (name.startsWith("FINAL "))
            name = name.substring(6);

        if (name.equals ("BOOLEAN"))
            return (CLEAN_BOOLEAN);

        if (name.equals ("BOOLEAN?"))
            return (NULLABLE_BOOLEAN);

        if (name.equals ("INTEGER"))
            return (CLEAN_INTEGER);

        if (name.equals ("INTEGER?"))
            return (NULLABLE_INTEGER);

        if (name.equals ("FLOAT"))
            return (CLEAN_FLOAT);

        if (name.equals ("FLOAT?"))
            return (NULLABLE_FLOAT);

        if (name.equals("DECIMAL"))
            return (CLEAN_DECIMAL);

        if (name.equals("DECIMAL?"))
            return (NULLABLE_DECIMAL);

        if (name.equals ("VARCHAR"))
            return (CLEAN_VARCHAR);

        if (name.equals ("VARCHAR?"))
            return (NULLABLE_VARCHAR);

        if (name.equals ("CHAR"))
            return (CLEAN_CHAR);

        if (name.equals ("CHAR?"))
            return (NULLABLE_CHAR);

        if (name.equals ("TIMESTAMP"))
            return (CLEAN_TIMESTAMP);

        if (name.equals ("TIMESTAMP?"))
            return (NULLABLE_TIMESTAMP);

        if (name.equals ("TIMEOFDAY"))
            return (CLEAN_TIMEOFDAY);

        if (name.equals ("TIMEOFDAY?"))
            return (NULLABLE_TIMEOFDAY);

        if (name.equals ("BINARY"))
            return (CLEAN_BINARY);

        if (name.equals ("BINARY?"))
            return (NULLABLE_BINARY);

        if (name.equals ("QUERY"))
            return (CLEAN_QUERY);

        if (name.equals ("QUERY?"))
            return (NULLABLE_QUERY);

        return TYPES_MAP.get(name);
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
}