package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.UnknownChannelException;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.ConsumerPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Adapter of topics to {@link DirectChannel} interface.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicClientChannel implements DirectChannel {
    private final TopicDB client;
    private final String topicKey;

    public TopicClientChannel(TopicDB client, String topicKey) {
        this.client = client;
        this.topicKey = topicKey;
    }

    @Nonnull
    @Override
    public String getKey() {
        return topicKey;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Nonnull
    @Override
    public RecordClassDescriptor[] getTypes() {
        return client.getTypes(topicKey);
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(ChannelPreferences options) {
        return client.createConsumer(topicKey, ConsumerPreferences.from(options), null);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(ChannelPreferences options) {
        return client.createPublisher(
                topicKey,
                PublisherPreferences.from(options),
                null
        );
    }

    @Override
    public MessagePoller createPollingConsumer(ChannelPreferences options) throws UnknownChannelException {
        return client.createPollingConsumer(topicKey, ConsumerPreferences.from(options));
    }
}
