package com.epam.deltix.qsrv.hf.pub;

import com.epam.deltix.qsrv.hf.codec.MessageSizeCodec;
import com.epam.deltix.qsrv.hf.pub.codec.TimeCodec;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.service.SystemMessage;
import com.epam.deltix.util.collections.generated.ByteArrayList;

/**
 * @author Alexei Osipov
 */
public class TimeMessage extends SystemMessage {
    private static final String NAME = TimeMessage.class.getName();
    public static final String DESCRIPTOR_GUID = "SYS:TimeMessage:1";
    private static final int STUB_SIZE = 90; // bytes


    private ByteArrayList stubData;

    public static final RecordClassDescriptor DESCRIPTOR = new RecordClassDescriptor(
            DESCRIPTOR_GUID, NAME, NAME, false, null,
            new NonStaticDataField ("stubData", "Stub data to increase message size", BinaryDataType.getDefaultInstance())
    );

    public TimeMessage() {
        this.symbol = "";
        this.stubData = new ByteArrayList(STUB_SIZE);
        this.stubData.setSize(STUB_SIZE);
    }

    /**
     * @return estimated size of binary representation of {@link TimeMessage} when it serialized into Transient stream.
     * Note: Actual value may be smaller depending on time value.
     */
    public static int getTimeMessageSizeInTransientStream() {
        int messageBodySize = TimeCodec.TIME_SCALE_MILLISECONDS_FIELD_SIZE
                + STUB_SIZE + 5 // Stub
                + 1 // type
                + 2; // symbol
        return MessageSizeCodec.fieldSize(messageBodySize) + messageBodySize;
    }

    public ByteArrayList getStubData() {
        return stubData;
    }

    public void setStubData(ByteArrayList stubData) {
        this.stubData = stubData;
    }

    public boolean hasStubData() {
        return stubData != null;
    }

    public void nullifyStubData() {
        this.stubData = null;
    }
}
