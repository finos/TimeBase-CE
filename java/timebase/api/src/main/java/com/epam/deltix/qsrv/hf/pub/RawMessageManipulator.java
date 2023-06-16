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
package com.epam.deltix.qsrv.hf.pub;

import java.util.*;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.memory.*;

public class RawMessageManipulator {

    public static final String    OBJECT_CLASS_NAME        = "objectClassName";

    private final MemoryDataInput mdi = new MemoryDataInput();
    private final ObjectToObjectHashMap<String, UnboundDecoder> _decoders = new ObjectToObjectHashMap<>();
    private final MemoryDataOutput mdo = new MemoryDataOutput();
    private final ObjectToObjectHashMap<String, FixedUnboundEncoder> _encoders = new ObjectToObjectHashMap<>();

    private final RawDecoder reader;

    public RawMessageManipulator() {
        this(new RawDecoder());
    }

    public RawMessageManipulator(RawDecoder reader) {
        this.reader = reader;
    }

    public void writeValues(RawMessage msg, Map<String, Object> values) {

        if (values == null && msg.data != null)
            return;

        mdo.reset();

        final FixedUnboundEncoder encoder = getEncoder(msg);
        encoder.beginWrite(mdo);

        while (encoder.nextField()) {
            final NonStaticFieldInfo info = encoder.getField();
            final Object value = values == null ? null : values.get(info.getName());

            writeValue(encoder, info, value);
        }

        msg.copyBytes(mdo, 0);
    }

    public Map<String, Object> generateValues(final RawMessage msg) {
        final HashMap<String, Object> values = new HashMap<>();

        if (msg.data == null)
            return null;

        mdi.setBytes(msg.data, msg.offset, msg.length);
        final UnboundDecoder decoder = getDecoder(msg);
        decoder.beginRead(mdi);
        while (decoder.nextField()) {

            final NonStaticFieldInfo info = decoder.getField();
            Object value = reader.readField(info.getType(), decoder);
            values.put(info.getName(), value);

        }
        return values;
    }

    public static void writeValue(final FixedUnboundEncoder encoder,
                                  final NonStaticFieldInfo info,
                                  final Object value) {
        try {
            if (value == null)
                encoder.writeNull();
            else {
                final DataType type = info.getType();
                if (type instanceof FloatDataType) {
                    if (((FloatDataType) type).isFloat()) {
                        encoder.writeFloat((Float) value);
                    } else {
                        encoder.writeDouble((Double) value);
                    }
                } else if (type instanceof IntegerDataType) {
                    switch (((IntegerDataType) type).getSize()) {
                        case 1: {
                            encoder.writeInt(((Number) value).byteValue());
                        }
                        break;

                        case 2: {
                            encoder.writeInt(((Number) value).shortValue());
                        }
                        break;

                        case 4:
                        case IntegerDataType.PACKED_UNSIGNED_INT:
                        case IntegerDataType.PACKED_INTERVAL: {
                            encoder.writeInt(((Number) value).intValue());
                        }
                        break;
                        default: {
                            encoder.writeLong((Long) value);
                        }
                    }
                } else if (type instanceof EnumDataType) {
                    encoder.writeString((String) value);
                } else if (type instanceof VarcharDataType) {
                    encoder.writeString((String) value);
                } else if (type instanceof BooleanDataType) {
                    encoder.writeBoolean((Boolean) value);
                } else if (type instanceof CharDataType) {
                    encoder.writeString(String.valueOf(value));
                } else if (type instanceof DateTimeDataType) {
                    encoder.writeLong((Long) value);
                } else if (type instanceof TimeOfDayDataType) {
                    encoder.writeInt((Integer) value);
                } else if (type instanceof ArrayDataType) {
                    writeArray(encoder, (ArrayDataType) type, (Object[]) value);
                } else if (type instanceof ClassDataType) {
                    writeObject(encoder, (ClassDataType) type, value);
                } else if (type instanceof BinaryDataType) {
                    byte[] data = (byte[]) value;
                    encoder.writeBinary(data, 0, data.length);
                }
                else
                    encoder.writeString(String.valueOf(value));
            }
        } catch (final Throwable x) {
            throw new IllegalArgumentException("Can not write value to the field '" + info.getName() +
                "'. Reason: " + x.getLocalizedMessage(),
                x);
        }
    }

    private static void writeArray(WritableValue uenc, ArrayDataType type, Object[] values) {
        final DataType dataType = type.getElementDataType();
        if (dataType instanceof ClassDataType) {
            final int len = values.length;
            uenc.setArrayLength(len);

            for (int i = 0; i < len; i++) {
                if (values[i] instanceof HashMap) {
                    Object descriptor = ((HashMap) values[i]).get(OBJECT_CLASS_NAME);
                    if (descriptor instanceof RecordClassDescriptor) {
                        UnboundEncoder encoder = uenc.nextWritableElement().getFieldEncoder((RecordClassDescriptor) descriptor);
                        if (encoder instanceof FixedUnboundEncoder) {
                            while (encoder.nextField()) {
                                NonStaticFieldInfo field = encoder.getField();
                                Object value = ((HashMap) values[i]).get(field.getName());

                                if (field.getType() instanceof DateTimeDataType) {
                                    if (value instanceof String)
                                        value = field.getType().parse((String) value);
                                }

                                writeValue((FixedUnboundEncoder) encoder, field, value);
                            }
                        }
                    }
                }
            }
        } else {
            final int len = values.length;
            uenc.setArrayLength(len);
            for (int i = 0; i < len; i++) {
                final Object value = values[i];
                final WritableValue rv = uenc.nextWritableElement();
                if (value != null || type.getElementDataType().isNullable())
                    MessageEncoderUtils.writeField(value, dataType, rv);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeObject(WritableValue uenc, ClassDataType type, Object values) {

        RecordClassDescriptor descriptor = type.getDescriptors()[0];
        if (descriptor != null && values instanceof Map) {
            UnboundEncoder encoder = uenc.getFieldEncoder(descriptor);
            if (encoder instanceof FixedUnboundEncoder) {
                while (encoder.nextField()) {
                    NonStaticFieldInfo field = encoder.getField();
                    Object value = ((Map<String, Object>) values).get(field.getName());

                    if (field.getType() instanceof DateTimeDataType){
                        if (value instanceof String)
                            value = field.getType().parse((String) value);
                    }

                    if (field.getType() instanceof ArrayDataType) {
                        if (value instanceof ArrayList) {
                            value = ((ArrayList) value).toArray();
                        }
                    }

                    writeValue((FixedUnboundEncoder) encoder, field, value);
                }
            }
        }
    }

    public final Object getValue(final RawMessage msg,
                                 final String fieldName,
                                 final Object notFoundValue) {

        try {
            final Object obj = getValue(msg, fieldName);
            return obj == null ? notFoundValue : obj;
        } catch (final KeyNotFoundException e) {
            return notFoundValue;
        }
    }

    public Object getValue(final RawMessage msg, final String fieldName)
        throws KeyNotFoundException {

        final UnboundDecoder decoder = getDecoder(msg);
        return getValue(msg, fieldName, decoder);
    }

    private Object getValue(final RawMessage msg,
                            final String fieldName,
                            final UnboundDecoder decoder)
        throws KeyNotFoundException {

        if (msg.data == null)
            return null;

        mdi.setBytes(msg.data, msg.offset, msg.length);

        decoder.beginRead(mdi);
        while (decoder.nextField()) {
            final NonStaticFieldInfo info = decoder.getField();

            if (fieldName.equals(info.getName()))
                return reader.readField(info.getType(), decoder);
        }

        throw new KeyNotFoundException(fieldName);
    }

    private UnboundDecoder          getDecoder(final RawMessage msg) {
        final String guid = msg.type.getGuid();
        UnboundDecoder decoder = _decoders.get(guid,
            null);
        if (decoder == null) {
            decoder = InterpretingCodecMetaFactory.INSTANCE.createFixedUnboundDecoderFactory(msg.type).create();
            _decoders.put(guid,
                decoder);
        }
        return decoder;
    }

    private FixedUnboundEncoder     getEncoder(final RawMessage msg) {
        final String guid = msg.type.getGuid();
        FixedUnboundEncoder encoder = _encoders.get(guid, null);
        if (encoder == null) {
            encoder = CompiledCodecMetaFactory.INSTANCE.createFixedUnboundEncoderFactory(msg.type).create();
            _encoders.put(guid, encoder);
        }
        return encoder;
    }

//    @SuppressWarnings("unchecked")
//    private Object[] readArray(ArrayDataType type, ReadableValue udec) throws NullValueException {
//        AbstractList<Object> objList = (AbstractList<Object>) MessageDecoderUtils.readField(type, udec);
//        return objList != null ? objList.toArray(new Object[objList.size()]) : null;
//    }

}