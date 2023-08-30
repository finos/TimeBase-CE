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
package com.epam.deltix.qsrv.servlet;

import com.epam.deltix.qsrv.QSHome;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class HomeServlet extends HttpServlet {

    // HomeServlet is never protected by SSL or UAC

    @Override
    protected void      service (HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        resp.setContentType("text/plain");
        DataOutputStream dout = new DataOutputStream(resp.getOutputStream());
        dout.writeUTF(QSHome.get());
        dout.flush();
    }

    public static String get(String host, int port) throws IOException {
        URLConnection connection =
                new URL("http", host, port, "/gethome").openConnection();
        connection.setReadTimeout(5000);

        InputStream in = connection.getInputStream();
        String value = read(in);
        in.close();

        return value;
    }

    public static String read(InputStream in) throws IOException {
        return new DataInputStream(in).readUTF();
    }
}