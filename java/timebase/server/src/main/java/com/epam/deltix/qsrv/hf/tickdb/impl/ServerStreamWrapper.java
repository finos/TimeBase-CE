package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.SchemaChangeTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.StreamCopyTask;
import com.epam.deltix.qsrv.hf.tickdb.pub.task.TransformationTask;
import com.epam.deltix.qsrv.hf.security.TimeBasePermissions;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.lang.Wrapper;
import com.epam.deltix.util.time.Periodicity;

/**
 *
 */
public class ServerStreamWrapper extends ServerStreamImpl implements Wrapper<DXTickStream>, DXTickStream {

    private final DXTickStream              delegate;
    private final AuthorizationContext      context;

    public ServerStreamWrapper(DXTickStream delegate, AuthorizationContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public DXTickDB                 getDB() {
        return delegate.getDB();
    }

    @Override
    public TickLoader               createLoader() {
        context.checkWritable(this);

        return wrapLoader(delegate.createLoader());
    }

    @Override
    public TickLoader               createLoader(LoadingOptions options) {
        context.checkWritable(this);

        return wrapLoader(delegate.createLoader(options));
    }

    @Override
    public void                     truncate(long time, IdentityKey... ids) {
        context.checkWritable(this);

        delegate.truncate(time, ids);
    }

    @Override
    public void                     delete(TimeStamp from, TimeStamp to, IdentityKey... ids) {
        context.checkWritable(this);

        delegate.delete(from, to, ids);
    }

    @Override
    public void                     clear(IdentityKey... ids) {
        context.checkWritable(this);

        delegate.clear(ids);
    }

    @Override
    public String                   getKey() {
        return delegate.getKey();
    }

    @Override
    public String                   getName() {
        return delegate.getName();
    }

    @Override
    public String                   getDescription() {
        return delegate.getDescription();
    }

    @Override
    public long                     getTypeVersion() {
        return delegate.getTypeVersion();
    }

    @Override
    public int                      getFormatVersion() {
        return delegate.getFormatVersion();
    }

    @Override
    public boolean                  isPolymorphic() {
        return delegate.isPolymorphic();
    }

    @Override
    public boolean                  isFixedType() {
        return delegate.isFixedType();
    }

    @Override
    public RecordClassDescriptor    getFixedType() {
        return delegate.getFixedType();
    }

    @Override
    public RecordClassDescriptor[]  getPolymorphicDescriptors() {
        return delegate.getPolymorphicDescriptors();
    }

    @Override
    public ClassDescriptor[]        getAllDescriptors() {
        return delegate.getAllDescriptors();
    }

    @Override
    public TickCursor               select(long time, SelectionOptions options, String[] types, IdentityKey[] entities) {
        context.checkReadable(this);

        return wrapCursor(delegate.select(time, options, types, entities));
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols) {
        context.checkReadable(this);

        return wrapCursor(delegate.select(time, options, types, symbols));
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types) {
        context.checkReadable(this);

        return wrapCursor(delegate.select(time, options, types));
    }

    @Override
    public TickCursor select(long time, SelectionOptions options) {
        context.checkReadable(this);

        return wrapCursor(delegate.select(time, options));
    }

    @Override
    public TickCursor               createCursor(SelectionOptions options) {
        context.checkReadable(this);

        return wrapCursor(delegate.createCursor(options));
    }

    @Override
    public IdentityKey[]     listEntities() {
        return delegate.listEntities();
    }

    @Override
    public long[]                   getTimeRange(IdentityKey... entities) {
        return delegate.getTimeRange(entities);
    }

    @Override
    public TimeInterval[]           listTimeRange(IdentityKey... entities) {
        return delegate.listTimeRange(entities);
    }

    @Override
    public int                      getDistributionFactor() {
        return delegate.getDistributionFactor();
    }

    @Override
    public void                     setTargetNumFiles(int value) {
        context.checkWritable(this);

        delegate.setTargetNumFiles(value);
    }

    @Override
    public void                     setName(String name) {
        context.checkWritable(this);

        delegate.setName(name);
    }

    @Override
    public void                     setDescription(String description) {
        context.checkWritable(this);

        delegate.setDescription(description);
    }

    @Override
    public void                     setOwner(String newOwner) {
        context.checkCanImpersonate(delegate.getOwner());
        context.checkCanImpersonate(newOwner);

        delegate.setOwner(newOwner);
    }

    @Override
    public String                   getOwner() {
        return delegate.getOwner();
    }

    @Override
    public StreamScope              getScope() {
        return delegate.getScope();
    }

    @Override
    public void                     setPolymorphic(RecordClassDescriptor... cds) {
        context.checkWritable(this);

        delegate.setPolymorphic(cds);
    }

    @Override
    public void                     setFixedType(RecordClassDescriptor cd) {
        context.checkWritable(this);

        delegate.setFixedType(cd);
    }

    @Override
    public void                     delete() {
        context.checkWritable(this);

        delegate.delete();
    }

    @Override
    public void                     rename(String key) {
        context.checkWritable(this);

        delegate.rename(key);
    }

    @Override
    public void                     purge(long time) {
        context.checkWritable(this);

        delegate.purge(time);
    }

    @Override
    public void                     execute(TransformationTask task) {
        context.checkWritable(this);

        if (task instanceof SchemaChangeTask) {
            context.checkPermission(TimeBasePermissions.CHANGE_SCHEMA_PERMISSION, this);
        }
        else if (task instanceof StreamCopyTask) {
            StreamCopyTask copy = (StreamCopyTask)task;
            for (DXTickStream source : copy.getSources(getDB()))
                context.checkReadable(source);
        }
        delegate.execute(task);
    }

    @Override
    public BackgroundProcessInfo    getBackgroundProcess() {
        return delegate.getBackgroundProcess();
    }

    @Override
    public long getSizeOnDisk() {
        return ((ServerStreamImpl) delegate).getSizeOnDisk();
    }

    @Override
    public void                     abortBackgroundProcess() {
        delegate.abortBackgroundProcess();
    }

    @Override
    public Periodicity              getPeriodicity() {
        return delegate.getPeriodicity();
    }

    @Override
    public void                     setPeriodicity(Periodicity periodicity) {
        context.checkWritable(this);

        delegate.setPeriodicity(periodicity);
    }

    @Override
    public void                     setHighAvailability(boolean value) {
        context.checkWritable(this);

        delegate.setHighAvailability(value);
    }

    @Override
    public boolean                  getHighAvailability() {
        return delegate.getHighAvailability();
    }

    @Override
    public StreamOptions            getStreamOptions() {
        return delegate.getStreamOptions();
    }

    @Override
    public DBLock                   lock() throws StreamLockedException, UnsupportedOperationException {
        return delegate.lock();
    }

    @Override
    public DBLock                   lock(LockType type) throws StreamLockedException, UnsupportedOperationException {
        return delegate.lock(type);
    }

    @Override
    public DBLock                   tryLock(long timeout) throws StreamLockedException, UnsupportedOperationException {
        return delegate.tryLock(timeout);
    }

    @Override
    public DBLock                   tryLock(LockType type, long timeout) throws StreamLockedException, UnsupportedOperationException {
        return delegate.tryLock(type, timeout);
    }

    @Override
    public DBLock                   verify(DBLock lock, LockType type) throws StreamLockedException, UnsupportedOperationException {
        return delegate.verify(lock, type);
    }

    @Override
    public boolean                  enableVersioning() {
        context.checkWritable(this);

        return delegate.enableVersioning();
    }

    @Override
    public long                     getDataVersion() {
        return delegate.getDataVersion();
    }

    @Override
    public long                     getReplicaVersion() {
        return delegate.getReplicaVersion();
    }

    @Override
    public void                     setReplicaVersion(long version) {
        context.checkWritable(this);

        delegate.setReplicaVersion(version);
    }

    @Override
    public IdentityKey[]     getComposition(IdentityKey... ids) {
        return delegate.getComposition(ids);
    }

    @Override
    public void renameInstruments(IdentityKey[] from, IdentityKey[] to) {
        delegate.renameInstruments(from, to);
    }

    @Override
    public void addEventListener(LockEventListener listener) {
        if (delegate instanceof LockHandler)
            ((LockHandler) delegate).addEventListener(listener);
    }

    @Override
    public void removeEventListener(LockEventListener listener) {
        if (delegate instanceof LockHandler)
            ((LockHandler) delegate).removeEventListener(listener);
    }

    @Override
    final void                      cursorCreated(TickCursor cursor) {
        ((ServerStreamImpl) delegate).cursorCreated(cursor);
    }

    @Override
    void cursorClosed(TickCursor cursor) {
        ((ServerStreamImpl) delegate).cursorClosed(cursor);
    }

    public DXTickStream             getNestedInstance() {
        return delegate;
    }

    private TickCursor              wrapCursor(TickCursor cursor) {
        return new TickCursorWrapper(cursor, context);
    }

    private TickLoader              wrapLoader(TickLoader loader) {
        return new TickLoaderWrapper(loader, context);
    }


    @Override
    public RecordClassDescriptor[] getTypes() {
        return delegate.getTypes();
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(ChannelPreferences options) {
        SelectionOptions opts;

        if (options instanceof SelectionOptions) {
            opts = (SelectionOptions) options;
        } else {
            opts = new SelectionOptions();
            opts.raw = options.raw;
            opts.typeLoader = options.typeLoader;
            opts.channelPerformance = options.channelPerformance;
        }

        return createCursor(opts);
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(ChannelPreferences options) {
        LoadingOptions opts;

        if (options instanceof LoadingOptions) {
            opts = (LoadingOptions) options;
        } else {
            opts = new LoadingOptions();
            opts.raw = options.raw;
            opts.typeLoader = options.typeLoader;
            opts.channelPerformance = options.channelPerformance;
        }

        return createLoader(opts);
    }
}
