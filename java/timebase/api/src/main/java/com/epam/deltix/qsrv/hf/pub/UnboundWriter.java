package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.UnboundEncoder;
import com.epam.deltix.qsrv.hf.pub.md.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by Alex Karpovich on 10/3/2018.
 */
public class UnboundWriter<T> {

    protected final String typePropertyName;

    public UnboundWriter(String name) {
        this.typePropertyName = name;
    }

    /**
     * Return object type specification: can be RecordClassDescriptor instance, name of RecordClassDescriptor, GUID of RecordClassDescriptor
     * @param value values container
     * @return object type specification
     */
    protected Object                   getObjectType(T value) {
        if (value instanceof Map)
            return ((Map) value).get(typePropertyName);

        throw new UnsupportedOperationException("Object " + value + " is not supported");
    }

    protected RecordClassDescriptor    matchObjectType(T value, ClassDataType type) {
        return  matchObjectType(value, type.getDescriptors());
    }

    protected RecordClassDescriptor    matchObjectType(T value, RecordClassDescriptor[] rcds) {
        Object name = getObjectType(value);

        RecordClassDescriptor rcd = null;
        if (name != null) {
            for (RecordClassDescriptor descriptor : rcds) {
                if (name instanceof String) {
                    if (descriptor.getName().endsWith((String) name) || descriptor.getGuid().equals(name))
                        rcd = descriptor;
                } else if (descriptor.equals(name)) {
                    rcd = descriptor;
                }
            }
        } else if (rcds.length == 1) {
            rcd = rcds[0];
        } else {
            throw new IllegalStateException("Undefined object type for the " + value);
        }

        return rcd;
    }

    public void             writeField(T value, UnboundEncoder uenc) {
        final DataType type = uenc.getField().getType();
        writeField(value, type, uenc);
    }

    public void             writeField(T value, DataType type, WritableValue uenc) {
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
            RecordClassDescriptor rcd = matchObjectType(value, (ClassDataType) type);
            writeObject(value, uenc.getFieldEncoder(rcd));
        }
        else if (type instanceof BinaryDataType) {
            byte[] bin = (byte[]) value;
            uenc.writeBinary(bin, 0, bin.length);
        } else
            throw new RuntimeException("Unrecognized DataType: " + type);
    }

    public void             writeArray(Iterable<T> value, int size, ArrayDataType type, WritableValue uenc) {
        uenc.setArrayLength(size);
        final DataType underlineType = type.getElementDataType();

        Iterator<T> it = value.iterator();
        while (it.hasNext()) {
            final T v = it.next();
            final WritableValue rv = uenc.nextWritableElement();

            if (v != null || type.getElementDataType().isNullable())
                writeField(v, underlineType, rv);
        }
    }

    public void             writeObject(Map<String, T> values, UnboundEncoder encoder) {
        while (encoder.nextField()) {
            T v = values.get(encoder.getField().getName());
            if (v != null)
                writeField(v, encoder);
        }
    }

    @SuppressWarnings("unchecked")
    public void             writeArray(T value, ArrayDataType type, WritableValue uenc) {
        if (value instanceof Iterable)
            writeArray((Iterable)value, ((Collection)value).size(), type, uenc);
        else
            throw new UnsupportedOperationException("Array " + value + " is not supported");
    }

    @SuppressWarnings("unchecked")
    public void             writeObject(T value, UnboundEncoder encoder) {
        if (value instanceof Map)
            writeObject((Map)value, encoder);
        else
            throw new UnsupportedOperationException("Object " + value + " is not supported");
    }
}
