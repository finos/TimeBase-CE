package com.epam.deltix.util.parquet;

import com.epam.deltix.dfp.Decimal64Utils;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.RawMessageWriter;
import com.epam.deltix.qsrv.hf.pub.ReadableValue;
import com.epam.deltix.qsrv.hf.pub.codec.NonStaticFieldInfo;
import com.epam.deltix.qsrv.hf.pub.codec.UnboundDecoder;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.util.collections.generated.ObjectToObjectHashMap;
import com.epam.deltix.util.memory.MemoryDataInput;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.MessageType;

import java.io.IOException;
import java.util.HashMap;

public class RawMessageWriteSupport extends WriteSupport<RawMessage> implements RawMessageWriter {

    private static final Log LOGGER = LogFactory.getLog(RawMessageWriteSupport.class);

    private final MessageType schema;
    private final ObjectToObjectHashMap<String, UnboundDecoder> decoders = new ObjectToObjectHashMap<>();
    private final MemoryDataInput buffer = new MemoryDataInput();

    private final boolean fixed;
    private byte[] binaryBuffer = new byte[128];

    private RecordConsumer consumer;

    public RawMessageWriteSupport(String name, RecordClassDescriptor... rcds) {
        this.schema = ParquetUtil.createParquetSchema(name, rcds);
        fixed = rcds.length == 1;
    }

    @Override
    public WriteContext init(Configuration configuration) {
        return new WriteContext(schema, new HashMap<>());
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        consumer = recordConsumer;
    }

    @Override
    public void write(long value) {
        consumer.addLong(value);
    }

    @Override
    public void write(int value) {
        consumer.addInteger(value);
    }

    @Override
    public void write(boolean value) {
        consumer.addBoolean(value);
    }

    @Override
    public void write(float value) {
        consumer.addFloat(value);
    }

    @Override
    public void write(double value) {
        consumer.addDouble(value);
    }

    @Override
    public void write(byte[] value, int length) {
        consumer.addBinary(Binary.fromReusedByteArray(value, 0, length));
    }

    @Override
    public void write(CharSequence value) {
        consumer.addBinary(Binary.fromCharSequence(value));
    }

    @Override
    public void write(CharDataType type, ReadableValue decoder) {
        write(decoder.getChar());
    }

    @Override
    public byte[] getBinaryBuffer() {
        return binaryBuffer;
    }

    @Override
    public void write(FloatDataType type, ReadableValue decoder) {
        if (type.isDecimal64()) {
            write(Decimal64Utils.toDouble(decoder.getLong()));
        } else {
            RawMessageWriter.super.write(type, decoder);
        }
    }

    private void writeGroup(String fieldName, GroupType groupType, UnboundDecoder decoder) {
        if (groupType.containsField(fieldName)) {
            int index = groupType.getFieldIndex(fieldName);
            consumer.startField(fieldName, index);
            consumer.startGroup();

            writeGroupFields(groupType.getType(fieldName).asGroupType(), decoder);

            consumer.endGroup();
            consumer.endField(fieldName, index);
        } else {
            LOGGER.warn()
                    .append("Type '" + fieldName + "' wasn't found in parquet schema. Message dropped.")
                    .commit();
        }

    }

    private void writeGroupFields(GroupType groupType, UnboundDecoder decoder) {

        while (decoder.nextField()) {
            if (decoder.isNull())
                continue;

            NonStaticFieldInfo field = decoder.getField();
            String fieldName = field.getName();

            if (groupType.containsField(fieldName)) {
                DataType fieldType = field.getType();

                if (fieldType instanceof ClassDataType) {
                    int index = groupType.getFieldIndex(fieldName);

                    consumer.startField(fieldName, index);
                    writeObject(fieldName, groupType, decoder, (ClassDataType) fieldType);
                    consumer.endField(fieldName, index);
                } else if (fieldType instanceof ArrayDataType) {
                    writeArray(fieldName, groupType, decoder, (ArrayDataType) fieldType);
                } else {
                    int index = groupType.getFieldIndex(fieldName);
                    consumer.startField(fieldName, index);
                    writePrimitive(decoder, fieldType);
                    consumer.endField(fieldName, index);
                }
            }
        }
    }

    private void writeObject(String fieldName, GroupType groupType, ReadableValue decoder, ClassDataType classDataType) {
        consumer.startGroup();

        UnboundDecoder elementDecoder = decoder.getFieldDecoder();
        if (classDataType.isFixed()) {
            writeGroupFields(groupType.getType(fieldName).asGroupType(), elementDecoder);
        } else {
            String typeName = elementDecoder.getClassInfo().getDescriptor().getName();
            writeGroup(typeName, groupType.getType(fieldName).asGroupType(), elementDecoder);
        }

        consumer.endGroup();
    }

    private void writeArray(String fieldName, GroupType groupType, ReadableValue decoder, ArrayDataType arrayDataType) {
        DataType elementType = arrayDataType.getElementDataType();
        int length = decoder.getArrayLength();

        if (length <= 0)
            return;

        int index = groupType.getFieldIndex(fieldName);
        consumer.startField(fieldName, index);

        if (elementType instanceof ClassDataType) {
            for (int i = 0; i < length; i++) {
                writeObject(fieldName, groupType, decoder.nextReadableElement(), (ClassDataType) elementType);
            }
        } else {
            for (int i = 0; i < length; i++) {
                writePrimitive(decoder.nextReadableElement(), elementType);
            }
        }

        consumer.endField(fieldName, index);
    }

    private void writeHeader(RawMessage msg) {
        String timestamp = "timestamp";
        if (schema.containsField(timestamp)) {
            int tsIndex = schema.getFieldIndex(timestamp);
            consumer.startField(timestamp, tsIndex);
            write(msg.getTimeStampMs());
            consumer.endField(timestamp, tsIndex);
        }

        String nanoTime = "nanoTime";
        if (schema.containsField(nanoTime)) {
            int tsIndex = schema.getFieldIndex(nanoTime);
            consumer.startField(nanoTime, tsIndex);
            write(msg.getNanoTime());
            consumer.endField(nanoTime, tsIndex);
        }

        String symbol = "symbol";
        if (schema.containsField(symbol) && msg.getSymbol() != null) {
            int symbolIndex = schema.getFieldIndex(symbol);
            consumer.startField(symbol, symbolIndex);
            write(msg.getSymbol());
            consumer.endField(symbol, symbolIndex);
        }

//        String instrumentType = "instrumentType";
//        if (schema.containsField(instrumentType) && msg.getInstrumentType() != null) {
//            int instrTypeIndex = schema.getFieldIndex(instrumentType);
//            consumer.startField(instrumentType, instrTypeIndex);
//            write(msg.getInstrumentType().toString());
//            consumer.endField(instrumentType, instrTypeIndex);
//        }
    }

    private void                writeMessageFields(RawMessage msg) {
        UnboundDecoder decoder = getDecoder(msg.type);
        buffer.setBytes(msg.data, msg.offset, msg.length);
        decoder.beginRead(buffer);

        String typeName = msg.type.getName();
        if (!fixed) {
            writeGroup(typeName, schema, decoder);
        } else {
            writeGroupFields(schema, decoder);
        }
    }



    @Override
    public void write(RawMessage raw) {
        consumer.startMessage();
        writeHeader(raw);
        writeMessageFields(raw);
        consumer.endMessage();
    }

    @Override
    public UnboundDecoder getDecoder(RecordClassDescriptor type) {
        return getDecoder(type, decoders);
    }

    public static final CompressionCodecName COMPRESSION = CompressionCodecName.GZIP;
    public static final int ROW_GROUP_SIZE = ParquetWriter.DEFAULT_BLOCK_SIZE;
    public static final int PAGE_SIZE = ParquetWriter.DEFAULT_PAGE_SIZE;

    public static class Builder extends ParquetWriter.Builder<RawMessage, Builder> {

        private final RawMessageWriteSupport writeSupport;

        Builder(Path file, RawMessageWriteSupport writeSupport) {
            super(file);
            this.writeSupport = writeSupport;
        }


        @Override
        protected Builder self() {
            return this;
        }

        @Override
        protected WriteSupport<RawMessage> getWriteSupport(Configuration conf) {
            return writeSupport;
        }
    }

    public static void main(String[] args) throws IOException {
        try (DXTickDB db = TickDBFactory.createFromUrl("dxtick://localhost:8011")) {
            db.open(true);
            DXTickStream stream = db.getStream("test");
            RawMessageWriteSupport writeSupport = new RawMessageWriteSupport("test", stream.getTypes());
            Builder builder = new Builder(new Path("test.parquet"), writeSupport)
                    .withCompressionCodec(COMPRESSION)
                    .withRowGroupSize(ROW_GROUP_SIZE)
                    .withPageSize(PAGE_SIZE)
                    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE);
            try (TickCursor cursor = db.select(Long.MIN_VALUE, new SelectionOptions(true, false), stream);
                 ParquetWriter<RawMessage> writer = builder.build()) {
                while (cursor.next()) {
                    writer.write((RawMessage) cursor.getMessage());
                }
            }
        }
    }
}
