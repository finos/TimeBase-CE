package com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron;

import com.google.common.base.Preconditions;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import io.aeron.ChannelUriStringBuilder;
import io.aeron.CommonContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicChannelFactory {

    @NotNull
    public static String createPublisherChannel(TopicType topicType, @Nullable String channel, Map<TopicChannelOption, String> channelOptions) {
        if (channel != null) {
            // This topic has legacy predefined channel. Use it as is.
            return channel;
        }

        switch (topicType) {
            case IPC:
                return createIpcChannel();

            case MULTICAST:
                return createMulticastChannel(channelOptions);

            case UDP_SINGLE_PUBLISHER:
                return createSinglePublisherChannelForPublisher(
                        Preconditions.checkNotNull(channelOptions.get(TopicChannelOption.PUBLISHER_HOST)),
                        asInt(channelOptions.get(TopicChannelOption.PUBLISHER_PORT))
                );

            default:
                throw new IllegalArgumentException("Unknown topic type: " + topicType);
        }
    }

    @NotNull
    public static String createSubscriberChannel(TopicType topicType, @Nullable String channel, Map<TopicChannelOption, String> channelOptions, @Nullable String subscriberHost) {
        if (channel != null) {
            // This topic has legacy predefined channel. Use it as is.
            return channel;
        }

        switch (topicType) {
            case IPC:
                return createIpcChannel();

            case MULTICAST:
                return createMulticastChannel(channelOptions);

            case UDP_SINGLE_PUBLISHER:
                if (subscriberHost == null) {
                    throw new IllegalArgumentException("subscriberHost must be set for SINGLE_PUBLISHER topic");
                }
                return createSinglePublisherChannelForSubscriber(
                        Preconditions.checkNotNull(channelOptions.get(TopicChannelOption.PUBLISHER_HOST), "Publisher host must be specified"),
                        asInt(channelOptions.get(TopicChannelOption.PUBLISHER_PORT)),
                        subscriberHost,
                        asInt(channelOptions.get(TopicChannelOption.SUBSCRIBER_PORT))
                );

            default:
                throw new IllegalArgumentException("Unknown topic type: " + topicType);
        }
    }

    // TODO: Make publisher and subscriber channel to use smaller term length
    @NotNull
    public static String createMetadataPublisherChannel(TopicType topicType, @Nullable String channel, Map<TopicChannelOption, String> channelOptions, @Nullable String timebaseServerHost) {
        if (channel != null) {
            // This topic has legacy predefined channel. Use it as is.
            return channel;
        }

        switch (topicType) {
            case IPC:
                return createIpcChannel();

            case MULTICAST:
                // TODO: We can use different port and host for metadata
                return createMulticastChannel(channelOptions);

            case UDP_SINGLE_PUBLISHER:
                if (timebaseServerHost == null) {
                    throw new IllegalArgumentException("timebaseServerHost must be set for SINGLE_PUBLISHER topic");
                }
                return createSinglePublisherChannelForPublisher(
                        timebaseServerHost,
                        DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_METADATA_PUBLISHER_PORT
                );

            default:
                throw new IllegalArgumentException("Unknown topic type: " + topicType);
        }
    }

    @NotNull
    public static String createMetadataSubscriberChannel(TopicType topicType, @Nullable String channel, Map<TopicChannelOption, String> channelOptions, @Nullable String timebaseServerHost) {
        if (channel != null) {
            // This topic has legacy predefined channel. Use it as is.
            return channel;
        }

        switch (topicType) {
            case IPC:
                return createIpcChannel();

            case MULTICAST:
                // TODO: We can use different port and host for metadata
                return createMulticastChannel(channelOptions);

            case UDP_SINGLE_PUBLISHER:
                if (timebaseServerHost == null) {
                    throw new IllegalArgumentException("timebaseServerHost must be set for SINGLE_PUBLISHER topic");
                }
                String publisherHost = Preconditions.checkNotNull(channelOptions.get(TopicChannelOption.PUBLISHER_HOST), "Publisher host must be specified");
                return createSinglePublisherChannelForSubscriber(
                        timebaseServerHost,
                        DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_METADATA_PUBLISHER_PORT,
                        publisherHost,
                        DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_METADATA_SUBSCRIBER_PORT
                );

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
    public static String createIpcChannel() {
        return CommonContext.IPC_CHANNEL + "?term-length=" + getTopicTermBufferLength();
    }

    @NotNull
    private static String createMulticastChannel(Map<TopicChannelOption, String> channelOptions) {
        return createMulticastChannel(
                channelOptions.get(TopicChannelOption.MULTICAST_ENDPOINT_HOST),
                asInt(channelOptions.get(TopicChannelOption.MULTICAST_ENDPOINT_PORT)),
                channelOptions.get(TopicChannelOption.MULTICAST_NETWORK_INTERFACE),
                asInt(channelOptions.get(TopicChannelOption.MULTICAST_TTL))
        );
    }

    /**
     * Constructs Aeron channel URI for Multicast-based topics.
     */
    @NotNull
    public static String createMulticastChannel(@Nullable String endpointHost, @Nullable Integer endpointPort, @Nullable String networkInterface, @Nullable Integer ttl) {
        ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media("udp")
                .termLength(getTopicTermBufferLength());

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

    @NotNull
    private static String createSinglePublisherChannelForPublisher(String publisherHost, @Nullable Integer publisherPort) {
        ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media("udp")
                .termLength(getTopicTermBufferLength());

        if (publisherPort == null) {
            publisherPort = DXServerAeronContext.SINGLE_PUBLISHER_TOPIC_DEFAULT_PUBLISHER_PORT;
        }

        builder.controlEndpoint(publisherHost + ":" + publisherPort);

        return builder.build();
    }

    @NotNull
    private static String createSinglePublisherChannelForSubscriber(String publisherHost, @Nullable Integer publisherPort, String subscriberHost, @Nullable Integer subscriberPort) {
        ChannelUriStringBuilder builder = new ChannelUriStringBuilder()
                .media("udp")
                .termLength(getTopicTermBufferLength());

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

    private static int getTopicTermBufferLength() {
        // Note: we override buffer size because TB sets it to relatively low value (2Mb).
        return DXServerAeronContext.TOPIC_IPC_TERM_BUFFER_LENGTH;
    }
}
