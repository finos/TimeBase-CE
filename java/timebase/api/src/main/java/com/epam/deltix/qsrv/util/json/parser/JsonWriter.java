package com.epam.deltix.qsrv.util.json.parser;

import com.epam.deltix.anvil.util.CharSequenceParser;
import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.WritableValue;
import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.util.json.DateFormatter;
import com.epam.deltix.util.collections.generated.*;
import com.epam.deltix.util.memory.MemoryDataOutput;
import org.apache.commons.io.output.StringBuilderWriter;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.Collections;

public class JsonWriter {

    public CodecMetaFactory factory = CompiledCodecMetaFactory.INSTANCE;
    private final MemoryDataOutput output = new MemoryDataOutput();
    private ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    private final JsonPool jsonPool;
    private final DateFormatter dateFormatter = new DateFormatter();

    public JsonWriter(JsonPool pool) {
        this.jsonPool = pool;
    }

    private final ObjectToObjectHashMap<String, FixedUnboundEncoder> encoders = new ObjectToObjectHashMap<String, FixedUnboundEncoder>();

    public void writeValues(RawMessage msg, ObjectToObjectHashMap<String, Object> values) {
        if (values == null)
            return;

        output.reset();

        FixedUnboundEncoder encoder = getEncoder(msg.type);
        encoder.beginWrite(output);

        while (encoder.nextField()) {
            NonStaticFieldInfo info = encoder.getField();
            writeValue(encoder, info, values.get(info.getName(), null));
        }

        msg.copyBytes(output, 0);
    }

    public void writeValue(WritableValue encoder, NonStaticFieldInfo info, Object value) {
        try {
            if (value == null)
                encoder.writeNull();
            else {
                final DataType type = info.getType();

                if (type instanceof FloatDataType)
                    writeFloat((StringBuilderWriter) value, encoder, (FloatDataType) type);
                else if (type instanceof IntegerDataType)
                    writeInteger(value, encoder, (IntegerDataType) type);
                else if (type instanceof EnumDataType)
                    writeEnum((StringBuilderWriter) value, encoder, (EnumDataType) type);
                else if (type instanceof VarcharDataType)
                    writeVarchar((StringBuilderWriter) value, encoder);
                else if (type instanceof BooleanDataType)
                    encoder.writeBoolean((boolean) value);
                else if (type instanceof CharDataType)
                    writeVarchar((StringBuilderWriter) value, encoder);
                else if (type instanceof DateTimeDataType)
                    writeDateTime((StringBuilderWriter) value, encoder);
                else if (type instanceof TimeOfDayDataType)
                    encoder.writeInt(((Number)value).intValue());
                else if (type instanceof BinaryDataType) {
                    writeBinary((LongArrayList) value, encoder);
                } else if (type instanceof ArrayDataType) {
                    writeArray((ArrayDataType) type, encoder, value);
                } else if (type instanceof ClassDataType) {
                    writeObject((ObjectToObjectHashMap<String, Object>) value, encoder, (ClassDataType) type);
                } else {
                    throw new RuntimeException();
                }
            }
        } catch (final Throwable x) {
            throw new IllegalArgumentException("Can not write value to the field '" + info.getName() +
                    "'. Reason: " + x.getLocalizedMessage(),
                    x);
        }
    }

    private void writeInteger(Object value, WritableValue encoder, IntegerDataType type) {
        switch (type.getSize()) {
            case 1:
                encoder.writeInt(((Number) value).byteValue());
                break;
            case 2:
                encoder.writeInt(((Number) value).shortValue());
                break;
            case 4:
            case IntegerDataType.PACKED_UNSIGNED_INT:
            case IntegerDataType.PACKED_INTERVAL:
                encoder.writeInt(((Number) value).intValue());
                break;
            default:
                encoder.writeLong(((Number) value).longValue());
                break;
        }
    }

    private void writeFloat(StringBuilderWriter writer, WritableValue encoder, FloatDataType type) {
        if (type.isFloat()) {
            encoder.writeFloat(CharSequenceParser.parseFloat(writer.getBuilder()));
        } else if (type.isDecimal64()) {
            try {
                encoder.writeLong(Decimal64Utils.fromDouble(CharSequenceParser.parseDouble(writer.getBuilder())));
            } catch (NumberFormatException exc) {
                System.out.println();
            }
        } else {
            encoder.writeDouble(CharSequenceParser.parseDouble(writer.getBuilder()));
        }
        jsonPool.returnToPool(writer);
    }

    private void writeVarchar(StringBuilderWriter writer, WritableValue encoder) {
        encoder.writeString(writer.getBuilder());
        jsonPool.returnToPool(writer);
    }

    private void writeEnum(StringBuilderWriter writer, WritableValue encoder, EnumDataType enumDataType) {
        encoder.writeLong(enumDataType.getDescriptor().stringToLong(writer.getBuilder()));
        jsonPool.returnToPool(writer);
    }

    private void writeBinary(LongArrayList bytes, WritableValue encoder) {
        if (bytes.size() > byteBuffer.array().length) {
            byteBuffer = ByteBuffer.allocate(bytes.size());
        }
        for (int i = 0; i < bytes.size(); i++) {
            byteBuffer.put(i, bytes.get(i).byteValue());
        }
        encoder.writeBinary(byteBuffer.array(), 0, bytes.size());
        jsonPool.returnToPool(bytes);
    }

    private void writeDateTime(StringBuilderWriter writer, WritableValue encoder) throws ParseException {
        encoder.writeLong(dateFormatter.fromDateString(writer.getBuilder().toString()));
        jsonPool.returnToPool(writer);
    }

    private void writeObject(ObjectToObjectHashMap<String, Object> object, WritableValue encoder, ClassDataType type) {
        RecordClassDescriptor rcd = matchObjectType(object, type.getDescriptors());
        UnboundEncoder objectEncoder = encoder.getFieldEncoder(rcd);
        while (objectEncoder.nextField()) {
            writeValue(objectEncoder, objectEncoder.getField(), object.get(objectEncoder.getField().getName(), null));
        }
        jsonPool.returnToPool(object);
    }

    private void writeArray(ArrayDataType type, WritableValue encoder, Object value) throws ParseException {
        DataType elementType = type.getElementDataType();
        if (value.equals(Collections.EMPTY_LIST)) {
            writeEmptyArray(encoder);
        } else if (elementType instanceof IntegerDataType) {
            writeLongArray((LongArrayList) value, (IntegerDataType) elementType, encoder);
        } else if (elementType instanceof FloatDataType) {
            writeFloatArray(value, (FloatDataType) elementType, encoder);
        } else if (elementType instanceof BooleanDataType) {
            writeBooleanArray((BooleanArrayList) value, encoder);
        } else if (elementType instanceof ClassDataType) {
            writeClassArray((ObjectArrayList<Object>) value, encoder, (ClassDataType) elementType);
        } else if (elementType instanceof DateTimeDataType) {
            writeDateTimeArray((ObjectArrayList<Object>) value, encoder);
        } else if (elementType instanceof TimeOfDayDataType) {
            writeTimeOfDayArray((LongArrayList) value, encoder);
        } else if (elementType instanceof VarcharDataType) {
            writeVarcharArray((ObjectArrayList<Object>) value, encoder);
        } else if (elementType instanceof EnumDataType) {
            writeEnumArray((ObjectArrayList<Object>) value, encoder, (EnumDataType) elementType);
        } else {
            throw new RuntimeException(); // toDo
        }
    }

    private void writeEmptyArray(WritableValue encoder) {
        encoder.setArrayLength(0);
    }

    private void writeLongArray(LongArrayList list, IntegerDataType type, WritableValue encoder) {
        encoder.setArrayLength(list.size());
        switch (type.getSize()) {
            case 1:
                for (Long l : list) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeInt(l.byteValue());
                }
                break;
            case 2:
                for (Long l : list) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeInt(l.shortValue());
                }
                break;
            case 4:
            case IntegerDataType.PACKED_UNSIGNED_INT:
            case IntegerDataType.PACKED_INTERVAL:
                for (Long l : list) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeInt(l.intValue());
                }
                break;
            default:
                for (Long l : list) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeLong(l);
                }
                break;
        }
    }

    private void writeFloatArray(Object list, FloatDataType type, WritableValue encoder) {
        if (type.isFloat()) {
            if (list instanceof DoubleArrayList) {
                DoubleArrayList dl = (DoubleArrayList) list;
                encoder.setArrayLength(dl.size());
                for (Double d : dl) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeFloat(d.floatValue());
                }
                jsonPool.returnToPool(dl);
            } else {
                ObjectArrayList<Object> l = (ObjectArrayList<Object>) list;
                encoder.setArrayLength(l.size());
                for (Object o : l) {
                    WritableValue writable = encoder.nextWritableElement();
                    writeFloat((StringBuilderWriter) o, writable, type);
                }
                jsonPool.returnToPool(l);
            }
        } else if (type.isDecimal64()) {
            if (list instanceof DoubleArrayList) {
                DoubleArrayList dl = (DoubleArrayList) list;
                encoder.setArrayLength(dl.size());
                for (Double d : dl) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeLong(Decimal64Utils.fromDouble(d));
                }
                jsonPool.returnToPool(dl);
            } else {
                ObjectArrayList<Object> l = (ObjectArrayList<Object>) list;
                encoder.setArrayLength(l.size());
                for (Object o : l) {
                    WritableValue writable = encoder.nextWritableElement();
                    writeFloat((StringBuilderWriter) o, writable, type);
                }
                jsonPool.returnToPool(l);
            }
        } else {
            if (list instanceof DoubleArrayList) {
                DoubleArrayList dl = (DoubleArrayList) list;
                encoder.setArrayLength(dl.size());
                for (Double d : dl) {
                    WritableValue writable = encoder.nextWritableElement();
                    writable.writeDouble(d);
                }
                jsonPool.returnToPool(dl);
            } else {
                ObjectArrayList<Object> l = (ObjectArrayList<Object>) list;
                encoder.setArrayLength(l.size());
                for (Object o : l) {
                    WritableValue writable = encoder.nextWritableElement();
                    writeFloat((StringBuilderWriter) o, writable, type);
                }
                jsonPool.returnToPool(l);
            }
        }
    }

    private void writeBooleanArray(BooleanArrayList list, WritableValue encoder) {
        encoder.setArrayLength(list.size());
        for (Boolean b : list) {
            WritableValue writable = encoder.nextWritableElement();
            writable.writeBoolean(b);
        }
        jsonPool.returnToPool(list);
    }

    private void writeDateTimeArray(ObjectArrayList<Object> list, WritableValue encoder) throws ParseException {
        encoder.setArrayLength(list.size());
        for (Object o : list) {
            WritableValue writable = encoder.nextWritableElement();
            writeDateTime((StringBuilderWriter) o, writable);
        }
        jsonPool.returnToPool(list);
    }

    private void writeTimeOfDayArray(LongArrayList list, WritableValue encoder) {
        encoder.setArrayLength(list.size());
        for (Long o : list) {
            WritableValue writable = encoder.nextWritableElement();
            writable.writeInt(o.intValue());
        }
        jsonPool.returnToPool(list);
    }

    private void writeVarcharArray(ObjectArrayList<Object> list, WritableValue encoder) {
        encoder.setArrayLength(list.size());
        for (Object o : list) {
            WritableValue writable = encoder.nextWritableElement();
            writeVarchar((StringBuilderWriter) o, writable);
        }
        jsonPool.returnToPool(list);
    }

    private void writeEnumArray(ObjectArrayList<Object> list, WritableValue encoder, EnumDataType type) {
        encoder.setArrayLength(list.size());
        for (Object o : list) {
            WritableValue writable = encoder.nextWritableElement();
            writeEnum((StringBuilderWriter) o, writable, type);
        }
        jsonPool.returnToPool(list);
    }

    private void writeClassArray(ObjectArrayList<Object> list, WritableValue encoder, ClassDataType type) {
        encoder.setArrayLength(list.size());
        for (Object o : list) {
            WritableValue writable = encoder.nextWritableElement();
            writeObject((ObjectToObjectHashMap<String, Object>) o, writable, type);
        }
        jsonPool.returnToPool(list);
    }

    private RecordClassDescriptor    matchObjectType(ObjectToObjectHashMap<String, Object> value, RecordClassDescriptor[] rcds) {
        StringBuilderWriter name = (StringBuilderWriter) value.get("type", null);
        RecordClassDescriptor rcd = null;
        if (name != null) {
            String stringName = name.getBuilder().toString();
            for (RecordClassDescriptor descriptor : rcds) {
                if (descriptor.getName().endsWith(stringName) || descriptor.getGuid().equals(stringName)) {
                    rcd = descriptor;
                    break;
                }
            }
            value.remove("type");
            jsonPool.returnToPool(name);
        } else if (rcds.length == 1) {
            rcd = rcds[0];
        } else {
            throw new IllegalStateException("Undefined object type for the " + value);
        }
        return rcd;
    }

    private FixedUnboundEncoder getEncoder(final RecordClassDescriptor type) {
        String guid = type.getGuid();
        FixedUnboundEncoder encoder = encoders.get(guid, null);

        if (encoder == null) {
            encoder = factory.createFixedUnboundEncoderFactory(type).create();
            encoders.put(guid, encoder);
        }
        return encoder;
    }

    public static void main(String[] args) {
        System.out.println();
    }
}
