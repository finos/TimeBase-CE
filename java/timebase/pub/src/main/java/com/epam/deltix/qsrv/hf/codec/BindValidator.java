package com.epam.deltix.qsrv.hf.codec;

import com.epam.deltix.dfp.Decimal64;
import com.epam.deltix.qsrv.hf.pub.ExchangeCodec;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.SmallArrays;
import com.epam.deltix.util.collections.generated.ByteArrayList;
import com.epam.deltix.util.lang.Util;
import rtmath.containers.BinaryArray;
import rtmath.containers.interfaces.BinaryArrayReadOnly;
import rtmath.containers.interfaces.BinaryArrayReadWrite;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 *
 */
public abstract class BindValidator {
    private final static Class<?>[] BINARY_TYPES = {ByteArrayList.class, BinaryArrayReadOnly.class, BinaryArrayReadWrite.class, BinaryArray.class};
    // TODO: specific
    private final static Class<?>[] BOOLEAN_TYPES = new Class<?>[]{boolean.class, byte.class};
    private final static Class<?>[] CHAR_TYPES = new Class<?>[]{char.class};
    private final static Class<?>[] DATETIME_TYPES = new Class<?>[]{long.class};
    private final static Class<?>[] ENUM_TYPES = new Class<?>[] { CharSequence.class, byte.class, short.class, int.class, long.class };
    private final static Class<?>[] FLOAT_TYPES = new Class<?>[] { double.class, float.class, long.class, Decimal64.class };
    private final static Class<?>[] INTEGER_TYPES = new Class<?>[] { byte.class, short.class, int.class, long.class};
    private final static Class<?>[] STRING_TYPES = new Class<?>[] { String.class, CharSequence.class, long.class,  ByteArrayList.class};
    private final static Class<?>[] TIMEOFDAY_TYPES = new Class<?>[]{int.class};

    public static boolean                isBinaryArray(Class<?> type) {
        return SmallArrays.indexOf(type, BindValidator.BINARY_TYPES) != -1;
    }

    public static void validateType(DataType dataType, Field jfield) {
        final Class<?> boundType = jfield.getType();
        validateTypeInternal(dataType, boundType, jfield.getGenericType());
    }

    public static void validateType(DataType dataType, Class<?> boundType) {
        validateTypeInternal(dataType, boundType, null);
    }

    public static void validateGenericType(DataType dataType, Class<?> boundType, Type genericType) {
        validateTypeInternal(dataType, boundType, genericType);
    }

    private static void validateTypeInternal(DataType dataType, Class<?> boundType, Type genericType) {

        if (dataType instanceof BinaryDataType)
            validateType(dataType, boundType, BINARY_TYPES);
        else if (dataType instanceof BooleanDataType)
            validateTypeBoolean((BooleanDataType) dataType, boundType);
        else if (dataType instanceof CharDataType)
            validateType(dataType, boundType, CHAR_TYPES);
        else if (dataType instanceof DateTimeDataType)
            validateType(dataType, boundType, DATETIME_TYPES);
        else if (dataType instanceof EnumDataType)
            validateTypeEnum((EnumDataType) dataType, boundType);
        else if (dataType instanceof FloatDataType)
            validateTypeFloat((FloatDataType) dataType, boundType);
        else if (dataType instanceof IntegerDataType)
            validateTypeInteger((IntegerDataType) dataType, boundType);
        else if (dataType instanceof VarcharDataType)
            // in case of long isJavaUnderIKVM value doesn't matter
            validateTypeString((VarcharDataType) dataType, boundType);
        else if (dataType instanceof TimeOfDayDataType)
            validateType(dataType, boundType, TIMEOFDAY_TYPES);
        else if (dataType instanceof ArrayDataType)
            validateArrayType((ArrayDataType) dataType, boundType, genericType);
        else if (dataType instanceof ClassDataType) {
            if (!Object.class.isAssignableFrom(boundType))
                throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound to " + boundType.getName() + " field");
        }
        else
            throw new IllegalArgumentException("unexpected underline type " + dataType.getClass().getName());

    }

    public static void validateTypeStatic(DataType dataType, Field jfield, String staticValue) {
        final Class<?> boundType = jfield.getType();
        validateTypeStatic(dataType, boundType, jfield, null, staticValue);
    }

    public static void validateTypeStatic(DataType dataType, Class<?> boundType, Field jfield, Method method, String staticValue) {
        if (dataType instanceof BooleanDataType)
            validateTypeStaticBoolean((BooleanDataType)dataType, boundType, staticValue);
        else if (dataType instanceof EnumDataType)
            validateTypeStaticEnum((EnumDataType) dataType, boundType, staticValue);
        else if (dataType instanceof FloatDataType)
            validateTypeStaticFloat((FloatDataType) dataType, boundType, staticValue);
        else if (dataType instanceof IntegerDataType)
            validateTypeStaticInteger((IntegerDataType) dataType, boundType, staticValue);
        else if (dataType instanceof VarcharDataType)
            validateTypeStaticString((VarcharDataType) dataType, boundType, staticValue);
        else if (dataType instanceof ArrayDataType) {
            if (jfield != null || method != null)
                throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound to " + ((jfield != null) ? jfield.getName() : method.getName()) +
                    " field, because ARRAY static fields are not supported for now");
            else
                throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound, because ARRAY static fields are not supported for now");
        }
        else
            validateTypeInternal(dataType, boundType, null);
    }

    private static void validateType(DataType dataType, Class<?> boundType, Class<?>[] supportedTypes) {
        if (dataType instanceof ClassDataType) {
            if (boundType.isInterface())
                throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound to " + boundType.getName() + " field");

        } else {
            if (Util.indexOf(supportedTypes, boundType) == -1)
                throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound to " + boundType.getName() + " field");
        }
    }

    private static void validateTypeEnum(EnumDataType dataType, Class<?> type) {
        if (!type.isEnum() ){
            validateType(dataType, type, ENUM_TYPES);

            // encoding value size must be <= bound type size
            if (MdUtil.isIntegerType(type) && dataType.descriptor.computeStorageSize() > MdUtil.getSize(type))
                throw new IllegalArgumentException(dataType.descriptor.getName() + " cannot be bound to " + type.getName() + " field");
        }
    }

    private static void validateTypeStaticEnum(EnumDataType dataType, Class<?> type, String staticValue) {
        if (!type.isEnum()) {
            validateType(dataType, type, ENUM_TYPES);

            if (staticValue != null) {
                if (MdUtil.isIntegerType(type)) {
                    final long v = dataType.descriptor.stringToLong(staticValue);
                    MdUtil.validateIntegerRange(type, v);
                }
            }
        }
    }

    private static void validateTypeFloat(FloatDataType dataType, Class<?> type) {
        validateType(dataType, type, FLOAT_TYPES);

        // any float type supported
        if (dataType.isDecimal64())
            return;

        // float can be bound only to IEEE32
        if ((dataType.isFloat() && type != float.class))
            throw new IllegalArgumentException(dataType.getEncoding() + " cannot be bound to " + type.getName() + " field");

        if (!dataType.isFloat() && type != double.class)
            throw new IllegalArgumentException(dataType.getEncoding() + " cannot be bound to " + type.getName() + " field");
    }

    private static void validateTypeStaticFloat(FloatDataType dataType, Class<?> type, String staticValue) {
        validateType(dataType, type, FLOAT_TYPES);

        if (staticValue != null) {
            final Object v = dataType.parse (staticValue);

            // validate that static value can fit into the bound type
            MdUtil.validateFloatRange(type, (Number) v);
        }
    }

    private static void validateTypeInteger(IntegerDataType dataType, Class<?> type) {
        validateType(dataType, type, INTEGER_TYPES);

        // encoding value size must be <= bound type size
        if (dataType.getNativeTypeSize() > MdUtil.getSize(type))
            throw new IllegalArgumentException(dataType.getEncoding() + " cannot be bound to " + type.getName() + " field");
    }

    private static void validateTypeStaticInteger(IntegerDataType dataType, Class<?> type, String staticValue) {
        validateType(dataType, type, INTEGER_TYPES);

        final Object v;
        if (staticValue != null && (v = dataType.parse (staticValue)) != null) {
            // validate that static value can fit into the bound type
            MdUtil.validateIntegerRange(type, (Number) v);
        }
    }

    private static void validateTypeString(VarcharDataType dataType, Class<?> type) {

        validateType(dataType, type, STRING_TYPES);

        // long can be bound only if n<=10
        if ((type == long.class ) && (dataType.getEncodingType() != VarcharDataType.ALPHANUMERIC || dataType.getLength() > 10))
            throw new IllegalArgumentException(dataType.getEncoding() + " cannot be bound to " + type.getName() + " field");
    }

    private static void validateTypeStaticString(VarcharDataType dataType, Class<?> type, String staticValue) {

        validateType(dataType, type, STRING_TYPES);

        // validate that value can be encoded as ALPHANUMERIC(10)
        if (staticValue != null && type == long.class) {
            ExchangeCodec.codeToLong(staticValue);
        }
    }

    private static void validateTypeBoolean(BooleanDataType dataType, Class<?> type) {
        validateType(dataType, type, BOOLEAN_TYPES);

        if (type == boolean.class && dataType.isNullable())
            throw new IllegalArgumentException("Nullable BOOLEAN cannot be bound to " + type.getName() + " field");
    }

    private static void validateTypeStaticBoolean(BooleanDataType dataType, Class<?> type, String staticValue) {
        validateType(dataType, type, BOOLEAN_TYPES);

        if (type == boolean.class && staticValue == null)
            throw new IllegalArgumentException("Nullable BOOLEAN cannot be bound to " + type.getName() + " field");
    }

    private static void validateArrayType(ArrayDataType dataType, Class<?> boundType, Type genericType) {
        // special case is System.Collections.Generic.List<T>
        final Class<?> baseType;
        if (ArrayTypeUtil.isSupported(boundType))
            baseType = ArrayTypeUtil.getUnderline(boundType, genericType);
        else
            throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound to " + boundType.getName() + " field");

        // validate underline
        try {
            validateType(dataType.getElementDataType(), baseType);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(dataType.getBaseName() + " cannot be bound to " + boundType.getName() + " field, because " + e.getMessage());
        }
    }

}
