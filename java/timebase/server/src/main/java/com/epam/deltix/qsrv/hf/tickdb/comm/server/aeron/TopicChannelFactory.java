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
package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.google.common.base.Preconditions;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicChannelFactory {

    @NotNull
    public static String createPublisherChannel(TopicType topicType, @Nullable String channel, Map<TopicChannelOption, String> channelOptions, int defaultTopicTermBufferLength) {
        if (channel != null) {
            // This topic has legacy predefined channel. Use it as is.
            return channel;
        }
        int termBufferLength = getTermBufferLength(channelOptions, defaultTopicTermBufferLength);

        switch (topicType) {
            case IPC:
                return createIpcChannel(termBufferLength);

            case MULTICAST:
                return createMulticastChannel(channelOptions, termBufferLength);

            case UDP_SINGLE_PUBLISHER:
                return createSinglePublisherChannelForPublisher(
                        Preconditions.checkNotNull(channelOptions.get(TopicChannelOption.PUBLISHER_HOST)),
                        asInt(channelOptions.get(TopicChannelOption.PUBLISHER_PORT)),
                        termBufferLength);

            default:
                throw new IllegalArgumentException("Unknown topic type: " + topicType);
        }
    }

    @NotNull
    public static String createSubscriberChannel(TopicType topicType, @Nullable String channel, Map<TopicChannelOption, String> channelOptions, @Nullable String subscriberHost, int defaultTopicTermBufferLength) {
        if (channel != null) {
            // This topic has legacy predefined channel. Use it as is.
            return channel;
        }
        int termBufferLength = getTermBufferLength(channelOptions, defaultTopicTermBufferLength);

        switch (topicType) {
            case IPC:
                return createIpcChannel(termBufferLength);

            case MULTICAST:
                return createMulticastChannel(channelOptions, termBufferLength);

            case UDP_SINGLE_PUBLISHER:
                if (subscriberHost == null) {
                    throw new IllegalArgumentException("subscriberHost must be set for SINGLE_PUBLISHER topic");
                }
                return createSinglePublisherChannelForSubscriber(
                        Preconditions.checkNotNull(channelOptions.get(TopicChannelOption.PUBLISHER_HOST), "Publisher host must be specified"),
                        asInt(channelOptions.get(TopicChannelOption.PUBLISHER_PORT)),
                        subscriberHost,
                        asInt(channelOptions.get(TopicChannelOption.SUBSCRIBER_PORT)),
                        termBufferLength);

            default:
                throw new IllegalArgumentException("Unknown topic type: " + topicType);
        }
    }

    @Nullable
    private static Integer asInt(@Nullable String value) {
        if (value == null) {
            return null;
        } else {
            return Integer.parseInt(value);
        }
    }


    @NotNull
    public static String createIpcChannel(int termBufferLength) {
        return CommonContext.IPC_CHANNEL + "?term-length=" + termBufferLength;
    }

    @NotNull
    private static String createMulticastChannel(Map<TopicChannelOption, String> channelOptions, int topicTermBufferLength) {
        return createMulticastChannel(
                channelOptions.get(TopicChannelOption.MULTICAST_ENDPOINT_HOST),
                asInt(channelOptions.get(TopicChannelOption.MULTICAST_ENDPOINT_PORT)),
                channelOptions.get(TopicChannelOption.MULTICAST_NETWORK_INTERFACE),
                asInt(channelOptions.get(TopicChannelOption.MULTICAST_TTL)),
                topicTermBufferLength
        );
    }

    /**
     * Constructs Aeron channel URI for Multicast-based topics.
     */
    @NotNull
    public static String createMulticastChannel(
            @Nullable String endpointHost, @Nullable Integer endpointPort, @Nullable String networkInterface, @Nullable Integer ttl, int topicTermBufferLength
    ) {
        ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media("udp")
                .termLength(topicTermBufferLength);

        if (endpointHost != null) {
            try {
                if (!InetAddress.getByName(endpointHost).isMulticastAddress()) {
                    throw new IllegalArgumentException("Endpoint address is not a valid multicast address");
                }
            } catch (UnknownHostException x) {
                throw new IllegalArgumentException("Invalid endpoint address", x);
            }
        }

        if (endpointHost == null || endpointPort == null) {
            String[] parts = DXServerAeronContext.MULTICAST_ADDRESS.split(":");
            if (endpointHost == null) {
                endpointHost = parts[0];
            }
            if (endpointPort == null) {
                endpointPort = Integer.parseInt(parts[1]);
            }
        }
        builder.endpoint(endpointHost + ":" + endpointPort);

        if (networkInterface != null) {
            builder.networkInterface(networkInterface);
        }

        if (ttl != null) {
            builder.ttl(ttl);
        }

        return builder.build();
    }

    @Nonnull
    private static String createSinglePublisherChannelForPublisher(String publisherHost, @Nullable Integer publisherPort, int termBufferLength) {
        ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media("udp")
                .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
                .flowControl("min")
                .termLength(termBufferLength);

        if (publisherPort == null) {
            publisherPort = DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_DEFAULT_PUBLISHER_PORT;
        }

        builder.controlEndpoint(publisherHost + ":" + publisherPort);

        return builder.build();
    }

    @Nonnull
    private static String createSinglePublisherChannelForSubscriber(
            String publisherHost,
            @Nullable Integer publisherPort,
            String subscriberHost,
            @Nullable Integer subscriberPort,
            int termBufferLength
    ) {
        ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media("udp")
                .controlMode(CommonContext.MDC_CONTROL_MODE_DYNAMIC)
                .termLength(termBufferLength);

        if (publisherPort == null) {
            publisherPort = DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_DEFAULT_PUBLISHER_PORT;
        }

        builder.controlEndpoint(publisherHost + ":" + publisherPort);

        if (subscriberPort == null) {
            subscriberPort = DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_DEFAULT_SUBSCRIBER_PORT;
        }

        builder.endpoint(subscriberHost + ":" + subscriberPort);

        return builder.build();
    }

    public static int getTermBufferLength(Map<TopicChannelOption, String> channelOptions, int defaultTopicTermBufferLength) {
        Integer termBufferLength = asInt(channelOptions.get(TopicChannelOption.TERM_BUFFER_LENGTH));
        return Objects.requireNonNullElse(termBufferLength, defaultTopicTermBufferLength);
    }
}