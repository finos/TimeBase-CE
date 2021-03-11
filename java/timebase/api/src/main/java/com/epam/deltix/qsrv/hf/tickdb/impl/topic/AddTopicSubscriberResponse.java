package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class AddTopicSubscriberResponse {
    private final TopicTransferType transferType;
    private final List<ConstantIdentityKey> mapping;
    private final List<RecordClassDescriptor> types;
    private final String channel;
    private final String aeronDir;
    private final int dataStreamId;

    public AddTopicSubscriberResponse(TopicTransferType transferType, List<ConstantIdentityKey> mapping, List<RecordClassDescriptor> types, String channel, @Nullable String aeronDir, int dataStreamId) {
        this.transferType = transferType;
        this.mapping = mapping;
        this.types = types;
        this.channel = channel;
        this.aeronDir = aeronDir;
        this.dataStreamId = dataStreamId;
    }

    public TopicTransferType getTransferType() {
        return transferType;
    }

    public List<ConstantIdentityKey> getMapping() {
        return mapping;
    }

    public ConstantIdentityKey[] getMappingArray() {
        return getMapping().toArray(new ConstantIdentityKey[0]);
    }

    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    public String getChannel() {
        return channel;
    }

    @Nullable
    public String getAeronDir() {
        return aeronDir;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }
}
