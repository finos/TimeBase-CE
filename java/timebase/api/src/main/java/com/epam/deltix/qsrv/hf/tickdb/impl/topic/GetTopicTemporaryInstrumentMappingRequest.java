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
package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

/**
 * @author Alexei Osipov
 */
public class GetTopicTemporaryInstrumentMappingRequest extends BaseTopicRequest {
    private final int dataStreamId; // We send data streamId so we can ensure that snapshot is for the exactly same topic
    private final int requestedTempEntityIndex; // Temp entry index that client wants to look up

    public GetTopicTemporaryInstrumentMappingRequest(String topicKey, int dataStreamId, int requestedTempEntityIndex) {
        super(topicKey);
        this.dataStreamId = dataStreamId;
        this.requestedTempEntityIndex = requestedTempEntityIndex;
    }

    public int getDataStreamId() {
        return dataStreamId;
    }

    public int getRequestedTempEntityIndex() {
        return requestedTempEntityIndex;
    }
}