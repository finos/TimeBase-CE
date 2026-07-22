/*
 * Copyright 2026 EPAM Systems, Inc
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

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.jul.JulBridge;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.vsocket.util.TestVServerSocketFactory;
import org.jetbrains.annotations.NotNull;

import org.netcrusher.NetFreezer;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Contains tests that use an intermediate proxy to simulate network issues between {@link VSClient} and server.
 */
public class Test_ClientReconnect {


    // Warning: having more than 120 transports may lead to Gradle test instability because
    //  of insufficient off-heap buffer capacity for all connections.
    private static final int RECOVERY_TEST_TRANSPORTS = Integer.parseInt(System.getProperty("Test_ClientReconnect.transports", "100"));

    // Linger interval is 10 seconds + 5 seconds for notification delays (especially on CI)
    private static final int DISCONNECT_WAIT_TIMEOUT = 15;

    static {
        JulBridge.install();
    }

    private static final Log LOG = LogFactory.getLog(Test_ClientReconnect.class);

    public static final int PROXY_PORT = 34_781;
    public static final int EMBEDDED_SERVER_PORT = 35_782;


    private VSServer server;
    private NioReactor nioReactor;
    private TcpCrusher tcpCrusher;
    private NetFreezer acceptorFreezer;
    private final AtomicInteger connectCounter = new AtomicInteger(0);

    private volatile Consumer<InetSocketAddress> proxyConnectListener = null;

    @BeforeEach
    public void start() throws Throwable {
        this.server = TestVServerSocketFactory.createBinaryEchoVServer(EMBEDDED_SERVER_PORT);
        this.server.setTransportsLimit((short) 1000); // Allow many transports for reconnect tests
        this.server.start();

        int serverPort = server.getLocalPort();
        String serverHost = "localhost";

        this.nioReactor = new NioReactor();

        this.tcpCrusher = TcpCrusherBuilder.builder()
                .withReactor(nioReactor)
                .withBindAddress("localhost", PROXY_PORT)
                .withConnectAddress(serverHost, serverPort)
                //.withBacklog(1)
                .withCreationListener(clientAddress -> {
                    int connectionNumber = connectCounter.incrementAndGet();
                    LOG.info("Proxy: Client %s connected: %s").with(connectionNumber).with(clientAddress);
                    if (proxyConnectListener != null) {
                        proxyConnectListener.accept(clientAddress);
                    }
                })
                .buildAndOpen();
        this.acceptorFreezer = tcpCrusher.getAcceptorFreezer();
    }

    @AfterEach
    public void stop() {
        proxyConnectListener = null;
//        if (acceptorFreezer != null && acceptorFreezer.isFrozen()) {
//            // Prevent incorrect state on close
//            try {
//                acceptorFreezer.unfreeze();
//            } catch (RuntimeException e) {
//                LOG.warn("Failed to unfreeze acceptor during cleanup: %s").with(e);
//            }
//        }

        if (tcpCrusher != null) {
            tcpCrusher.close();
        }
        if (nioReactor != null) {
            nioReactor.close();
        }
        if (server != null) {
            server.close();
        }
    }

    @NotNull
    private static VSClient connectClient() throws IOException {
        return new VSClient("localhost", PROXY_PORT);
    }

    private int getConnectedClientCount() {
        return tcpCrusher.getClientAddresses().size();
    }

    /**
     * Client should not get blocked on connection loss.
     */
    @RepeatedTest(1)
    @Timeout(20)
    public void testConnectionLoss() throws Exception {
        VSClient client = connectClient();
        try {
            assertEquals(0, getConnectedClientCount());
            client.connect();
            LOG.info("Client connected");
            assertTrue(client.isConnected());
            assertEquals(3, getConnectedClientCount());
            Test_VSocket_Correctness.assertEchoClientCorrectness(client, 1_000);

            CountDownLatch disconnectedLatch = new CountDownLatch(1);
            AtomicInteger disconnectCount = new AtomicInteger(0);
            long listenerInstallTime = System.currentTimeMillis();
            client.setDisconnectedListener(new DisconnectEventListener() {
                @Override
                public void onDisconnected() {
                    LOG.info("Client onDisconnected listener triggered after %s ms")
                            .with(System.currentTimeMillis() - listenerInstallTime);
                    disconnectCount.incrementAndGet();
                    disconnectedLatch.countDown();
                }

                @Override
                public void onReconnected() {
                }
            });

            long disconnectStart = System.currentTimeMillis();

            // Close all connections and disable proxy
            tcpCrusher.close();

            boolean success = disconnectedLatch.await(DISCONNECT_WAIT_TIMEOUT, TimeUnit.SECONDS);
            long disconnectEnd = System.currentTimeMillis();
            LOG.info("Stopped to wait for disconnected after %s ms").with(disconnectEnd - disconnectStart);
            LOG.info("Disconnect event count: %s").with(disconnectCount.get());

            assertTrue(success, "Client did not receive disconnect event in time");
            // Client is still disconnected
            assertFalse(client.isConnected());

            // Wait for any redundant events
            Thread.sleep(10);
            assertEquals(1, disconnectCount.get(), "Disconnect event should be fired exactly once");
        } finally {
            // TODO: Probably we may want to reconsider this in future and allow graceful client close
            // For this test we do not care if client throws exception
            Util.close(client);
        }
    }

    /**
     * Client should be able to reconnect after single recoverable connection loss.
     */
    @RepeatedTest(1)
    @Timeout(200)
    public void testReconnectAfterSingleDisconnected() throws Exception {
        try (VSClient client = connectClient()) {
            client.connect();
            var eventListener = installListener(client);

            // Multiple iterations to ensure that we return to stable state
            for (int i = 0; i < 10; i++) {
                assertTrue(client.isConnected());
                assertEquals(3, getConnectedClientCount());
                Test_VSocket_Correctness.assertEchoClientCorrectness(client, 1_000);

                // Simulates temporary connection loss for single connection
                InetSocketAddress clientSocketAddress = tcpCrusher.getClientAddresses().iterator().next();
                Assertions.assertNotNull(clientSocketAddress);

                boolean closed = tcpCrusher.closeClient(clientSocketAddress);
                assertTrue(closed, "Failed to close client connection");

                Thread.sleep(1000); // Wait for the connection to be closed

                boolean gotDisconnectEvent = eventListener.disconnectedLatch.await(DISCONNECT_WAIT_TIMEOUT, TimeUnit.SECONDS);
                assertFalse(gotDisconnectEvent, "Client is not supposed to generate disconnect if it was able to reconnect");
                assertEquals(0, eventListener.disconnectCount.get());
                //Assert.assertEquals(0, eventListener.reconnectCount.get());

                LOG.info("State: %s").with(client.getDispatcher().getInternalState());
                assertTrue(client.tryGetConnectionStatus());
                assertEquals(3, getConnectedClientCount());
            }
        }
    }

    /**
     * Client should be able to reconnect after connection loss if network is restored.
     */
    @RepeatedTest(1)
    @Timeout(200)
    public void testReconnectAfterAllDisconnected() throws Exception {
        try (VSClient client = connectClient()) {
            client.connect();
            var eventListener = installListener(client);

            // Multiple iterations to ensure that we return to stable state
            for (int i = 0; i < 10; i++) {
                assertTrue(client.isConnected());
                assertEquals(3, getConnectedClientCount());
                Test_VSocket_Correctness.assertEchoClientCorrectness(client, 1_000);

                // Simulates temporary connection loss for all transports
                tcpCrusher.close();
                tcpCrusher.open();

                Thread.sleep(1000); // Wait for the connection to be closed


                boolean gotDisconnectEvent = eventListener.disconnectedLatch.await(DISCONNECT_WAIT_TIMEOUT, TimeUnit.SECONDS);
                assertFalse(gotDisconnectEvent, "Client is not supposed to generate disconnect if it was able to reconnect");
                assertEquals(0, eventListener.disconnectCount.get());
                // TODO: Uncomment - currently client fires redundant reconnect event
                //Assert.assertEquals(0, eventListener.reconnectCount.get());

                LOG.info("State: %s").with(client.getDispatcher().getInternalState());
                assertTrue(client.tryGetConnectionStatus());
                assertEquals(3, getConnectedClientCount());
            }
        }
        // Should be closed gracefully
    }

    /**
     * Ensure that if client is closed during disconnect event, it does not get stuck.
     */
    @RepeatedTest(1)
    @Timeout(20)
    public void testCloseOnDisconnect() throws Exception {
        try (VSClient client = connectClient()) {
            client.connect();

            assertTrue(client.isConnected());
            assertEquals(3, getConnectedClientCount());
            Test_VSocket_Correctness.assertEchoClientCorrectness(client, 1_000);

            CountDownLatch disconnectedLatch = new CountDownLatch(1);
            AtomicInteger disconnectCount = new AtomicInteger(0);
            client.setDisconnectedListener(new DisconnectEventListener() {
                @Override
                public void onDisconnected() {
                    LOG.info("Client disconnected");
                    disconnectCount.incrementAndGet();
                    client.close();
                    disconnectedLatch.countDown();
                }

                @Override
                public void onReconnected() {
                }
            });

            // Simulates connection loss for all transports
            tcpCrusher.close();

            disconnectedLatch.await();

            // Wait for any redundant events
            Thread.sleep(10);
            assertEquals(1, disconnectCount.get(), "Disconnect event should be fired exactly once");
        }
    }

    /**
     * Starts with 1000 connections, kills them, recovers only some of them.
     * Expected to end up in DISCONNECTED state.
     */
    @RepeatedTest(1)
    //@Test
    @Timeout(60)
    public void testPartialRecovery() throws Exception {
        int transports = RECOVERY_TEST_TRANSPORTS;

        int halfTransports = transports / 2;
        if (halfTransports == 0) {
            throw new IllegalStateException("Number of transports is too low for this test");
        }

        try (VSClient client = connectClient()) {
            client.setNumTransportChannels(transports);
            client.connect();

            waitUntil(10_000, "Client is not connected in time", client::isConnected);

            assertEquals(transports, getConnectedClientCount());
            LOG.info("Client connected with %s transports").with(transports);
            Test_VSocket_Correctness.assertEchoClientCorrectness(client, 1_000);

            List<InetSocketAddress> initialConnections = new ArrayList<>(tcpCrusher.getClientAddresses());


            // Allow only one reconnection
            AtomicInteger reconnectedCount = new AtomicInteger(0);
            proxyConnectListener = (inetSocketAddress) -> {
                // Warning: this listener is asynchronous, so it's not guaranteed that exactly 500 connections will fail

                int newCount = reconnectedCount.incrementAndGet();

                if (newCount == halfTransports) {
                    // Disable new connections
                    acceptorFreezer.freeze();
                }
            };

            LOG.info("Killing initial connections");
            for (InetSocketAddress address : initialConnections) {
                tcpCrusher.closeClient(address);
            }

            VSDispatcher[] dispatchers = server.getDispatchers();
            assertEquals(1, dispatchers.length);
            VSDispatcher dispatcher = dispatchers[0];

            // Wait until dispatcher detects broken transport
            LOG.info("Waiting for dispatcher to detect disconnection");
            long now = System.currentTimeMillis();
            long start = now;
            long deadline = now + 20_000;
            while ((now = System.currentTimeMillis()) < deadline) {
                VSDispatcherState state = dispatcher.getInternalState();
                if (state != VSDispatcherState.DISCONNECTED) {
                    Thread.sleep(100);
                } else {
                    break;
                }
            }
            LOG.info("Waited %s ms for dispatcher to detect disconnection").with(now - start);

            LOG.info("Dispatcher state: %s").with(dispatcher.getInternalState());

            // Loss of any transport must cause dispatcher to go to DISCONNECTED state
            assertEquals(VSDispatcherState.DISCONNECTED, dispatcher.getInternalState());
        }
    }

    static void waitUntil(int timeoutMs, String errorMessage, BooleanSupplier condition) {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + timeoutMs;
        while (System.currentTimeMillis() < endTime) {
            if (condition.getAsBoolean()) {
                // Success
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting", e);
            }
        }

        fail(errorMessage);
    }

    static TestEventListener installListener(VSClient client) {
        TestEventListener eventListener = new TestEventListener();
        client.setDisconnectedListener(eventListener);
        return eventListener;
    }

    static class TestEventListener implements DisconnectEventListener {
        CountDownLatch disconnectedLatch = new CountDownLatch(1);
        CountDownLatch reconnectedLatch = new CountDownLatch(1);
        AtomicInteger disconnectCount = new AtomicInteger(0);
        AtomicInteger reconnectCount = new AtomicInteger(0);

        public TestEventListener() {
        }

        @Override
        public void onDisconnected() {
            LOG.info("Client disconnected");
            disconnectCount.incrementAndGet();
            disconnectedLatch.countDown();
        }

        @Override
        public void onReconnected() {
            LOG.info("Client reconnected");
            reconnectCount.incrementAndGet();
            reconnectedLatch.countDown();
        }
    }
}