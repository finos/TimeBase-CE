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
package com.epam.deltix.util.vsocket;

import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.vsocket.util.SocketTestUtilities;
import com.epam.deltix.util.vsocket.util.TestServerSocketFactory;
import org.junit.Test;

import java.io.*;
import java.net.*;

/**
 *
 */
public class Test_SocketEcho {
    public static void main (String [] args) throws Exception {
        int port = SocketTestUtilities.parsePort(args);

        TestServerSocketFactory.ServerThread server = TestServerSocketFactory.createEchoServerSocket(port);
        server.start ();

        client ("localhost", server.getLocalPort());
    }

    public static void  client (String host, int port)
        throws IOException {
        String utfString = "Hello world";

        Socket s = null;
        try {
            s = new Socket(host, port);
            DataOutputStream os = new DataOutputStream(s.getOutputStream());
            DataInputStream is = new DataInputStream(s.getInputStream());

            os.writeUTF(utfString);
            os.flush();

            String readStr = is.readUTF();
            if (!readStr.equals(utfString))
                throw new AssertionError(readStr + " != " + utfString);

        } catch (Throwable x) {
            x.printStackTrace();
            throw x;
        } finally {
            IOUtil.close(s);
        }
    }

    @Test
    public void TestSocket() throws Exception {
        Test_SocketEcho.main(new String[0]);
    }
}