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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.pub.ChannelCompression;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.vsocket.VSChannel;

import java.io.IOException;

public interface DXRemoteDB extends DXTickDB {

    VSChannel           connect() throws IOException;

    /**
     * @param channelBufferSize Sets channel buffer size. If value is 0 then buffer size determined automatically.
     */
    VSChannel           connect(ChannelType type, boolean autoCommit, boolean noDelay, ChannelCompression c, int channelBufferSize) throws IOException;

    int                 getServerProtocolVersion();

    CodecFactory        getCodecFactory(ChannelQualityOfService channelQOS);

    SessionClient       getSession(); // TODO: refactor to interface

    /**
     * Sets CPU affinity for TB client threads, if needed.
     */
    void                setAffinityConfig(AffinityConfig affinityConfig);

    QuickExecutor       getQuickExecutor();
}