package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.google.common.collect.ImmutableList;
import com.epam.deltix.qsrv.hf.blocks.InstrumentKeyToIntegerHashMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public interface TopicRegistryEventListener {
    void topicCreated(String topicKey, @Nullable String channel, ImmutableList<RecordClassDescriptor> types, InstrumentKeyToIntegerHashMap entities, TopicType topicType, Map<TopicChannelOption, String> channelOptions, @Nullable String copyToStreamKey);

    void topicDeleted(String topicKey);
}
