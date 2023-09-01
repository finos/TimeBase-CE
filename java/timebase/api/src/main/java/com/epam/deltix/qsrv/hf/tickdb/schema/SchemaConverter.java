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
package com.epam.deltix.qsrv.hf.tickdb.schema;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.qsrv.hf.tickdb.schema.encoders.MixedWritableValue;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.collections.SmallArrays;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.memory.MemoryDataOutput;
import com.epam.deltix.util.memory.MemoryDataInput;

import java.util.*;

public class SchemaConverter {
    private final MemoryDataOutput  buffer = new MemoryDataOutput();
    private final MemoryDataInput   input = new MemoryDataInput();

    private final RawMessage        result = new RawMessage();

    private final MetaDataChange    change;

    private byte[] tmp;

    private final ObjectToObjectHashMap<RecordClassDescriptor, ClassDescriptorMapping> typeMappings = new
            ObjectToObjectHashMap<>();

    public SchemaConverter(MetaDataChange change) {
        this.change = change;        
        
        // create mappings
        RecordClassDescriptor[] input = change.source.getContentClasses();

        // create mapping for each source type
        for (RecordClassDescriptor source : input) {
            ClassDescriptorChange typeChange = change.getChange(source, null);
            if (typeChange != null) {
                if (typeChange.getSource() != null && typeChange.getTarget() != null) {
                    typeMappings.put(source, new ClassDescriptorMapping(typeChange, change.mapping));
                }
            } else {
                RecordClassDescriptor target = (RecordClassDescriptor)
                    change.mapping.findClassDescriptor(source, change.target);

                if (target != null)
                    typeMappings.put(source, new ClassDescriptorMapping(source, target, change.mapping));
            }
        }

        // make sure that we do not miss additional changes
        for (ClassDescriptorChange typeChange : change.changes) {

            if (typeChange.getSource() instanceof RecordClassDescriptor) {
                RecordClassDescriptor source = (RecordClassDescriptor)typeChange.getSource();

                if (!typeMappings.containsKey(source))
                    typeMappings.put(source, typeChange.getTarget() != null ?
                            new ClassDescriptorMapping(typeChange, change.mapping) : new ClassDescriptorMapping(source));
            }
        }
    }

    public boolean canConvert() {
        return canConvert(change);
    }
    
    public static boolean canConvert(MetaDataChange change) {
        ArrayList input =
                new ArrayList<RecordClassDescriptor>(Arrays.asList(change.source.getContentClasses()));

        for (ClassDescriptorChange c : change.changes) {
            if (c.getSource() != null && c.getTarget() != null)
                return true;
            else if (c.getSource() != null)
                input.remove(c.getSource());
        }

        return input.size() != 0;
    }

    public final RawMessage convert(RawMessage msg) {
        ClassDescriptorMapping mapping = typeMappings.get(msg.type, null);
        
        if (mapping != null && mapping.isValid()) {

            if (mapping.decoder == null)
                throw new IllegalStateException("Decoder for " + msg.type + " is not defined.");

            result.type = mapping.target;
            result.setSymbol(msg.getSymbol());
            result.setNanoTime(msg.getNanoTime());

            if (mapping.hasChanges) {
            buffer.reset(0);
            mapping.encoder.beginWrite(buffer);

            msg.setUpMemoryDataInput(input);
            mapping.decoder.beginRead(input);

            for (FieldMapping fieldMapping : mapping.mappings) {
                mapping.encoder.nextField();

                convertField(fieldMapping, mapping.decoder, mapping.encoder);
            }

            mapping.encoder.endWrite();
            result.setBytes(buffer, 0);
            } else {
                result.setBytes(msg.data, msg.offset, msg.length);
            }

            return result;
        }

        return null;
    }

    private boolean convertObject(ReadableValue reader, ClassDataType inType, MixedWritableValue writable, ClassDataType outType) {

        UnboundDecoder decoder = reader.getFieldDecoder();

        RecordClassDescriptor descriptor = decoder.getClassInfo().getDescriptor();

        ClassDescriptorMapping mapping = typeMappings.get(descriptor, null);

        if (mapping != null) {

            if (mapping.target != null) {
                UnboundEncoder encoder = writable.getFieldEncoder(mapping.target);

                for (FieldMapping fieldMapping : mapping.mappings) {
                    encoder.nextField();
                    convertField(fieldMapping, decoder, encoder);
                }
                encoder.endWrite();
            } else {
                return false;
            }

            return true;
        } else { // type is not changed - copy "as is"

            int index = SmallArrays.indexOf(descriptor, inType.getDescriptors());

            UnboundEncoder encoder = writable.getFieldEncoder(outType.getDescriptors()[index]);
            MixedWritableValue out = writable.clone(encoder);

            while (decoder.nextField()) {
                encoder.nextField();

                convertField(decoder, decoder.getField().getType(), null, out, encoder.getField().getType(), null);
            }
            encoder.endWrite();

            return true;
        }
    }

    public void convert(MessageSource<InstrumentMessage> in, MessageChannel<InstrumentMessage> out) {
        while (in.next()) {
            RawMessage msg = (RawMessage) in.getMessage();
            RawMessage result = convert(msg);
            if (result != null)
                out.send(result);
        }
    }

    private void convertField(FieldMapping mapping, UnboundDecoder decoder, UnboundEncoder encoder) {
        MixedWritableValue writable = mapping.getWritable(encoder);

        if (mapping.source == null) {
            // field may be ignored
            writable.writeDefault();
        } else {
            decoder.seekField(mapping.sourceIndex);
            convertField(decoder, mapping.sourceType, mapping.sourceTypeIndex, writable, mapping.targetType, mapping.targetTypeIndex);
        }
    }

    private static boolean isDecimal64(DataType type) {
        return ((type instanceof FloatDataType) && ((FloatDataType)type).isDecimal64());
    }

    private boolean convertField(ReadableValue in, DataType inType, DataTypeIndex inTypeIndex,
                              MixedWritableValue out, DataType outType, DataTypeIndex outTypeIndex) {

        DataTypeIndex from = inTypeIndex == null ? FieldMapping.getTypeIndex(inType) : inTypeIndex;
        DataTypeIndex to = outTypeIndex == null ? FieldMapping.getTypeIndex(outType) : outTypeIndex;

        try {
            switch (from) {
                case Boolean:
                    boolean bValue = in.getBoolean();
                    switch (to) {
                        case Boolean: out.writeBoolean(bValue); break;
                        case Int: out.writeInt(bValue ? 1 : 0); break;
                        case Long: out.writeLong(bValue ? 1 : 0); break;
                        case Float: out.writeFloat(bValue ? 1.0f : 0.0f); break;
                        case Double: out.writeDouble(bValue ? 1.0 : 0.0); break;
                        case String: out.writeString(String.valueOf(bValue)); break;
                        case Enum: out.writeEnum(String.valueOf(bValue)); break;
                    }
                    break;

                case Int:
                    int iValue = in.getInt();
                    switch (to) {
                        case Boolean: out.writeBoolean(iValue != 0); break;
                        case Int: out.writeInt(iValue); break;
                        case Long: out.writeLong(iValue); break;
                        case Float: out.writeFloat(iValue); break;
                        case Double: out.writeDouble(iValue); break;
                        case String: out.writeString(String.valueOf(iValue)); break;
                        case Enum: out.writeEnum(String.valueOf(iValue)); break;
                    }
                    break;

                case Long:
                    long lValue = in.getLong();
                    switch (to) {
                        case Boolean: out.writeBoolean(lValue != 0); break;
                        case Int: out.writeInt(lValue); break;
                        case Long: out.writeLong(lValue); break;

                        case Float: {
                            if (isDecimal64(inType))
                                out.writeFloat(Decimal64Utils.toDouble(lValue));
                           else
                                out.writeFloat(lValue);
                            break;
                        }
                        case Double: {
                            if (isDecimal64(inType))
                                out.writeDouble(Decimal64Utils.toDouble(lValue));
                            else
                                out.writeDouble(lValue);
                            break;
                        }
                        case String: out.writeString(String.valueOf(lValue)); break;
                        case Enum: out.writeEnum(String.valueOf(lValue)); break;
                    }
                    break;

                case Float:
                    float fValue = in.getFloat();
                    switch (to) {
                        case Boolean: out.writeBoolean(fValue != 0); break;
                        case Int: out.writeInt(fValue); break;
                        case Long: out.writeLong(fValue); break;
                        case Float: out.writeFloat(fValue); break;
                        case Double: out.writeDouble(fValue); break;
                        case String: out.writeString(String.valueOf(fValue)); break;
                        case Enum: out.writeEnum(String.valueOf(fValue)); break;
                    }
                    break;

                case Double:
                    double dValue = in.getDouble();
                    switch (to) {
                        case Boolean: out.writeBoolean(dValue); break;
                        case Int: out.writeInt(dValue); break;
                        case Long: {
                            if (isDecimal64(outType))
                                out.writeLong(Decimal64Utils.fromDouble(dValue));
                            else
                                out.writeLong(dValue);
                            break;
                        }
                        case Float: out.writeFloat(dValue); break;
                        case Double: out.writeDouble(dValue); break;
                        case String: out.writeString(String.valueOf(dValue)); break;
                        case Enum: out.writeEnum(String.valueOf(dValue)); break;
                    }
                    break;

                case String:
                    String sValue = in.getString();
                    out.writeString(sValue);
                    break;

                case Enum:
                    long enumValue = in.getLong();
                    out.writeLong(change.enumMapping.getMapped(((EnumDataType) inType).getDescriptor(), enumValue, enumValue));
                    break;

                case Array:
                    if (to == DataTypeIndex.Array) {

                        DataType inArrayType = ((ArrayDataType)inType).getElementDataType();
                        DataTypeIndex inArrayTypeIndex = FieldMapping.getTypeIndex(inArrayType);

                        DataType outArrayType = ((ArrayDataType)outType).getElementDataType();
                        DataTypeIndex outArrayTypeIndex = FieldMapping.getTypeIndex(outArrayType);

                        int count = 0;
                        out.setArrayLength(in.getArrayLength());

                        MixedWritableValue element = null;
                        boolean converted = true;

                        for (int i = 0, length = in.getArrayLength(); i < length; i++) {

                            // move to next element when previous was converter only
                            if (converted)
                                element = out.clone(out.nextWritableElement());

                            converted = convertField(
                                    in.nextReadableElement(), inArrayType, inArrayTypeIndex,
                                    element, outArrayType, outArrayTypeIndex
                            );

                            count = converted ? count + 1 : count;
                        }

                        out.setArrayLength(count);

                        // when no elements converted - then just set array length to 0 and call endWrite()
                        if (count == 0) {
                            if (out instanceof UnboundEncoder)
                                ((UnboundEncoder)out).endWrite();
                        }

                    } else {
                        out.writeDefault();
                    }
                    break;

                case Object:
                    if (to == DataTypeIndex.Object)
                        return convertObject(in, (ClassDataType) inType, out, (ClassDataType) outType);
                    else
                        out.writeDefault();
                    break;

                case Binary:
                    if (to == DataTypeIndex.Binary) {
                        int length = in.getBinaryLength();
                        if (tmp == null || tmp.length < length)
                            tmp = new byte[length];

                        in.getBinary(0, length, tmp, 0);
                        out.writeBinary(tmp, 0, length);
                    } else {
                        out.writeDefault();
                    }
                    break;
            }
        } catch (NullValueException e) {
            if (outType.isNullable())
                out.writeNull();
            else
                out.writeDefault();
        }

        return true;
    }

    public enum DataTypeIndex {
        Boolean, Int, Long, Float, Double, Enum, String, Array, Object, Binary
    }
}