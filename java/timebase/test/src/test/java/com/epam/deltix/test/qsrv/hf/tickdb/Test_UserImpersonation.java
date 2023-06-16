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
package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.qsrv.SetHome;
import com.epam.deltix.qsrv.comm.cat.StartConfiguration;

import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.test.messages.MarketMessage;
import com.epam.deltix.qsrv.test.messages.TradeMessage;
import com.epam.deltix.util.JUnitCategories;
import com.epam.deltix.util.io.Home;
import com.epam.deltix.util.io.IOUtil;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.InputStream;
import java.security.AccessControlException;
import java.security.Principal;


@Category(JUnitCategories.TickDBFast.class)
public class Test_UserImpersonation  {

    static {
        SetHome.check();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Begin of copy from Test_TDBSecured
    ///
    //private static final String       SECURITY_DIR        = Home.getPath("testdata/tickdb/security");
    private static final String       TB_LOCATION         = Home.getPath("temp/tdbsecurity/tickdb");
    private static final String       CONFIG_LOCATION     = Home.getPath("temp/tdbsecurity/config");
    private static final String       SECURITY_FILE_NAME  = "uac-file-security.xml";
    private static final String       SECURITY_RULES_NAME = "uac-access-rules.xml";

    private static final String       ADMIN_USER          = "admin";
    private static final String       ADMIN_PASS          = "admin";

    private static final String       JOHN_DOE_USER       = "JohnDoe";
//    private static final String       JOHN_DOE_PASS       = "QWERTY";
//
//    private static final String       OWNERS_MANAGER_USER = "OwnersManager";
//    private static final String       OWNERS_MANAGER_PASS = "123";

    private static TDBRunner runner;

    @BeforeClass
    public static void start() throws Throwable {
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
    public static void stop() throws Throwable {
        runner.shutdown();
        runner = null;
    }

    ///
    /// end of copy from Test_TDBSecured
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private DXTickDB impersonate(String userId) {
        return TickDBFactory.wrap(getAdminClient(), makePrincipal(userId));
    }

    private DXTickDB impersonate(Principal principal) {
        return TickDBFactory.wrap(getAdminClient(), principal);
    }


    @Test
    public void userCantWriteOwnStream () {
        createPopulatedStream(impersonate(JOHN_DOE_USER), "userStream");

        assertCanRead(JOHN_DOE_USER, "userStream");
        assertCanWrite(JOHN_DOE_USER, "userStream");

        assertCanRead(ADMIN_USER, "userStream");
        assertCanWrite(ADMIN_USER, "userStream");
    }

    @Test
    public void userCantWriteAdminStream () {
        createPopulatedStream(getAdminClient(), "adminStream");

        assertCanRead(JOHN_DOE_USER, "adminStream");
        assertCanNotWrite(JOHN_DOE_USER, "adminStream");

        assertCanRead(ADMIN_USER, "adminStream");
        assertCanWrite(ADMIN_USER, "adminStream");
    }

    /// Specialized asserts

    @SuppressWarnings("unused")
    private void assertCanNotRead (String user, String streamKey) { // TODO: permissions file change to test this
        DXTickDB userClient = impersonate(user);
        DXTickStream stream = userClient.getStream(streamKey);
        //limitation of current version// Assert.assertNull("User \"" + user + "\" should not be allowed to see stream " + streamKey, stream);

        try (TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(false, false))) {
            cursor.next();
            Assert.fail("User \"" + user + "\" should not be allowed to read stream " + streamKey);
        } catch (AccessControlException e) {
            // normal if security exception
        }

        // Does not support //userClient.close();
    }

    private void assertCanRead (String user, String streamKey) {
        DXTickDB userClient = impersonate(user);
        DXTickStream stream = userClient.getStream(streamKey);
        Assert.assertNotNull("User \"" + user + "\" can't see stream " + streamKey, stream);


        try (TickCursor cursor = stream.select(Long.MIN_VALUE, new SelectionOptions(false, false))) {
            Assert.assertTrue("User \"" + user + "\"can see existing data in " + stream, cursor.next());
        } catch (AccessControlException e) {
            Assert.fail("User \"" + user + "\" can't read stream " + streamKey);
        }

        // Does not support //userClient.close();
    }

    private void assertCanWrite (String user, String streamKey) {
        assertCanWrite(user, streamKey, createTradeMessage("FB"));
    }

    private void assertCanWrite (String user, String streamKey, MarketMessage ... msgs) {
        DXTickDB userClient = impersonate(user);

        DXTickStream stream = userClient.getStream(streamKey);
        Assert.assertNotNull("User \"" + user + "\" can't see stream " + streamKey, stream);

        try {
            populate (stream, msgs);
        } catch (AccessControlException e) {
            Assert.fail("User \"" + user + "\" can't WRITE stream " + streamKey);
        }

        // Does not support //userClient.close();
    }

    private void assertCanNotWrite (String user, String streamKey) {
        assertCanNotWrite(user, streamKey, createTradeMessage("FB"));
    }

    private void assertCanNotWrite (String user, String streamKey, MarketMessage ... msgs) {
        DXTickDB userClient = impersonate(user);

        DXTickStream stream = userClient.getStream(streamKey);

        try {
            populate (stream, msgs);
            Assert.fail("User \"" + user + "\" should not be allowed to WRITE stream " + streamKey);
        } catch (AccessControlException e) {
            // normal if security exception
        }

        // Does not support //userClient.close();
    }

    /// Helpers

    private static DXTickStream createPopulatedStream(DXTickDB client, String streamKey) {
        DXTickStream result = createStream(client, streamKey);
        populate(result);
        return result;
    }

    private static DXTickStream createStream(DXTickDB client, String streamKey) {
        StreamOptions so = new StreamOptions();
        so.scope = StreamScope.DURABLE;
        so.distributionFactor = StreamOptions.MAX_DISTRIBUTION;
        so.setPolymorphic(StreamConfigurationHelper.getStandardMarketMessageDescriptors());
        return client.createStream(streamKey, so);
    }

    private static void populate(DXTickStream stream) {
        populate(stream,
            createTradeMessage("MSFT"),
            createTradeMessage("GOOG"),
            createTradeMessage("AAPL"));
    }
    private static void populate(DXTickStream stream, MarketMessage... msgs) {
        try (TickLoader loader = stream.createLoader()) {
            for (MarketMessage msg : msgs)
                loader.send(msg);
        }
    }

    private static TradeMessage createTradeMessage(String symbol) {
        return createTradeMessage(symbol, System.currentTimeMillis());
    }

    private static TradeMessage createTradeMessage(String symbol, long timestamp) {
        TradeMessage result = new TradeMessage();
        result.setTimeStampMs(timestamp);
        result.setSymbol(symbol);
        result.setCurrencyCode((short)999);
        result.setSize(123);
        result.setPrice(25);
        return result;
    }


    // alias
    private DXTickDB getAdminClient () {
        return runner.getTickDb();
    }

    //    @Before
//    public static void setupPermissions() {
//        TDBSecurityController sc = mock(TDBSecurityController.class);
//
//        when(sc.authenticate(ADMIN_USER, ADMIN_PASS)).thenReturn(makePrincipal(ADMIN_USER));
//        when(sc.authenticate(JOHN_DOE_USER, JOHN_DOE_PASS)).thenReturn(makePrincipal(JOHN_DOE_USER));
//
//        when(sc.impersonate(ADMIN_USER, ADMIN_PASS, JOHN_DOE_USER)).thenReturn(makePrincipal(JOHN_DOE_USER));
//
//        doThrow(new AccessControlException("Permission ABC is denied.")).when(sc).checkPermission(PrincipalMatcher.match(JOHN_DOE_USER), anyString(), anyObject()); //TODO: TDBAccessController.READ_PERMISSION
//        when(sc.hasPermission(PrincipalMatcher.match(JOHN_DOE_USER), anyString(), anyObject())).thenReturn(false);
//
//        GlobalQuantServer.SC = sc;
//    }
//
//    private static class PrincipalMatcher extends ArgumentMatcher<Principal> {
//
//        private final String userName;
//
//        private PrincipalMatcher(String userName) {
//            this.userName = userName;
//        }
//
//        @Override
//        public boolean matches(Object argument) {
//            if (argument instanceof Principal)
//                return userName.equals(((Principal) argument).getName());
//            return false;
//        }
//
//        static Principal match(String name) {
//            return argThat(new PrincipalMatcher(name));
//        }
//    }

    private static GenericPrincipal makePrincipal(String user) {
        return new GenericPrincipal(user, null, null);  //TODO
    }

}