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

/*  ##TICKDB.FAST## */

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.http.*;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.ListEntitiesRequest;
import com.epam.deltix.qsrv.hf.tickdb.http.stream.ListEntitiesResponse;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.util.codec.Base64EncoderEx;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.lang.Util;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AccessControlException;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 *
 */
public class Test_HttpSecured {
    private static String       TEST_DIR            = Home.getPath("temp/tdbhttpsecurity");
    private static String       SECURITY_DIR        = Home.getPath("testdata/tickdb/security");
    private static String       TB_LOCATION         = TEST_DIR + "/tickdb";
    private static String       CONFIG_LOCATION     = TEST_DIR + "/config";
    private static String       SECURITY_FILE_NAME  = "uac-file-security.xml";
    private static String       SECURITY_RULES_NAME = "uac-access-rules.xml";

    private static String       ADMIN_STREAM        = "test_http";
    private static int          ADMIN_STREAM_MSGS   = 50_000;

    private static String       ADMIN_USER          = "admin";
    private static String       ADMIN_PASS          = "admin";

    private static String       JOHN_DOE_USER       = "JohnDoe";
    private static String       JOHN_DOE_PASS       = "QWERTY";

    private static String       OWNERS_MANAGER_USER = "OwnersManager";
    private static String       OWNERS_MANAGER_PASS = "123";

    private static String       USER1 = "User1";
    private static String       USER1_PASS = "123";

    private static String       USER2 = "User2";
    private static String       USER2_PASS = "123";

    private static String       USER3 = "User3";
    private static String       USER3_PASS = "123";

    private static String       USER4 = "User4";
    private static String       USER4_PASS = "123";

    private static TDBRunner    runner;

    @BeforeClass
    public static void start() throws Throwable {
        File home = new File(TEST_DIR);
        System.out.print("Removing directory...");
        FileUtils.deleteDirectory(home);
        System.out.println("OK");

        if (!home.mkdirs())
            throw new IllegalStateException("Cannot create:" + home.getAbsolutePath());

        QSHome.set(home.getAbsolutePath());

        FileUtils.copyFile(new File(SECURITY_DIR + File.separator + SECURITY_FILE_NAME),
                new File(CONFIG_LOCATION + File.separator + SECURITY_FILE_NAME));
        FileUtils.copyFile(new File(SECURITY_DIR + File.separator + SECURITY_RULES_NAME),
                new File(CONFIG_LOCATION + File.separator + SECURITY_RULES_NAME));

        StartConfiguration configuration = StartConfiguration.create(true, false, false);
        configuration.quantServer.setProperty("security", "FILE");
        configuration.quantServer.setProperty("security.config", SECURITY_FILE_NAME);

        runner = new TDBRunner(true, true, TB_LOCATION, new TomcatServer(configuration));
        runner.user = ADMIN_USER;
        runner.pass = ADMIN_PASS;
        runner.startup();

        //create admin's stream
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();
        createAndLoadBarsStream(admin_connection, ADMIN_STREAM, ADMIN_STREAM_MSGS);
    }

    @AfterClass
    public static void      stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @Test
    public void                 testHTTPSimple() throws IOException, JAXBException {
        testListStreams(ADMIN_USER, ADMIN_PASS, "events#", "test_http");
        testListStreams(USER4, USER4_PASS, "test_http");
        testListStreams(USER3, USER3_PASS);

        testListEntities(USER4, USER4_PASS, ADMIN_STREAM, false,
                new ConstantIdentityKey("ORCL"),
                new ConstantIdentityKey("DLTX"));

        testListEntities(USER3, USER3_PASS, ADMIN_STREAM, true,
                new ConstantIdentityKey("ORCL"),
                new ConstantIdentityKey("DLTX"));
    }

    @Test
    public void                 testHTTPSelect() throws IOException, JAXBException {
        SelectRequest sr = new SelectRequest();
        sr.streams = new String[] { ADMIN_STREAM };
        sr.types = new String[] { BarMessage.CLASS_NAME};
        sr.typeTransmission = TypeTransmission.DEFINITION;

        assert testSelect(USER4, USER4_PASS, sr, false) == ADMIN_STREAM_MSGS;
        testSelect(USER3, USER3_PASS, sr, true);
    }

    @Test
    public void testQQLSelect() throws IOException, JAXBException {
        QQLRequest qqlRequest = new QQLRequest();
        qqlRequest.qql = "select * from " + ADMIN_STREAM;

        assert testSelect(USER4, USER4_PASS, qqlRequest, false) == ADMIN_STREAM_MSGS;
        try {
            testSelect(USER3, USER3_PASS, qqlRequest, false);
            assert false;
        } catch (RuntimeException e) {
            assert true;
        }
    }

    @Test
    public void testSelectAsStruct() throws IOException, JAXBException {
        SelectAsStructRequest structRequest = new SelectAsStructRequest();
        RecordType im = new RecordType();
        RecordType rt = new RecordType();
        rt.name = BarMessage.CLASS_NAME;
        rt.columns = new Column[] { new Column("close", 0) };
        structRequest.types = new RecordType[] { rt };
        structRequest.stream = ADMIN_STREAM;

        try {
            query(USER3, USER3_PASS, structRequest);
            assert false;
        } catch (AccessControlException e) {
            assert true;
        }
    }

    @Test
    public void testLoader() throws IOException {
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();

        String streamName = "bars_for_loading";
        DXTickStream bars = createBarsStream(admin_connection, streamName);
        try {
            testBarsLoad(ADMIN_USER, ADMIN_PASS, streamName, 10_000, false);
            testBarsLoad(USER4, USER4_PASS, streamName, 10_000, true);
        } finally {
            if (bars != null)
                bars.delete();
        }
    }

    private void testListStreams(String user, String password, String... names) throws IOException, JAXBException {
        ListStreamsRequest req = new ListStreamsRequest();
        ListStreamsResponse resp = (ListStreamsResponse) query(user, password, req);

        Set<String> namesSet = new HashSet<>();
        for (String name : names)
            namesSet.add(name);

        System.out.println("streams: " + Util.printArray(resp.streams));

        compareSets(names, resp.streams);
    }

    private void testListEntities(String user, String password, String stream, boolean forbidden, IdentityKey... identities)
            throws IOException, JAXBException
    {
        ListEntitiesRequest req = new ListEntitiesRequest();
        req.stream = stream;

        try {
            ListEntitiesResponse resp = (ListEntitiesResponse) query(user, password, req);
            System.out.println("stream: " + stream + " " + Util.printArray(resp.identities));
            compareSets(identities, resp.identities);
            assert !forbidden;
        } catch (AccessControlException e) {
            assert forbidden;
        }
    }

    private long testSelect(String user, String password, SelectRequest sr, boolean forbidden) throws IOException, JAXBException {
        try {
            InputStream is = selectQuery(user, password, sr);
            HTTPCursor cursor = new HTTPCursor(is, sr.isBigEndian, TypeTransmission.DEFINITION, null);

            assert !forbidden;

            long count = 0;
            while (cursor.next())
                count++;

            return count;
        } catch (AccessControlException e) {
            assert forbidden;
        }

        return 0;
    }

    private void testBarsLoad(String user, String password, String streamName, int messageCount, boolean forbidden) throws IOException {
        try {
            RecordClassDescriptor type = StreamConfigurationHelper.mkUniversalBarMessageDescriptor();
            long time = System.currentTimeMillis();
            HTTPLoader loader = createLoader(user, password, streamName, type);

            RawMessage raw = new RawMessage();
            raw.type = type;
            raw.data = new byte[1024];

            for (int i = 0; i < messageCount; i++) {
                for (String symbol : new String[]{"DLTX", "ORCL"}) {
                    raw.setSymbol(symbol);
                    raw.setTimeStampMs(time);
                    loader.send(raw);
                }

                time += com.epam.deltix.util.time.TimeConstants.MINUTE;
            }
            loader.close();

            assert !forbidden;
        } catch (RuntimeException e) {
            if (e.getMessage().indexOf("Forbidden") >= 0)
                assert forbidden;
            else
                assert false;
        }
    }

    private <T> void compareSets(T[] set1, T[] set2) {
        Set<T> s1 = new HashSet<>(Arrays.asList(set1));

        if (set2 != null)
            for (T elem : set2)
                assert s1.remove(elem) : "element '" + elem + "' not exists in original set.";

        assert s1.isEmpty();
    }

    private static Object query(String user, String password, XmlRequest request) throws IOException, JAXBException {
        Marshaller m = TBJAXBContext.createMarshaller();

        final URL url = new URL("http://localhost:" + runner.getPort() + "/tb/xml");
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

        if (rc == 403)
            throw new AccessControlException(conn.getResponseMessage());

        if (rc != 200)
            throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());

        InputStream is = conn.getInputStream();
        Unmarshaller u = TBJAXBContext.createUnmarshaller();
        return u.unmarshal(is);
    }

    private InputStream selectQuery(String user, String password, SelectRequest sr) throws IOException, JAXBException {
        Marshaller m = TBJAXBContext.createMarshaller();

        URL url = new URL("http://localhost:" + runner.getPort() + "/tb/xml");
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

        if (rc == 403)
            throw new AccessControlException(conn.getResponseMessage());

        if (rc != 200)
            throw new RuntimeException("HTTP rc=" + rc + " " + conn.getResponseMessage());

        // Content-Encoding [gzip]
        return HTTPProtocol.GZIP.equals(conn.getHeaderField(HTTPProtocol.CONTENT_ENCODING)) ?
                new GZIPInputStream(conn.getInputStream()) : conn.getInputStream();
    }

    private HTTPLoader createLoader(String user, String password, String streamName, RecordClassDescriptor... rcds) throws IOException {
        final URL url = new URL("http://localhost:" + runner.getPort() + "/tb/bin");
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        if (user != null) {
            String authStr = user + ":" + password;
            final String encodedAuth = Base64EncoderEx.encode(authStr.getBytes());
            conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        }
        conn.setDoOutput(true);
        // Send in chunks (to avoid out of memory error)
        conn.setChunkedStreamingMode(0x100000);
        final OutputStream os;
        os = conn.getOutputStream();

        final DataOutput dout = new DataOutputStream(os);

        // endianness version stream write_mode allowed_errors
        dout.writeByte(1);
        dout.writeShort(HTTPProtocol.VERSION);
        dout.writeUTF(streamName);
        dout.write(LoadingOptions.WriteMode.REWRITE.ordinal());
        dout.writeShort(0);

        return new HTTPLoader(conn, dout, null, rcds);
    }

    private static DXTickStream createBarsStream(DXTickDB db, String name) {
        StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, name, "", 0);
        streamOptions.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());
        return db.createStream(name, streamOptions);
    }

    private static DXTickStream createAndLoadBarsStream(DXTickDB db, String name, int messageCount) {
        DXTickStream stream = createBarsStream(db, name);

        TDBRunner.BarsGenerator generator =
                new TDBRunner.BarsGenerator(
                        new GregorianCalendar(2000, 1, 1),
                        (int) BarMessage.BAR_SECOND, messageCount,
                        "ORCL", "DLTX"
                );

        LoadingOptions options = new LoadingOptions();
        options.writeMode = LoadingOptions.WriteMode.APPEND;
        TickLoader loader = stream.createLoader(options);
        while (generator.next())
            loader.send(generator.getMessage());
        loader.close();

        return stream;
    }

}
