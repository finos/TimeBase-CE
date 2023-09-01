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
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.UserPrincipal;
import com.epam.deltix.util.vsocket.VSChannel;

import java.io.IOException;

/**
 *
 */
public class SecuredDbClient extends TickDBClient {

    public SecuredDbClient(String host, int port, boolean ssl, String user, String pass) {
        super(host, port, ssl, user, pass);
    }

    public SecuredDbClient(TickDBClient client) {
        super(client.getHost(), client.getPort(), client.enableSSL, new UserPrincipal(client.getUser()));
    }

    public VSChannel               connect(ChannelType type, boolean autoCommit, boolean noDelay, ChannelCompression c, int channelBufferSize)
            throws IOException
    {
        VSChannel channel = createChannel(type, autoCommit, noDelay, c, 0);
        TDBProtocol.writeCredentials(channel, getUser(), UserContext.get());
        return channel;
    }
}