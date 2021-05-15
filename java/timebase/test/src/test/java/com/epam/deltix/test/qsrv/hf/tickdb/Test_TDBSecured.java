package com.epam.deltix.test.qsrv.hf.tickdb;

/*  ##TICKDB.FAST## */

import com.epam.deltix.qsrv.QSHome;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.test.messages.BarMessage;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import com.epam.deltix.util.parsers.CompilationException;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.security.AccessControlException;
import java.util.GregorianCalendar;

import org.junit.experimental.categories.Category;
import com.epam.deltix.util.JUnitCategories.TickDBFast;

import static org.junit.Assert.*;

/**
 *
 */
@Category(TickDBFast.class)
public class Test_TDBSecured {
    private static String       TEST_DIR            = Home.getPath("temp/tdbsecurity");
    private static String       TB_LOCATION         = TEST_DIR + "/tickdb";
    private static String       CONFIG_LOCATION     = TEST_DIR + "/config";
    private static String       SECURITY_FILE_NAME  = "uac-file-security.xml";
    private static String       SECURITY_RULES_NAME = "uac-access-rules.xml";

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

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
        System.out.print("Removing directory...");
        File file = new File(TEST_DIR);
        FileUtils.deleteDirectory(file);
        System.out.println("OK");

        file.mkdirs();
        QSHome.set(file.getAbsolutePath());

        try (InputStream stream = IOUtil.openResourceAsStream("com/epam/deltix/security/" + SECURITY_FILE_NAME)) {
            FileUtils.copyToFile(stream,
                    new File(CONFIG_LOCATION + File.separator + SECURITY_FILE_NAME));
        }

        try (InputStream stream = IOUtil.openResourceAsStream("com/epam/deltix/security/" + SECURITY_RULES_NAME)) {
            FileUtils.copyToFile(stream,
                    new File(CONFIG_LOCATION + File.separator + SECURITY_RULES_NAME));
        }

        StartConfiguration configuration = StartConfiguration.create(true, false, false);
        configuration.quantServer.setProperty("security", "FILE");
        configuration.quantServer.setProperty("security.config", SECURITY_FILE_NAME);

        runner = new TDBRunner(true, true, TB_LOCATION, new TomcatServer(configuration));
        runner.user = ADMIN_USER;
        runner.pass = ADMIN_PASS;
        runner.startup();
    }

    @AfterClass
    public static void      stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    @After
    public void             after() {
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();
        DXTickStream[] streams = admin_connection.listStreams();
        for (DXTickStream stream : streams)
            if (!stream.getKey().equals("events#"))
                stream.delete();
    }

    @Test
    public void             testCreateStreamWithOwner() {
        //Admin's connection
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();

        try (TickDBClient user_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, JOHN_DOE_USER, JOHN_DOE_PASS);
             TickDBClient om_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, OWNERS_MANAGER_USER, OWNERS_MANAGER_PASS))
        {
            user_connection.open(false);
            om_connection.open(false);

            //Admin's stream
            StreamOptions options = new StreamOptions(StreamScope.DURABLE, "admins_stream", "description", 0);
            DXTickStream stream = admin_connection.createStream("admins_stream", options);
            assert ADMIN_USER.equals(stream.getOwner());

            //Admin create stream with not-admin owner
            options = new StreamOptions(StreamScope.DURABLE, "admins_stream2", "description", 0);
            options.owner = "NotAdmin";
            stream = admin_connection.createStream("admins_stream2", options);
            assert "NotAdmin".equals(stream.getOwner());

            //John's stream
            options = new StreamOptions(StreamScope.DURABLE, "johns_stream", "description", 0);
            stream = user_connection.createStream("johns_stream", options);
            assert JOHN_DOE_USER.equals(stream.getOwner());

            //John create stream with not-John owner
            options = new StreamOptions(StreamScope.DURABLE, "johns_stream2", "description", 0);
            options.owner = "NotJohnDoe";
            stream = user_connection.createStream("johns_stream2", options);
            assert JOHN_DOE_USER.equals(stream.getOwner());

            //John create stream with Admin owner
            //(He has IMPERSONATE Admin permission)
            options = new StreamOptions(StreamScope.DURABLE, "johns_stream3", "description", 0);
            options.owner = ADMIN_USER;
            stream = user_connection.createStream("johns_stream3", options);
            assert ADMIN_USER.equals(stream.getOwner());

            //John gives his stream to admin
            //(He has IMPERSONATE Admin permission)
            stream = user_connection.getStream("johns_stream");
            stream.setOwner(ADMIN_USER);
            assert ADMIN_USER.equals(stream.getOwner());

            //John gets it back
            stream.setOwner(JOHN_DOE_USER);
            assert JOHN_DOE_USER.equals(stream.getOwner());

            //But John can't give his stream to Owners manager
            try {
                stream.setOwner(OWNERS_MANAGER_USER);
                assert false;
            } catch (AccessControlException e) {
            }

            //Owners manager has full access impersonate permission
            stream = om_connection.getStream("admins_stream");
            stream.setOwner(JOHN_DOE_USER);
            assert JOHN_DOE_USER.equals(stream.getOwner());
            stream.setOwner(ADMIN_USER);
            assert ADMIN_USER.equals(stream.getOwner());
        }
    }

    @Test
    public void             testStreamPermissions() {
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();

        try (TickDBClient user1_connection = (TickDBClient) TickDBFactory.connect(
                "localhost", runner.getPort(), false, USER1, USER1_PASS);
             TickDBClient user2_connection = (TickDBClient) TickDBFactory.connect(
                     "localhost", runner.getPort(), false, USER2, USER2_PASS))
        {
            StreamOptions options = new StreamOptions(StreamScope.DURABLE, "admins_stream_a", "description", 0);
            DXTickStream stream = admin_connection.createStream("admins_stream_a", options);

            user1_connection.open(false);
            user2_connection.open(false);

            options = new StreamOptions(StreamScope.DURABLE, "user1_stream", "description", 0);
            stream = user1_connection.createStream("user1_stream", options);

            options = new StreamOptions(StreamScope.DURABLE, "user2_stream", "description", 0);
            stream = user2_connection.createStream("user2_stream", options);

            //http://rm.orientsoft.by/issues/756
            //getStream throws AccessControlException
            try {
                stream = user1_connection.getStream("user2_stream");
                assert false;
            } catch (AccessControlException e) {
            }

            stream = user1_connection.getStream("user2_stream_not_exists");
            assert stream == null;

            stream = user1_connection.getStream("admins_stream_a");
            assert stream != null;

            stream = user2_connection.getStream("user1_stream");
            assert stream != null;

            stream = user2_connection.getStream("admins_stream_a");
            assert stream != null;

            ((DXTickStream) user1_connection.getStream("admins_stream_a")).createLoader(
                    new LoadingOptions(true));

            ((DXTickStream) user2_connection.getStream("user1_stream")).createLoader(
                    new LoadingOptions(true));

            try {
                ((DXTickStream) user2_connection.getStream("admins_stream_a")).createLoader(
                        new LoadingOptions(true));
                assert false;
            } catch (AccessControlException e) {
            }
        }
    }

    @Test
    public void             testLoaderCursor() {
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();

        String streamName = "test_cursor_loader";
        int messages = 50_000;
        DXTickStream stream = createAndLoadStream(admin_connection, streamName, messages);

        try (TickDBClient user4_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, USER4, USER4_PASS);
             TickDBClient user3_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, USER3, USER3_PASS))
        {
            user3_connection.open(false);
            user4_connection.open(false);

            //User4 test
            DXTickStream stream1 = user4_connection.getStream(streamName);

            TickCursor cursor = user4_connection.createCursor(new SelectionOptions(false, false), stream1);
            cursor.reset(Long.MIN_VALUE);
            cursor.subscribeToAllEntities();
            int count = 0;
            while (cursor.next()) {
                ++count;
            }
            assert count == messages;

            try {
                stream1.createLoader();
                assert false : "User4 has not access to write Admin's stream";
            } catch (AccessControlException e) {
            }
        }

    }

    @Test
    public void             testDDL() {
        final String streamName = "test_ddl";

        final String CREATE_STATEMENT = "CREATE DURABLE STREAM \"" + streamName + "\" '" + streamName + "' (\n" +
                "    CLASS \"deltix.qsrv.hf.pub.MarketMessage\" (\n" +
                "        \"currencyCode\" 'Currency Code' INTEGER SIGNED (16),\n" +
                "        \"sequenceNumber\" 'Sequence Number' INTEGER\n" +
                "    );\n" +
                ")\n" +
                "OPTIONS (FIXEDTYPE; DF = 12; HIGHAVAILABILITY = FALSE)\n";

        final String MODIFY_STATEMENT = "MODIFY STREAM \"" + streamName + "\" '" + streamName + "' (\n" +
                "    CLASS \"deltix.qsrv.hf.pub.MarketMessage\" (\n" +
                "        \"currencyCode\" 'Currency Code' INTEGER SIGNED (16),\n" +
                "        \"sequenceNumber\" 'Sequence Number' INTEGER\n" +
                "    );\n" +
                ")\n" +
                "OPTIONS (FIXEDTYPE; DF = 132; HIGHAVAILABILITY = FALSE) CONFIRM DROP_DATA\n";

        final String DROP_STATEMENT = "DROP STREAM \"" + streamName + "\"";

        //admin can create streams
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();

        try (TickDBClient user4_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, USER4, USER4_PASS);
             TickDBClient user3_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, USER3, USER3_PASS))
        {
            user3_connection.open(false);
            user4_connection.open(false);

            checkStatement(admin_connection, CREATE_STATEMENT, true);

            DXTickStream streamDDL1 = admin_connection.getStream(streamName);
            System.out.println(streamDDL1.getOwner());

            DXTickStream streamDDL2 = user4_connection.getStream(streamName);
            System.out.println(streamDDL2.getOwner());

            //USER4 has not permission to write (modify or remove) admin's streams
            checkStatement(user4_connection, MODIFY_STATEMENT, false);
            checkStatement(user4_connection, DROP_STATEMENT, false);

            //admin can remove his streams
            checkStatement(admin_connection, MODIFY_STATEMENT, true);
            checkStatement(admin_connection, DROP_STATEMENT, true);

            //USER4 can create, modify and remove its own streams
            checkStatement(user4_connection, CREATE_STATEMENT, true);
            checkStatement(user4_connection, MODIFY_STATEMENT, true);
            checkStatement(user4_connection, DROP_STATEMENT, true);

            //USER3 has not create stream permission
            checkStatement(user3_connection, CREATE_STATEMENT, false);
        }
    }

    @Test
    public void                 testQQL() {
        TickDBClient admin_connection = (TickDBClient) runner.getTickDb();

        String streamName = "test_qql";
        int messages = 50_000;
        createAndLoadStream(admin_connection, streamName, messages);

        try (TickDBClient user4_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, USER4, USER4_PASS);
             TickDBClient user3_connection = (TickDBClient) TickDBFactory.connect("localhost", runner.getPort(), false, USER3, USER3_PASS))
        {
            user3_connection.open(false);
            user4_connection.open(false);

            String query = "select * from " + streamName;

            //USER4 has read permission on Admin
            try (InstrumentMessageSource source = user4_connection.executeQuery(query, new SelectionOptions(true, false))) {
                int count = 0;
                while (source.next())
                    ++count;

                assert count == messages;
            }

            //USER3 has not permission to read streams
            try (InstrumentMessageSource source = user3_connection.executeQuery(query, new SelectionOptions(true, false))) {
                source.next();
                fail();
            } catch (CompilationException e) {
                //ok. USER3 can't read stream 'test_qql', so he can't see the stream
                assertEquals("1.15..23: UnknownIdentifierException: Unknown identifier: TEST_QQL", e.getMessage());
            }
        }
    }

    private void                checkStatement(DXTickDB db, String statement, boolean success) {
        try (InstrumentMessageSource source = db.executeQuery(statement, new SelectionOptions(true, false))) {
            assert source.next() : "Error create stream with qql.";

            InstrumentMessage message = source.getMessage();
            if (success)
                assert message.toString().contains("SUCCESS") : "Failed execution statement.";
            else
                assert message.toString().contains("AccessControlException") : "Execution statement is denied.";
        }
    }

    private DXTickStream        createAndLoadStream(DXTickDB db, String name, int messageCount) {
        StreamOptions streamOptions = new StreamOptions(StreamScope.DURABLE, name, "", 0);
        streamOptions.setFixedType(StreamConfigurationHelper.mkUniversalBarMessageDescriptor());
        DXTickStream stream = db.createStream(name, streamOptions);

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
