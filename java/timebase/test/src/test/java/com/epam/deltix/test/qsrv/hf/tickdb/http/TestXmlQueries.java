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

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import com.epam.deltix.qsrv.hf.tickdb.http.ListStreamsRequest;
import com.epam.deltix.qsrv.hf.tickdb.http.ListStreamsResponse;
import com.epam.deltix.qsrv.hf.tickdb.http.TBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.http.XmlRequest;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

import com.epam.deltix.qsrv.hf.tickdb.http.stream.*;
import com.epam.deltix.util.codec.Base64EncoderEx;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.GMT;

/**
 *
 */
public class TestXmlQueries {
    public static void main(String[] args) throws Exception {
        final String user = args.length > 0 ? args[0] : null;
        final String password = args.length > 1 ? args[1] : "";
        testListStreams(user, password);
        testListEntities(user, password, "1min");
        //testListEntities(user, password, "1min", InstrumentType.BOND);
        testGetRange(user, password, "iq_fp", new ConstantIdentityKey("AAPL"));
        testGetRange(user, password, "iq_fp", new ConstantIdentityKey("BAD"));
        testGetPeriodicity(user, password, "iq_fp");
        testGetPeriodicity(user, password, "1min");
    }

    private static void testListStreams(String user, String password) throws IOException, JAXBException {
        ListStreamsRequest req = new ListStreamsRequest();
        ListStreamsResponse resp = (ListStreamsResponse) query(user, password, req);
        System.out.println("streams: " + Util.printArray(resp.streams));
    }

    private static void testListEntities(String user, String password, String stream) throws IOException, JAXBException {
        ListEntitiesRequest req = new ListEntitiesRequest();
        req.stream = stream;
        ListEntitiesResponse resp = (ListEntitiesResponse) query(user, password, req);
        System.out.println("stream: " + stream + " " + Util.printArray(resp.identities));
    }

    private static void testGetRange(String user, String password, String stream, IdentityKey... instruments) throws IOException, JAXBException {
        GetRangeRequest req = new GetRangeRequest();
        req.stream = stream;
        req.identities = BaseTest.getSymbols(instruments);
        GetRangeResponse resp = (GetRangeResponse) query(user, password, req);

        System.out.print("stream: " + stream + " instruments: " + Util.printArray(instruments));
        if (resp.timeRange != null)
            System.out.println(" [" + GMT.formatDateTimeMillis(resp.timeRange.from) + "," + GMT.formatDateTimeMillis(resp.timeRange.to) + "]");
        else
            System.out.println(" null");
    }

    private static void testGetPeriodicity(String user, String password, String stream) throws IOException, JAXBException {
        GetPeriodicityRequest req = new GetPeriodicityRequest();
        req.stream = stream;
        GetPeriodicityResponse resp = (GetPeriodicityResponse) query(user, password, req);

        System.out.println("stream: " + stream + " periodicity: " + resp.periodicity.toString());
    }

    public static Object query(String user, String password, XmlRequest request) throws IOException, JAXBException {
        return query(new URL("http://localhost:8011/tb/xml"), user, password, request);
    }

    public static Object query(URL url, String user, String password, XmlRequest request) throws IOException, JAXBException {
        return query(url, user, password, request, TBJAXBContext.createUnmarshaller());
    }

    public static Object query(URL url, String user, String password, XmlRequest request, Unmarshaller u) throws IOException, JAXBException {
        Marshaller m = TBJAXBContext.createMarshaller();

        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();

        m.marshal(request, os);
        int rc = conn.getResponseCode();
        if (rc != 200) {
            throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());
        }

        InputStream is = conn.getInputStream();

//        try {
//            System.out.println(IOUtil.readFromStream(is));
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        if (is.available() > 0)
            return u.unmarshal(is);

        return null;
    }
}
