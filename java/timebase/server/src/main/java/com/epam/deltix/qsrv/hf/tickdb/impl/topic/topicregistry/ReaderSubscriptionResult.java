package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.google.common.collect.ImmutableList;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

/**
 * @author Alexei Osipov
 */
public class ReaderSubscriptionResult {
    private final TopicType topicType;
    private final ConstantIdentityKey[] mapping;
    private final ImmutableList<RecordClassDescriptor> types;
    private final String subscriberChannel;
    private final int dataStreamId;

    public ReaderSubscriptionResult(TopicType topicType, ConstantIdentityKey[] mapping, ImmutableList<RecordClassDescriptor> types, String subscriberChannel, int dataStreamId) {
        this.topicType = topicType;
        this.mapping = mapping;
        this.types = types;
        this.subscriberChannel = subscriberChannel;
        this.dataStreamId = dataStreamId;
    }

    public TopicType getTopicType() {
        return topicType;
    }

    public ImmutableList<RecordClassDescriptor> getTypes() {
        return types;
    }

    public String getSubscriberChannel() {
        return subscriberChannel;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }

    public ConstantIdentityKey[] getMapping() {
        return mapping;
    }
}
