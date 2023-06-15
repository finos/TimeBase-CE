/*
 * Copyright 2021 EPAM Systems, Inc
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

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class CreateTopicRequest {
    private final String topicKey;
    private final List<RecordClassDescriptor> types;
    private final Collection<? extends IdentityKey> initialEntitySet;
    private final String targetStream;

    public CreateTopicRequest(String topicKey, List<RecordClassDescriptor> types, @Nullable Collection<? extends IdentityKey> initialEntitySet, @Nullable String targetStream) {
        this.topicKey = topicKey;
        this.types = types;
        if (initialEntitySet == null) {
            initialEntitySet = Collections.emptyList();
        }
        this.initialEntitySet = initialEntitySet;
        this.targetStream = targetStream;
    }

    @Nonnull
    public String getTopicKey() {
        return topicKey;
    }

    @Nonnull
    public List<RecordClassDescriptor> getTypes() {
        return types;
    }

    @Nonnull
    public Collection<? extends IdentityKey> getInitialEntitySet() {
        return initialEntitySet;
    }

    @Nullable
    public String getTargetStream() {
        return targetStream;
    }
}