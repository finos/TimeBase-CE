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

import com.epam.deltix.qsrv.hf.pub.md.UHFJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.JettyServer;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.http.CreateStreamRequest;
import com.epam.deltix.qsrv.hf.tickdb.http.StreamDef;
import com.epam.deltix.qsrv.hf.tickdb.http.TBJAXBContext;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.testsetup.TickDBCreator;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;

public class BaseTest {

    protected static TDBRunner      runner;
    protected static Marshaller     marshaller;
    protected static String         TB_HOST = "localhost";
    protected static java.net.URL   URL;

    protected static Marshaller     global;

    @BeforeClass
    public static void start() throws Throwable {
        runner = new TDBRunner(true, true, new JettyServer());
        runner.startup();

        TickDBCreator.createBarsStream(runner.getServerDb(), TickDBCreator.BARS_STREAM_KEY);

        marshaller = TBJAXBContext.createMarshaller();
        global = UHFJAXBContext.createMarshaller();
        URL = new URL("http://" + TB_HOST + ":" + runner.getHttpPort() + "/tb/xml");
        //URL = new URL("http://localhost:8011/tb/xml");
    }

    public static URL   getPath(String path) throws MalformedURLException {
        return new URL("http://localhost:" + runner.getPort() + "/" + path);
    }

    public static void         createStream(String key, StreamOptions options) throws JAXBException, IOException {
        CreateStreamRequest request = new CreateStreamRequest();
        request.key = key;
        request.options = new StreamDef(options);

        StringWriter writer = new StringWriter();
        global.marshal(options.getMetaData(), writer);
        request.options.metadata = writer.getBuffer().toString();

        TestXmlQueries.query(URL, null, null, request);
    }

    @AfterClass
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }
}
