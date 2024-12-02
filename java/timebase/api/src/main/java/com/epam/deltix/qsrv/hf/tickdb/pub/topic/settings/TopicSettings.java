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
package com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.BitUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexei Osipov
 */
public class TopicSettings {
    private String copyToStream = null;
    private String copyToSpace = null;

    private TopicType topicMediaType;
    private MulticastTopicSettings multicastSettings = null;
    private String publisherAddress;
    private Integer termBufferLength;

    public TopicSettings() {
    }

    /**
     * @deprecated This method is deprecated. Entity data is not used anymore.
     */
    @Nullable
    @Deprecated
    public List<? extends IdentityKey> getInitialEntitySet() {
        return Collections.emptyList();
    }

    /**
     * Defines initial entity set (it may be empty).
     * Providing entity set that matches expected data removes extra overhead that associated
     * with generation of indexes for new entities.
     *
     * @deprecated This method is deprecated. Entity data is not used anymore.
     */
    @Deprecated
    public TopicSettings setInitialEntitySet(@SuppressWarnings("unused") List<? extends IdentityKey> initialEntitySet) {
        return this;
    }

    @Nullable
    public String getCopyToStream() {
        return copyToStream;
    }

    /**
     * See {@link #setCopyToStream(String, String)}
     */
    public TopicSettings setCopyToStream(String copyToStreamKey) {
        this.copyToStream = copyToStreamKey;
        return this;
    }

    /**
     * Enables background process that will copy all the data passed to this topic into a stream with the specified name.
     * Keep in mind that if the topic data rate is too high then the stream may be unable to cope with it.
     * In that case the topic's data producers will be blocked (any may lose data).
     *
     * @param copyToStreamKey key for
     * @param copyToSpace allows to specify space inside of stream to be used
     */
    public TopicSettings setCopyToStream(String copyToStreamKey, String copyToSpace) {
        if (copyToSpace != null && copyToStreamKey == null) {
            throw new IllegalArgumentException("copyToSpace must be used together with copyToStreamKey");
        }
        this.copyToStream = copyToStreamKey;
        this.copyToSpace = copyToSpace;
        return this;
    }

    @Nullable
    public String getCopyToSpace() {
        return copyToSpace;
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
     * <p>Ony one publisher at a time can be used for this topic.
     * This publisher can run only on the specified host.
     *
     * @param publisherAddress IP address or hostname of the host that will run publisher. Optionally with port (like "10.15.100.1:5555").
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

    @Nullable
    public Integer getTermBufferLength() {
        return termBufferLength;
    }

    /**
     * Sets term buffer length for topic.
     * <p>
     * Overrides default term buffer length set on server.
     */
    public TopicSettings setTermBufferLength(@Nullable Integer termBufferLength) {
        if (termBufferLength != null) {
            if (termBufferLength <= 0) {
                throw new IllegalArgumentException("Term buffer length must be positive");
            }

            if (!BitUtil.isPowerOfTwo(termBufferLength)) {
                throw new IllegalArgumentException("Term buffer length must be power of 2");
            }
        }
        this.termBufferLength = termBufferLength;
        return this;
    }

    @Nonnull
    public TopicType getTopicType() {
        return topicMediaType == null ? TopicType.IPC : topicMediaType;
    }
}