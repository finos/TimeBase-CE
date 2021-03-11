package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.ChannelPreferences;
import com.epam.deltix.streaming.MessageChannel;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.TimeInterval;
import com.epam.deltix.qsrv.hf.pub.md.ClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.md.Introspector;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;
import com.epam.deltix.qsrv.hf.tickdb.pub.LoadingOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamOptions;
import com.epam.deltix.qsrv.hf.tickdb.pub.StreamScope;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickLoader;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.DBLock;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.LockType;
import com.epam.deltix.qsrv.hf.tickdb.pub.lock.StreamLockedException;
import com.epam.deltix.timebase.messages.TimeStamp;
import com.epam.deltix.util.lang.Disposable;
import com.epam.deltix.util.time.Periodicity;

import javax.annotation.Nullable;

/**
 * @author Alexei Osipov
 */
class StubTimeStream extends ServerStreamImpl implements Disposable {
    static final IdentityKey STUB_IDENTITY = new ConstantIdentityKey("STUB_TIME");

    static final RecordClassDescriptor STUB_RCD = getDescriptorForInstrumentMessage();

    private static RecordClassDescriptor getDescriptorForInstrumentMessage() {
        Introspector ix = Introspector.createEmptyMessageIntrospector();
        try {
            return ix.introspectRecordClass("Get RD for StubTimeStream", InstrumentMessage.class);
        } catch (Introspector.IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    private final String key;
    private final String name;
    private final TickDBImpl db;

    StubTimeStream(@Nullable String key, @Nullable String name, @Nullable TickDBImpl db) {
        this.key = key;
        this.name = name;
        this.db = db;
    }

    @Override
    public TickLoader createLoader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TickLoader createLoader(LoadingOptions options) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void truncate(long time, IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(TimeStamp from, TimeStamp to, IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear(IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DXTickDB getDB() {
        return db;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTypeVersion() {
        return 0;
    }

    @Override
    public int getFormatVersion() {
        return 5;
    }

    @Override
    public boolean isPolymorphic() {
        return false;
    }

    @Override
    public boolean isFixedType() {
        return true;
    }

    @Override
    public RecordClassDescriptor getFixedType() {
        return STUB_RCD;
    }

    @Override
    public RecordClassDescriptor[] getPolymorphicDescriptors() {
        return null;
    }

    @Override
    public ClassDescriptor[] getAllDescriptors() {
        return new ClassDescriptor[]{STUB_RCD};
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, IdentityKey[] entities) {
        // TODO: Add filters?
        StubTimeStreamCursor stubTimeStreamCursor = new StubTimeStreamCursor(this);
        stubTimeStreamCursor.reset(time);
        return stubTimeStreamCursor;
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types, CharSequence[] symbols) {
        StubTimeStreamCursor stubTimeStreamCursor = new StubTimeStreamCursor(this);
        stubTimeStreamCursor.reset(time);
        return stubTimeStreamCursor;
    }

    @Override
    public TickCursor select(long time, SelectionOptions options, String[] types) {
        StubTimeStreamCursor stubTimeStreamCursor = new StubTimeStreamCursor(this);
        stubTimeStreamCursor.reset(time);
        return stubTimeStreamCursor;
    }

    @Override
    public TickCursor select(long time, SelectionOptions options) {
        StubTimeStreamCursor stubTimeStreamCursor = new StubTimeStreamCursor(this);
        stubTimeStreamCursor.reset(time);
        return stubTimeStreamCursor;
    }

    @Override
    public TickCursor createCursor(SelectionOptions options) {
        return new StubTimeStreamCursor(this);
    }

    @Override
    public IdentityKey[] listEntities() {
        // TODO
        return new IdentityKey[0];
    }

    @Override
    public long[] getTimeRange(IdentityKey... entities) {
        // TODO
        return new long [] { Long.MIN_VALUE, Long.MAX_VALUE };
    }

    @Override
    public TimeInterval[] listTimeRange(IdentityKey... entities) {
        // TODO
        return new TimeInterval[0];
    }

    @Override
    public int getDistributionFactor() {
        return 0;
    }

    @Override
    public void setName(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setDescription(String description) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setOwner(String owner) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getOwner() {
        return null;
    }

    @Override
    public StreamScope getScope() {
        return StreamScope.RUNTIME;
    }

    @Override
    public void setPolymorphic(RecordClassDescriptor... cds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setFixedType(RecordClassDescriptor cd) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete() {
        if (db != null) {
            db.streamDeleted(getKey());
        }
    }

    @Override
    public void rename(String key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Periodicity getPeriodicity() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPeriodicity(Periodicity periodicity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setHighAvailability(boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean getHighAvailability() {
        return false;
    }

    @Override
    public StreamOptions getStreamOptions() {
        StreamOptions streamOptions = new StreamOptions();
        streamOptions.setFlag(TDBProtocol.AF_STUB_STREAM, true);
        streamOptions.scope = StreamScope.RUNTIME;
        streamOptions.name = name;
        return streamOptions;
    }

    @Override
    public DBLock lock() throws StreamLockedException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DBLock lock(LockType type) throws StreamLockedException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DBLock tryLock(long timeout) throws StreamLockedException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DBLock tryLock(LockType type, long timeout) throws StreamLockedException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public DBLock verify(DBLock lock, LockType type) throws StreamLockedException, UnsupportedOperationException {
        return null;
    }

    @Override
    public boolean enableVersioning() {
        return false;
    }

    @Override
    public long getDataVersion() {
        return -1;
    }

    @Override
    public long getReplicaVersion() {
        return -1;
    }

    @Override
    public void setReplicaVersion(long version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityKey[] getComposition(IdentityKey... ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void renameInstruments(IdentityKey[] from, IdentityKey[] to) {

    }

    @Override
    void cursorCreated(TickCursor cur) {

    }

    @Override
    void cursorClosed(TickCursor cursor) {

    }

    @Override
    public long getSizeOnDisk() {
        return 0;
    }

    @Override
    public void close() {

    }

    @Override
    public RecordClassDescriptor[] getTypes() {
        return new RecordClassDescriptor[0];
    }

    @Override
    public MessageSource<InstrumentMessage> createConsumer(ChannelPreferences options) {
        return null;
    }

    @Override
    public MessageChannel<InstrumentMessage> createPublisher(ChannelPreferences options) {
        return null;
    }
}
