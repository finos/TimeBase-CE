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
package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.RawDecoder;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;

public class RawMessageHelper {

    public CodecMetaFactory         factory = InterpretingCodecMetaFactory.INSTANCE;

    private final MemoryDataInput   input = new MemoryDataInput ();
    private final MemoryDataOutput  output = new MemoryDataOutput ();

    private final ObjectToObjectHashMap<String, FixedUnboundEncoder> encoders = new ObjectToObjectHashMap<String, FixedUnboundEncoder> ();
    private final ObjectToObjectHashMap<String, UnboundDecoder> decoders = new ObjectToObjectHashMap<String, UnboundDecoder> ();

    private final RawDecoder rawDecoder = new RawDecoder();

    public void                 setValues (RawMessage msg, Map<String, Object> values) {
        if (values == null)
            return;

        output.reset ();

        FixedUnboundEncoder encoder = getEncoder (msg.type);
        encoder.beginWrite (output);

        while (encoder.nextField ()) {
            NonStaticFieldInfo info = encoder.getField ();
            setValue(encoder, info, values.get(info.getName()));
        }

        msg.copyBytes (output, 0);
    }

    public Map<String, Object>  getValues (RawMessage msg) {
        final HashMap<String, Object> values = new HashMap<String, Object> ();

        if (msg.data == null)
            return null;

        final UnboundDecoder decoder = getDecoder (msg.type);
        input.setBytes (msg.data, msg.offset, msg.length);

        decoder.beginRead (input);
        while (decoder.nextField ()) {

            final NonStaticFieldInfo info = decoder.getField();

            Object value = rawDecoder.readField(info.getType(), decoder);
            values.put(info.getName(), value);
        }

        return values;
    }

    public static void setValue (UnboundEncoder encoder, NonStaticFieldInfo info, Object value) {
        try {
            if (value == null)
                encoder.writeNull ();
            else {
                final DataType type = info.getType ();

                if (type instanceof FloatDataType) {
                    FloatDataType dataType = (FloatDataType) type;

                    if (dataType.isFloat ())
                        encoder.writeFloat ((Float) value);
                    else if (dataType.isDecimal64()) {
                        if (value instanceof Long)
                            encoder.writeLong((Long)value);
                        else
                            encoder.writeLong(Decimal64Utils.fromDouble((Double) value));
                    } else {
                        encoder.writeDouble((Double) value);
                    }
                }
                else if (type instanceof IntegerDataType) {
                    switch (((IntegerDataType) type).getSize ()) {
                        case 1:
                            encoder.writeInt ((Byte) value);
                            break;

                        case 2:
                            encoder.writeInt ((Short) value);
                            break;

                        case 4:
                        case IntegerDataType.PACKED_UNSIGNED_INT:
                        case IntegerDataType.PACKED_INTERVAL:
                            encoder.writeInt ((Integer) value);
                            break;

                        default:
                            encoder.writeLong ((Long) value);
                            break;
                    }
                } else if (type instanceof EnumDataType)
                    encoder.writeString((String) value);
                else if (type instanceof VarcharDataType)
                    encoder.writeString((String) value);
                else if (type instanceof BooleanDataType)
                    encoder.writeBoolean((Boolean) value);
                else if (type instanceof CharDataType)
                    encoder.writeString(String.valueOf(value));
                else if (type instanceof DateTimeDataType)
                    encoder.writeLong((Long) value);
                else if (type instanceof TimeOfDayDataType)
                    encoder.writeInt((Integer) value);
                else if (type instanceof BinaryDataType) {
                    byte[] bytes = (byte[]) value;
                    encoder.writeBinary(bytes, 0, bytes.length);
                } else if (type instanceof ArrayDataType) {
                     writeArray((Object[])value, (ArrayDataType)type, encoder);
                } else if (type instanceof ClassDataType) {
                    Map<String, Object> v = (Map<String, Object>) value;
                    RecordClassDescriptor objectType = matchObjectType(v, (ClassDataType) type);
                    UnboundEncoder innerEncoder = encoder.getFieldEncoder(objectType);
                    while (innerEncoder.nextField()) {
                        NonStaticFieldInfo fieldInfo = innerEncoder.getField ();
                        setValue(innerEncoder, fieldInfo, v.get(fieldInfo.getName()));
                    }
                } else {
                    encoder.writeString(String.valueOf(value));
                }
            }
        } catch (final Throwable x) {
            throw new IllegalArgumentException ("Can not write value to the field '" + info.getName () +
                                                        "'. Reason: " + x.getLocalizedMessage (),
                                                x);
        }
    }

    private static RecordClassDescriptor matchObjectType(Map<String, Object> value, ClassDataType type) {
        return  matchObjectType(value, type.getDescriptors());
    }

    private static RecordClassDescriptor matchObjectType(Map<String, Object> value, RecordClassDescriptor[] rcds) {
        Object name = value.get("type");

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

    public static Object parseValue(DataType type, String s) {
        if (s == null)
            return (null);
        try {
            if (type instanceof FloatDataType) {
                if   (((FloatDataType) type).isFloat())
                    return Float.valueOf(s);
                else
                    return Double.valueOf(s);
            }else if (type instanceof IntegerDataType) {
                switch (((IntegerDataType) type).getSize()) {
                    case 1:
                        return Byte.valueOf(s);
                    case 2:
                        return Short.valueOf(s);
                    case 4:
                    case IntegerDataType.PACKED_UNSIGNED_INT:
                    case IntegerDataType.PACKED_INTERVAL:
                        return Integer.valueOf(s);
                    default:
                        return Long.valueOf(s);
                }
            } else if (type instanceof EnumDataType) {
                return s;
            } else if (type instanceof VarcharDataType) {
                return s;
            } else if (type instanceof BooleanDataType) {
                return Boolean.valueOf(s);
            } else if (type instanceof CharDataType) {
                return s;
            } else if (type instanceof DateTimeDataType) {
                return Long.valueOf(s);
            } else if (type instanceof TimeOfDayDataType) {
                return Integer.valueOf(s);
            } else if (type instanceof ArrayDataType) {
                return parseArray(s);
            } else if (type instanceof ClassDataType) {
                //TODO: @LEGACY
                throw new IllegalArgumentException();
            } else {
                return s;
            }
        } catch (final Throwable x) {
            throw new IllegalArgumentException("Can not convert String object " + s +" to DataType " + type.getBaseName() +
                    ". Reason: " + x.getLocalizedMessage(),
                    x);
        }
    }

    private static Object[] parseArray (String s){

        StringBuilder strBuilder = new StringBuilder(s);
        if ('[' == strBuilder.charAt(0)){
            strBuilder.deleteCharAt(0);
        }
        if (strBuilder.charAt(strBuilder.length() -1) == ']'){
            strBuilder.deleteCharAt(strBuilder.length() -1);
        }
        String[] numbers = strBuilder.toString().split(",");
        Object[] array = new Object[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            array[i] = Double.parseDouble(numbers[i]);
        }

        return array;
    }

    private static void writeArray(Object[] values, ArrayDataType type, WritableValue uenc) {
        final int len = values.length;
        uenc.setArrayLength(len);

        for (int i = 0; i < len; i++) {
            final double v = (Double)values[i];
            if (Double.isNaN(v) && type.getElementDataType().isNullable())
                continue;

            final WritableValue wv = uenc.nextWritableElement();
            wv.writeDouble(v);
        }
    }

    public Object               getValue (RawMessage msg, String field, Object notFoundValue) {
        try {
            Object obj = getValue (msg, field);
            return obj == null ? notFoundValue : obj;
        } catch (FieldNotFoundException e) {
            return notFoundValue;
        }
    }

    public Object               getValue (RawMessage msg, String field)
        throws FieldNotFoundException
    {
        if (msg.data == null)
            return null;

        final UnboundDecoder decoder = getDecoder (msg.type);

        input.setBytes (msg.data, msg.offset, msg.length);
        decoder.beginRead (input);

        while (decoder.nextField ()) {
            final NonStaticFieldInfo info = decoder.getField();

            if (field.equals(info.getName()))
                return rawDecoder.readField(info.getType(), decoder);
        }

        throw new FieldNotFoundException (field);
    }

    private UnboundDecoder          getDecoder (final RecordClassDescriptor type) {
        String guid = type.getGuid ();
        UnboundDecoder decoder = decoders.get (guid, null);

        if (decoder == null) {
            decoder = factory.createFixedUnboundDecoderFactory (type).create ();
            decoders.put (guid, decoder);
        }
        return decoder;
    }

    private FixedUnboundEncoder     getEncoder (final RecordClassDescriptor type) {
        String guid = type.getGuid ();
        FixedUnboundEncoder encoder = encoders.get (guid, null);

        if (encoder == null) {
            encoder = factory.createFixedUnboundEncoderFactory (type).create ();
            encoders.put (guid, encoder);
        }
        return encoder;
    }

    /**
     * Checks whether message is accepted with given parameters.<br>
     * <p>
     * Example. We've got 3 messages: <i>1) {a:1,b:2,c:4}, 2) {a:1,b:4,c:3}, 3) {a:1,c:3,d:5}</i>.<br>
     * Also we've got params: <i>{a:[1], b:[1,2,3]}</i>.
     * <ul>
     * <li>1st message will be accepted, cause a=1 and b=2.</li>
     * <li>2nd message won't be accepted, cause a=1, but b=4.</li>
     * <li>3rd message won't be accepted, cause it doesn't contain field b.</li>
     * </ul>
     *
     * @param msg    raw message
     * @param params map with params in String form
     */
    public boolean                  isAccepted (RawMessage msg, Map<String, ? extends Iterable<String>> params) {
        if (msg.data == null)
            return false;

        if (!isSymbolAccepted(msg, params) || !isTimestampAccepted(msg, params) || !isTypeAccepted(msg, params)) {
            return false;
        }

        int count = countSpecial(params);

        final UnboundDecoder decoder = getDecoder(msg.type);
        input.setBytes(msg.data, msg.offset, msg.length);

        decoder.beginRead(input);

        fields_loop:
        while (decoder.nextField()) {

            final NonStaticFieldInfo info = decoder.getField();
            if (!params.containsKey(info.getName())) {
                continue;
            }
            count++;
            Iterable<String> values = params.get(info.getName());
            Object value = rawDecoder.readField(info.getType(), decoder);
            for (String v : values) {
                Object parsed;
                try {
                    parsed = parseValue(info.getType(), v);
                } catch (IllegalArgumentException exc) {
                    continue;
                }
                if (Objects.equals(parsed, value)) {
                    continue fields_loop;
                }
            }
            return false;
        }

        return count == params.size();
    }

    public boolean                  isAcceptedObjects (RawMessage msg, Map<String, ? extends Iterable<?>> params) {
        if (msg.data == null)
            return false;

        if (!isSymbolAcceptedObject(msg, params) ||
                !isTimestampAcceptedObject(msg, params) || !isTypeAcceptedObject(msg, params)) {
            return false;
        }

        int count = countSpecial(params);

        final UnboundDecoder decoder = getDecoder(msg.type);
        input.setBytes(msg.data, msg.offset, msg.length);

        decoder.beginRead(input);

        fields_loop:
        while (decoder.nextField()) {

            final NonStaticFieldInfo info = decoder.getField();
            if (!params.containsKey(info.getName())) {
                continue;
            }
            count++;
            Iterable<?> values = params.get(info.getName());
            Object value = rawDecoder.readField(info.getType(), decoder);
            for (Object v : values) {
                if (Objects.equals(v, value)) {
                    continue fields_loop;
                }
            }
            return false;
        }

        return count == params.size();
    }

    public Collection<RawMessage> filter(Collection<RawMessage> messages, Map<String, ? extends Iterable<String>> params) {
        return messages.stream().filter(x -> isAccepted(x, params))
                .collect(Collector.of(ObjectArrayList::new, ObjectArrayList::add, (l1, l2) -> {l1.addAll(l2); return l2;}));
    }

    public Collection<RawMessage> filterObjects(Collection<RawMessage> messages, Map<String, ? extends Iterable<?>> params) {
        return messages.stream().filter(x -> isAcceptedObjects(x, params))
                .collect(Collector.of(ObjectArrayList::new, ObjectArrayList::add, (l1, l2) -> {l1.addAll(l2); return l2;}));
    }

    private int                     countSpecial (Map<String, ? extends Iterable<?>> params) {
        int result = 0;

        if (params.containsKey("symbol"))
            result++;

        if (params.containsKey("instrumentType"))
            result++;

        if (params.containsKey("timestamp"))
            result++;

        return result;
    }

    private boolean                 isSymbolAccepted (RawMessage msg, Map<String, ? extends Iterable<String>> params) {
        Iterable<String> symbols = params.get("symbol");
        if (symbols == null)
            return true;
        String symbol = msg.getSymbol().toString();
        for (String s : symbols) {
            if (s.equals(symbol)) {
                return true;
            }
        }
        return false;
    }

    private boolean                 isSymbolAcceptedObject(RawMessage msg, Map<String, ? extends Iterable<?>> params) {
        Iterable<?> symbols = params.get("symbol");
        if (symbols == null)
            return true;
        CharSequence symbol = msg.getSymbol();
        for (Object o : symbols) {
            if (Objects.equals(o, symbol)) {
                return true;
            }
        }
        return false;
    }

//    private boolean                 isInstrumentTypeAccepted(RawMessage msg, Map<String, ? extends Iterable<String>> params) {
//        Iterable<String> instrumentTypes = params.get("instrumentType");
//        if (instrumentTypes == null)
//            return true;
//        InstrumentType instrumentType = msg.getInstrumentType();
//        for (String type : instrumentTypes) {
//            InstrumentType iType;
//            try {
//                iType = InstrumentType.valueOf(type);
//            } catch (IllegalArgumentException exc) {
//                continue;
//            }
//            if (iType == instrumentType) {
//                return true;
//            }
//        }
//        return false;
//    }

//    private boolean                 isInstrumentTypeAcceptedObject(RawMessage msg, Map<String, ? extends Iterable<?>> params) {
//        Iterable<?> instrumentTypes = params.get("instrumentType");
//        if (instrumentTypes == null)
//            return true;
//        InstrumentType instrumentType = msg.getInstrumentType();
//        for (Object type : instrumentTypes) {
//            if (Objects.equals(type, instrumentType)) {
//                return true;
//            }
//        }
//        return false;
//    }

    @VisibleForTesting
    boolean                 isTypeAccepted(RawMessage msg, Map<String, ? extends Iterable<String>> params) {
        Iterable<String> types = params.get("type");
        if (types == null)
            return true;
        String typeName = msg.type.getName();
        String shortTypeName = typeName.substring(typeName.lastIndexOf('.') + 1);
        for (String type : types) {
            String shortType = type.substring(type.lastIndexOf('.') + 1);
            if (shortType.equals(shortTypeName) || type.equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    private boolean                 isTypeAcceptedObject(RawMessage msg, Map<String, ? extends Iterable<?>> params) {
        Iterable<?> types = params.get("type");
        if (types == null)
            return true;
        String typeName = msg.type.getName();
        String shortTypeName = typeName.substring(typeName.lastIndexOf('.') + 1);
        for (Object typeObject : types) {
            if (typeObject instanceof CharSequence) {
                String type = ((CharSequence) typeObject).toString();
                String shortType = type.substring(type.lastIndexOf('.') + 1);
                if (shortType.equals(shortTypeName) || type.equals(typeName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean                 isTimestampAccepted(RawMessage msg, Map<String, ? extends Iterable<String>> params) {
        Iterable<String> timestamps = params.get("timestamp");
        if (timestamps == null)
            return true;
        long timestamp = msg.getTimeStampMs();
        for (String s : timestamps) {
            long t;
            try {
                t = Long.valueOf(s);
            } catch (NumberFormatException exc) {
                continue;
            }
            if (timestamp == t) {
                return true;
            }
        }
        return false;
    }

    private boolean                 isTimestampAcceptedObject(RawMessage msg, Map<String, ? extends Iterable<?>> params) {
        Iterable<?> timestamps = params.get("timestamp");
        if (timestamps == null)
            return true;
        long timestamp = msg.getTimeStampMs();
        for (Object t : timestamps) {
            if (Objects.equals(t, timestamp)) {
                return true;
            }
        }
        return false;
    }

}