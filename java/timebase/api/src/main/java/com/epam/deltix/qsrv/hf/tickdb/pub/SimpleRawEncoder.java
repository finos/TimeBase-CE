package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.data.stream.MessageEncoder;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.codec.RecordTypeMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Created by Alex Karpovich on 29/07/2020.
 */
public class SimpleRawEncoder implements MessageEncoder<InstrumentMessage> {

    private final RecordTypeMap<RecordClassDescriptor> map;
    private int contentOffset;

    public SimpleRawEncoder(RecordClassDescriptor[] types) {
        map = (types.length > 1) ? new RecordTypeMap<>(types) : null;
    }

    @Override
    public boolean encode(InstrumentMessage message, MemoryDataOutput out) {
        RawMessage raw = (RawMessage)message;

        if (map != null) {
            int typeCode = map.getCode(raw.type);
            out.writeUnsignedByte(typeCode);
        }

        out.writeString(message.getSymbol());
        raw.writeTo(out);

        contentOffset = out.getPosition();

        return true;
    }

    @Override
    public int getContentOffset() {
        return contentOffset;
    }

    @Override
    public int getTypeIndex() {
        return 0;
    }
}
