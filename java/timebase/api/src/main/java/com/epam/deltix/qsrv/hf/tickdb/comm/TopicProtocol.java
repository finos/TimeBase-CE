/*
 * Copyright 2023 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.util.collections.ElementsEnumeration;
import com.epam.deltix.util.collections.generated.IntegerEntry;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicProtocol {

    private static final int TOPIC_TO_STREAM_SUPPORT_VERSION = 108;
    public static final int SINGLE_PUBLISHER_TOPIC_SUPPORT_VERSION = 109;

    public static void writeCreateTopicRequest(DataOutputStream out, CreateTopicRequest request, int serverProtocolVersion) throws IOException {
        // Key
        out.writeUTF(request.getTopicKey());

        // Types
        writeTypes(out, request.getTypes());

        // Entries
        writeEntities(out, request.getInitialEntitySet());

        if (serverProtocolVersion >= TOPIC_TO_STREAM_SUPPORT_VERSION) {
            // Target Stream
            out.writeUTF(StringUtils.defaultString(request.getTargetStream()));
        }
    }

    public static CreateTopicRequest readCreateTopicRequest(DataInputStream in, int clientVersion) throws IOException {
        // Key
        String key = in.readUTF();

        // Types
        List<RecordClassDescriptor> types = readTypes(in);

        // Entities
        List<ConstantIdentityKey> entries = readEntities(in);

        // Target Stream
        String targetStream;
        if (clientVersion >= TOPIC_TO_STREAM_SUPPORT_VERSION) {
            targetStream = in.readUTF();
            if (targetStream.length() == 0) {
                targetStream = null;
            }
        } else {
            targetStream = null;
        }

        return new CreateTopicRequest(key, types, entries, targetStream);
    }

    public static void writeCreateMulticastTopicRequest(DataOutputStream out, CreateMulticastTopicRequest request, int serverProtocolVersion) throws IOException {
        writeCreateTopicRequest(out, request, serverProtocolVersion);

        out.writeUTF(ObjectUtils.defaultIfNull(request.getEndpointHost(), ""));
        out.writeInt(ObjectUtils.defaultIfNull(request.getEndpointPort(), -1));
        out.writeUTF(ObjectUtils.defaultIfNull(request.getNetworkInterface(), ""));
        out.writeInt(ObjectUtils.defaultIfNull(request.getTtl(), -1));
    }

    public static CreateMulticastTopicRequest readCreateMulticastTopicRequest(DataInputStream in, int clientVersion) throws IOException {
        CreateTopicRequest createTopicRequest = readCreateTopicRequest(in, clientVersion);

        String endpointHost = emptyToNull(in.readUTF());
        Integer endpointPort = in.readInt();
        if (endpointPort < 0) {
            endpointPort = null;
        }
        String networkInterface = emptyToNull(in.readUTF());
        Integer ttl = in.readInt();
        if (ttl < 0) {
            ttl = null;
        }

        return new CreateMulticastTopicRequest(createTopicRequest.getTopicKey(), createTopicRequest.getTypes(), createTopicRequest.getInitialEntitySet(),
                createTopicRequest.getTargetStream(), endpointHost, endpointPort, networkInterface, ttl);
    }



    private static final BiMap<TopicType, Integer> TOPIC_TYPE_TO_CODE_MAP = ImmutableBiMap.of(
            TopicType.IPC, 1,
            TopicType.MULTICAST, 2,
            TopicType.UDP_SINGLE_PUBLISHER, 3
    );

    public static void writeCreateCustomTopicRequest(DataOutputStream out, CreateCustomTopicRequest request, int serverProtocolVersion) throws IOException {
        writeCreateTopicRequest(out, request, serverProtocolVersion);

        Map<CreateCustomTopicRequest.Field, String> attributes = request.getAttributes();

        // topicType
        out.writeInt(TOPIC_TYPE_TO_CODE_MAP.get(request.getTopicType()));

        out.writeInt(attributes.size());
        for (Map.Entry<CreateCustomTopicRequest.Field, String> entry : attributes.entrySet()) {
            CreateCustomTopicRequest.Field key = entry.getKey();
            String value = entry.getValue();

            assert key != null;
            assert value != null;

            out.writeUTF(key.name());
            out.writeUTF(value);
        }
    }

    public static CreateCustomTopicRequest readCreateCustomTopicRequest(DataInputStream in, int clientVersion) throws IOException {
        CreateTopicRequest createTopicRequest = readCreateTopicRequest(in, clientVersion);

        // topicType
        TopicType topicType = TOPIC_TYPE_TO_CODE_MAP.inverse().get(in.readInt());
        if (topicType == null) {
            throw new IllegalStateException();
        }

        int attributeCount = in.readInt();
        Map<CreateCustomTopicRequest.Field, String> attributes = new HashMap<>(attributeCount);
        for (int i = 0; i < attributeCount; i++) {
            String key = in.readUTF();
            String value = in.readUTF();
            attributes.put(CreateCustomTopicRequest.Field.valueOf(key), value);
        }

        return new CreateCustomTopicRequest(createTopicRequest.getTopicKey(), createTopicRequest.getTypes(), createTopicRequest.getInitialEntitySet(),
                createTopicRequest.getTargetStream(), topicType, attributes);
    }

    public static void writeDeleteTopicRequest(DataOutputStream out, DeleteTopicRequest request) throws IOException {
        // Key
        out.writeUTF(request.getTopicKey());
    }

    public static DeleteTopicRequest readDeleteTopicRequest(DataInputStream in) throws IOException {
        // Key
        String key = in.readUTF();

        return new DeleteTopicRequest(key);
    }

    public static void writeAddTopicPublisherRequest(DataOutputStream out, AddTopicPublisherRequest request) throws IOException {
        // Key
        out.writeUTF(request.getTopicKey());
        // Entities
        writeEntities(out, request.getInitialEntitySet());
    }

    public static AddTopicPublisherRequest readAddTopicPublisherRequest(DataInputStream in) throws IOException {
        // Key
        String key = in.readUTF();
        // Entities
        List<ConstantIdentityKey> entities = readEntities(in);

        return new AddTopicPublisherRequest(key, entities);
    }

    public static void writeAddTopicPublisherResponse(DataOutputStream out, AddTopicPublisherResponse response, int clientVersion) throws IOException {
        writeTransferType(out, response.getTransferType());
        writeEntities(out, response.getMapping());
        writeTypes(out, response.getTypes());

        out.writeUTF(response.getPublisherChannel());
        if (clientVersion >= SINGLE_PUBLISHER_TOPIC_SUPPORT_VERSION) {
            out.writeUTF(response.getMetadataSubscriberChannel());
        }
        out.writeUTF(StringUtils.defaultString(response.getAeronDir()));
        out.writeInt(response.getDataStreamId());
        out.writeInt(response.getServerMetadataStreamId());
        out.write(response.getLoaderNumber());
        out.writeInt(response.getMinTempEntityIndex());
        out.writeInt(response.getMaxTempEntityIndex());
    }

    public static AddTopicPublisherResponse readAddTopicPublisherResponse(DataInputStream in, int serverVersion) throws IOException {
        TopicTransferType transferType = readTransferType(in);
        List<ConstantIdentityKey> mapping = readEntities(in);
        List<RecordClassDescriptor> types = readTypes(in);

        String publisherChannel = in.readUTF();
        String metadataChannel;
        if (serverVersion >= SINGLE_PUBLISHER_TOPIC_SUPPORT_VERSION) {
            metadataChannel = in.readUTF();
        } else {
            metadataChannel = publisherChannel;
        }
        String aeronDir = emptyToNull(in.readUTF());
        int dataStreamId = in.readInt();
        int serverMetadataStreamId = in.readInt();
        byte loaderNumber = (byte) in.read();
        int minTempEntityIndex = in.readInt();
        int maxTempEntityIndex = in.readInt();

        return new AddTopicPublisherResponse(transferType, mapping, types, publisherChannel,
                metadataChannel, aeronDir, dataStreamId, serverMetadataStreamId, loaderNumber, minTempEntityIndex, maxTempEntityIndex);
    }

    public static void writeAddTopicSubscriberRequest(DataOutputStream out, AddTopicSubscriberRequest request) throws IOException {
        // Key
        out.writeUTF(request.getTopicKey());
    }

    public static AddTopicSubscriberRequest readAddTopicSubscriberRequest(DataInputStream in) throws IOException {
        // Key
        String key = in.readUTF();

        return new AddTopicSubscriberRequest(key);
    }

    public static void writeAddTopicSubscriberResponse(DataOutputStream out, AddTopicSubscriberResponse response) throws IOException {
        writeTransferType(out, response.getTransferType());
        writeEntities(out, response.getMapping());
        writeTypes(out, response.getTypes());
        out.writeUTF(response.getChannel());
        out.writeUTF(StringUtils.defaultString(response.getAeronDir()));
        out.writeInt(response.getDataStreamId());
    }

    public static AddTopicSubscriberResponse readAddTopicSubscriberResponse(DataInputStream in) throws IOException {
        TopicTransferType transferType = readTransferType(in);
        List<ConstantIdentityKey> mapping = readEntities(in);
        List<RecordClassDescriptor> types = readTypes(in);
        String channel = in.readUTF();
        String aeronDir = emptyToNull(in.readUTF());
        int dataStreamId = in.readInt();

        return new AddTopicSubscriberResponse(transferType, mapping, types, channel, aeronDir, dataStreamId);
    }

    public static void writeListTopicsResponse(DataOutputStream out, ListTopicsResponse response) throws IOException {
        List<String> topics = response.getTopics();
        int topicCount = topics.size();
        out.writeInt(topicCount);
        for (String topic : topics) {
            out.writeUTF(topic);
        }
    }

    public static ListTopicsResponse readListTopicsResponse(DataInputStream in) throws IOException {
        int topicCount = in.readInt();
        List<String> topics = new ArrayList<>(topicCount);
        for (int i = 0; i < topicCount; i++) {
            String topicKey = in.readUTF();
            topics.add(topicKey);
        }

        return new ListTopicsResponse(topics);
    }

    // GetTopicMetadata

    public static void writeGetTopicMetadataRequest(DataOutputStream out, GetTopicMetadataRequest request) throws IOException {
        out.writeUTF(request.getTopicKey()); // Key
    }

    public static GetTopicMetadataRequest readGetTopicMetadataRequest(DataInputStream in) throws IOException {
        String key = in.readUTF(); // Key
        return new GetTopicMetadataRequest(key);
    }

    public static void writeGetTopicMetadataResponse(DataOutputStream out, GetTopicMetadataResponse response) throws IOException {
        writeTypes(out, response.getTypes());
    }

    public static GetTopicMetadataResponse readGetTopicMetadataResponse(DataInputStream in) throws IOException {
        List<RecordClassDescriptor> types = readTypes(in);

        return new GetTopicMetadataResponse(types);
    }

    // GetTopicInstrumentMapping

    public static void writeGetTopicInstrumentMappingRequest(DataOutputStream out, GetTopicInstrumentMappingRequest request) throws IOException {
        out.writeUTF(request.getTopicKey()); // Key
        out.writeInt(request.getDataStreamId());
    }

    public static GetTopicInstrumentMappingRequest readGetTopicInstrumentMappingRequest(DataInputStream in) throws IOException {
        String key = in.readUTF(); // Key
        int dataStreamId = in.readInt();
        return new GetTopicInstrumentMappingRequest(key, dataStreamId);
    }

    public static void writeGetTopicInstrumentMappingResponse(DataOutputStream out, GetTopicInstrumentMappingResponse response) throws IOException {
        writeEntities(out, response.getMapping());
    }

    public static GetTopicInstrumentMappingResponse readGetTopicInstrumentMappingResponse(DataInputStream in) throws IOException {
        List<ConstantIdentityKey> entries = readEntities(in);
        return new GetTopicInstrumentMappingResponse(entries);
    }

    // GetTopicInstrumentMapping

    public static void writeGetTopicTemporaryInstrumentMappingRequest(DataOutputStream out, GetTopicTemporaryInstrumentMappingRequest request) throws IOException {
        out.writeUTF(request.getTopicKey()); // Key
        out.writeInt(request.getDataStreamId());
        out.writeInt(request.getRequestedTempEntityIndex());
    }

    public static GetTopicTemporaryInstrumentMappingRequest readGetTopicTemporaryInstrumentMappingRequest(DataInputStream in) throws IOException {
        String key = in.readUTF(); // Key
        int dataStreamId = in.readInt();
        int requestedTempEntityIndex = in.readInt();
        return new GetTopicTemporaryInstrumentMappingRequest(key, dataStreamId, requestedTempEntityIndex);
    }

    public static void writeGetTopicTemporaryInstrumentMappingResponse(DataOutputStream out, GetTopicTemporaryInstrumentMappingResponse response) throws IOException {
        IntegerToObjectHashMap<ConstantIdentityKey> mapping = response.getMapping();
        out.writeInt(mapping.size());
        ElementsEnumeration<ConstantIdentityKey> elements = mapping.elements();
        IntegerEntry entry = (IntegerEntry) elements;
        while (elements.hasMoreElements()) {
            int key = entry.keyInteger();
            ConstantIdentityKey element = elements.nextElement();
            out.writeInt(key);
            writeIdentityKey(out, element);
        }
    }

    public static GetTopicTemporaryInstrumentMappingResponse readGetTopicTemporaryInstrumentMappingResponse(DataInputStream in) throws IOException {
        int entryCount = in.readInt();
        IntegerToObjectHashMap<ConstantIdentityKey> mapping = new IntegerToObjectHashMap<>(entryCount);

        //InstrumentType[] instrumentTypes = InstrumentType.values();
        for (int i = 0; i < entryCount; i++) {
            int key = in.readInt();
            ConstantIdentityKey instrumentKey = readInstrumentKey(in);
            mapping.put(key, instrumentKey);
        }

        return new GetTopicTemporaryInstrumentMappingResponse(mapping);
    }

    /////////

    private static void writeTransferType(DataOutputStream out, TopicTransferType transferType) throws IOException {
        out.write(transferType.getProtocolCode());
    }

    private static TopicTransferType readTransferType(DataInputStream in) throws IOException {
        return TopicTransferType.getByCode(in.readByte());
    }


    private static void writeEntities(DataOutputStream out, Collection<? extends IdentityKey> entities) throws IOException {
        out.writeInt(entities.size());
        for (IdentityKey entry : entities) {
            writeIdentityKey(out, entry);
        }
    }

    private static void writeIdentityKey(DataOutputStream out, IdentityKey entry) throws IOException {
        out.writeUTF(entry.getSymbol().toString()); // TODO: Check if we can avoid this cast
    }

    @Nonnull
    private static ConstantIdentityKey readInstrumentKey(DataInputStream in) throws IOException {
        String symbol = in.readUTF();
        return new ConstantIdentityKey(symbol);
    }

    @Nonnull
    private static List<ConstantIdentityKey> readEntities(DataInputStream in) throws IOException {
        int entryCount = in.readInt();
        List<ConstantIdentityKey> entries;
        if (entryCount > 0) {
            entries = new ArrayList<>(entryCount);
            for (int i = 0; i < entryCount; i++) {
                ConstantIdentityKey instrumentKey = readInstrumentKey(in);
                entries.add(instrumentKey);
            }
        } else {
            entries = Collections.emptyList();
        }
        return entries;
    }


    private static void writeTypes(DataOutputStream out, List<RecordClassDescriptor> types) throws IOException {
        RecordClassSet md = new RecordClassSet();
        md.addContentClasses(types.toArray(new RecordClassDescriptor[0]));
        TDBProtocol.writeClassSet(out, md);
    }

    @Nonnull
    private static List<RecordClassDescriptor> readTypes(DataInputStream in) throws IOException {
        RecordClassSet recordClassSet = (RecordClassSet) TDBProtocol.readClassSet(in);
        RecordClassDescriptor[] contentClasses = recordClassSet.getContentClasses();
        return Arrays.asList(contentClasses);
    }

    @Nullable
    private static String emptyToNull(@Nonnull String str) {
        return str.isEmpty() ? null : str;
    }
}