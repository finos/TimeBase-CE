/*
 * Copyright 2024 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.epam.deltix.qsrv.hf.tickdb.pub.topic;

import com.epam.deltix.data.stream.ChannelPreferences;
import org.jetbrains.annotations.Nullable;

/**
 * Fluent-style API for setting up topic consumer channel preferences.
 *
 * @author Alexei Osipov
 */
public class ConsumerPreferences extends TopicChannelPreferences<ConsumerPreferences> {
    private TopicDataLossHandler topicDataLossHandler;

    public ConsumerPreferences() {
    }

    public static ConsumerPreferences from(ChannelPreferences channelPreferences) {
        if (channelPreferences instanceof ConsumerPreferences) {
            return (ConsumerPreferences) channelPreferences;
        }
        return new ConsumerPreferences().copyFrom(channelPreferences);
    }

    @Nullable
    public TopicDataLossHandler getTopicDataLossHandler() {
        return topicDataLossHandler;
    }

    /**
     * In case of data loss this handler will be called.
     * It may return {@code true} to ignore the data loss and continue polling.
     *
     * <p>If not set, poller will stop on data loss.
     */
    public ConsumerPreferences setTopicDataLossHandler(@Nullable TopicDataLossHandler topicDataLossHandler) {
        this.topicDataLossHandler = topicDataLossHandler;
        return this;
    }
}