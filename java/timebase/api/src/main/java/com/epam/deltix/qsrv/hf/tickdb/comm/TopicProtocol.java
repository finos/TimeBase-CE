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
package com.epam.deltix.qsrv.hf.tickdb.comm;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
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

    public static void writeCreateTopicRequest(DataOutputStream out, CreateTopicRequest request, @SuppressWarnings("unused") int serverProtocolVersion) throws IOException {
        // Key
        out.writeUTF(request.getTopicKey());

        // Types
        writeTypes(out, request.getTypes());

        // Target Stream
        out.writeUTF(StringUtils.defaultString(request.getTargetStream()));

        // Target Space
        out.writeUTF(StringUtils.defaultString(request.getTargetSpace()));
    }

    public static CreateTopicRequest readCreateTopicRequest(DataInputStream in, @SuppressWarnings("unused") int clientVersion) throws IOException {
        // Key
        String key = in.readUTF();

        // Types
        List<RecordClassDescriptor> types = readTypes(in);

        // Target Stream
        String targetStream = in.readUTF();
        if (targetStream.isEmpty()) {
            targetStream = null;
        }

        // Target Space
        String targetSpace = in.readUTF();
        if (targetSpace.isEmpty()) {
            targetSpace = null;
        }

        return new CreateTopicRequest(key, types, targetStream, targetSpace);
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

        return new CreateMulticastTopicRequest(createTopicRequest.getTopicKey(), createTopicRequest.getTypes(),
                createTopicRequest.getTargetStream(), createTopicRequest.getTargetSpace(),
                endpointHost, endpointPort, networkInterface, ttl);
    }



    private static final EnumMap<TopicType, Integer> TOPIC_TYPE_TO_CODE_MAP = new EnumMap<>(TopicType.class);
    static {
        TOPIC_TYPE_TO_CODE_MAP.put(TopicType.IPC, 1);
        TOPIC_TYPE_TO_CODE_MAP.put(TopicType.MULTICAST, 2);
        TOPIC_TYPE_TO_CODE_MAP.put(TopicType.UDP_SINGLE_PUBLISHER, 3);
    }
    private static final TopicType[] CODE_TO_TOPIC_TYPE_MAP = new TopicType[TOPIC_TYPE_TO_CODE_MAP.size() + 1];
    static {
        for (Map.Entry<TopicType, Integer> entry : TOPIC_TYPE_TO_CODE_MAP.entrySet()) {
            int code = entry.getValue();
            if (CODE_TO_TOPIC_TYPE_MAP[code] != null) {
                throw new IllegalStateException("Multiple values mapped to single type code");
            }
            CODE_TO_TOPIC_TYPE_MAP[code] = entry.getKey();
        }
    }

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
        int topicTypeCode = in.readInt();
        TopicType topicType = null;
        if (topicTypeCode >= 0 && topicTypeCode < CODE_TO_TOPIC_TYPE_MAP.length) {
            topicType = CODE_TO_TOPIC_TYPE_MAP[topicTypeCode];
        }
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

        return new CreateCustomTopicRequest(createTopicRequest.getTopicKey(), createTopicRequest.getTypes(),
                createTopicRequest.getTargetStream(), createTopicRequest.getTargetSpace(), topicType, attributes);
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
    }

    public static AddTopicPublisherRequest readAddTopicPublisherRequest(DataInputStream in) throws IOException {
        // Key
        String key = in.readUTF();

        return new AddTopicPublisherRequest(key);
    }

    public static void writeAddTopicPublisherResponse(
            DataOutputStream out, AddTopicPublisherResponse response, @SuppressWarnings("unused") int clientVersion
    ) throws IOException {
        writeTransferType(out, response.getTransferType());
        writeTypes(out, response.getTypes());

        out.writeUTF(response.getPublisherChannel());

        out.writeUTF(StringUtils.defaultString(response.getAeronDir()));
        out.writeInt(response.getDataStreamId());
    }

    public static AddTopicPublisherResponse readAddTopicPublisherResponse(
            DataInputStream in, @SuppressWarnings("unused") int serverVersion
    ) throws IOException {
        TopicTransferType transferType = readTransferType(in);
        List<RecordClassDescriptor> types = readTypes(in);

        String publisherChannel = in.readUTF();

        String aeronDir = emptyToNull(in.readUTF());
        int dataStreamId = in.readInt();

        return new AddTopicPublisherResponse(transferType, types, publisherChannel,
                aeronDir, dataStreamId);
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
        writeTypes(out, response.getTypes());
        out.writeUTF(response.getChannel());
        out.writeUTF(StringUtils.defaultString(response.getAeronDir()));
        out.writeInt(response.getDataStreamId());
    }

    public static AddTopicSubscriberResponse readAddTopicSubscriberResponse(DataInputStream in) throws IOException {
        TopicTransferType transferType = readTransferType(in);
        List<RecordClassDescriptor> types = readTypes(in);
        String channel = in.readUTF();
        String aeronDir = emptyToNull(in.readUTF());
        int dataStreamId = in.readInt();

        return new AddTopicSubscriberResponse(transferType, types, channel, aeronDir, dataStreamId);
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

    /////////

    private static void writeTransferType(DataOutputStream out, TopicTransferType transferType) throws IOException {
        out.write(transferType.getProtocolCode());
    }

    private static TopicTransferType readTransferType(DataInputStream in) throws IOException {
        return TopicTransferType.getByCode(in.readByte());
    }

    private static void writeTypes(DataOutputStream out, List<RecordClassDescriptor> types) throws IOException {
        RecordClassSet md = new RecordClassSet();
        md.addContentClasses(types.toArray(new RecordClassDescriptor[0]));
        TDBProtocol.writeClassSet(out, md, TDBProtocol.VERSION);
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

    public static void writeTopicProtocolVersion(DataOutputStream out, int protocolVersion) throws IOException {
        out.writeInt(protocolVersion);
    }

    public static int readTopicProtocolVersion(DataInputStream in) throws IOException {
        return in.readInt();
    }
}