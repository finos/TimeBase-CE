/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.pub.TimeSource;
import com.epam.deltix.qsrv.hf.pub.md.*;
import com.epam.deltix.qsrv.hf.tickdb.comm.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.util.io.SSLClientContextProvider;
import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.UnknownChannelException;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.ChannelCompression;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.ReconnectableImpl;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicPublisherRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicPublisherResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicSubscriberRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicSubscriberResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateCustomTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateMulticastTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.DeleteTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicMetadataRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicMetadataResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.ListTopicsResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicClientChannel;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Token;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TokenType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.ConsumerPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.DirectChannel;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessagePoller;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.MessageProcessor;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.PublisherPreferences;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicApiException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.exception.TopicNotFoundException;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.MulticastTopicSettings;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicType;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.settings.TopicSettings;
import com.epam.deltix.qsrv.hf.topic.consumer.DirectReaderFactory;
import com.epam.deltix.qsrv.hf.topic.consumer.SubscriptionWorker;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.PinnedThreadFactoryWrapper;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.GrowthPolicy;
import com.epam.deltix.util.lang.JavaVerifier;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.net.NetUtils;
import com.epam.deltix.util.oauth.Oauth2Client;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.time.KeeperTimeSource;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSClient;
import com.epam.deltix.util.vsocket.VSProtocol;
import io.aeron.Aeron;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public class TickDBClient implements DXRemoteDB, DBStateNotifier, RemoteTickDB, ReconnectableImpl.Reconnector, TopicDB {

    // Verifying, that we are under JDK, not JRE.
    static {
        JavaVerifier.verify();
    }

    public static final Log LOGGER = LogFactory.getLog("tickdb.client");

    private static int                           getConnectionsNumber(boolean isRemote) {
        int value = 2;

        String sockets = System.getProperty("TimeBase.sockets");
        try {
            return (sockets != null) ? Integer.parseInt(sockets) : value;
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private UserPrincipal                       user;

    private final String                        host;
    private final int                           port;
    private int                                 timeout;
    private SSLContext                          sslContext;

    private boolean                             isOpen = false;
    private boolean                             isReadOnly = false;
    private int                                 serverProtocolVersion;
    private String                              serverVersion = "";

    private long[]                              latency;
    private long                                availableBandwidth = 0;

    private final ReconnectableImpl             connMgr = new ReconnectableImpl("TickDBClient");
    private final Runnable                      updater =
        new Runnable () {
            public void         run () {
                sendMetaDataUpdate ();
            }
        };

    private VSClient                            connection;
    //
    // The following variables are guarded by "this"
    //
    private long                                mdVersion = Long.MIN_VALUE;
    private RecordClassSet                      md;

    // unique identifier
    private final String                        id = new GUID().toString();

    // external application id which uses this class
    private String                              applicationId;
    private String                              address;

    private boolean                             secured = false;

    private final CodecFactory                  intpCodecFactory =
        CodecFactory.newInterpretingCachingFactory();

    private final CodecFactory                  compCodecFactory =
        CodecFactory.newCompiledCachingFactory ();

    private boolean                             useCompression = false;
    private boolean                             isRemoteConnection = false;

    protected volatile SessionClient            session;

    protected final boolean                     enableSSL;

    protected Boolean                           sslTermination; // null stands for default value from VSClient.SSL_TERMINATION

    private final ContextContainer      contextContainer;
    private final DXClientAeronContext  aeronContext;

    private static final ThreadFactory topicNoAffinityConsumerThreadFactory = new TopicConsumerThreadFactory();
    private ThreadFactory topicConsumerThreadFactory;
    private final TimeSource timeSource;

    private final CopyOnWriteArrayList<DBStateListener> stateListeners = new CopyOnWriteArrayList<>();

    private final DisconnectEventListener       listener = new DisconnectEventListener() {
        @Override
        public void onDisconnected() {
            TickDBClient.this.onDisconnected();
        }
        @Override
        public void onReconnected() {
            TickDBClient.this.onReconnected();
        }
    };

    protected final UserPrincipalResolver userPrincipalResolver = new UserPrincipalResolver();

    protected TickDBClient (String host, int port, boolean enableSSL, UserPrincipal user) {
        this.host = host;
        this.port = port;
        this.contextContainer = new ContextContainer();
        this.contextContainer.setQuickExecutorName("TickDBClient Executor");
        this.aeronContext = new DXClientAeronContext(contextContainer.getAffinityConfig());

        try {
            isRemoteConnection = !InetAddress.getByName(host).isLoopbackAddress();
        } catch (UnknownHostException x) {
            LOGGER.warn("Host '%s' is currently unknown.").with(host);
        }

        this.timeout = isRemoteConnection ? 5000 : 1000;

        //connMgr.setLazyLogger(LOGGER);
//        connMgr.setLogger(LOGGER);
//        connMgr.setLogLevel (Level.INFO);
        connMgr.setReconnector (this);
        connMgr.setAdjuster (new ReconnectableImpl.LinearIntervalAdjuster(500, 600000));

        this.user = user;
        this.enableSSL = enableSSL;

        this.topicConsumerThreadFactory = createTopicConsumerThreadFactory();

        this.timeSource = null;
        // DefaultTimeSourceProvider.getTimeSourceForApp("TickDBClient"); TODO: @MERGE
    }

    public TickDBClient (String host, int port, String user, String pass) {
        this(host, port, false, new UserPrincipal(user, pass));
    }

    public TickDBClient (String host, int port, boolean enableSSL, String user, String pass) {
        this(host, port, enableSSL, new UserPrincipal(user, pass));
    }

    public TickDBClient (String host, int port) {
        this(host, port, false, UserPrincipal.UNDEFINED);
    }

    public TickDBClient(String host, int port, boolean enableSSL) {
        this(host, port, enableSSL, UserPrincipal.UNDEFINED);
    }

    @Override
    public SessionClient                getSession() {
        assertOpen();
        return session;
    }

    public void                         setOauth2Client(Oauth2Client oauth2Client) {
        this.userPrincipalResolver.setOauth2Client(oauth2Client);
    }

    /**
     * Sets user access token to login to the Timebase server when OAUTH type of authentication defined on server.
     * @param token Access token
     */
    public void                         setAccessToken(String token) {
        String name = this.user != null ? user.getName() : "";
        setAccessToken(name, token);
    }

    /**
     * Sets user access token to login to the Timebase server when OAUTH type of authentication defined on server.
     * @param user Username access token is requested for
     * @param token Access token
     */
    public void                         setAccessToken(String user, String token) {
        this.user = new UserPrincipal(user, token);
    }

    public void                         setSslTermination(Boolean sslTermination) {
        this.sslTermination = sslTermination;
    }

    public boolean                      getSslTermination() {
        return false;
        // TODO: @MERGE

//        if (this.sslTermination != null) {
//            return this.sslTermination;
//        } else {
//            return VSClient.SSL_TERMINATION;
//        }
    }

    /*
            Tests round-trip latency (in nanoseconds)
         */
    public long[]                       testConnectLatency(int iterations) throws IOException {
        long[] times = new long[iterations];

        for (int i = 0; i < iterations; i++)
            times[i] = connection.getLatency();

        Arrays.sort(times);
        return times;
    }

    /*
        Returns round-trip latency (in nanoseconds)
     */
    public long[]                       getConnectLatency() {
        return latency;
    }

    public VSChannel        connect () throws IOException {
        return (connect (ChannelType.Simple, false, false, ChannelCompression.AUTO, 0));
    }

    VSChannel               connect (ChannelType type, boolean autoCommit, boolean noDelay)
            throws IOException
    {
        return connect(type, autoCommit, noDelay, ChannelCompression.AUTO, 0);
    }

    public VSChannel               connect(ChannelType type, boolean autoCommit, boolean noDelay, ChannelCompression c, int channelBufferSize)
        throws IOException
    {
        UserPrincipal user = userPrincipalResolver.resolve(getUser());
        VSChannel channel = createChannel(type, autoCommit, noDelay, c, channelBufferSize);
        TDBProtocol.writeCredentials(channel, user);
        return channel;
    }

    protected UserPrincipal     getUser() {
        return user;
    }

    protected VSChannel          createChannel(ChannelType type, boolean autoCommit, boolean noDelay, ChannelCompression c, int channelBufferSize)
            throws IOException
    {
        synchronized (this) {
            if (connection == null || !connection.isConnected()) {
                Util.close(connection);

                //int connectionPort = port;
                String idd = (applicationId != null ? id + ":" + applicationId : id);

                if (sslTermination == null) {
                    connection = new VSClient(host, port, idd, enableSSL, contextContainer);
                } else {
                    connection = new VSClient(host, port, idd, enableSSL, contextContainer);
                }

//                if (address != null)
//                    connection.setClientAddress(address, idd);

                connection.setNumTransportChannels(isRemoteConnection ? 1 : getConnectionsNumber(isRemoteConnection));
                connection.setTimeout(timeout);
                connection.setDisconnectedListener(listener);
                connection.setSslContext(SSLClientContextProvider.getSSLContext());
                connection.connect();
            } else if (isRemoteConnection) {
                // lazy initialization of additional sockets transports
                int number = getConnectionsNumber(true);
                if (connection.getNumTransportChannels() < number)
                    connection.increaseNumTransportChannels();
            }
        }

        boolean compressed = c == ChannelCompression.AUTO ? useCompression : (c == ChannelCompression.ON);

        int inCapacity;
        int outCapacity;

        int capacity = isRemoteConnection ? VSProtocol.CHANNEL_MAX_BUFFER_SIZE : VSProtocol.CHANNEL_BUFFER_SIZE;

        if (channelBufferSize > 0) {
            // Override ChannelType
            inCapacity = channelBufferSize;
            outCapacity = channelBufferSize;
        } else if (type == ChannelType.Input) {
            inCapacity = capacity;
            outCapacity = capacity / 4;
        } else if (type == ChannelType.Output) {
            inCapacity = capacity / 4;
            outCapacity = capacity;
        } else {
            inCapacity = capacity;
            outCapacity = capacity / 2;
        }

//        int inCapacity = isRemoteConnection ?
//            (type == ChannelType.Input ? VSProtocol.CHANNEL_MAX_BUFFER_SIZE : VSProtocol.CHANNEL_BUFFER_SIZE) :
//            (type == ChannelType.Input ? VSProtocol.CHANNEL_BUFFER_SIZE : VSProtocol.CHANNEL_BUFFER_SIZE / 4);
//
//        int outCapacity = isRemoteConnection ?
//            (type == ChannelType.Output ? VSProtocol.CHANNEL_MAX_BUFFER_SIZE : VSProtocol.CHANNEL_BUFFER_SIZE) :
//            (type == ChannelType.Output ? VSProtocol.CHANNEL_BUFFER_SIZE: VSProtocol.CHANNEL_BUFFER_SIZE / 4);

        VSChannel channel = connection.openChannel(inCapacity, outCapacity, compressed);
        channel.setAutoflush(autoCommit);
        channel.setNoDelay(noDelay);
        channel.getDataOutputStream().writeInt (TDBProtocol.VERSION);

        return channel;
    }

    @Override
    public String               getId() {
        return (enableSSL ? TDBProtocol.SSL_PROTOCOL_PREFIX : TDBProtocol.PROTOCOL_PREFIX) + host + ":" + port;
    }

    public String               getConnectionString() {
        return NetUtils.INSTANCE.formatUrl(
                TDBProtocol.getProtocol(enableSSL),
                host,
                port,
                null,
                user.getName(),
                user.getPass());
    }

    public CodecFactory         getCodecFactory (ChannelQualityOfService qos) {
        return (
            CodecFactory.useInterpretedCodecs (qos == ChannelQualityOfService.MIN_INIT_TIME) ?
                intpCodecFactory :
                compCodecFactory
        );
    }

    /**
     * Gets timeout value for socket connections.
     * @return timeout value, in milliseconds.
     */

    @Override
    public int                  getTimeout() {
        return timeout;
    }

    /**
     *  Sets timeout for socket connections, in milliseconds.
     *  A timeout of zero is interpreted as an infinite timeout.
     *
     *  By default - 5 sec for remote connections, 1 sec for local connections.
     *  @param timeout the specified timeout, in milliseconds.
     *  @see #getTimeout()
     */

    @Override
    public void                 setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public SSLContext           getSslContext() {
        return sslContext;
    }

    public void                 setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    protected SSLContext        getOrCreateContext() {
        if (enableSSL) {
            if (sslContext == null)
                sslContext = SSLClientContextProvider.getSSLContext();
            return sslContext;
        }

        return null;
    }

    /**
     * Returns server protocol version if client is already connected.
     * @return server version
     * @throws IllegalStateException if is not open
     */
    @Override
    public int                  getServerProtocolVersion() {
        assertOpen();
        return serverProtocolVersion;
    }

    /*
       Returns server start time. If not connected - returns -1;
    */
    @Override
    public synchronized long                 getServerStartTime() {
        assertOpen();

        return connection != null ? connection.getServerStartTime() : -1;
    }

    public void                             open(boolean readOnly) {
        if (syncOpen(readOnly))
            onReconnected();
    }

    private synchronized boolean            syncOpen (boolean readOnly) {

        if (isOpen && connMgr.isConnected())
            throw new IllegalStateException("Database already opened & connected.");

        if (!connMgr.isConnected()) {
            // Close previous Aeron context because it's not valid
            this.aeronContext.stopIfStarted();
        }

        isReadOnly = readOnly;

        VSChannel              ds = null;

        try {
            ds = connect();

            final DataOutputStream  out = ds.getDataOutputStream ();
            final DataInputStream   in = ds.getDataInputStream();

            out.writeInt (TDBProtocol.REQ_CONNECT);
            out.flush ();

            int result = in.readInt ();

            if (result == TDBProtocol.RESP_OK) {
                serverProtocolVersion = in.readInt ();

                byte                    accept = in.readByte ();

                if (!readOnly && accept == TDBProtocol.READ_ONLY)
                    throw new com.epam.deltix.util.io.UncheckedIOException("Server is read-only");

                serverVersion = in.readUTF ();
                secured = in.readBoolean();
                availableBandwidth = in.readLong();

                if (serverProtocolVersion < TDBProtocol.MIN_SERVER_VERSION)
                    throw new IncompatibleServerVersionException (serverProtocolVersion, serverVersion);

            } else if (result == TDBProtocol.RESP_LICENSE_ERROR) {
                throw new IllegalStateException("Server is not licensed");
            } else {
                checkResponse(result, in, serverProtocolVersion);
            }

            session = new SessionClient(this, serverProtocolVersion);

            // reuse quick executor to support disconnects processing
            if (!isOpen) {
                contextContainer.getQuickExecutor().reuseInstance();
            }

            isOpen = true;

            // check compression mode
            latency = testConnectLatency(6);
            useCompression = latency[2] > 10 * 1000 * 1000; // 10 ms

            if (useCompression)
                LOGGER.info("Using compression: connection latency[%s ms] > 10 ms").with(latency[2] / 1000 / 1000);

            this.aeronContext.start();
        }
        catch (EOFException ex) {
            throw new NotConfiguredException("Cannot open timebase [" + TDBProtocol.getProtocol(enableSSL) + "://" + host + ":" + port + "]", ex);
        }
        catch (IOException iox) {
            if (iox instanceof SocketException)
                onDisconnected();

            throw new com.epam.deltix.util.io.UncheckedIOException("Cannot open timebase [" + TDBProtocol.getProtocol(enableSSL)+ "://" + host + ":" + port + "]" , iox);
        } finally {
            Util.close (ds);
        }

        return isOpen;
    }

    public QuickExecutor                getQuickExecutor() {
        return contextContainer.getQuickExecutor();
    }

    void                         checkResponse (VSChannel ds)
        throws IOException
    {
      checkResponse(ds, serverProtocolVersion);
    }

    static void                         checkResponse (VSChannel ds, int protocolVersion)
            throws IOException
    {
        DataInputStream in = ds.getDataInputStream();
        checkResponse(in.readInt(), in, protocolVersion);
    }

    static void                 checkResponse (int code, DataInputStream in, int protocolVersion)
            throws IOException
    {
        if (code == TDBProtocol.RESP_OK)
            return;
        if (code == TDBProtocol.RESP_ERROR) {
            Throwable ex;
            try {
                ex = TDBProtocol.readError(in);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Cannot deserialize exception: " + e.getMessage());
            }

            if (ex instanceof StreamLockedException)
                throw (StreamLockedException)ex;
            else if (ex instanceof UnknownStreamException)
                throw (UnknownStreamException)ex;
            else if (ex instanceof UnknownSpaceException)
                throw new UnknownSpaceException(ex.getMessage(), ex);
            else if (ex instanceof AccessControlException)
                throw (AccessControlException)ex;
            else if (ex instanceof TopicApiException && ex instanceof RuntimeException)
                // All TopicException's are supposed to extend from RuntimeException
                // We don't want to wrap topic-related exceptions into ServerException
                throw (RuntimeException) ex;

            throw new ServerException(ex);

        } else if (code == TDBProtocol.RESP_EXCEPTION) {
            try {
                Exception ex = (Exception) TDBProtocol.readBinary(in);

                if (ex instanceof AccessControlException)
                    throw (AccessControlException)ex;

                throw new ServerException(ex);
            } catch (ClassNotFoundException e) {
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            }
        } else if (code == TDBProtocol.RESP_LICENSE_ERROR) {
            throw new IllegalStateException("Server is not licensed");
        }
    }

    private void                sendSimpleRequest (int req) {
        VSChannel              ds = null;

        try {
            ds = connect ();

            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (req);
            out.flush ();

            checkResponse (ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public long                             getSizeOnDisk () {
        assertOpen();

        return (getLongProperty (TDBProtocol.REQ_GET_SIZE));
    }

    public synchronized TickStreamClient    getStream (String key) {
        assertOpen();

        if (session == null) {
            throw new IllegalStateException("Timebase is disconnected from server");
        }

        return session.getStream(key, false);
    }

    /*
        Returns Server available bandwidth (bytes per second). Valid after opening timebase.
     */
    public long                             getServerAvailableBandwidth() {
        return availableBandwidth;
    }

    @Override
    public long                             getServerTime() {
        assertOpen();

        return (getLongProperty (TDBProtocol.REQ_GET_SERVER_TIME));
    }

    public synchronized TickStreamClient [] listStreams () {
        assertOpen();

        return session.listStreams();
    }

    @Override
    public DXChannel[]                      listChannels() {
        List<DXChannel> all = new ArrayList<>();
        all.addAll(Arrays.asList(listStreams()));

        if (serverProtocolVersion > 106) {
            List<String> names = listTopics();
            for (String topic : names)
                all.add(getTopic(topic));
        }

        return all.toArray(new DXChannel[all.size()]);
    }

    private void                            assertOpen() {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");
    }

    public void                 coolDown () {
        //  Ignore
    }

    public DXTickStream                   createAnonymousStream (
        StreamOptions                           options
    )
    {
        throw new UnsupportedOperationException();
//        TransientStreamImpl stream = new TransientStreamImpl(options, contextContainer.getQuickExecutor());
//        stream.open(false);
//        return stream;
    }

    public TickStreamClient     createStream (
        String                      key,
        String                      name,
        String                      description,
        int                         distributionFactor
    )
    {
        return (
            createStream (
                key,
                new StreamOptions (StreamScope.DURABLE, name, description, distributionFactor)
            )
        );
    }

    public synchronized TickStreamClient  createStream (
        String                      key,
        StreamOptions               options
    )
    {
        assertOpen();

        if (isReadOnly ())
            throw new IllegalStateException ("Database is open in read-only mode");

        //invalidateStreamCacheNow ();

        VSChannel                  ds = null;

        try {
            ds = connect ();

            final DataOutputStream  out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_CREATE_STREAM);
            out.writeUTF (key);
            TDBProtocol.writeStreamOptions (out, options, serverProtocolVersion);
            out.flush ();

            checkResponse (ds);
            //onReconnected();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }

        return (session.getStream (key, true));
    }

    @Override
    public DirectChannel createTopic(@Nonnull String topicKey, @Nonnull RecordClassDescriptor[] types, @Nullable TopicSettings settings) {
        TopicSettings topicSettings = settings != null ? settings : new TopicSettings();

        if (topicSettings.getTopicType() == TopicType.IPC) {
            createIpcTopic(topicKey, types, topicSettings);
        } else if (topicSettings.getTopicType() == TopicType.MULTICAST) {
            createMulticastTopic(topicKey, types, topicSettings);
        } else if (topicSettings.getTopicType() == TopicType.UDP_SINGLE_PUBLISHER) {
            createSinglePublisherUdpTopic(topicKey, types, topicSettings);
        } else {
            throw new IllegalArgumentException();
        }
        return new TopicClientChannel(this, topicKey);
    }

    /**
     * @param backwardsCompatibleCall if true then this means that calling method has API that was not changed during the topic protocol refactoring and still can work
     */
    private void writeTopicProtocolVersion(DataOutputStream out, boolean backwardsCompatibleCall) throws IOException {
        if (serverProtocolVersion >= 133) {
            TopicProtocol.writeTopicProtocolVersion(out, DirectProtocol.PROTOCOL_VERSION);
        } else {
            if (backwardsCompatibleCall) {
                // No need so send version for old server
                LOGGER.debug("Using old topic API");
            } else {
                throw new IllegalArgumentException("Connected server uses old topic protocol format. Client and server protocol version mismatch: client="
                        + TDBProtocol.VERSION + ", server=" + serverProtocolVersion
                        + ". Recommended TimeBase client version: " + serverVersion);
            }
        }
    }

    private void createIpcTopic(@Nonnull String topicKey, @Nonnull RecordClassDescriptor[] types, TopicSettings topicSettings) {
        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_TOPIC);
            writeTopicProtocolVersion(out, false);
            TopicProtocol.writeCreateTopicRequest(out, new CreateTopicRequest(
                            topicKey,
                            Arrays.asList(types),
                            topicSettings.getCopyToStream(),
                            topicSettings.getCopyToSpace()
                    ),
                    serverProtocolVersion);
        });
    }

    private void createMulticastTopic(@Nonnull String topicKey, @Nonnull RecordClassDescriptor[] types, TopicSettings topicSettings) {
        MulticastTopicSettings multicastTopicSettings = topicSettings.getMulticastSettings();
        MulticastTopicSettings mts = multicastTopicSettings != null ? multicastTopicSettings : new MulticastTopicSettings();
        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_MULTICAST_TOPIC);
            writeTopicProtocolVersion(out, false);
            TopicProtocol.writeCreateMulticastTopicRequest(out, new CreateMulticastTopicRequest(
                    topicKey,
                    Arrays.asList(types),
                    topicSettings.getCopyToStream(),
                    topicSettings.getCopyToSpace(),
                    mts.getEndpointHost(),
                    mts.getEndpointPort(),
                    mts.getNetworkInterface(),
                    mts.getTtl()
            ), serverProtocolVersion);
        });
    }

    private void createSinglePublisherUdpTopic(@Nonnull String topicKey, @Nonnull RecordClassDescriptor[] types, TopicSettings topicSettings) {
        String publisherAddress = topicSettings.getPublisherAddress();
        if (publisherAddress == null) {
            throw new IllegalArgumentException("Publisher address ins not set");
        }

        Map<CreateCustomTopicRequest.Field, Object> attributes = new HashMap<>();

        attributes.put(CreateCustomTopicRequest.Field.PUBLISHER_ADDRESS, publisherAddress);

        Integer termBufferLength = topicSettings.getTermBufferLength();
        if (termBufferLength != null) {
            attributes.put(CreateCustomTopicRequest.Field.TERM_BUFFER_LENGTH, termBufferLength);
        }

        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_CUSTOM_TOPIC);
            writeTopicProtocolVersion(out, false);
            TopicProtocol.writeCreateCustomTopicRequest(out, new CreateCustomTopicRequest(
                    topicKey,
                    Arrays.asList(types),
                    topicSettings.getCopyToStream(),
                    topicSettings.getCopyToSpace(), topicSettings.getTopicType(),
                    attributes
            ), serverProtocolVersion);
        });
    }

    @Nullable
    @Override
    public DirectChannel getTopic(@Nonnull String topicKey) {
        try {
            // We do this call to check if this topic exist
            // TODO: Consider introduction of separate API call for this method
            @SuppressWarnings("unused")
            RecordClassDescriptor[] types = getTypes(topicKey);

            return new TopicClientChannel(this, topicKey);
        } catch (UnknownChannelException e) {
            return null;
        }
    }

    @Override
    public void deleteTopic(@Nonnull String topicKey) {
        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_DELETE_TOPIC);
            writeTopicProtocolVersion(out, false);
            TopicProtocol.writeDeleteTopicRequest(out, new DeleteTopicRequest(topicKey));
        });
    }

    @Override
    public List<String> listTopics() {
        ListTopicsResponse response = executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_LIST_TOPICS);
            writeTopicProtocolVersion(out, true);
        }, TopicProtocol::readListTopicsResponse);

        return response.getTopics();
    }

    @Override
    public RecordClassDescriptor[] getTypes(@Nonnull String topicKey) {
        GetTopicMetadataResponse response = executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_GET_TOPIC_METADATA);
            writeTopicProtocolVersion(out, true);
            TopicProtocol.writeGetTopicMetadataRequest(out, new GetTopicMetadataRequest(topicKey));
        }, TopicProtocol::readGetTopicMetadataResponse);
        return response.getTypes().toArray(new RecordClassDescriptor[0]);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(@Nonnull String topicKey, @Nullable PublisherPreferences pref, @Nullable IdleStrategy idleStrategy) throws TopicNotFoundException {
        assertOpen();

        if (isReadOnly()) {
            throw new IllegalStateException("Database is open in read-only mode");
        }
        if (pref == null) {
            pref = new PublisherPreferences();
        }

        AddTopicPublisherResponse response;
        VSChannel tempDs = null;
        boolean closeDs = true;
        DataOutputStream out;
        try {
            tempDs = connect();

            out = tempDs.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_CREATE_TOPIC_PUBLISHER);
            writeTopicProtocolVersion(out, false);
            TopicProtocol.writeAddTopicPublisherRequest(out, new AddTopicPublisherRequest(topicKey));

            out.flush();

            checkResponse(tempDs);
            DataInputStream in = tempDs.getDataInputStream();

            response = TopicProtocol.readAddTopicPublisherResponse(in, serverProtocolVersion);
            closeDs = false;
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            if (closeDs) {
                Util.close(tempDs);
            }
        }
        VSChannel ds = tempDs;

        DirectLoaderFactory loaderFactory = new DirectLoaderFactory(compCodecFactory, pref.getTypeLoader());

        //Direct
        Aeron aeron = aeronContext.getAeronInstance(response.getAeronDir(), response.getTransferType());

        return loaderFactory.create(
                aeron, pref.raw, response.getPublisherChannel(), response.getDataStreamId(),
                response.getTypes(),
                () -> Util.close(ds),
                pref.getEffectiveIdleStrategy(idleStrategy), timeSource,
                pref.isPreserveNullTimestamp()
        );
    }

    @Override
    public Disposable createConsumerWorker(
            @Nonnull String topicKey,
            @Nullable ConsumerPreferences pref,
            @Nullable IdleStrategy idleStrategy,
            @Nullable ThreadFactory threadFactory,
            @Nonnull MessageProcessor processor
    ) {

        AddTopicSubscriberResponse response = executeSubscribeRequest(topicKey);

        if (pref == null) {
            pref = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, pref.getTypeLoader());

        Aeron aeron = aeronContext.getAeronInstance(response.getAeronDir(), response.getTransferType());
        SubscriptionWorker subscriptionWorker = factory.createListener(aeron, pref.raw, response.getChannel(),
                response.getDataStreamId(), response.getTypes(), processor,
                pref.getEffectiveIdleStrategy(idleStrategy), pref.getTopicDataLossHandler());

        if (threadFactory == null) {
            threadFactory = topicConsumerThreadFactory;
        }
        Thread thread = threadFactory.newThread(subscriptionWorker::processMessagesUntilStopped);
        thread.start();
        return subscriptionWorker;
    }

    @Override
    public MessagePoller createPollingConsumer(
            @Nonnull String topicKey,
            @Nullable ConsumerPreferences pref
    ) {

        AddTopicSubscriberResponse response = executeSubscribeRequest(topicKey);

        if (pref == null) {
            pref = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, pref.getTypeLoader());

        Aeron aeron = aeronContext.getAeronInstance(response.getAeronDir(), response.getTransferType());
        return factory.createPoller(aeron, pref.raw, response.getChannel(), response.getDataStreamId(), response.getTypes(), pref.getTopicDataLossHandler());
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(@Nonnull String topicKey, @Nullable ConsumerPreferences pref,
                                                           @Nullable IdleStrategy idleStrategy) {
        AddTopicSubscriberResponse response = executeSubscribeRequest(topicKey);

        if (pref == null) {
            pref = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, pref.getTypeLoader());

        Aeron aeron = aeronContext.getAeronInstance(response.getAeronDir(), response.getTransferType());
        return factory.createMessageSource(aeron, pref.raw, response.getChannel(), response.getDataStreamId(), response.getTypes(), pref.getEffectiveIdleStrategy(idleStrategy), pref.getTopicDataLossHandler());
    }

    private AddTopicSubscriberResponse executeSubscribeRequest(@Nonnull String topicKey) {
        return executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_TOPIC_SUBSCRIBER);
            writeTopicProtocolVersion(out, false);
            TopicProtocol.writeAddTopicSubscriberRequest(out, new AddTopicSubscriberRequest(topicKey));
        }, TopicProtocol::readAddTopicSubscriberResponse);
    }

    @Override
    public TickCursor           createCursor (
        SelectionOptions            options,
        TickStream ...              streams
    )
    {
        assertOpen();

        long time = options != null && options.reversed ? Long.MIN_VALUE : Long.MAX_VALUE;
        return TickCursorClientFactory.create(this, options, time, null, null, new IdentityKey[0], null, getAeronContext(), streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols, TickStream... streams) {
        assertOpen();

        if (streams != null && streams.length == 1)
            return streams[0].select(time, options, types, symbols);

        return TickCursorClientFactory.create(this, options, time, Long.MAX_VALUE, null, null, symbols, types, getAeronContext(), streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, TickStream... streams) {
        assertOpen();

        if (streams != null && streams.length == 1)
            return streams[0].select(time, options, types);

        return TickCursorClientFactory.create(this, options, time, null, null, types, getAeronContext(), streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, TickStream... streams) {
        assertOpen();

        if (streams != null && streams.length == 1)
            return streams[0].select(time, options);

        return TickCursorClientFactory.create(this, options, time, null, null, getAeronContext(), streams);
    }

    public void                 setGrowthPolicy (GrowthPolicy policy) {
        throw new UnsupportedOperationException ("Not supported yet.");
    }

    public void                 trimToSize () {
        sendSimpleRequest (TDBProtocol.REQ_TRIM_TO_SIZE);
    }

    public void                 warmUp () {
        //  Ignore
    }

    public void                 delete () {
        throw new UnsupportedOperationException ("Not supported in remote mode.");
    }

    public void                 format () {
        throw new UnsupportedOperationException ("Not supported in remote mode.");
    }

    public synchronized boolean isOpen () {
        return (isOpen);
    }

    public boolean              isReadOnly () {
        return (isReadOnly);
    }

    public void    close () {

        connMgr.cancelReconnect();

        boolean shutdown = false;

        synchronized (this) {

            // free locks
            if (session != null) {
                TickStreamClient[] streams = session.getStreams();
                for (TickStreamClient stream : streams) {
                    try {
                        stream.unlock();
                    } catch (Throwable e) {
                    
                        LOGGER.warn("Cannot unlock stream [%s]. Error: %s").with(stream.getKey()).with(e);
                    }
                }
                session.close();
            }

            session = null;

            if (connection != null) {
                connection.setDisconnectedListener(null);
                connection.close();
                LOGGER.info("Timebase connection closed by API call");
            }
            connection = null;

            shutdown = isOpen;

            isOpen = false;
        }

        // shutdown QuickExecutor only if 'open'
        if (shutdown) {
            contextContainer.getQuickExecutor().shutdownInstance();
            // We can be already stopped due to a connection loss
            aeronContext.stopIfStarted();
        }

        if (connMgr.isConnected())
            connMgr.disconnected();

        userPrincipalResolver.close();
    }

    public File[]               getDbDirs() {
        throw new UnsupportedOperationException ("Not supported in remote mode.");
    }

    private VSChannel          sendRequest (int req) throws IOException {

        final VSChannel             ds = connect ();
        final DataOutputStream      out = ds.getDataOutputStream ();

        out.writeInt (req);
        out.flush ();

        checkResponse (ds);

        return (ds);
    }

    private long                getLongProperty (int req) {
        VSChannel                  ds = null;

        try {
            ds = sendRequest (req);
            return ds.getDataInputStream().readLong();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    public long                 getMetaDataVersion () {
        assertOpen();
        
        return (getLongProperty (TDBProtocol.REQ_GET_MD_VERSION));
    }

    private void                sendMetaDataUpdate () {
        assertOpen();
        
        VSChannel                  ds = null;

        try {
            ds = connect ();

            final DataOutputStream      out = ds.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_SET_METADATA);
            TDBProtocol.writeClassSet (ds.getDataOutputStream (), md, serverProtocolVersion);
            out.flush ();

            checkResponse (ds);
            //onReconnected();
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close (ds);
        }
    }

    private void                refreshMetaData () {
        assertOpen();

        assert Thread.holdsLock (this);

        long                        version = getMetaDataVersion ();

        if (version != mdVersion) {
            VSChannel                  ds = null;

            try {
                ds = sendRequest (TDBProtocol.REQ_GET_METADATA);

                md = (RecordClassSet) TDBProtocol.readClassSet (ds.getDataInputStream());
                mdVersion = version;

                md.addChangeListener (updater);
                //onReconnected();
            } catch (IOException iox) {
                throw new com.epam.deltix.util.io.UncheckedIOException(iox);
            } finally {
                Util.close (ds);
            }
        }
    }

    public synchronized MetaData            getMetaData () {
        refreshMetaData ();
        return (md);
    }   

    // DisconnectableImpl.Reconnector impl.
    @Override
    public boolean tryReconnect(int numAttempts, long timeSinceDisconnected, ReconnectableImpl helper) throws Exception {
        if (isOpen)
            open(isReadOnly);

        return (true);
    }

    // Disconnectable impl.
    @Override
    public void         addDisconnectEventListener(DisconnectEventListener listener) {
        connMgr.addDisconnectEventListener(listener);
    }

    @Override
    public void         removeDisconnectEventListener(DisconnectEventListener listener) {
        connMgr.removeDisconnectEventListener(listener);
    }

    @Override
    public boolean      isSecured() {
        assertOpen();
        return secured;
    }

    public boolean      isLoopback() {
        return !isRemoteConnection;
    }

    @Override
    public boolean                          isConnected() {
        if (!connMgr.isConnected())
            return false;

        synchronized (this) {
            return (connection != null && connection.isConnected());
        }
    }

    void                                    onSessionDisconnected() {
        if (connMgr.isConnected()) {
            synchronized (this) {
                session = new SessionClient(this, serverProtocolVersion);
            }
        } else {
            synchronized (this) {
                session = null;
            }
        }
    }

    private void                            onDisconnected() {
        if (connMgr.isConnected()) {
            connMgr.scheduleReconnect();
            // listeners can actually stop reconnecting using "close"
            connMgr.disconnected();

            // closing session
            synchronized (this) {
                session = Util.close(session);
            }
        }
    }

    private void                             onReconnected() {
        if (!connMgr.isConnected()) {

            synchronized (this) {
                if (session == null)
                    session = new SessionClient(this, serverProtocolVersion);
            }

            connMgr.connected();
        }
    }

    /**
     * Compiles QQL/DDL Query.
     * Returns CompilationResult contains parsed tokens information.
     * If query contains errors, throws CompilationException.
     * @param query query to compile.
     * @param tokens
     */
    public void                         compileQuery(String query, List<Token> tokens) {
        VSChannel                  ds = null;

        try {
            ds = connect ();
            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_COMPILE_QQL);
            out.writeUTF(query);
            out.flush();

            checkResponse(ds);

            DataInputStream in = ds.getDataInputStream();
            long errorLocation = in.readLong();
            Throwable exception = errorLocation != Long.MIN_VALUE ? TDBProtocol.readError(in, new CompileExceptionResolver(errorLocation)) : null;

            int length = in.readInt();

            for (int i = 0; i < length; i++) {
                long location = in.readLong();
                TokenType type = TokenType.valueOf(in.readUTF());
                tokens.add(new Token(type, location));
            }

            if (exception instanceof CompilationException)
                throw (CompilationException) exception;

        } catch (IOException | ClassNotFoundException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Override
    public ClassSet<ClassDescriptor> describeQuery(String qql, SelectionOptions options, Parameter... params)
            throws CompilationException {

        VSChannel                  ds = null;

        try {
            ds = connect ();
            final DataOutputStream out = ds.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_DESCRIBE_QUERY);
            out.writeUTF(qql);
            SelectionOptionsCodec.write (out, options == null ? new SelectionOptions() : options, serverProtocolVersion);
            TDBProtocol.writeParameters(params, out, serverProtocolVersion);
            out.flush();

            DataInputStream in = ds.getDataInputStream();
            int code = in.readInt();
            if (code == TDBProtocol.RESP_ERROR) {
                Throwable exception = TDBProtocol.readError(in, new CompileExceptionResolver(-1));
                if (exception instanceof CompilationException) {
                    throw (CompilationException) exception;
                } else {
                    throw new RuntimeException(exception);
                }
            }

            return TDBProtocol.readClassSet(ds.getDataInputStream());
        } catch (IOException | ClassNotFoundException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executeQuery (qql, null, null, params));
    }

    @Override
    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executeQuery (qql, options, null, params));
    }

    @Override
    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        CharSequence []                         ids,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executeQuery (qql, options, null, ids, Long.MIN_VALUE, params));
    }

//    @Override
//    public InstrumentMessageSource          executeQuery (
//        Element                                 qql,
//        SelectionOptions                        options,
//        TickStream []                           streams,
//        CharSequence []                         ids,
//        long                                    time,
//        Parameter ...                           params
//    )
//        throws CompilationException
//    {
//        return (executeQuery (qql.toString (), options, streams, ids, time, params));
//    }

    @Override
    public InstrumentMessageSource executeQuery(
            String              qql,
            SelectionOptions    options,
            TickStream[]        streams,
            CharSequence[]      ids,
            long                startTimestamp,
            long                endTimestamp,
            Parameter...        params)
        throws CompilationException
    {
        return TickCursorClientFactory.create(this, options, startTimestamp, endTimestamp, qql, params, ids, null, getAeronContext(), streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, IdentityKey[] ids, TickStream... streams) {
        return TickCursorClientFactory.create(this, options, time, Long.MAX_VALUE, null, null, ids, types, getAeronContext(), streams);
    }

    public InstrumentMessageSource          executeQuery (
        String                                  qql,
        SelectionOptions                        options,
        TickStream []                           streams,
        CharSequence []                         ids,
        long                                    time,
        Parameter ...                           params
    )
        throws CompilationException
    {
        assertOpen();
        return TickCursorClientFactory.create(this, options, time, Long.MAX_VALUE, qql, params, ids, null, getAeronContext(), streams);
    }

//    @Override
//    public InstrumentMessageSource executeQuery(
//            String              qql,
//            SelectionOptions    options,
//            TickStream[]        streams,
//            String[]            ids,
//            long                startTimestamp,
//            long                endTimestamp,
//            Parameter...        params)
//        throws CompilationException
//    {
//        assertOpen();
//        return TickCursorClientFactory.create(this, options, startTimestamp, endTimestamp, qql, params, ids, null, getAeronContext(), streams);
//    }

    public String                           getHost () {
        return host;
    }

    public int                              getPort () {
        return port;
    }

    public void                             setCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    public void                             setApplicationId(String id) {
        if (id != null)
            this.connMgr.setLogPrefix("TickDBClient (" + id + ")");

        this.applicationId = id;
    }

    public void                             setExternalAddress(String address) {
        this.address = address;
    }

    public String                           getApplicationId() {
        return applicationId;
    }

    @Override
    public void setAffinityConfig(AffinityConfig affinityConfig) {
        this.contextContainer.setAffinityConfig(affinityConfig);
        this.aeronContext.setAffinityConfig(affinityConfig);
        this.topicConsumerThreadFactory = createTopicConsumerThreadFactory();
    }

    @Nonnull
    private ThreadFactory createTopicConsumerThreadFactory() {
        AffinityConfig affinityConfig = contextContainer.getAffinityConfig();
        if (affinityConfig == null || affinityConfig.getAffinityLayout() == null) {
            return topicNoAffinityConsumerThreadFactory;
        } else {
            return new PinnedThreadFactoryWrapper(topicNoAffinityConsumerThreadFactory, affinityConfig.getAffinityLayout());
        }
    }

    public boolean                          isSSLEnabled() { return (connection == null) ? false : connection.isSSLEnabled(); }

    public int                              getSSLPort() { return (connection == null) ? 0 : connection.getSSLPort(); }

    @Override
    public void                             addStateListener(DBStateListener listener) {
        stateListeners.addIfAbsent(listener);
    }

    @Override
    public void                             removeStateListener(DBStateListener listener) {
        stateListeners.remove(listener);
    }

    @Override
    public void                             fireStateChanged(final String key) {
        for (DBStateListener stateListener : stateListeners)
            stateListener.changed(key);
    }

    @Override
    public void                             fireAdded(final String key) {
        for (DBStateListener stateListener : stateListeners)
            stateListener.added(key);
    }

    @Override
    public void                             fireDeleted(final String key) {
        for (DBStateListener stateListener : stateListeners)
            stateListener.deleted(key);
    }

    @Override
    public void                             fireRenamed(String fromKey, String toKey) {
        for (DBStateListener stateListener : stateListeners)
            stateListener.renamed(fromKey, toKey);
    }

    @Override
    public String                           toString () {
        return (enableSSL ? TDBProtocol.SSL_PROTOCOL_PREFIX : TDBProtocol.PROTOCOL_PREFIX) + host + ":" + port;
    }

    public String getServerVersion() {
        return serverVersion;
    }

    DXClientAeronContext getAeronContext() {
        return aeronContext;
    }

    @FunctionalInterface
    private interface RequestSender {
        void writeRequest(DataOutputStream out) throws IOException;
    }

    @FunctionalInterface
    private interface ResponseReader<T> {
        T readResponse(DataInputStream in) throws IOException;
    }

    private void executeModifyingOperation(RequestSender requestSender) {
        assertOpen();

        if (isReadOnly()) {
            throw new IllegalStateException("Database is open in read-only mode");
        }

        VSChannel ds = null;
        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            requestSender.writeRequest(out);
            out.flush();

            checkResponse(ds);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    private <T> T executeRequest(boolean needWriteAccess, RequestSender requestSender, ResponseReader<T> responseReader) {
        assertOpen();

        if (needWriteAccess && isReadOnly()) {
            throw new IllegalStateException("Database is open in read-only mode");
        }

        VSChannel ds = null;
        try {
            ds = connect();

            final DataOutputStream out = ds.getDataOutputStream();

            requestSender.writeRequest(out);
            out.flush();

            checkResponse(ds);
            DataInputStream in = ds.getDataInputStream();

            return responseReader.readResponse(in);
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        } finally {
            Util.close(ds);
        }
    }

    @Override
    public TopicDB getTopicDB() {
        return this;
    }

    @Override
    public boolean isTopicDBSupported() {
        return true;
    }

    private static class TopicConsumerThreadFactory implements ThreadFactory {
        private final AtomicLong threadCounter = new AtomicLong(0L);
        private final ThreadFactory backingThreadFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = backingThreadFactory.newThread(r);
            long index = threadCounter.incrementAndGet();
            String name = String.format(Locale.ROOT, "topic-consumer-%d", index);
            thread.setName(name);
            return thread;
        }
    }
}