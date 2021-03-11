package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.UnknownChannelException;
import com.epam.deltix.qsrv.dtb.fs.common.DelegatingOutputStream;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.TopicChannelOptionMap;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.ConsumerPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.DuplicateTopicException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.MulticastTopicSettings;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import com.epam.deltix.qsrv.hf.topic.consumer.SubscriptionWorker;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.CreateTopicResult;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.LoaderSubscriptionResult;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.ReaderSubscriptionResult;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Disposable;
import io.aeron.Aeron;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Implementation of TopicDB for embedded database.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class TickDBTopicDBImpl implements TopicDB {
    private final DXTickDB db;
    private final DXServerAeronContext aeronContext;
    private final DirectTopicRegistry topicRegistry;
    private final QuickExecutor executor;
    private final AeronThreadTracker aeronThreadTracker;

    private final CodecFactory compCodecFactory = CodecFactory.newCompiledCachingFactory();

    public TickDBTopicDBImpl(DXTickDB db, DXServerAeronContext aeronContext, DirectTopicRegistry topicRegistry, QuickExecutor executor, AeronThreadTracker aeronThreadTracker) {
        this.db = db;
        this.aeronContext = aeronContext;
        this.topicRegistry = topicRegistry;
        this.executor = executor;
        this.aeronThreadTracker = aeronThreadTracker;
        startCopyToStreamThreadsForAllTopics();
    }

    private void startCopyToStreamThreadsForAllTopics() {
        CopyTopicToStreamTaskManager copyTopicToStreamManager = new CopyTopicToStreamTaskManager(db, aeronContext, aeronThreadTracker, topicRegistry);
        copyTopicToStreamManager.startCopyToStreamThreadsForAllTopics();
    }

    @Override
    public DirectChannel createTopic(String topicKey, RecordClassDescriptor[] types, @Nullable TopicSettings settings) throws DuplicateTopicException {
        if (settings == null) {
            settings = new TopicSettings();
        }
        List<RecordClassDescriptor> typeList = Arrays.asList(types);
        if (typeList.isEmpty()) {
            throw new IllegalArgumentException("Type set can't be empty");
        }
        String copyToStreamKey = settings.getCopyToStream();

        CopyTopicToStreamTaskManager.preValidateCopyToStreamKey(db, typeList, copyToStreamKey);

        CreateTopicResult createTopicResult;
        TopicType topicType = settings.getTopicType();
        if (topicType == TopicType.MULTICAST) {
            createTopicResult = createMulticastClient(topicKey, types, settings.getInitialEntitySet(), settings.getMulticastSettings(), copyToStreamKey);
        } else if (topicType == TopicType.IPC) {
            createTopicResult = createIpcClient(topicKey, types, settings);
        } else {
            throw new IllegalArgumentException("Unsupported topicMediaType: " + topicType);
        }

        if (copyToStreamKey != null) {
            CopyTopicToStreamTaskManager copyTopicToStream = new CopyTopicToStreamTaskManager(db, aeronContext, aeronThreadTracker, topicRegistry);
            MappingProvider mappingProvider = getMappingProvider(topicKey);
            copyTopicToStream.subscribeToStreamCopyOrRollback(topicKey, typeList, copyToStreamKey, createTopicResult, mappingProvider);
        }
        return new TopicClientChannel(this, topicKey);
    }

    @NotNull
    private CreateTopicResult createIpcClient(String topicKey, RecordClassDescriptor[] types, @Nonnull TopicSettings settings) {
        return topicRegistry.createDirectTopic(topicKey, Arrays.asList(types), null, aeronContext.getStreamIdGenerator(), settings.getInitialEntitySet(), TopicType.IPC, null, settings.getCopyToStream());
    }

    @NotNull
    private CreateTopicResult createMulticastClient(String topicKey, RecordClassDescriptor[] types, @Nullable List<? extends IdentityKey> initialEntitySet, @Nullable MulticastTopicSettings multicastTopicSettings, @Nullable String targetStreamKey) {
        MulticastTopicSettings mts = multicastTopicSettings != null ? multicastTopicSettings : new MulticastTopicSettings();

        TopicChannelOptionMap channelOptions = new TopicChannelOptionMap();
        channelOptions.put(TopicChannelOption.MULTICAST_ENDPOINT_HOST, mts.getEndpointHost());
        channelOptions.put(TopicChannelOption.MULTICAST_ENDPOINT_PORT, mts.getEndpointPort());
        channelOptions.put(TopicChannelOption.MULTICAST_NETWORK_INTERFACE, mts.getNetworkInterface());
        channelOptions.put(TopicChannelOption.MULTICAST_TTL, mts.getTtl());

        return topicRegistry.createDirectTopic(topicKey, Arrays.asList(types), null, aeronContext.getStreamIdGenerator(), initialEntitySet, TopicType.MULTICAST, channelOptions.getValueMap(), targetStreamKey);
    }

    @Nullable
    @Override
    public DirectChannel getTopic(String topicKey) {
        try {
            // We do this call to check if this topic exist
            // TODO: Consider introduction of separate API call for this method
            @SuppressWarnings("unused")
            RecordClassDescriptor[] types = getTypes(topicKey);

            return new TopicClientChannel(this, topicKey);
        } catch (UnknownChannelException e) {
            return null;
        }
    }

    @Override
    public void deleteTopic(String topicKey) throws TopicNotFoundException {
        topicRegistry.deleteDirectTopic(topicKey);
    }

    @Override
    public List<String> listTopics() {
        return topicRegistry.listDirectTopics();
    }

    @Override
    public RecordClassDescriptor[] getTypes(String topicKey) throws TopicNotFoundException {
        List<RecordClassDescriptor> topicTypes = topicRegistry.getTopicTypes(topicKey);
        return topicTypes.toArray(new RecordClassDescriptor[0]);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(String topicKey, @Nullable PublisherPreferences pref, @Nullable IdleStrategy idleStrategy) throws TopicNotFoundException {
        if (pref == null) {
            pref = new PublisherPreferences();
        }
        List<? extends IdentityKey> initialEntitySet = pref.getInitialEntitySet();
        if (initialEntitySet == null) {
            initialEntitySet = Collections.emptyList();
        }
        CommunicationPipe pipe = new CommunicationPipe(8 * 1024);

        LoaderSubscriptionResult result = topicRegistry.addLoader(topicKey, pipe.getInputStream(), initialEntitySet, this.executor, aeronContext.getAeron(), true, null);
        Runnable dataAvailabilityCallback = result.getDataAvailabilityCallback();

        OutputStream baseOutputStream = pipe.getOutputStream();
        DelegatingOutputStream wrappedOutputStream = new DelegatingOutputStream(baseOutputStream) {
            @Override
            public void flush() throws IOException {
                super.flush();
                dataAvailabilityCallback.run();
            }
        };

        DirectLoaderFactory loaderFactory = new DirectLoaderFactory(compCodecFactory, pref.getTypeLoader());

        //Direct
        Aeron aeron = aeronContext.getAeron();

        Runnable closeCallback = null;
        return loaderFactory.create(
                aeron, pref.raw, result.getPublisherChannel(), result.getMetadataSubscriberChannel(), result.getDataStreamId(),
                result.getServerMetadataStreamId(), result.getTypes(),
                result.getLoaderNumber(), wrappedOutputStream, Arrays.asList(result.getMapping()),
                closeCallback, pref.getEffectiveIdleStrategy(idleStrategy)
        );
    }

    @Override
    public Disposable createConsumerWorker(String topicKey, @Nullable ConsumerPreferences preferences, @Nullable IdleStrategy idleStrategy, @Nullable ThreadFactory threadFactory, MessageProcessor processor) throws TopicNotFoundException {
        ReaderSubscriptionResult result = topicRegistry.addReader(topicKey, true, aeronContext.getPublicAddress(), null);

        if (preferences == null) {
            preferences = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, preferences.getTypeLoader());

        Aeron aeron = aeronContext.getAeron();
        SubscriptionWorker subscriptionWorker = factory.createListener(aeron, preferences.raw, result.getSubscriberChannel(), result.getDataStreamId(), result.getTypes(), processor, preferences.getEffectiveIdleStrategy(idleStrategy), getMappingProvider(topicKey));
        if (threadFactory == null) {
            // TODO: Add affinity support (inherit affinity config from TickDBImpl)
            threadFactory = new ThreadFactoryBuilder().setNameFormat("topic-embedded-consumer-%d").build();
        }
        Thread thread = threadFactory.newThread(subscriptionWorker::processMessagesUntilStopped);
        thread.start();
        return subscriptionWorker;
    }

    @Override
    public MessagePoller createPollingConsumer(String topicKey, @Nullable ConsumerPreferences preferences) throws TopicNotFoundException {
        ReaderSubscriptionResult result = topicRegistry.addReader(topicKey, true, aeronContext.getPublicAddress(), null);

        if (preferences == null) {
            preferences = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, preferences.getTypeLoader());

        Aeron aeron = aeronContext.getAeron();
        return factory.createPoller(aeron, preferences.raw, result.getSubscriberChannel(), result.getDataStreamId(), result.getTypes(), getMappingProvider(topicKey));
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(String topicKey, @Nullable ConsumerPreferences preferences, @Nullable IdleStrategy idleStrategy) throws TopicNotFoundException {
        ReaderSubscriptionResult result = topicRegistry.addReader(topicKey, true, aeronContext.getPublicAddress(), null);

        if (preferences == null) {
            preferences = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, preferences.getTypeLoader());

        Aeron aeron = aeronContext.getAeron();
        return factory.createMessageSource(aeron, preferences.raw, result.getSubscriberChannel(), result.getDataStreamId(), result.getTypes(), preferences.getEffectiveIdleStrategy(idleStrategy), getMappingProvider(topicKey));
    }

    @NotNull
    private MappingProvider getMappingProvider(String topicKey) {
        return topicRegistry.getMappingProvider(topicKey);
    }

    DXServerAeronContext getAeronContext() {
        return aeronContext;
    }
}
