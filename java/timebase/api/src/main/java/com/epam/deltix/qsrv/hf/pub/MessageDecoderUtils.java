package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.RecordLayout;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.epam.deltix.qsrv.hf.pub.RawMessageManipulator.OBJECT_CLASS_NAME;

/**
 * User: TurskiyS
 * Date: 12/17/13
 */
public class MessageDecoderUtils {

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
            for (int i = 0; i < len; i++)
                a.add(null);

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

        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object readArrayUncasted(int idx, final AbstractList a, DataType underlineType, ReadableValue udec) {
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

    protected static Object readField(UnboundDecoder udec) {
        final DataType type = udec.getField().getType();
        return readField(type, udec);
    }

    public static Object readField(DataType type, ReadableValue udec) {
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
                 return readObjectValues(udec);
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

    public static Map<String, Object> readObjectValues(ReadableValue udec) throws NullValueException {
        try {
            final UnboundDecoder decoder = udec.getFieldDecoder();
            Map<String, Object> valuesMap = new LinkedHashMap<>();

            if (decoder.getClassInfo() != null){
                valuesMap.put(OBJECT_CLASS_NAME, decoder.getClassInfo().getDescriptor());
            }

            // dump field/value pairs
            while (decoder.nextField()) {
                NonStaticFieldInfo field = decoder.getField();
                Object value = readField(decoder);
                if (field.getType() instanceof DateTimeDataType) {
                    value = formatDate (field.getType(), value);
                }
                valuesMap.put(field.getName(), value);
            }
            return valuesMap;
        } catch (NullValueException e) {
            return null;
        }
    }

    private static Object formatDate (DataType dataType, Object value){
        if (value instanceof Long){
            return dataType.toString(value);
        }

        return value;

    }

}
