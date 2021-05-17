/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings;

import com.epam.deltix.timebase.messages.IdentityKey;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Alexei Osipov
 */
public class TopicSettings {
    private List<? extends IdentityKey> initialEntitySet = null;
    private String copyToStream = null;

    private TopicType topicMediaType;
    private MulticastTopicSettings multicastSettings = null;
    private String publisherAddress;

    public TopicSettings() {
    }

    @Nullable
    public List<? extends IdentityKey> getInitialEntitySet() {
        return initialEntitySet;
    }

    /**
     * Defines initial entity set (may be empty).
     * Providing entity set that matches expected data removes extra overhead that associated
     * with generation of indexes for new entities.
     */
    public TopicSettings setInitialEntitySet(List<? extends IdentityKey> initialEntitySet) {
        this.initialEntitySet = initialEntitySet;
        return this;
    }

    @Nullable
    public String getCopyToStream() {
        return copyToStream;
    }

    /**
     * Enables background process that will copy all the data passed to this topic into a stream with the specified name.
     * Keep in mind that if the topic data rate is too high then the stream may be unable to cope with it.
     * In that case the topics's data producers will be blocked (any may loose data).
     */
    public TopicSettings setCopyToStream(String copyToStreamKey) {
        this.copyToStream = copyToStreamKey;
        return this;
    }

    @Nullable
    public MulticastTopicSettings getMulticastSettings() {
        return multicastSettings;
    }

    /**
     * Warning: This API is a "work in progress" (WIP) and is a subject to change.
     * <p>
     * Enables multicast operation mode.
     * </p>
     * <p>
     * In this mode all Publishers send data via UDP multicast.
     * Note: this should be used only in the networks under your control and only if you sure if the multicast traffic
     * will not clog other applications.
     * </p>
     *
     * @param multicastSettings multicast settings. {@code null} value will enable multicast with default settings.
     */
    public TopicSettings setMulticastSettings(@Nullable MulticastTopicSettings multicastSettings) {
        if (topicMediaType != null) {
            throw new IllegalStateException("Media type is already set");
        }
        this.multicastSettings = multicastSettings;
        this.topicMediaType = TopicType.MULTICAST;
        return this;
    }

    /**
     * Enables UDP operation mode with single predefined publisher.
     *
     * Ony one publisher at a time can be used for this topic.
     * This publisher can run only on the specified host.
     *
     * @param publisherAddress IP address or hostname of the host that will run publisher.
     */
    public TopicSettings setSinglePublisherUdpMode(String publisherAddress) {
        if (topicMediaType != null) {
            throw new IllegalStateException("Media type is already set");
        }
        if (publisherAddress == null) {
            throw new IllegalArgumentException("Publisher address is required");
        }
        this.topicMediaType = TopicType.UDP_SINGLE_PUBLISHER;
        this.publisherAddress = publisherAddress;
        return this;
    }

    public String getPublisherAddress() {
        return publisherAddress;
    }

    @Nonnull
    public TopicType getTopicType() {
        return topicMediaType == null ? TopicType.IPC : topicMediaType;
    }
}
