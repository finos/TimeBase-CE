package com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry;

import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicChannelOptionMap {
    private final Map<TopicChannelOption, String> valueMap;

    public TopicChannelOptionMap() {
        this.valueMap = new HashMap<>();
    }

    public TopicChannelOptionMap(Map<TopicChannelOption, String> backingMap) {
        this.valueMap = backingMap;
    }

    public void put(TopicChannelOption option, @Nullable String value) {
        if (value != null) {
            valueMap.put(option, value);
        } else {
            valueMap.remove(option);
        }
    }

    public void put(TopicChannelOption option, @Nullable Integer value) {
        if (value != null) {
            valueMap.put(option, value.toString());
        } else {
            valueMap.remove(option);
        }
    }

    public Map<TopicChannelOption, String> getValueMap() {
        return valueMap;
    }

    public boolean hasValue(TopicChannelOption option) {
        return valueMap.get(option) != null;
    }
}
