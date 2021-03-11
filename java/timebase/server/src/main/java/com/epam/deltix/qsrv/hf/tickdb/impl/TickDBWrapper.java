package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.MetaData;
import com.epam.deltix.qsrv.hf.tickdb.comm.server.TickDBServer;
import com.epam.deltix.qsrv.hf.tickdb.lang.compiler.qcache.PQCache;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.CompilerUtil;
import com.epam.deltix.qsrv.hf.tickdb.lang.pub.ParamSignature;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.qsrv.hf.security.TimeBasePermissions;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.util.lang.GrowthPolicy;
import com.epam.deltix.util.lang.Wrapper;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;
import com.epam.deltix.util.security.SecurityController;
import com.epam.deltix.util.security.SecurityReloadListener;
import com.epam.deltix.util.security.SecurityReloadNotifier;

import java.io.File;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TickDBWrapper
        implements Wrapper<DXTickDB>, AuthorizationContext,
        DXTickDB, StreamStateNotifier, StreamStateListener, SecurityReloadNotifier
{
    private final SecurityController        securityController;
    protected final Principal               user;

    private final DXTickDB                  delegate;

    private final PQCache                   pqCache = new PQCache(this);

    private final Map<String, DXTickStream> streams = new HashMap<>();

    public TickDBWrapper(DXTickDB delegate, SecurityController securityController, Principal user) {
        if (securityController == null)
            throw new IllegalArgumentException("Security controller is null.");
        if (user == null)
            throw new IllegalArgumentException("Unknown user.");

        this.securityController = securityController;
        this.user = user;

        this.delegate = delegate;
        addStreamStateListener(this);
    }

    private void    checkState() {
        if (!isOpen ())
            throw new IllegalStateException ("Database is not open");
    }

    @Override
    public void                 warmUp() {
        delegate.warmUp();
    }

    @Override
    public void                 coolDown() {
        delegate.coolDown();
    }

    @Override
    public void                 trimToSize() {
        checkState();

        if (isReadOnly ())
            throw new IllegalStateException ("Database is open in read-only mode");

        try {
            for (DXTickStream stream : streams.values()) {
                if (canRead(stream))
                    ((ServerStreamImpl) stream).trimToSize();
            }
        } catch (IOException iox) {
            throw new com.epam.deltix.util.io.UncheckedIOException(iox);
        }

    }

    @Override
    public long                 getSizeOnDisk() {
        return delegate.getSizeOnDisk();
    }

    @Override
    public DXTickStream         getStream(String key) {
        DXTickStream stream = delegate.getStream(key);
        if (stream == null)
            return null;

        checkReadable(stream);

        return wrapOrGetCached(stream);
    }

    /**
     * @return only streams accessed by the user.
     */
    @Override
    public DXTickStream[]       listStreams() {
        DXTickStream[] streams = delegate.listStreams();
        ArrayList<DXTickStream> accessedStreams = new ArrayList<>();

        for (int i = 0; i < streams.length; ++i) {
            DXTickStream stream = streams[i];
            if (canRead(stream)) {
                accessedStreams.add(wrapOrGetCached(stream));
            } else
                TickDBServer.LOGGER.warning(
                        "Principal '" + user.getName() + "' has no permission to read stream '" + streams[i].getKey() + "'. ");
        }

        return accessedStreams.toArray(new DXTickStream[accessedStreams.size()]);
    }

    /**
     * @return only streams accessed by the user.
     */
    @Override
    public DXChannel[]       listChannels() {
        return delegate.listChannels();
//        ArrayList<DXChannel> accessedStreams = new ArrayList<>();
//
//        for (int i = 0; i < channels.length; ++i) {
//            DXChannel channel = channels[i];
//            if (canRead(channel)) {
//                accessedStreams.add(wrapOrGetCached(stream));
//            } else
//                TickDBServer.LOGGER.warning(
//                        "Principal '" + user.getName() + "' has no permission to read stream '" + channels[i].getKey() + "'. ");
//        }
//
//        return accessedStreams.toArray(new DXTickStream[accessedStreams.size()]);
    }

    @Override
    public TickCursor           createCursor(SelectionOptions options, TickStream... streams) {
        checkState();

        for (TickStream stream : streams)
            if (stream instanceof DXTickStream)
                checkReadable((DXTickStream) stream);

        return wrapCursor(delegate.createCursor(options, streams));
    }

//    @Override
//    public TickCursor           select(long time, SelectionOptions options, String[] types,
//                                       CharSequence[] ids, TickStream... streams)
//    {
//        checkState();
//
//        for (TickStream stream : streams)
//            if (stream instanceof DXTickStream)
//                checkReadable((DXTickStream) stream);
//
//        return wrapCursor(delegate.select(time, options, types, ids, streams));
//    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols, TickStream... streams) {
        checkState();

        for (TickStream stream : streams)
            if (stream instanceof DXTickStream)
                checkReadable((DXTickStream) stream);

        return wrapCursor(delegate.select(time, options, types, symbols, streams));
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, IdentityKey[] ids, TickStream... streams) {
        checkState();

        for (TickStream stream : streams)
            if (stream instanceof DXTickStream)
                checkReadable((DXTickStream) stream);

        return wrapCursor(delegate.select(time, options, types, ids, streams));
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, TickStream... streams) {
        checkState();

        for (TickStream stream : streams)
            if (stream instanceof DXTickStream)
                checkReadable((DXTickStream) stream);

        return wrapCursor(delegate.select(time, options, types, streams));
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, TickStream... streams) {
        checkState();

        for (TickStream stream : streams)
            if (stream instanceof DXTickStream)
                checkReadable((DXTickStream) stream);

        return wrapCursor(delegate.select(time, options, streams));
    }

    @Override
    public File[]               getDbDirs() {
        return delegate.getDbDirs();
    }

    @Override
    public DXTickStream         createAnonymousStream(StreamOptions options) {
        checkState();
        checkCanCreateStreams();

        return wrapAndCache(delegate.createAnonymousStream(options));
    }

    @Override
    public long                 getServerTime() {
        return delegate.getServerTime();
    }

    @Override
    public DXTickStream         createStream(String key, StreamOptions options) {
        checkState();
        checkCanCreateStreams();

        if (options.owner == null) {
            options.owner = user.getName();
        } else {
            if (!canImpersonate(options.owner)){
                TickDBServer.LOGGER.warning(
                        "Principal '" + user.getName() + "' has no permission to set owner to '" + options.owner + "'. " +
                                "Stream will be created with owner '" + user.getName() + "'.");
                options.owner = user.getName();
            }
        }

        return wrapAndCache(delegate.createStream(key, options));
    }

    @Override
    public DXTickStream         createFileStream(String key, String dataFile) {
        checkState();
        checkCanCreateStreams();

        DXTickStream newStream = delegate.createFileStream(key, dataFile);
        newStream.setOwner(user != null ? user.getName() : null);

        return wrapAndCache(newStream);
    }

    @Override
    public DXTickStream         createStream(String key, String name, String description, int distributionFactor) {
        checkState();
        checkCanCreateStreams();

        return wrapAndCache(delegate.createStream(key, name, description, distributionFactor));
    }

    @Override
    public void                 setGrowthPolicy(GrowthPolicy policy) {
        delegate.setGrowthPolicy(policy);
    }

    @Override
    public long                 getMetaDataVersion() {
        return delegate.getMetaDataVersion();
    }

    @Override
    public MetaData             getMetaData() {
        return delegate.getMetaData();
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql,
                                                SelectionOptions options,
                                                CharSequence[] ids,
                                                Parameter... params) throws CompilationException
    {
        if (delegate instanceof PQExecutor)
            return executeQuery((PQExecutor) delegate, qql, options, null, ids, true, Long.MIN_VALUE, params);

        return delegate.executeQuery(qql, options, ids, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql,
                                                Parameter... params) throws CompilationException
    {
        if (delegate instanceof PQExecutor)
            return executeQuery((PQExecutor) delegate, qql, null, null, null, true, Long.MIN_VALUE, params);

        return delegate.executeQuery(qql, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql,
                                                SelectionOptions options,
                                                Parameter... params) throws CompilationException
    {
        if (delegate instanceof PQExecutor)
            return executeQuery((PQExecutor) delegate, qql, options, null, null, true, Long.MIN_VALUE, params);

        return delegate.executeQuery(qql, options, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql,
                                                SelectionOptions options,
                                                TickStream[] streams,
                                                CharSequence[] ids,
                                                long time,
                                                Parameter... params) throws CompilationException
    {
        if (delegate instanceof PQExecutor)
            return executeQuery((PQExecutor) delegate, qql, options, streams, ids, time == TimeConstants.TIMESTAMP_UNKNOWN, time, params);

        return delegate.executeQuery(qql, options, streams, ids, time, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(Element parsedQQL,
                                                SelectionOptions options,
                                                TickStream[] streams,
                                                CharSequence[] ids,
                                                long time,
                                                Parameter... params) throws CompilationException
    {
        if (delegate instanceof PQExecutor)
            return executeQuery((PQExecutor) delegate, parsedQQL, options, streams, ids, time == TimeConstants.TIMESTAMP_UNKNOWN, time, params);

        return delegate.executeQuery(parsedQQL, options, streams, ids, time, params);
    }

    private InstrumentMessageSource executeQuery(PQExecutor executor,
                                                 String qql,
                                                 SelectionOptions options,
                                                 TickStream[] streams,
                                                 CharSequence[] ids,
                                                 boolean fullScan,
                                                 long time,
                                                 Parameter[] params)
    {
        return executeQuery(executor, CompilerUtil.parse(qql), options, streams, ids, fullScan, time, params);
    }

    private InstrumentMessageSource executeQuery(PQExecutor executor,
                                                 Element parsedQQL,
                                                 SelectionOptions options,
                                                 TickStream[] streams,
                                                 CharSequence[] ids,
                                                 boolean fullScan,
                                                 long time,
                                                 Parameter[] params)
    {
        PreparedQuery pq = pqCache.prepareQuery(parsedQQL, ParamSignature.signatureOf(params));
        return executor.executePreparedQuery(pq, options, streams, ids, fullScan, time, params);
    }

    @Override
    public void                 format() {
        pqCache.clear();
        delegate.format();
    }

    @Override
    public void                 delete() {
        delegate.delete();
    }

    @Override
    public boolean              isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean              isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public void                 open(boolean readOnly) {
        delegate.open(readOnly);
    }

    @Override
    public String               getId() {
        return delegate.getId();
    }

    @Override
    public void                 close() {
        removeStreamStateListener(this);
        pqCache.clear();

        delegate.close();

        synchronized (streams) {
            streams.clear();
        }
    }

    @Override
    public void                 addStreamStateListener(StreamStateListener listener) {
        if (delegate instanceof StreamStateNotifier)
            ((StreamStateNotifier) delegate).addStreamStateListener(listener);
    }

    @Override
    public void                 removeStreamStateListener(StreamStateListener listener) {
        if (delegate instanceof StreamStateNotifier)
            ((StreamStateNotifier) delegate).removeStreamStateListener(listener);
    }

    @Override
    public void                 addReloadListener(SecurityReloadListener listener) {
        if (securityController instanceof SecurityReloadNotifier) {
            ((SecurityReloadNotifier) securityController).addReloadListener(listener);
        }
    }

    @Override
    public void                 removeReloadListener(SecurityReloadListener listener) {
        if (securityController instanceof SecurityReloadNotifier) {
            ((SecurityReloadNotifier) securityController).removeReloadListener(listener);
        }
    }

    @Override
    public void                 changed(DXTickStream stream, int property) {
    }

    @Override
    public void                 writerCreated(DXTickStream stream, IdentityKey[] ids) {
    }

    @Override
    public void                 writerClosed(DXTickStream stream, IdentityKey[] ids) {
    }

    @Override
    public void                 created(DXTickStream stream) {
    }

    @Override
    public void                 renamed(DXTickStream stream, String oldKey) {
        synchronized (streams) {
            streams.remove(oldKey);
        }

        invalidateQueryCache();
    }

    @Override
    public void                 deleted(DXTickStream stream) {
        synchronized (streams) {
            streams.remove(stream.getKey());
        }

        invalidateQueryCache();
    }

    @Override
    public DXTickDB             getNestedInstance() {
        return delegate;
    }

    private void                invalidateQueryCache() {
        pqCache.clear();
    }

    private DXTickStream        wrapAndCache(DXTickStream stream) {
        DXTickStream wrappedStream = wrapStream(stream);

        synchronized (streams) {
            streams.put(stream.getKey(), wrappedStream);
        }

        return wrappedStream;
    }

    private DXTickStream        wrapOrGetCached(DXTickStream stream) {
        String key = stream.getKey();

        DXTickStream cachedStream;

        synchronized (streams) {
            cachedStream = streams.get(key);
            if (cachedStream == null)
                streams.put(key, cachedStream = wrapStream(stream));
        }

        return cachedStream;
    }

    private DXTickStream        wrapStream(DXTickStream stream) {
        return new ServerStreamWrapper(stream, this);
    }

    private TickCursor          wrapCursor(TickCursor cursor) {
        return new TickCursorWrapper(cursor, this);
    }

    @Override
    public void                 checkCanCreateStreams() {
        checkPermission(TimeBasePermissions.CREATE_STREAM_PERMISSION);
    }

    @Override
    public void                 checkReadable(DXTickStream stream) {
        checkPermission(TimeBasePermissions.READ_PERMISSION, stream);
    }

    @Override
    public boolean              canRead(DXTickStream stream) {
        return hasPermission(TimeBasePermissions.READ_PERMISSION, stream);
    }

    private boolean              canRead(DXChannel channel) {
        return securityController.hasPermission(user, TimeBasePermissions.READ_PERMISSION, channel);
    }

    @Override
    public void                 checkWritable(DXTickStream stream) {
        checkPermission(TimeBasePermissions.WRITE_PERMISSION, stream);
    }

    @Override
    public void                 checkPermission(String permission) {
        securityController.checkPermission(user, permission);
    }

    @Override
    public void                 checkPermission(String permission, DXTickStream stream) {
        securityController.checkPermission(user, permission, stream);
    }

    @Override
    public boolean              hasPermission(String permission) {
        return securityController.hasPermission(user, permission);
    }

    @Override
    public boolean              hasPermission(String permission, DXTickStream stream) {
        return securityController.hasPermission(user, permission, stream);
    }

    @Override
    public void                 checkCanImpersonate(String anotherUserId) {
        if (!canImpersonate(anotherUserId))
            throw new AccessControlException("You do not have permission to impersonate \"" + anotherUserId + '"');
    }

    @Override
    public boolean              canImpersonate(String anotherUserId) {
        return securityController.hasPermissionOverPrincipal(
                user,
                TimeBasePermissions.IMPERSONATE_PERMISSION,
                anotherUserId);
    }

    @Override
    public TopicDB getTopicDB() {
        // TODO: Implement?
        throw new UnsupportedOperationException("Topics are not supported for this DB instance");
    }

    @Override
    public boolean isTopicDBSupported() {
        return false;
    }
}
