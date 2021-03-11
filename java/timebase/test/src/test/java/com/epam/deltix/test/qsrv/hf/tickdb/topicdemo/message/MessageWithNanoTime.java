package com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.message;

import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.BinaryDataType;
import com.epam.deltix.qsrv.hf.pub.md.DataField;
import com.epam.deltix.qsrv.hf.pub.md.IntegerDataType;
import com.epam.deltix.qsrv.hf.pub.md.NonStaticDataField;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.test.qsrv.hf.tickdb.topicdemo.util.DemoConf;
import com.epam.deltix.util.collections.generated.ByteArrayList;

/**
 * @author Alexei Osipov
 */
public class MessageWithNanoTime extends InstrumentMessage {
    private static final int TARGET_MESSAGE_SIZE = DemoConf.TARGET_MESSAGE_SIZE;
    private static final int BASE_MESSAGE_SIZE = 16 /* InstrumentMessage headers */ + 16 /* messageId + publisherNanoTime */ + 4 /* approximate size of empty stubData field */;

    public int experimentId;
    public long messageId;
    public long publisherNanoTime;
    public ByteArrayList stubData = new ByteArrayList();

    public MessageWithNanoTime() {
        stubData.setSize(getStubByteArraySize());
    }

    private static int getStubByteArraySize() {
        return Math.max(0, TARGET_MESSAGE_SIZE - BASE_MESSAGE_SIZE);
    }

    public int getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    public int getMessageSizeEstimate() {
        return BASE_MESSAGE_SIZE + stubData.size();
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getPublisherNanoTime() {
        return publisherNanoTime;
    }

    public void setPublisherNanoTime(long publisherNanoTime) {
        this.publisherNanoTime = publisherNanoTime;
    }

    public ByteArrayList getStubData() {
        return stubData;
    }

    public void setStubData(ByteArrayList stubData) {
        this.stubData = stubData;
    }

    public static RecordClassDescriptor getRecordClassDescriptor() {
        final String name = MessageWithNanoTime.class.getName();
        final DataField[] fields = {
                new NonStaticDataField(
                        "experimentId", "Experiment ID",
                        new IntegerDataType(IntegerDataType.ENCODING_INT32, false)),
                new NonStaticDataField(
                        "messageId", "Message ID",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                new NonStaticDataField(
                        "publisherNanoTime", "Publisher nano time",
                        new IntegerDataType(IntegerDataType.ENCODING_INT64, false)),
                new NonStaticDataField(
                        "stubData", "Stub data to increase message size",
                        new BinaryDataType(false, BinaryDataType.MIN_COMPRESSION)),
        };

        return new RecordClassDescriptor(name, name, false, null, fields);
    }
}
