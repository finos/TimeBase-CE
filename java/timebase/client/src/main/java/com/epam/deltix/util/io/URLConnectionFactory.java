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
package com.epam.deltix.util.io;

import com.epam.deltix.util.net.NetUtils;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class URLConnectionFactory {

    /*
        Create new URL connection from given url.
     */
    public static URLConnection create(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        connection.setReadTimeout(5000);

        if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection) connection).setSSLSocketFactory(SSLClientContextProvider.getSSLContext().getSocketFactory());

        return connection;
    }

    public static URLConnection verify(URLConnection connection, String user, String pass) throws IOException {
        if (connection instanceof HttpURLConnection) {
            HttpURLConnection http = (HttpURLConnection) connection;

            if (http.getResponseCode() == 302) { // redirect
                URLConnection redirect = create(new URL(http.getHeaderField("Location")));
                if (user != null && pass != null)
                    NetUtils.INSTANCE.authorize(redirect, user, pass);

                return redirect;
            }
        }

        return connection;
    }

    /*
        Create new URL connection.
     */
    public static URLConnection create(String host, int port, String file, boolean secured) throws IOException {
        URLConnection connection = new URL(getHttpProtocol(secured), host, port, file).openConnection();
        connection.setReadTimeout(5000);

        if (connection instanceof HttpsURLConnection)
            ((HttpsURLConnection) connection).setSSLSocketFactory(SSLClientContextProvider.getSSLContext().getSocketFactory());

        return connection;
    }

    public static String                getHttpProtocol(boolean ssl) {
        return ssl ? "https" : "http";
    }
}