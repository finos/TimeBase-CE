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

package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.qsrv.hf.pub.RawMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicReplicationUtil {
    private static final Log LOGGER = LogFactory.getLog(TopicReplicationUtil.class);

    /**
     * Returns map with type matches between sourceTypeList and destinationTypes.
     *
     * @param sourceTypeList list of types to search match for
     * @param destinationTypes list of types to search match in
     * @return mapping between topic types and stream types.
     * @throws IllegalArgumentException if there is no match for some types from sourceTypeList in destinationTypes
     */
    public static Map<RecordClassDescriptor, RecordClassDescriptor> findTypeMatches(List<RecordClassDescriptor> sourceTypeList, RecordClassDescriptor[] destinationTypes) {
        if (sourceTypeList.size() == 1) {
            RecordClassDescriptor topicType = sourceTypeList.get(0);
            return Collections.singletonMap(topicType, findTypeMatch(topicType, destinationTypes));
        }
        Map<RecordClassDescriptor, RecordClassDescriptor> result = new HashMap<>(sourceTypeList.size());
        for (RecordClassDescriptor topicType : sourceTypeList) {
            result.put(topicType, findTypeMatch(topicType, destinationTypes));
        }
        return result;
    }

    @NotNull
    static RecordClassDescriptor findTypeMatch(RecordClassDescriptor topicType, RecordClassDescriptor[] streamTypes) {
        RecordClassDescriptor match = com.epam.deltix.qsrv.hf.stream.MessageProcessor.findMatch(topicType, streamTypes);
        if (match == null) {
            throw new IllegalArgumentException("Destination stream does not support all topic's types. Missing or incompatible type: " + topicType.getName());
        }
        return match;
    }

    /**
     * Transforms message type according to provided mapping.
     */
    public static MessageProcessor createTypeTransformingMessageProcessor(MessageProcessor baseMessageProcessor, Map<RecordClassDescriptor, RecordClassDescriptor> typeTransformationMap) {

        // In trace level mode we can log messages
        MessageProcessor messageProcessor;
        if (LOGGER.isEnabled(LogLevel.TRACE)) {
            messageProcessor = message -> {
                baseMessageProcessor.process(message);
                LOGGER.log(LogLevel.TRACE,"Message copied to stream: ts=%s and symbol=%s").with(message.getTimeStampMs()).with(message.getSymbol());
            };
        } else {
            messageProcessor = baseMessageProcessor;
        }

        return message -> {
            RawMessage raw = (RawMessage) message;
            RecordClassDescriptor oldType = raw.type;
            RecordClassDescriptor newType = typeTransformationMap.get(oldType);
            if (newType == null) {
                throw new IllegalStateException("Unexpected type:" + oldType);
            }
            raw.type = newType; // Update type on message
            messageProcessor.process(raw);
        };
    }
}
