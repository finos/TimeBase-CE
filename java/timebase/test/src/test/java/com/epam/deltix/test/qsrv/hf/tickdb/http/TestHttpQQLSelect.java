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
package com.epam.deltix.test.qsrv.hf.tickdb.http;

import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.zip.GZIPInputStream;

import com.epam.deltix.qsrv.hf.tickdb.http.HTTPProtocol;
import com.epam.deltix.qsrv.hf.tickdb.http.QQLRequest;
import com.epam.deltix.qsrv.hf.tickdb.http.TBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.http.TypeTransmission;
import com.epam.deltix.util.codec.Base64EncoderEx;
import com.epam.deltix.util.time.GMT;

/**
 *
 */
public class TestHttpQQLSelect {

    public static void main(String[] args) throws Exception {
        final long ts0 = System.currentTimeMillis();

        try {
            final boolean isBigEndian = args.length > 0 && "big".equals(args[0]);
            final boolean useCompression = args.length > 1 && HTTPProtocol.GZIP.equals(args[1]);
            final String user = args.length > 2 ? args[2] : null;
            final String password = args.length > 3 ? args[3] : "";
            test(useCompression, isBigEndian, user, password);
        } finally {
            final long ts1 = System.currentTimeMillis();
            System.out.println("Total time: " + (ts1 - ts0) + "ms");
        }
    }

    private static void test(boolean useCompression, boolean isBigEndian, String user, String password) throws Exception {
        Marshaller m = TBJAXBContext.createMarshaller();

        QQLRequest sr = request2Bars();
        //SelectQQLRequest sr = request2L1();
        sr.isBigEndian = isBigEndian;
        sr.useCompression = useCompression;

        final long ts0 = System.currentTimeMillis();

        final URL url = new URL("http://localhost:8011/tb/xml");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        m.marshal(sr, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            System.out.println("HTTP rc=" + rc + " " + conn.getResponseMessage());
            return;
        }

        // Content-Encoding [gzip]
        final InputStream is = HTTPProtocol.GZIP.equals(conn.getHeaderField(HTTPProtocol.CONTENT_ENCODING)) ?
                new GZIPInputStream(conn.getInputStream()) : conn.getInputStream();
        readSelectResponse(is, sr.isBigEndian);
        //readSelectResponse(is, "c:\\temp\\x.bin");
        final long ts1 = System.currentTimeMillis();
        System.out.println("select time: " + (ts1 - ts0) + "ms");
    }

    private static QQLRequest request2Bars() throws ParseException {
        QQLRequest sr = new QQLRequest();
        sr.qql = "select * from \"1min\"";
        sr.from = GMT.parseDateTime("2010-03-29 14:31:00").getTime();
        sr.to = GMT.parseDateTime("2010-03-29 14:35:00").getTime();
        return sr;
    }

    private static QQLRequest request2L1() throws ParseException {
        QQLRequest sr = new QQLRequest();
        sr.qql = "select * from \"iq_fp\"";
        sr.from = GMT.parseDateTime("2013-10-07 15:40:00").getTime();
        sr.to = GMT.parseDateTime("2013-10-07 17:50:00").getTime();
        return sr;
    }

    private static void readSelectResponse(InputStream is, boolean isBigEndian) throws IOException {

        long cnt = 0;
        HTTPCursor cur = new HTTPCursor(is, isBigEndian, TypeTransmission.DEFINITION, null);
        while (cur.next()) {
            System.out.println(cur.getMessage());
            cnt++;
        }

        System.out.println("Number of messages: " + cnt);
    }
}