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
import com.epam.deltix.timebase.messages.IdentityKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * Fluent-style API for setting up topic loader channel preferences.
 *
 * @author Alexei Osipov
 */
public class PublisherPreferences extends TopicChannelPreferences<PublisherPreferences> {
    /**
     * If disabled, the loader will replace {@link com.epam.deltix.timebase.messages.TimeStampedMessage#TIMESTAMP_UNKNOWN} with the current time on client side.
     * So topic consumer always receives messages with valid timestamps.
     *
     * <p>If enabled, the loader will propagate {@link com.epam.deltix.timebase.messages.TimeStampedMessage#TIMESTAMP_UNKNOWN} to all consumers.
     * This may be useful in scenarios when there are multiple producers for single topic, and you want to write
     * data from all of them to a carbon copy stream. So you want to allow TimeBase server to set timestamps.
     * Keep in mind that other topic consumers will get messages with {@link com.epam.deltix.timebase.messages.TimeStampedMessage#TIMESTAMP_UNKNOWN}.
     */
    private boolean preserveNullTimestamp = false;


    public PublisherPreferences() {
    }

    /**
     * @param initialEntitySet initial entry set (it may be empty) - list of known {@link IdentityKey} to be used
     *
     * @deprecated This method is deprecated. Entity data is not used anymore.
     */
    @Deprecated
    public PublisherPreferences setInitialEntitySet(@SuppressWarnings("unused") @Nonnull List<? extends IdentityKey> initialEntitySet) {
        return this;
    }

    /**
     * @deprecated This method is deprecated. Entity data is not used anymore.
     */
    @Nullable
    @Deprecated
    public List<? extends IdentityKey> getInitialEntitySet() {
        return Collections.emptyList();
    }

    public boolean isPreserveNullTimestamp() {
        return preserveNullTimestamp;
    }

    /**
     * {@link #preserveNullTimestamp}
     */

    public PublisherPreferences setPreserveNullTimestamp(boolean preserveNullTimestamp) {
        this.preserveNullTimestamp = preserveNullTimestamp;
        return this;
    }

    public static PublisherPreferences from(ChannelPreferences channelPreferences) {
        if (channelPreferences instanceof PublisherPreferences) {
            return (PublisherPreferences) channelPreferences;
        }
        return new PublisherPreferences().copyFrom(channelPreferences);
    }
}