package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.data.stream.ChannelPreferences;

/**
 * Fluent-style API for setting up topic consumer channel preferences.
 *
 * @author Alexei Osipov
 */
public class ConsumerPreferences extends TopicChannelPreferences<ConsumerPreferences> {
    public ConsumerPreferences() {
    }

    public static ConsumerPreferences from(ChannelPreferences channelPreferences) {
        if (channelPreferences instanceof ConsumerPreferences) {
            return (ConsumerPreferences) channelPreferences;
        }
        return new ConsumerPreferences().copyFrom(channelPreferences);
    }
}
