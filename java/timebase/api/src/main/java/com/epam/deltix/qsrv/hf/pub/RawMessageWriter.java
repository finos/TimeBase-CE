package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.pub.codec.InterpretingCodecMetaFactory;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.lang.Util;

/**
 * Base class for implementing different writers, that consume {@link RawMessage}.
 */
public interface RawMessageWriter {

    void write(final long value);

    void write(final int value);

    void write(final boolean value);

    void write(final float value);

    void write(final double value);

    void write(final byte[] value, int length);

    void write(final CharSequence value);

    byte[] getBinaryBuffer();

    UnboundDecoder getDecoder(final RecordClassDescriptor type);

    default void write(IntegerDataType type, ReadableValue decoder) {
        if (type.getNativeTypeSize() >= 6)
            write(decoder.getLong());
        else
            write(decoder.getInt());
    }

    default void write(FloatDataType type, ReadableValue decoder) {
        if (type.isFloat()) {
            write(decoder.getFloat());
        } else if (type.isDecimal64()) {
            write(decoder.getLong());
        } else {
            write(decoder.getDouble());
        }
    }

    default void write(BooleanDataType type, ReadableValue decoder) {
        write(decoder.getBoolean());
    }

    default void write(EnumDataType type, ReadableValue decoder) {
        write(type.descriptor.longToString(decoder.getLong()));
    }

    default void write(DateTimeDataType type, ReadableValue decoder) {
        write(decoder.getLong());
    }

    default void write(TimeOfDayDataType type, ReadableValue decoder) {
        write(decoder.getInt());
    }

    default void write(CharDataType type, ReadableValue decoder) {
        write(Character.toString(decoder.getChar()));
    }

    default void write(VarcharDataType type, ReadableValue decoder) {
        write((decoder.getString()));
    }

    default void write(BinaryDataType type, ReadableValue decoder) {
        int length = decoder.getBinaryLength();
        write(readBinary(decoder, length), length);
    }

    default byte[] readBinary(ReadableValue decoder, int length) {
        byte[] binaryBuffer = getBinaryBuffer();
        if (binaryBuffer.length < length)
            binaryBuffer = new byte[Util.doubleUntilAtLeast(binaryBuffer.length, length)];
        decoder.getBinary(0, length, binaryBuffer, 0);
        return binaryBuffer;
    }

    default void writePrimitive(ReadableValue decoder, DataType type) {
        if (type instanceof IntegerDataType) {
            write((IntegerDataType) type, decoder);
        } else if (type instanceof FloatDataType) {
            write((FloatDataType) type, decoder);
        } else if (type instanceof BooleanDataType) {
            write((BooleanDataType) type, decoder);
        } else if (type instanceof EnumDataType) {
            write((EnumDataType) type, decoder);
        } else if (type instanceof DateTimeDataType) {
            write((DateTimeDataType) type, decoder);
        } else if (type instanceof TimeOfDayDataType) {
            write((TimeOfDayDataType) type, decoder);
        } else if (type instanceof VarcharDataType) {
            write((VarcharDataType) type, decoder);
        } else if (type instanceof CharDataType) {
            write((CharDataType) type, decoder);
        } else if (type instanceof BinaryDataType) {
            write((BinaryDataType) type, decoder);
        } else {
            throw new IllegalArgumentException("Unsupported type " + type.getClass().getSimpleName());
        }
    }

    default UnboundDecoder getDecoder(final RecordClassDescriptor type, final ObjectToObjectHashMap<String, UnboundDecoder> decoders) {
        final String guid = type.getGuid();
        UnboundDecoder decoder;
        decoder = decoders.get(guid, null);
        if (decoder == null) {
            decoder = InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory(type).create();
            decoders.put(guid, decoder);
        }
        if (decoder == null) {
            System.out.println("Decoder is null");
        }
        return decoder;
    }

}
