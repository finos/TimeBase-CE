package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.UnknownChannelException;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.hf.pub.ChannelCompression;
import com.epam.deltix.qsrv.hf.pub.ChannelQualityOfService;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.codec.CodecFactory;
import com.epam.deltix.qsrv.hf.pub.md.MetaData;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.spi.conn.DisconnectEventListener;
import com.epam.deltix.qsrv.hf.spi.conn.ReconnectableImpl;
import com.epam.deltix.qsrv.hf.tickdb.comm.CompileExceptionResolver;
import com.epam.deltix.qsrv.hf.tickdb.comm.NotConfiguredException;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.TopicProtocol;
import com.epam.deltix.qsrv.hf.tickdb.comm.UnknownStreamException;
import com.epam.deltix.qsrv.hf.tickdb.comm.UserPrincipal;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicPublisherRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicPublisherResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicSubscriberRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.AddTopicSubscriberResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateCustomTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateMulticastTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.CreateTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.DeleteTopicRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicInstrumentMappingRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicInstrumentMappingResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicMetadataRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicMetadataResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicTemporaryInstrumentMappingRequest;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.GetTopicTemporaryInstrumentMappingResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.ListTopicsResponse;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicClientChannel;
import com.epam.deltix.qsrv.hf.tickdb.impl.topic.TopicTransferType;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.Token;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.TokenType;
import com.epam.deltix.qsrv.hf.tickdb.pub.DBStateListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.DBStateNotifier;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.RemoteTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
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
import com.epam.deltix.qsrv.hf.topic.consumer.MappingProvider;
import com.epam.deltix.qsrv.hf.topic.consumer.SubscriptionWorker;
import com.epam.deltix.qsrv.hf.topic.loader.DirectLoaderFactory;
import com.epam.deltix.thread.affinity.AffinityConfig;
import com.epam.deltix.thread.affinity.PinnedThreadFactoryWrapper;
import com.epam.deltix.util.ContextContainer;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.io.GUID;
import com.epam.deltix.util.io.SSLClientContextProvider;
import com.epam.deltix.util.io.idlestrat.IdleStrategy;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.lang.GrowthPolicy;
import com.epam.deltix.util.lang.JavaVerifier;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.net.NetUtils;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.vsocket.VSChannel;
import com.epam.deltix.util.vsocket.VSClient;
import com.epam.deltix.util.vsocket.VSProtocol;
import io.aeron.Aeron;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadFactory;

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

    private final UserPrincipal                 user;

    private final String                        host;
    private final int                           port;
    private int                                 timeout;

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

    private boolean                             secured = false;

    private final CodecFactory                  intpCodecFactory =
        CodecFactory.newInterpretingCachingFactory();

    private final CodecFactory                  compCodecFactory =
        CodecFactory.newCompiledCachingFactory ();

    private boolean                             useCompression = false;
    private boolean                             isRemoteConnection = false;

    protected volatile SessionClient            session;

    protected final boolean                     enableSSL;

    private final ContextContainer contextContainer;
    private final DXClientAeronContext aeronContext;
    private static final ThreadFactory topicNoAffinityConsumerThreadFactory = new ThreadFactoryBuilder().setNameFormat("topic-consumer-%d").build();
    private ThreadFactory topicConsumerThreadFactory;

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
        return session;
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
        VSChannel channel = createChannel(type, autoCommit, noDelay, c, channelBufferSize);
        TDBProtocol.writeCredentials(channel, getUser());
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

                connection = new VSClient(host, port, idd, enableSSL, contextContainer);
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

    @Override
    public int                  getServerProtocolVersion() {
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

//                if (accept == TDBProtocol.INCOMPATIBLE_CLIENT_PROTOCOL_VERSION)
//                    throw new IncompatibleClientVersionException (serverProtocolVersion, serverVersion);

            } else if (result == TDBProtocol.RESP_LICENSE_ERROR) {
                throw new UnlicensedServerException();
            } else {
                checkResponse(result, in);
            }

            session = new SessionClient(this);

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

    static void                         checkResponse (VSChannel ds)
        throws IOException
    {
        DataInputStream in = ds.getDataInputStream();
        checkResponse(in.readInt(), in);
    }

    private static void                 checkResponse (int code, DataInputStream in)
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
            throw new UnlicensedServerException();
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
            TDBProtocol.writeStreamOptions (out, options, TDBProtocol.VERSION);
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

    public synchronized TickStreamClient     createFileStream (
        String                      key,
        String                      dataFile
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

            out.writeInt (TDBProtocol.REQ_CREATE_FILE_STREAM);
            out.writeUTF (key);
            out.writeUTF (dataFile);
            out.flush ();

            checkResponse (ds);
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

    private void createIpcTopic(@Nonnull String topicKey, @Nonnull RecordClassDescriptor[] types, TopicSettings topicSettings) {
        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_TOPIC);
            TopicProtocol.writeCreateTopicRequest(out, new CreateTopicRequest(
                            topicKey,
                            Arrays.asList(types),
                            topicSettings.getInitialEntitySet(),
                            topicSettings.getCopyToStream()
                    ),
                    serverProtocolVersion);
        });
    }

    private void createMulticastTopic(@Nonnull String topicKey, @Nonnull RecordClassDescriptor[] types, TopicSettings topicSettings) {
        MulticastTopicSettings multicastTopicSettings = topicSettings.getMulticastSettings();
        MulticastTopicSettings mts = multicastTopicSettings != null ? multicastTopicSettings : new MulticastTopicSettings();
        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_MULTICAST_TOPIC);
            TopicProtocol.writeCreateMulticastTopicRequest(out, new CreateMulticastTopicRequest(
                    topicKey,
                    Arrays.asList(types),
                    topicSettings.getInitialEntitySet(),
                    topicSettings.getCopyToStream(),
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

        executeModifyingOperation(out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_CUSTOM_TOPIC);
            TopicProtocol.writeCreateCustomTopicRequest(out, new CreateCustomTopicRequest(
                    topicKey,
                    Arrays.asList(types),
                    topicSettings.getInitialEntitySet(),
                    topicSettings.getCopyToStream(),
                    topicSettings.getTopicType(),
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
            TopicProtocol.writeDeleteTopicRequest(out, new DeleteTopicRequest(topicKey));
        });
    }

    @Override
    public List<String> listTopics() {
        //noinspection CodeBlock2Expr
        ListTopicsResponse response = executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_LIST_TOPICS);
        }, TopicProtocol::readListTopicsResponse);

        return response.getTopics();
    }

    @Override
    public RecordClassDescriptor[] getTypes(@Nonnull String topicKey) {
        GetTopicMetadataResponse response = executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_GET_TOPIC_METADATA);
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
            TopicProtocol.writeAddTopicPublisherRequest(out, new AddTopicPublisherRequest(topicKey, pref.getInitialEntitySet()));

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
        Aeron aeron = getAeronInstance(response.getAeronDir(), response.getTransferType());

        return loaderFactory.create(
                aeron, pref.raw, response.getPublisherChannel(), response.getMetadataSubscriberChannel(), response.getDataStreamId(),
                response.getServerMetadataStreamId(), response.getTypes(),
                response.getLoaderNumber(), out, response.getMapping(),
                () -> Util.close(ds), pref.getEffectiveIdleStrategy(idleStrategy)
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

        Aeron aeron = getAeronInstance(response.getAeronDir(), response.getTransferType());
        MappingProvider mappingProvider = getMappingProvider(topicKey, response.getDataStreamId());
        SubscriptionWorker subscriptionWorker = factory.createListener(aeron, pref.raw, response.getChannel(), response.getDataStreamId(), response.getTypes(), processor, pref.getEffectiveIdleStrategy(idleStrategy), mappingProvider);
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

        Aeron aeron = getAeronInstance(response.getAeronDir(), response.getTransferType());
        MappingProvider mappingProvider = getMappingProvider(topicKey, response.getDataStreamId());
        return factory.createPoller(aeron, pref.raw, response.getChannel(), response.getDataStreamId(), response.getTypes(), mappingProvider);
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(@Nonnull String topicKey, @Nullable ConsumerPreferences pref, @Nullable IdleStrategy idleStrategy) {
        AddTopicSubscriberResponse response = executeSubscribeRequest(topicKey);

        if (pref == null) {
            pref = new ConsumerPreferences();
        }

        DirectReaderFactory factory = new DirectReaderFactory(compCodecFactory, pref.getTypeLoader());

        Aeron aeron = getAeronInstance(response.getAeronDir(), response.getTransferType());
        MappingProvider mappingProvider = getMappingProvider(topicKey, response.getDataStreamId());
        return factory.createMessageSource(aeron, pref.raw, response.getChannel(), response.getDataStreamId(), response.getTypes(), pref.getEffectiveIdleStrategy(idleStrategy), mappingProvider);
    }

    @Nonnull
    private ConstantIdentityKey[] getTopicInstrumentMapping(@Nonnull String topicKey, int dataStreamId) {
        GetTopicInstrumentMappingResponse response = executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_GET_TOPIC_INSTRUMENT_MAPPING);
            TopicProtocol.writeGetTopicInstrumentMappingRequest(out, new GetTopicInstrumentMappingRequest(topicKey, dataStreamId));
        }, TopicProtocol::readGetTopicInstrumentMappingResponse);
        return response.getMappingArray();
    }

    @Nonnull
    private IntegerToObjectHashMap<ConstantIdentityKey> getTopicTempMappingSnapshot(@Nonnull String topicKey, int dataStreamId, int requestedTempEntityIndex) {
        GetTopicTemporaryInstrumentMappingResponse response = executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_GET_TOPIC_TEMPORARY_INSTRUMENT_MAPPING);
            TopicProtocol.writeGetTopicTemporaryInstrumentMappingRequest(out, new GetTopicTemporaryInstrumentMappingRequest(topicKey, dataStreamId, requestedTempEntityIndex));
        }, TopicProtocol::readGetTopicTemporaryInstrumentMappingResponse);
        return response.getMapping();
    }

    private MappingProvider getMappingProvider(@Nonnull String topicKey, int dataStreamId) {
        return new MappingProvider() {
            @Override
            public ConstantIdentityKey[] getMappingSnapshot() {
                ConstantIdentityKey[] mapping = getTopicInstrumentMapping(topicKey, dataStreamId);
                LOGGER.debug("Got topic mapping snapshot for dataStreamId=%s of size=%s").with(dataStreamId).with(mapping.length);
                return mapping;
            }

            @Override
            public IntegerToObjectHashMap<ConstantIdentityKey> getTempMappingSnapshot(int neededTempEntityIndex) {
                IntegerToObjectHashMap<ConstantIdentityKey> mapping = getTopicTempMappingSnapshot(topicKey, dataStreamId, neededTempEntityIndex);
                LOGGER.debug("Got topic temporary mapping snapshot for dataStreamId=%s of size=%s").with(dataStreamId).with(mapping.size());
                return mapping;
            }
        };
    }

    @NotNull
    private Aeron getAeronInstance(@Nullable String aeronDir, @Nonnull TopicTransferType transferType) {
        if (aeronDir != null) {
            // Use shared Aeron driver (even for multicast)
            return aeronContext.getServerSharedAeronInstance(aeronDir);
        } else {
            switch (transferType) {
                case UDP:
                    // Use client-specific driver
                    return aeronContext.getStandaloneAeronInstance();
                case IPC:
                    throw new IllegalStateException("IPC transfer type MUST use shared Aeron driver");
                default:
                    throw new IllegalArgumentException("Unknown transfer type: " + transferType);
            }
        }
    }

    private AddTopicSubscriberResponse executeSubscribeRequest(@Nonnull String topicKey) {
        return executeRequest(false, out -> {
            out.writeInt(TDBProtocol.REQ_CREATE_TOPIC_SUBSCRIBER_NO_MAPPING);
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

//    @Override
//    public TickCursor             select(
//            long                                time,
//            SelectionOptions                    options,
//            String[]                            types,
//            IdentityKey[]                       entities,
//            TickStream ...                      streams)
//    {
//        assertOpen();
//
//        if (streams != null && streams.length == 1)
//            return streams[0].select(time, options, types, entities);
//
//        return TickCursorClientFactory.create(this, options, time, null, null, entities, types, getAeronContext(), streams);
//    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols, TickStream... streams) {
        assertOpen();

        if (streams != null && streams.length == 1)
            return streams[0].select(time, options, types, symbols);

        return TickCursorClientFactory.create(this, options, time, null, null, symbols, types, getAeronContext(), streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, IdentityKey[] ids, TickStream... streams) {
        assertOpen();

        if (streams != null && streams.length == 1)
            return streams[0].select(time, options, types, ids);

        return TickCursorClientFactory.create(this, options, time, null, null, ids, types, getAeronContext(), streams);
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
            TDBProtocol.writeClassSet (ds.getDataOutputStream (), md);
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

                md = TDBProtocol.readClassSet (ds.getDataInputStream());
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
                session = new SessionClient(this);
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
                    session = new SessionClient(this);
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

    @Override
    public InstrumentMessageSource          executeQuery (
        Element                                 qql,
        SelectionOptions                        options,
        TickStream []                           streams,
        CharSequence []                         ids,
        long                                    time,
        Parameter ...                           params
    )
        throws CompilationException
    {
        return (executeQuery (qql.toString (), options, streams, ids, time, params));
    }

    @Override
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

        return TickCursorClientFactory.create(this, options, time, qql, params, ids, null, getAeronContext(), streams);
    }

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
}
