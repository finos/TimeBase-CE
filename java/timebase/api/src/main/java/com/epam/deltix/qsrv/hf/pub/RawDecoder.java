package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.*;

import static com.epam.deltix.qsrv.hf.pub.RawMessageManipulator.OBJECT_CLASS_NAME;

/**
 * Created by Alex Karpovich on 4/11/2018.
 */
public class RawDecoder {

    public Object readField(DataType type, ReadableValue rv) {
        try {
            if (type instanceof IntegerDataType) {
                return readInteger((IntegerDataType) type, rv);

            } else if (type instanceof FloatDataType)
                return readFloat((FloatDataType) type, rv);

            else if (type instanceof CharDataType)
                return rv.getChar();
            else if (type instanceof EnumDataType || type instanceof VarcharDataType)
                return rv.getString();
            else if (type instanceof BooleanDataType)
                return readBoolean((BooleanDataType)type, rv);

            else if (type instanceof DateTimeDataType)
                return readDateTime((DateTimeDataType)type, rv);
            else if (type instanceof TimeOfDayDataType)
                return rv.getInt();
            else if (type instanceof ArrayDataType)
                return readArray((ArrayDataType) type, rv);
            else if (type instanceof ClassDataType)
                return readObjectValues(rv);
            else if (type instanceof BinaryDataType) {
                try {
                    final int size = rv.getBinaryLength();
                    final byte[] bin = new byte[size];
                    rv.getBinary(0, size, bin, 0);
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

    protected Object    readBoolean(BooleanDataType type, ReadableValue rv) {
       return rv.getBoolean();
    }

    protected Object    readInteger(IntegerDataType type, ReadableValue rv) {
        int size = type.getNativeTypeSize();

        if (size >= 6)
            return rv.getLong();
        else if (size == 1)
            return (byte) rv.getInt();
        else if (size == 2)
            return (short) rv.getInt();
        else
            return rv.getInt();
    }

    protected Object    readFloat(FloatDataType type, ReadableValue rv) {
        if (type.isFloat())
            return rv.getFloat();
        else
            return rv.getDouble();
    }

    protected Object    readDateTime(DateTimeDataType type, ReadableValue rv) {
        return rv.getLong();
    }

    private Object[]    readArray(ArrayDataType type, ReadableValue udec) throws NullValueException {
        final int len = udec.getArrayLength();
        final DataType elementType = type.getElementDataType();

        final boolean isNullableBool = (elementType instanceof BooleanDataType) && elementType.isNullable();

        Object[] values = new Object[len];

        for (int i = 0; i < len; i++) {
            Object value;
            try {
                final ReadableValue rv = udec.nextReadableElement();
                value = readField(elementType, rv);
            } catch (NullValueException e) {
                value = null;
            }

            if (isNullableBool) {
                Boolean b = (Boolean) value;
                values[i] = (byte) (b == null ? -1 : b ? 1 : 0);
            } else {
                values[i] = value;
            }

        }
        return values;
    }

    private Map<String, Object> readObjectValues(ReadableValue udec) throws NullValueException {

        final UnboundDecoder decoder = udec.getFieldDecoder();
        Map<String, Object> values = new LinkedHashMap<>();

        if (decoder.getClassInfo() != null)
            values.put(OBJECT_CLASS_NAME, decoder.getClassInfo().getDescriptor());

        // dump field/value pairs
        while (decoder.nextField()) {
            NonStaticFieldInfo field = decoder.getField();
            Object value = readField(field.getType(), decoder);
            values.put(field.getName(), value);
        }

        return values;
    }
}
