package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.gflog.api.LogLevel;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassSet;
import com.epam.deltix.qsrv.hf.tickdb.comm.*;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickStreamProperties;
import com.epam.deltix.qsrv.hf.tickdb.pub.BackgroundProcessInfo;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.util.concurrent.QuickExecutor;
import com.epam.deltix.util.concurrent.Signal;
import com.epam.deltix.util.io.UncheckedIOException;
import com.epam.deltix.util.lang.StringUtils;
import com.epam.deltix.util.lang.Util;
import com.epam.deltix.util.time.Periodicity;
import com.epam.deltix.util.time.TimeKeeper;
import com.epam.deltix.util.vsocket.*;

import java.io.*;
import java.security.AccessControlException;
import java.util.HashMap;

/**
 *
 */
public class SessionClient implements Closeable {

    static final Log LOGGER = LogFactory.getLog("tickdb.session");

    private final VSChannel         ds;
    private final TickDBClient      db;
    private final DataOutputStream  out;
    private final StreamsCache      cache = new StreamsCache();
    private volatile boolean        closed = false;

    private class ServerListener extends QuickExecutor.QuickTask {
        private final Signal closeSignal = new Signal();

        private DataInputStream     in;

        final Runnable              avlnr =
                new Runnable () {
                    @Override
                    public void                 run () {
                        ServerListener.this.submit ();
                    }
                };

        final DBStateNotifierTask stateNotifierTask;

        public ServerListener(DataInputStream in, QuickExecutor executor) {
            super (executor);
            this.stateNotifierTask = new DBStateNotifierTask(executor, db);
            this.in = in;
        }

        public void waitForClose(){
            try {
               closeSignal.await();
            } catch (InterruptedException e) {
                TickDBClient.LOGGER.info("Waiting interrupted: %s").with(e);
            }
        }

        private void notifyClose(Throwable error) {
            TickDBClient.LOGGER.debug(this + " closing signal");

            streamsSignal.set(error);
            closeSignal.set(error);

            cache.clear(); // clean cache and notify waiting property readers
        }

        @Override
        public synchronized String toString() {
            return "ServerListenerTask@" + hashCode();
        }

        @Override
        public void         run () {

            try {
                for (;;) {

                    if (in.available () < 4)
                        break;

                    processCommand ();
                }
            } catch (EOFException | ChannelClosedException iox) {
                TickDBClient.LOGGER.debug("Clean Disconnect: %s").with(iox);
                // valid close
                notifyClose(null);
            } catch (Throwable ex) {

                TickDBClient.LOGGER.error("Error while processing server command: %s").with(ex);
                notifyClose(ex);

                onDisconnected();
            }
        }

        private void processCommand() throws Exception {
            int         cmd = in.readInt();

            if (LOGGER.isEnabled(LogLevel.DEBUG))
                LOGGER.debug(this + " Processing command: " + cmd);

            String key;
            switch (cmd) {
                case TDBProtocol.STREAM_PROPERTY_CHANGED:
                    key = in.readUTF();
                    int p = in.readByte();

                    cache.onPropertyChanged(key, p);
                    stateNotifierTask.fireStateChanged(key);
                    break;

                case TDBProtocol.STREAM_RENAMED:
                    String oldKey = in.readUTF();
                    String newKey = in.readUTF();

                    cache.onStreamRenamed(oldKey, newKey);
                    stateNotifierTask.fireStateRenamed(oldKey, newKey);
                    break;

                case TDBProtocol.STREAM_CREATED:
                    key = in.readUTF();
                    cache.onStreamCreated(key);
                    stateNotifierTask.fireAdded(key);
                    break;

                case TDBProtocol.STREAM_DELETED:
                    key = in.readUTF();
                    cache.onStreamDeleted(key);
                    stateNotifierTask.fireDeleted(key);
                    break;

                case TDBProtocol.STREAMS_DEFINITION:
                    int count = in.readInt();
                    for (int i = 0; i < count; i++) {
                        TickStreamClient stream = readStream(in);
                        // cache may already contains this stream
                        if (cache.addStream(stream))
                            stateNotifierTask.fireStateChanged(stream.getKey());
                    }

                    streamsSignal.set();
                    break;

                case TDBProtocol.STREAM_PROPERTY:
                    readProperty(in);
                    break;

                case TDBProtocol.STREAMS_CHANGED:
                    TickStreamClient[] streams = cache.getStreams();

                    cache.clear();

                    for (final TickStreamClient stream : streams)
                        stateNotifierTask.fireStateChanged(stream.getKey());
                    break;

                case TDBProtocol.SESSION_CLOSED:
                    notifyClose(null);
                    break;

                default:
                    throw new RuntimeException("Unknown server command: " + cmd);
            }
        }
    }

    private ServerListener  serverListener;
    private final Signal    streamsSignal = new Signal();

    public SessionClient(TickDBClient db) {
        this.db = db;

        VSChannel channel = null;
        try {
            channel = db.connect (ChannelType.Session, false, false);
            out = channel.getDataOutputStream ();

            out.writeInt (TDBProtocol.REQ_START_SESSION);
            out.flush();

            serverListener = new ServerListener(channel.getDataInputStream(), db.getQuickExecutor());
            channel.setAvailabilityListener(serverListener.avlnr);
            serverListener.submit();

        } catch (IOException x) {
            Util.close(channel);
            throw new com.epam.deltix.util.io.UncheckedIOException(x);
        } finally {
            ds = channel;
        }
    }

    private void            readProperty(DataInputStream in) throws Exception {

        String key = in.readUTF();
        int property = in.readByte();

        TickStreamClient stream = cache.getStream(key);

        // stream can be null in case of async call
        if (LOGGER.isEnabled(LogLevel.DEBUG))
            LOGGER.debug("Stream '" + key + "' is not retrieved yet: for property = (" + property + ")");

        switch (property) {
            case TickStreamProperties.NAME:
                String name = in.readUTF();
                if (stream != null)
                    stream.options.name = !StringUtils.equals("<NULL>", name) ? name : null;
                break;

            case TickStreamProperties.DESCRIPTION:
                String desc = in.readUTF();
                if (stream != null)
                    stream.options.description = !StringUtils.equals("<NULL>", desc) ? desc : null;
                break;

            case TickStreamProperties.PERIODICITY:
                String period = in.readUTF();
                if (stream != null)
                    stream.options.periodicity = !StringUtils.equals("<NULL>", period) ? Periodicity.parse(period) : Periodicity.mkIrregular();
                break;

            case TickStreamProperties.SCHEMA:
                boolean poly = in.readBoolean();
                RecordClassSet classSet = TDBProtocol.readClassSet(in);
                if (stream != null)
                    stream.options.setMetaData(poly, classSet);
                break;

            case TickStreamProperties.ENTITIES:
                readInstruments(stream, in);
                break;

            case TickStreamProperties.TIME_RANGE:
                long[] range = TDBProtocol.readTimeRangeLong(in);
                if (stream != null)
                    stream.timeRange.set(range);
                break;

            case TickStreamProperties.BG_PROCESS:
                BackgroundProcessInfo info = TDBProtocol.readBGProcessInfo(in);
                if (stream != null)
                    stream.bgProcess = info;
                break;

            case TickStreamProperties.WRITER_CREATED:
                IdentityKey[] ids = TDBProtocol.readInstrumentIdentities(in);
                if (stream != null) {
                    for (int i = 0; ids != null && i < ids.length; i++)
                        stream.setWriteMode(ids[i], true);

                    if (ids == null)
                        stream.setWriteMode(true);
                }

                break;

            case TickStreamProperties.WRITER_CLOSED:
                IdentityKey[] instruments = TDBProtocol.readInstrumentIdentities(in);
                if (stream != null) {
                    for (int i = 0; instruments != null && i < instruments.length; i++)
                        stream.setWriteMode(instruments[i], false);

                    if (instruments == null)
                        stream.setWriteMode(false);
                }

                break;

            case TickStreamProperties.OWNER:
                String owner = in.readUTF();
                if (stream != null)
                    stream.options.owner = !StringUtils.equals("<NULL>", owner) ? owner : null;
                break;
        }

        StreamState state = cache.getState(key);

        if (LOGGER.isEnabled(LogLevel.DEBUG))
            LOGGER.debug(this + ": readProperty(" + key + "," + property + ") state=" + (state != null ? state.get(property) : "<null>"));

        if (state != null)
            state.setNotify(property);
    }

    private TickStreamClient readStream(DataInputStream in) throws IOException, ClassNotFoundException {
        String key = in.readUTF();

        if (LOGGER.isEnabled(LogLevel.DEBUG))
            LOGGER.debug(this + ": reading stream:" + key);

        StreamOptions options = TDBProtocol.readStreamOptions(in, TDBProtocol.VERSION);

        TickStreamClient stream = new TickStreamClient(db, key, options);

        // read instruments & ranges
        readInstruments(stream, in);

        stream.timeRange.set(TDBProtocol.readTimeRangeLong(in));
        stream.bgProcess = TDBProtocol.readBGProcessInfo(in);

        long dataVersion = in.readLong();
        long replicaVersion = in.readLong();
        stream.formatVersion = in.readInt();

        return stream;
    }

    private void                                    readInstruments (TickStreamClient stream, DataInputStream in)
            throws IOException
    {
        HashMap<IdentityKey, EntityTimeRange>  instruments = new HashMap<>();

        int length = in.readInt();

        long writers = 0;

        for (int i = 0; i < length; i++) {
            IdentityKey id = TDBProtocol.readIdentityKey(in);
            EntityTimeRange range = readTimeRange(in);

            if (range != null) {
                range.writing = in.readBoolean();
                if (range.writing) {
                    range.updated = TimeKeeper.currentTime;
                    writers++;
                }

                instruments.put(id, range);
            } else {
                instruments.put(id, null);
            }
        }

        if (stream != null)
            stream.setEntities(instruments, writers);
    }

    static EntityTimeRange       readTimeRange (DataInputStream in) throws IOException {
        boolean isNull = in.readBoolean();
        if (!isNull)
            return new EntityTimeRange(in.readLong(), in.readLong());
        return null;
    }

    TickStreamClient[]                          getStreams() {
        return cache.getStreams();
    }

    void                                        rename(String key, String newKey) {
        cache.onStreamRenamed(key, newKey);
    }

    void                                        delete(String key) {
        cache.onStreamDeleted(key);
    }

    public synchronized TickStreamClient[]      listStreams() {

        StreamsCache.State state = cache.getDbState();

        if (state == StreamsCache.State.INITIAL || state == StreamsCache.State.PARTIALLY_LOADED) {
            loadStreams();
            cache.setUnchanged();
        } else if (state == StreamsCache.State.CHANGED) {
            String[] names = cache.getUnmappedStreams();
            if (names.length > 0)
                loadStreams(names);
            cache.setUnchanged();
        }

        return cache.getStreams();
    }

    public synchronized TickStreamClient        getStream(String key, boolean isNew) {
        if (key == null)
            return null;

        boolean loaded = false;

        if (cache.getDbState() == StreamsCache.State.INITIAL) {
            loadStreams(key);
            loaded = true;
        }
        else if (isNew) {
            loadStreams(key);
            loaded = true;
        }

        TickStreamClient stream = cache.getStream(key);
        if (stream == null && !loaded)
            loadStreams(key);

        stream = cache.getStream(key);
        if (stream == null)
            stream = readStream(key); // case when UAC enabled - user should get an error when stream is not accessible

        return stream;
    }

    private TickStreamClient                    readStream(String key) {
        TickStreamClient stream = null;

        try (VSChannel channel = db.connect()) {
            DataInputStream in = channel.getDataInputStream();
            DataOutputStream out = channel.getDataOutputStream();

            out.writeInt(TDBProtocol.REQ_GET_STREAM);
            out.writeUTF(key);
            out.flush();

            TickDBClient.checkResponse(channel);
            boolean notNull = in.readBoolean();
            if (notNull)
                stream = readStream(in);
        } catch (AccessControlException ace) {
            throw ace;
        } catch (Exception e) {
            TickDBClient.LOGGER.warn(this + ": error reading stream from server: %s").with(e);
            throw new UncheckedIOException(e);
        }

        return stream;
    }

    private  void               loadStreams(String ... keys) {

        try {
            streamsSignal.reset();

            synchronized (out) {
                out.writeInt(TDBProtocol.REQ_GET_STREAMS);
                out.writeInt(keys.length);

                for (int i = 0; i < keys.length; i++)
                    out.writeUTF(keys[i]);
                out.flush();
            }

            // wait for server reply
            streamsSignal.await();
            streamsSignal.verify();

        } catch (IOException ex) {
            TickDBClient.LOGGER.warn(this + ": IO Exception on wait %s").with(ex);
            throw new com.epam.deltix.util.io.UncheckedIOException(ex);
        } catch (InterruptedException e) {
            TickDBClient.LOGGER.warn(this + ": Wait interrupted %s").with(e);
        }
    }

    public void                     resetProperty(String stream, int property) {
        StreamState state = cache.getState(stream);

        if (state != null)
            state.reset(property);
    }

    public boolean                  getStreamProperty(String key, int property) {

        StreamState state = cache.getState(key);

        if (state != null && !state.get(property)) {
            try {
                // should be called under lock to prevent race condition
                state.monitor(property);

                synchronized (out) {
                    out.writeInt(TDBProtocol.REQ_GET_STREAM_PROPERTY);
                    out.writeUTF(key);
                    out.writeByte(property);
                    out.flush();
                }

                state.wait(property);
                return true;

            } catch (IOException e) {
                TickDBClient.LOGGER.warn(this + ": IO Exception on wait: %s").with(e);
                throw new com.epam.deltix.util.io.UncheckedIOException(e);
            } catch (InterruptedException e) {
                TickDBClient.LOGGER.warn(this + ": wait interrupted: %s").with(e);
            }
        }

        return false;
    }

    private void                                onDisconnected() {
       if (closed)
            return;       

       LOGGER.warn(this + ": Session disconnected.");
       serverListener.unschedule();
       cache.clear();

       ds.setAvailabilityListener(null);
       ds.close(true);

       db.onSessionDisconnected();
    }

    public void                             close() {
        if (LOGGER.isEnabled(LogLevel.DEBUG))
            LOGGER.debug(this + ": Closing session " + ds);

        try {
            if (ds.getState() == VSChannelState.Connected) {
                synchronized (out) {
                    out.writeInt(TDBProtocol.REQ_CLOSE_SESSION);
                    out.flush();
                }
            } else {
                LOGGER.warn("Closing session without send signal.");
            }
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }

        serverListener.waitForClose();
        serverListener.unschedule();
        
        cache.clear();

        ds.setAvailabilityListener(null);
        ds.close(true);

        closed = true;
    }
}
