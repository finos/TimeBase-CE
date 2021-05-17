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
package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.impl.topic.topicregistry.DirectTopicRegistry;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.AeronThreadTracker;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.aeron.DXServerAeronContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.util.security.TimebaseAccessController;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSConnectionListener;
import com.epam.deltix.util.concurrent.QuickExecutor;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

/**
 * Date: Mar 10, 2010
 */
public class VSConnectionHandler implements VSConnectionListener {
    private final DXTickDB          tickdb;
    private final Map<String, DXTickDB> userToDbCache = new HashMap<>();

    private final ServerParameters  params;
    private final SecurityContext   context;
    private final DXServerAeronContext aeronContext;
    private final AeronThreadTracker aeronThreadTracker;
    private final DirectTopicRegistry topicRegistry;

    public VSConnectionHandler(DXTickDB tickdb, ServerParameters params, @Nonnull DXServerAeronContext aeronContext, @Nonnull AeronThreadTracker aeronThreadTracker, @Nonnull DirectTopicRegistry topicRegistry) {
        this(tickdb, params, null, null, aeronContext, aeronThreadTracker, topicRegistry);
    }

    public VSConnectionHandler(DXTickDB tickdb, ServerParameters params, SecurityController controller, TimebaseAccessController ac, @Nonnull DXServerAeronContext aeronContext, @Nonnull AeronThreadTracker aeronThreadTracker, @Nonnull DirectTopicRegistry topicRegistry) {
        this.aeronContext = aeronContext;
        this.aeronThreadTracker = aeronThreadTracker;
        this.topicRegistry = topicRegistry;
        if (tickdb == null)
            throw new IllegalArgumentException ("db == null");

        context = controller != null ? new SecurityContext(controller, ac) : null;

        this.params = params;
        this.tickdb = tickdb;
    }

    @Override
    public void                 connectionAccepted (QuickExecutor executor, VSChannel serverChannel) {
        new RequestHandler (serverChannel, tickdb, userToDbCache, params, executor, context, aeronContext, aeronThreadTracker, topicRegistry).submit ();
    }
}
