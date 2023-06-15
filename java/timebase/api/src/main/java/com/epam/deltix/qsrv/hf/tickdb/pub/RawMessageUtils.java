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


import com.epam.deltix.qsrv.hf.pub.NullValueException;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.memory.MemoryDataInput;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.time.GMT;
import com.epam.deltix.util.time.TimeFormatter;

import java.text.ParseException;
import java.util.Map;

public abstract class RawMessageUtils {

    public CodecMetaFactory factory = CompiledCodecMetaFactory.INSTANCE;

    protected final MemoryDataInput input = new MemoryDataInput ();
    protected final MemoryDataOutput output = new MemoryDataOutput ();

    protected final ObjectToObjectHashMap<String, FixedUnboundEncoder> encoders = new ObjectToObjectHashMap<String, FixedUnboundEncoder> ();
    protected final ObjectToObjectHashMap<String, UnboundDecoder> decoders = new ObjectToObjectHashMap<String, UnboundDecoder> ();


    /*
     *  Decoder
     */
    public Object decodeValue (ReadableValue decoder, DataType type) {
        try {
            if (type instanceof FloatDataType) {
                if (((FloatDataType) type).isFloat ())
                    return decoder.getFloat ();
                else
                    return decoder.getDouble ();
            }
            else if (type instanceof IntegerDataType) {
                switch (((IntegerDataType) type).getSize ()) {
                    case 1:
                        return (byte) decoder.getInt ();

                    case 2:
                        return (short) decoder.getInt ();

                    case 4:
                    case IntegerDataType.PACKED_UNSIGNED_INT:
                    case IntegerDataType.PACKED_INTERVAL:
                        return decoder.getInt();

                    default:
                        return decoder.getLong ();
                }
            }
            else if (type instanceof EnumDataType)
                return decoder.getString ();
            else if (type instanceof VarcharDataType)
                return decoder.getString ();
            else if (type instanceof BooleanDataType)
                return decoder.getBoolean ();
            else if (type instanceof CharDataType)
                return decoder.getString ();
            else if (type instanceof DateTimeDataType)
                return decoder.getLong ();
            else if (type instanceof TimeOfDayDataType)
               return decoder.getInt ();
            else if (type instanceof BinaryDataType) {
                return decodeBinary(decoder,type);
            }else if( type instanceof ClassDataType){
                return decodeClass(decoder, type);
            }else if( type instanceof  ArrayDataType){
                return decodeArray(decoder, (ArrayDataType) type);
            }
            else
                throw new IllegalArgumentException (type.toString ());

        } catch (final NullValueException nve) {
            return null;
        }
    }

    protected abstract Object decodeBinary(final ReadableValue decoder,final DataType type);
    protected abstract Object decodeClass(final ReadableValue decoder,final DataType type);
    protected abstract Object decodeArray(final ReadableValue decoder,final ArrayDataType type);

    /*
     *  Encoder
     */
    public void encodeValue (final WritableValue encoder,final NonStaticFieldInfo info,final Object value) {
        try {
            if (value == null) {
                encoder.writeNull();
            }
            else {
                final DataType type = info.getType ();
                encodeValue(encoder, type, value);
            }
        } catch (final Throwable x) {
            throw new IllegalArgumentException ("Can not write value to the field '" + info.getName () +
                    "'. Reason: " + x.getLocalizedMessage (),
                    x);
        }
    }
    public  void encodeValue (final WritableValue encoder,final DataType type,final Object value) {
        if (type instanceof FloatDataType) {
            if (((FloatDataType) type).isFloat ())
                encoder.writeFloat (((Number) value).floatValue());
            else
                encoder.writeDouble (((Number) value).doubleValue());
        }
        else if (type instanceof IntegerDataType) {
            switch (((IntegerDataType) type).getSize ()) {
                case 1:
                    encoder.writeInt (((Number) value).byteValue());
                    break;

                case 2:
                    encoder.writeInt (((Number) value).shortValue());
                    break;

                case 4:
                case IntegerDataType.PACKED_UNSIGNED_INT:
                case IntegerDataType.PACKED_INTERVAL:
                    encoder.writeInt (((Number) value).intValue());
                    break;

                default:
                    encoder.writeLong (((Number) value).longValue());
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
        else if (type instanceof DateTimeDataType) {
            if( value instanceof Number)
                encoder.writeLong(((Number) value).longValue());
            else if (value instanceof String){
                try {
                    encoder.writeLong((GMT.parseDateTimeMillis((String)value)).getTime());
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }else if (type instanceof TimeOfDayDataType) {
            if( value instanceof Number)
                encoder.writeInt(((Number) value).intValue());
            else if (value instanceof String)
                encoder.writeInt(TimeFormatter.parseTimeOfDayMillis((String)value));
        }else if (type instanceof BinaryDataType) {
            encodeBinary(value, encoder);
        } else if (type instanceof ArrayDataType) {
            encodeArray(value, (ArrayDataType)type, encoder);
        } else if (type instanceof ClassDataType){
            encodeClass(value, (ClassDataType)type, encoder);
        }else
            throw new IllegalArgumentException ("Unsupported type " + type.toString());
    }

    protected abstract void encodeBinary(final Object value, final WritableValue encoder);

    protected abstract void encodeArray(final Object values,final ArrayDataType type,final WritableValue encoder);

    protected abstract void encodeClass(final Object values,final ClassDataType type,final WritableValue encoder);



    protected UnboundDecoder          getDecoder (final RecordClassDescriptor type) {
        String guid = type.getGuid ();
        UnboundDecoder decoder = decoders.get (guid, null);

        if (decoder == null) {
            decoder = factory.createFixedUnboundDecoderFactory (type).create ();
            decoders.put (guid, decoder);
        }
        return decoder;
    }

    protected FixedUnboundEncoder     getEncoder (final RecordClassDescriptor type) {
        String guid = type.getGuid ();
        FixedUnboundEncoder encoder = encoders.get (guid, null);

        if (encoder == null) {
            encoder = factory.createFixedUnboundEncoderFactory (type).create ();
            encoders.put (guid, encoder);
        }
        return encoder;
    }


    public static int indexOfType(String obj, RecordClassDescriptor[] classDescriptors) {
        final int length = classDescriptors.length;
        switch (length) {
            default:
                for (int code = length - 1; code >= 7; code--)
                    if (obj.equals(classDescriptors [code].getName()))
                        return (code);

            case 7:     if (obj.equals(classDescriptors[6].getName())) return (6);  // else fall-through
            case 6:     if (obj.equals(classDescriptors[5].getName())) return (5);  // else fall-through
            case 5:     if (obj.equals(classDescriptors[4].getName())) return (4);  // else fall-through
            case 4:     if (obj.equals(classDescriptors[3].getName())) return (3);  // else fall-through
            case 3:     if (obj.equals(classDescriptors[2].getName())) return (2);  // else fall-through
            case 2:     if (obj.equals(classDescriptors[1].getName())) return (1);  // else fall-through
            case 1:     if (obj.equals(classDescriptors[0].getName())) return (0);  // else fall-through
            case 0:     break;
        }
        return -1;
    }

}