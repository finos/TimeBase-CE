package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.data.stream.DXChannel;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.pub.md.MetaData;
import com.epam.deltix.qsrv.hf.tickdb.impl.PQExecutor;
import com.epam.deltix.qsrv.hf.tickdb.impl.TickDBImpl;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamStateListener;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamStateNotifier;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.mon.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.InstrumentMessageSource;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.Parameter;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.PreparedQuery;
import com.epam.deltix.qsrv.hf.tickdb.pub.topic.TopicDB;
import com.epam.deltix.timebase.messages.service.EventMessage;
import com.epam.deltix.util.lang.GrowthPolicy;
import com.epam.deltix.util.lang.Wrapper;
import com.epam.deltix.util.parsers.CompilationException;
import com.epam.deltix.util.parsers.Element;

import java.io.File;

/**
 * Delegates all method calls to the provided delegate.
 *
 * @author Alexei Osipov
 */
public abstract class BaseDXTickDBWrapper implements Wrapper<DXTickDB>, DXTickDB, TBMonitor, StreamStateNotifier, StreamStateListener, PQExecutor {
    protected final TickDBImpl delegate;

    public BaseDXTickDBWrapper(DXTickDB delegate) {
        if (!(delegate instanceof TickDBImpl)) {
            // TODO: Add support for any original classes using DynamicProxies
            throw new IllegalArgumentException("The instance to be wrapped must be an TickDBImpl");
        }
        this.delegate = (TickDBImpl) delegate;
    }

    @Override
    public DXTickDB getNestedInstance() {
        return delegate;
    }

    @Override
    public void warmUp() {
        delegate.warmUp();
    }

    @Override
    public void coolDown() {
        delegate.coolDown();
    }

    @Override
    public void trimToSize() {
        delegate.trimToSize();
    }

    @Override
    public long getSizeOnDisk() {
        return delegate.getSizeOnDisk();
    }

    @Override
    public DXTickStream getStream(String key) {
        return delegate.getStream(key);
    }

    @Override
    public DXTickStream[] listStreams() {
        return delegate.listStreams();
    }

    @Override
    public DXChannel[] listChannels() {
        return delegate.listChannels();
    }

    @Override
    public File[] getDbDirs() {
        return delegate.getDbDirs();
    }

    @Override
    @Deprecated
    public DXTickStream createAnonymousStream(StreamOptions options) {
        return delegate.createAnonymousStream(options);
    }

    @Override
    public long getServerTime() {
        return delegate.getServerTime();
    }

    @Override
    public DXTickStream createStream(String key, StreamOptions options) {
        return delegate.createStream(key, options);
    }

    @Override
    public DXTickStream createFileStream(String key, String dataFile) {
        return delegate.createFileStream(key, dataFile);
    }

    @Override
    public DXTickStream createStream(String key, String name, String description, int distributionFactor) {
        return delegate.createStream(key, name, description, distributionFactor);
    }

    @Override
    public void setGrowthPolicy(GrowthPolicy policy) {
        delegate.setGrowthPolicy(policy);
    }

    @Override
    public long getMetaDataVersion() {
        return delegate.getMetaDataVersion();
    }

    @Override
    public MetaData getMetaData() {
        return delegate.getMetaData();
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, SelectionOptions options, CharSequence[] ids, Parameter... params) throws CompilationException {
        return delegate.executeQuery(qql, options, ids, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, Parameter... params) throws CompilationException {
        return delegate.executeQuery(qql, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, SelectionOptions options, Parameter... params) throws CompilationException {
        return delegate.executeQuery(qql, options, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(String qql, SelectionOptions options, TickStream[] streams, CharSequence[] ids, long time, Parameter... params) throws CompilationException {
        return delegate.executeQuery(qql, options, streams, ids, time, params);
    }

    @Override
    public InstrumentMessageSource executeQuery(Element qql, SelectionOptions options, TickStream[] streams, CharSequence[] ids, long time, Parameter... params) throws CompilationException {
        return delegate.executeQuery(qql, options, streams, ids, time, params);
    }

    @Override
    public InstrumentMessageSource executePreparedQuery(PreparedQuery pq, SelectionOptions options, TickStream[] streams, CharSequence[] ids, boolean fullScan, long time, Parameter[] params) throws CompilationException {
        return delegate.executePreparedQuery(pq, options, streams, ids, fullScan, time, params);
    }

    public void log(EventMessage msg) {
        delegate.log(msg);
    }

    @Override
    public void addStreamStateListener(StreamStateListener listener) {
        delegate.addStreamStateListener(listener);
    }

    @Override
    public void removeStreamStateListener(StreamStateListener listener) {
        delegate.removeStreamStateListener(listener);
    }

    @Override
    public void changed(DXTickStream stream, int property) {
        delegate.changed(stream, property);
    }

    @Override
    public void writerCreated(DXTickStream stream, IdentityKey[] ids) {
        delegate.writerCreated(stream, ids);
    }

    @Override
    public void writerClosed(DXTickStream stream, IdentityKey[] ids) {
        delegate.writerClosed(stream, ids);
    }

    @Override
    public void created(DXTickStream stream) {
        delegate.created(stream);
    }

    @Override
    public void deleted(DXTickStream stream) {
        delegate.deleted(stream);
    }

    @Override
    public void renamed(DXTickStream stream, String key) {
        delegate.renamed(stream, key);
    }

    @Override
    public void addPropertyMonitor(String component, PropertyMonitor listener) {
        delegate.addPropertyMonitor(component, listener);
    }

    @Override
    public TopicDB getTopicDB() {
        return delegate.getTopicDB();
    }

    @Override
    public boolean isTopicDBSupported() {
        return delegate.isTopicDBSupported();
    }

    @Override
    public TickCursor createCursor(SelectionOptions options, TickStream... streams) {
        return delegate.createCursor(options, streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols, TickStream... streams) {
        return delegate.select(time, options, types, symbols, streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, IdentityKey[] ids, TickStream... streams) {
        return delegate.select(time, options, types, ids, streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, TickStream... streams) {
        return delegate.select(time, options, types, streams);
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, TickStream... streams) {
        return delegate.select(time, options, streams);
    }

    @Override
    public void format() {
        delegate.format();
    }

    @Override
    public void delete() {
        delegate.delete();
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public boolean isReadOnly() {
        return delegate.isReadOnly();
    }

    @Override
    public void open(boolean readOnly) {
        delegate.open(readOnly);
    }

    @Override
    public String getId() {
        return delegate.getId();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public boolean getTrackMessages() {
        return delegate.getTrackMessages();
    }

    @Override
    public void setTrackMessages(boolean value) {
        delegate.setTrackMessages(value);
    }

    @Override
    public TBCursor[] getOpenCursors() {
        return delegate.getOpenCursors();
    }

    @Override
    public TBLoader[] getOpenLoaders() {
        return delegate.getOpenLoaders();
    }

    @Override
    public TBLock[] getLocks() {
        return delegate.getLocks();
    }

    @Override
    public void addObjectMonitor(TBObjectMonitor monitor) {
        delegate.addObjectMonitor(monitor);
    }

    @Override
    public void removeObjectMonitor(TBObjectMonitor monitor) {
        delegate.removeObjectMonitor(monitor);
    }

    @Override
    public TBObject getObjectById(long id) {
        return delegate.getObjectById(id);
    }
}
