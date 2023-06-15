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
package com.epam.deltix.util.vsocket;

import org.junit.Test;

import java.io.IOException;

public class Test_VSocketConnection {

    @Test
    public void testSocketClose() throws InterruptedException, IOException {

        VSServer server = new VSServer();

        server.setConnectionListener(
                (executor, serverChannel) -> {
                    //new ServerThread (executor, serverChannel).submit ();
                }
        );

        server.start();

        VSClient client = new VSClient("localhost", server.getLocalPort());
        client.connect();

        for (int i = 0; i < 10; i++) {
            VSChannel channel = client.openChannel();
            channel.close();
        }

        client.close();
        server.close();
    }
}