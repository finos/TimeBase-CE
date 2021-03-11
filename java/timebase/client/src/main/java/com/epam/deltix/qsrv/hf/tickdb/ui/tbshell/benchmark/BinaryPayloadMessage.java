package com.epam.deltix.qsrv.hf.tickdb.ui.tbshell.benchmark;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.util.collections.generated.ByteArrayList;

/**
 * @author Alexei Osipov
 */
public class BinaryPayloadMessage extends InstrumentMessage {

    public ByteArrayList payload = new ByteArrayList();

    public ByteArrayList getPayload() {
        return payload;
    }

    public void setPayload(ByteArrayList payload) {
        this.payload = payload;
    }

    public static RecordClassDescriptor getRecordClassDescriptor() {
        final String name = BinaryPayloadMessage.class.getName();
        final DataField[] fields = {
                new NonStaticDataField(
                        "payload", "Binary payload",
                        new BinaryDataType(false, BinaryDataType.MIN_COMPRESSION)),
        };

        return new RecordClassDescriptor(name, name, false, null, fields);
    }
}
