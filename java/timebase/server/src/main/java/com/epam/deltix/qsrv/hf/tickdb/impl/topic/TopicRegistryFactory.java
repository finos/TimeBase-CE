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

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class TopicRegistryFactory {
    public static DirectTopicRegistry initRegistryAtQSHome(DXServerAeronContext aeronContext) {
        return initRegistryAtPath(aeronContext, QSHome.get());
    }

    public static DirectTopicRegistry initRegistryAtPath(DXServerAeronContext aeronContext, String path) {
        TopicStorage topicStorage = TopicStorage.createAtPath(path);
        DirectTopicRegistry topicRegistry = new DirectTopicRegistry(topicStorage.getPersistingListener());
        topicStorage.loadTopicDataInto(topicRegistry, aeronContext.getStreamIdGenerator());
        return topicRegistry;
    }
}