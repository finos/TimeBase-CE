package com.epam.deltix.qsrv.hf.tickdb.pub.mapreduce;

import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.JobContext;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 *
 */
public class WritableRawMessage implements WritableMessage<RawMessage>, WritableComparable<RawMessage> {
    private RawMessage                  message = new RawMessage();

    private int                         code = -1;
    private RecordClassDescriptor[]     types;
    private RecordTypeMap<RecordClassDescriptor> typeMap;

    public                  WritableRawMessage() {
    }

    @Override
    public void             set(RawMessage message, JobContext context) throws IOException {
        if (types == null)
            initTypes(context);

        this.code = typeMap.getCode(message.type);
        this.message = message;
    }

    @Override
    public RawMessage       get(JobContext context) throws IOException {
        if (types == null)
            initTypes(context);

        message.type = types[code];
        return message;
    }

    public void             write(DataOutput dataOutput) throws IOException {
        WritableUtils.writeVInt(dataOutput, code);

        WritableUtils.writeString(dataOutput, message.getSymbol().toString());
        WritableUtils.writeVLong(dataOutput, message.getNanoTime());

        WritableUtils.writeCompressedByteArray(dataOutput, message.getData());
        WritableUtils.writeVInt(dataOutput, message.offset);
        WritableUtils.writeVInt(dataOutput, message.length);
    }

    public void             readFields(DataInput dataInput) throws IOException {
        code = WritableUtils.readVInt(dataInput);

        message.setSymbol(WritableUtils.readString(dataInput));
        message.setNanoTime(WritableUtils.readVLong(dataInput));

        message.setBytes(WritableUtils.readCompressedByteArray(dataInput),
                         WritableUtils.readVInt(dataInput),
                         WritableUtils.readVInt(dataInput));
    }

    private void            initTypes(JobContext context) throws IOException {
        types = TDBProtocol.readClassSet(context.getConfiguration().get(MAPPER_OUTPUT_TYPE)).getContentClasses();
        typeMap = new RecordTypeMap<>(types);
    }

    @Override
    public int              compareTo(RawMessage message) {
        if (this.message == null)
            return 1;
        else if (message == null)
            return -1;
        else
            return this.message.compareTo(message);
    }
}
