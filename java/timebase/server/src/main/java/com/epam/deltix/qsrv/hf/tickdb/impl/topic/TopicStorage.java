package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.epam.deltix.gflog.Log;
import com.epam.deltix.gflog.LogFactory;
import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.hf.blocks.InstrumentKeyToIntegerHashMap;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicChannelOption;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.impl.TopicDTO;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.IdGenerator;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.TopicRegistryEventListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.text.SimpleStringCodec;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicStorage {
    private static final Log LOGGER = LogFactory.getLog(TopicStorage.class);

    private static final String TOPICS_DIRECTORY = "topics";
    private static final String EXTENSION = ".topic.xml";

    private final boolean readOnly = false; // TODO: Make configurable and pass as constructor parameter
    private final AbstractPath topicDataFolder;
    private boolean enabled = true; // If false then event listener ignores all events


    TopicStorage(AbstractPath topicDataFolder) {
        this.topicDataFolder = topicDataFolder;
    }

    public static TopicStorage createAtQSHome() {
        return createAtPath(QSHome.get());
    }

    public static TopicStorage createAtPath(String path) {
        LocalFS localFS = new LocalFS();
        AbstractPath localPath = localFS.createPath(path).append(TOPICS_DIRECTORY);
        return new TopicStorage(localPath);
    }

    /**
     * Returns an event listener that can be provided to {@link DirectTopicRegistry} to collect topic changes.
     */
    public TopicRegistryEventListener getPersistingListener() {
        return new TopicRegistryEventListenerImpl();
    }

    /**
     * Loads topic data from storage into the specified topic registry.
     *
     * @param directTopicRegistry topic registry to load data into
     * @param idGenerator aeron stream id generator
     */
    public void loadTopicDataInto(DirectTopicRegistry directTopicRegistry, IdGenerator idGenerator) {
        enabled = false; // Helps to avoid storing changes during the load process
        try {
            iterateStoredTopics(topic -> {
                directTopicRegistry.createDirectTopic(topic.getTopicKey(), topic.getTypes(), topic.getChannel(), idGenerator, topic.getEntities(), topic.getTopicType(), parseChannelOptions(topic.getChannelSettings()), topic.getCopyToStream());
            });
        } finally {
            enabled = true;
        }
    }

    private Map<TopicChannelOption, String> parseChannelOptions(Map<String, String> channelOptions) {
        Map<TopicChannelOption, String> result = new HashMap<>();
        for (Map.Entry<String, String> entry : channelOptions.entrySet()) {
            result.put(TopicChannelOption.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private Map<String, String> serializeChannelOptions(Map<TopicChannelOption, String> channelOptions) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<TopicChannelOption, String> entry : channelOptions.entrySet()) {
            TopicChannelOption key = entry.getKey();
            result.put(getSerializedOptionKey(key), entry.getValue());
        }
        return result;
    }

    @NotNull
    public static String getSerializedOptionKey(TopicChannelOption key) {
        return key.name();
    }

    private void iterateStoredTopics(Consumer<TopicDTO> processor) {
        if (!topicDataFolder.exists()) {
            return;
        }
        // TODO: @LEGACY
        //SchemaUpdater migrator = new SchemaUpdater(new ClassMappings());
        try {


            for (String folderEntry : topicDataFolder.listFolder()) {
                if (folderEntry.endsWith(EXTENSION)) {
                    TopicDTO topicDTO = readTopic(topicDataFolder.append(folderEntry));
                    populateMissingFields(topicDTO);

                    // TODO: @LEGACY
                    //updateSchema(migrator, topicDTO);

                    // Validate topic consistency
                    if (topicDTO.getTopicType() == TopicType.UDP_SINGLE_PUBLISHER) {
                        if (StringUtils.isEmpty(topicDTO.getChannelSettings().get(TopicStorage.getSerializedOptionKey(TopicChannelOption.PUBLISHER_HOST)))) {
                            LOGGER.warn("Topic %s contains invalid data (PUBLISHER_HOST is not specified) and will not be created")
                                    .with(topicDTO.getTopicKey());
                            continue;
                        }
                    }

                    processor.accept(topicDTO);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void populateMissingFields(TopicDTO topic) {
        if (topic.getTopicType() == null) {
            topic.setTopicType(topic.isMulticast() ? TopicType.MULTICAST : TopicType.IPC);
        }
    }

    /*
    // TODO: @LEGACY
    private void updateSchema(SchemaUpdater migrator, TopicDTO topicDTO) {
        RecordClassSet metadata = new RecordClassSet(topicDTO.getTypes().toArray(new RecordClassDescriptor[0]));
        try {
            if (migrator.update(metadata)) {
                topicDTO.setTypes(Arrays.asList(metadata.getContentClasses()));
                LOGGER.info("Topic [%s] schema migrated successfully.").with(topicDTO.getTopicKey());
                if (!readOnly) {
                    writeTopic(topicDTO);
                }
            }
        } catch (Introspector.IntrospectionException | ClassNotFoundException | StackOverflowError e) {
            LOGGER.warn("Failed to update stream [%s] schema: %s").with(topicDTO.getTopicKey()).with(e);
        }
    }
    */

    @VisibleForTesting
    AbstractPath getTopicFilePath(String topicKey) {
        return topicDataFolder.append(SimpleStringCodec.DEFAULT_INSTANCE.encode(topicKey) + EXTENSION);
    }

    static TopicDTO readTopic(AbstractPath path) {
        try {
            return (TopicDTO) IOUtil.unmarshal(TickDBJAXBContext.createUnmarshaller(), path.openInput(0));
        } catch (Exception ex) {
            throw new UncheckedIOException("Error reading file: " + path.getPathString(), ex);
        }
    }

    static void writeTopic(AbstractPath path, TopicDTO topicDTO) {
        try {
            Marshaller marshaller = TickDBJAXBContext.createMarshaller();
            OutputStream outputStream = path.openOutput(0);
            IOUtil.marshall(marshaller, outputStream, topicDTO);
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing file: " + path.getPathString(), e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeTopic(TopicDTO topic) {
        try {
            topicDataFolder.makeFolderRecursive();
            writeTopic(getTopicFilePath(topic.getTopicKey()), topic);
        } catch (IOException | UncheckedIOException e) {
            LOGGER.warn().append("Failed to write topic file: ").append(e).commit();
        }
    }

    private final class TopicRegistryEventListenerImpl implements TopicRegistryEventListener {

        @Override
        public void topicCreated(String topicKey, @Nullable String channel, ImmutableList<RecordClassDescriptor> types, InstrumentKeyToIntegerHashMap entities, TopicType topicType, Map<TopicChannelOption, String> channelOptions, @Nullable String copyToStreamKey) {
            if (!enabled) {
                return;
            }

            TopicDTO topic = new TopicDTO();
            topic.setTopicKey(topicKey);
            topic.setChannel(channel);
            topic.setTypes(types);
            topic.setEntities(Collections.list(entities.keys()));
            topic.setTopicType(topicType);
            topic.setMulticast(topicType == TopicType.MULTICAST); // Forward compatibility
            topic.setCopyToStream(copyToStreamKey);
            topic.setChannelSettings(serializeChannelOptions(channelOptions));

            writeTopic(topic);
        }

        @Override
        public void topicDeleted(String topicKey) {
            if (!enabled) {
                return;
            }

            try {
                getTopicFilePath(topicKey).deleteIfExists();
            } catch (IOException e) {
                LOGGER.warn().append("Failed to delete topic file: ").append(e).commit();
            }
        }
    }
}
