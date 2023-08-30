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

import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.util.concurrent.QuickExecutor;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicDBSettings {
    private final DXServerAeronContext aeronContext;
    private final DirectTopicRegistry topicRegistry;
    private final QuickExecutor executor;

    public TopicDBSettings(DXServerAeronContext aeronContext, DirectTopicRegistry topicRegistry, QuickExecutor executor) {
        this.topicRegistry = topicRegistry;
        this.aeronContext = aeronContext;
        this.executor = executor;
    }

    public DXServerAeronContext getAeronContext() {
        return aeronContext;
    }

    public DirectTopicRegistry getTopicRegistry() {
        return topicRegistry;
    }

    public QuickExecutor getExecutor() {
        return executor;
    }
}