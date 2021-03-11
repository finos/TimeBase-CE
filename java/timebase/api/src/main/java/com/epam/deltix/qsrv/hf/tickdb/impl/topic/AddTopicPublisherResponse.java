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
public class AddTopicPublisherResponse {
    private final TopicTransferType transferType;
    private final List<ConstantIdentityKey> mapping;
    private final List<RecordClassDescriptor> types;
    private final String publisherChannel;
    private final String metadataSubscriberChannel;
    private final String aeronDir;
    private final int dataStreamId;
    private final int serverMetadataStreamId;
    private final byte loaderNumber;
    private final int minTempEntityIndex;
    private final int maxTempEntityIndex;

    public AddTopicPublisherResponse(TopicTransferType transferType, List<ConstantIdentityKey> mapping, List<RecordClassDescriptor> types, String publisherChannel, String metadataSubscriberChannel, @Nullable String aeronDir, int dataStreamId, int serverMetadataStreamId, byte loaderNumber, int minTempEntityIndex, int maxTempEntityIndex) {
        this.transferType = transferType;
        this.mapping = mapping;
        this.types = types;
        this.publisherChannel = publisherChannel;
        this.metadataSubscriberChannel = metadataSubscriberChannel;
        this.aeronDir = aeronDir;
        this.dataStreamId = dataStreamId;
        this.serverMetadataStreamId = serverMetadataStreamId;
        this.loaderNumber = loaderNumber;
        this.minTempEntityIndex = minTempEntityIndex;
        this.maxTempEntityIndex = maxTempEntityIndex;
    }

    public TopicTransferType getTransferType() {
        return transferType;
    }

    public List<ConstantIdentityKey> getMapping() {
        return mapping;
    }

    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    public String getPublisherChannel() {
        return publisherChannel;
    }

    public String getMetadataSubscriberChannel() {
        return metadataSubscriberChannel;
    }

    @Nullable
    public String getAeronDir() {
        return aeronDir;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }

    public int getServerMetadataStreamId() {
        return serverMetadataStreamId;
    }

    public byte getLoaderNumber() {
        return loaderNumber;
    }

    public int getMinTempEntityIndex() {
        return minTempEntityIndex;
    }

    public int getMaxTempEntityIndex() {
        return maxTempEntityIndex;
    }
}
