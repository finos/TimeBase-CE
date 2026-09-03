package com.epam.deltix.test.qsrv.hf.tickdb;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.jul.JulBridge;
import com.epam.deltix.qsrv.hf.pub.md.FloatDataType;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.tickdb.StreamConfigurationHelper;
import com.epam.deltix.qsrv.hf.tickdb.TDBRunner;
import com.epam.deltix.qsrv.hf.tickdb.comm.client.TickDBClient;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TomcatServer;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.util.JUnitCategories;
import org.jetbrains.annotations.NotNull;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.netcrusher.core.reactor.NioReactor;
import org.netcrusher.tcp.TcpCrusher;
import org.netcrusher.tcp.TcpCrusherBuilder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Contains tests that use an intermediate proxy to simulate network issues between {@link TickDBClient} and server.
 */
@Category(JUnitCategories.TickDBFast.class)
public class Test_TickDBClient_Reconnect {
    static {
        JulBridge.install();
    }
    private static final Log LOG = LogFactory.getLog(Test_TickDBClient_Reconnect.class);

    private static final String REMOTE_SERVER_HOST = System.getProperty("ClientReconnectTest.host");
    private static final int REMOTE_SERVER_PORT = Integer.getInteger("ClientReconnectTest.port", 8011);

    public static final int PROXY_PORT = 34781;

    private static final boolean USE_EMBEDDED = REMOTE_SERVER_HOST == null;


    private static TDBRunner runner;
    private NioReactor nioReactor;
    private TcpCrusher tcpCrusher;

    private volatile Consumer<InetSocketAddress> proxyConnectListener = null;


    /**
     * It should be possible to acquire a lock on a stream after client lost connection,
     * partially reconnected but failed to restore all transports.
     */
    @Test(timeout = 400_000) // 10 iterations x 40 seconds each
    public void testLockReleasedOnPartialReconnect() throws Exception {
        for (int i = 0; i < 10; i++) {
            LOG.info("=== Test iteration %s ===").with(i + 1);
            long startTime = System.currentTimeMillis();
            testLockReleasedOnPartialReconnectIteration();
            LOG.info("=== Iteration %s completed in %s ms ===").with(i + 1).with(System.currentTimeMillis() - startTime);
        }
    }

    private void testLockReleasedOnPartialReconnectIteration() throws Exception {
        String streamKey = "stream1";
        int transports = 16; // Current TB transport limit

        int halfTransports = transports / 2;
        if (halfTransports == 0) {
            throw new IllegalStateException("Number of transports is too low for this test");
        }

        try (TickDBClient client = (TickDBClient) connectClient()) {
            client.setNumTransportChannels(transports);
            client.setTimeout(10_000); // More connection time to allow the test to pass on GitHub (slow server)
            client.setReconnectIntervalAdjuster((numAttempts, timeSinceDisconnected, lastInterval) -> {
                // Effectively disable further reconnections
                return java.util.concurrent.TimeUnit.DAYS.toMillis(1);
            });
            long connectStrt = System.currentTimeMillis();
            try {
                client.open(false);
            } finally {
                long connectEnd = System.currentTimeMillis();
                String logLine = "Client .open() took " + (connectEnd - connectStrt) + " ms";
                LOG.info(logLine);
                System.out.println(logLine); // Log directly to console, so it can be observed on GitHub
            }
            assertTrue(client.isConnected());
            assertEquals(transports, getConnectedClientCount());

            LOG.info("Client connected with %s transports").with(transports);

            StreamOptions options = getTestStreamOptions();
            DXTickStream stream = client.createStream(streamKey, options);

            DBLock lock = stream.lock(LockType.WRITE);
            assertNotNull(lock);
            LOG.info("Acquired write lock on stream");

            List<InetSocketAddress> initialConnections = new ArrayList<>(tcpCrusher.getClientAddresses());


            // Allow only one reconnection
            AtomicInteger reconnectedCount = new AtomicInteger(0);
            proxyConnectListener = (inetSocketAddress) -> {
                // Warning: this listener is asynchronous, so it's not guaranteed that exactly 500 connections will fail

                int newCount = reconnectedCount.incrementAndGet();

                if (newCount == halfTransports) {
                    // Disable new connections
                    tcpCrusher.getAcceptorFreezer().freeze();
                }
            };

            LOG.info("Killing initial connections");
            for (InetSocketAddress address : initialConnections) {
                tcpCrusher.closeClient(address);
            }

            while (reconnectedCount.get() < halfTransports) {
                LOG.info("Waiting for %s transports to reconnect, current: %s")
                        .with(halfTransports).with(reconnectedCount.get());
                Thread.sleep(1000);
            }

            // Wait until dispatcher detects broken transport
            LOG.info("Waiting for dispatcher to detect disconnection");
            long now = System.currentTimeMillis();
            long start = now;
            long deadline = now + 20_000;
            while ((now = System.currentTimeMillis()) < deadline) {
                if (client.isConnected()) {
                    Thread.sleep(100);
                } else {
                    break;
                }
            }
            assertFalse(client.isConnected());
            LOG.info("Waited %s ms for dispatcher to detect disconnection").with(now - start);



            LOG.info("Client connected: %s").with(client.isConnected());

            tcpCrusher.close();

            LOG.info("Attempting to close client");
            client.close();
        }

        tcpCrusher.open();

        try (TickDBClient client2 = (TickDBClient) connectClient()) {
            LOG.info("Opening client again");
            client2.open(false);
            assertTrue(client2.isConnected());
            LOG.info("Client 2 connected");

            DXTickStream stream = client2.getStream(streamKey);
            DBLock lock2 = stream.lock(LockType.WRITE);
            assertNotNull(lock2);
            LOG.info("Acquired write lock on stream with client 2");

            stream.delete();
        }

        tcpCrusher.reopen();
    }


    @NotNull
    private static  StreamOptions getTestStreamOptions() {
        RecordClassDescriptor mcd = StreamConfigurationHelper.mkMarketMessageDescriptor(null, false);
        RecordClassDescriptor rcd = StreamConfigurationHelper.mkTradeMessageDescriptor(
                mcd, null, null, FloatDataType.ENCODING_SCALE_AUTO, FloatDataType.ENCODING_SCALE_AUTO);

        return StreamOptions.fixedType(StreamScope.DURABLE, "message", "message", 0, rcd);
    }

    @NotNull
    private static RemoteTickDB connectClient() {
        return TickDBFactory.connect("localhost", PROXY_PORT, false);
    }

    private int getConnectedClientCount() {
        return tcpCrusher.getClientAddresses().size();
    }

    @BeforeClass
    public static void startClass() throws Throwable {
        if (USE_EMBEDDED) {
            runner = new TDBRunner(true, true, TDBRunner.getTemporaryLocation(), new TomcatServer());
            runner.startup();
        }
    }

    @Before
    public void start() throws Throwable {
        int serverPort = USE_EMBEDDED ? runner.getPort() : REMOTE_SERVER_PORT;
        String serverHost = USE_EMBEDDED ? "localhost" : REMOTE_SERVER_HOST;

        this.nioReactor = new NioReactor();

        this.tcpCrusher = TcpCrusherBuilder.builder()
                .withReactor(nioReactor)
                .withBindAddress("localhost", PROXY_PORT)
                .withConnectAddress(serverHost, serverPort)
                .withCreationListener(clientAddress -> {
                    LOG.info("Proxy: Client connected: %s").with(clientAddress);
                    if (proxyConnectListener != null) {
                        proxyConnectListener.accept(clientAddress);
                    }
                })
                .buildAndOpen();

    }

    @After
    public void stop() throws Throwable {
        proxyConnectListener = null;
        if (tcpCrusher != null) {
            tcpCrusher.close();
        }
        if (nioReactor != null) {
            nioReactor.close();
        }
    }



    @AfterClass
    public static void stopClass() throws Throwable {
        if (USE_EMBEDDED && runner != null) {
            runner.shutdown();
            runner = null;
        }
    }

    private TestEventListener installListener(RemoteTickDB client) {
        TestEventListener eventListener = new TestEventListener();
        client.addDisconnectEventListener(eventListener);
        return eventListener;
    }

    private static class TestEventListener implements DisconnectEventListener {
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
