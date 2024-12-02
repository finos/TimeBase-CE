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

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CreateTopicRequest {
    private final String topicKey;
    private final List<RecordClassDescriptor> types;
    private final String targetStream;
    private final String targetSpace;

    public CreateTopicRequest(String topicKey, List<RecordClassDescriptor> types, @Nullable String targetStream, @Nullable String targetSpace) {
        if (targetSpace != null && targetStream == null) {
            throw new IllegalArgumentException("targetSpace is set but targetStream is not set");
        }
        this.topicKey = topicKey;
        this.types = types;

        this.targetStream = targetStream;
        this.targetSpace = targetSpace;
    }

    @Nonnull
    public String getTopicKey() {
        return topicKey;
    }

    @Nonnull
    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    @Nullable
    public String getTargetStream() {
        return targetStream;
    }

    @Nullable
    public String getTargetSpace() {
        return targetSpace;
    }
}