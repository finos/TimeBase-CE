package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickCursor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SymbolAndTypeSubscriptionControllerAdapter;
import com.epam.deltix.util.lang.Wrapper;

import java.util.ArrayList;

/**
 *
 */
public class TickCursorWrapper implements Wrapper<TickCursor>, TickCursor, SymbolAndTypeSubscriptionControllerAdapter {

    private final TickCursor                delegate;
    private final AuthorizationContext      context;

    public TickCursorWrapper(TickCursor delegate, AuthorizationContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public InstrumentMessage    getMessage() {
        return delegate.getMessage();
    }

    @Override
    public int              getCurrentStreamIndex() {
        return delegate.getCurrentStreamIndex();
    }

    @Override
    public String           getCurrentStreamKey() {
        return delegate.getCurrentStreamKey();
    }

    @Override
    public TickStream       getCurrentStream() {
        return delegate.getCurrentStream();
    }

    @Override
    public int              getCurrentEntityIndex() {
        return delegate.getCurrentEntityIndex();
    }

    @Override
    public boolean          isClosed() {
        return delegate.isClosed();
    }

    @Override
    public boolean          isRealTime() {
        return delegate.isRealTime();
    }

    @Override
    public boolean          realTimeAvailable() {
        return delegate.realTimeAvailable();
    }

    @Override
    public void             add(IdentityKey[] entities, String[] types) {
        delegate.add(entities, types);
    }

    @Override
    public void             remove(IdentityKey[] entities, String[] types) {
        delegate.remove(entities, types);
    }

    @Override
    public void             subscribeToAllEntities() {
        delegate.subscribeToAllEntities();
    }

    @Override
    public void             clearAllEntities() {
        delegate.clearAllEntities();
    }

    @Override
    public void             addEntity(IdentityKey id) {
        delegate.addEntity(id);
    }

    @Override
    public void             addEntities(IdentityKey[] ids, int offset, int length) {
        delegate.addEntities(ids, offset, length);
    }

    @Override
    public void             removeEntity(IdentityKey id) {
        delegate.removeEntity(id);
    }

    @Override
    public void             removeEntities(IdentityKey[] ids, int offset, int length) {
        delegate.removeEntities(ids, offset, length);
    }

    @Override
    public void             addStream(TickStream... tickStreams) {

        ArrayList<DXTickStream> streams = new ArrayList<>();

        for (TickStream stream : tickStreams) {
            context.checkReadable((DXTickStream) stream);
            if (stream instanceof Wrapper<?>)
                streams.add((DXTickStream) ((Wrapper)stream).getNestedInstance());
            else
                streams.add((DXTickStream)stream);
        }

        delegate.addStream(streams.toArray(new DXTickStream[streams.size()]));
    }

    @Override
    public void             removeAllStreams() {
        delegate.removeAllStreams();
    }

    @Override
    public void             removeStream(TickStream... tickStreams) {

        ArrayList<DXTickStream> streams = new ArrayList<>();

        for (TickStream stream : tickStreams) {
            context.checkReadable((DXTickStream) stream);
            if (stream instanceof Wrapper<?>)
                streams.add((DXTickStream) ((Wrapper)stream).getNestedInstance());
            else
                streams.add((DXTickStream)stream);
        }

        delegate.removeStream(streams.toArray(new DXTickStream[streams.size()]));
    }

    @Override
    public void             setTimeForNewSubscriptions(long time) {
        delegate.setTimeForNewSubscriptions(time);
    }

    @Override
    public void             reset(long time) {
        delegate.reset(time);
    }

    @Override
    public void             subscribeToAllTypes() {
        delegate.subscribeToAllTypes();
    }

    @Override
    public void             setTypes(String... names) {
        delegate.setTypes(names);
    }

    @Override
    public void             addTypes(String... names) {
        delegate.addTypes(names);
    }

    @Override
    public void             removeTypes(String... names) {
        delegate.removeTypes(names);
    }

    @Override
    public int              getCurrentTypeIndex() {
        return delegate.getCurrentTypeIndex();
    }

    @Override
    public RecordClassDescriptor    getCurrentType() {
        return delegate.getCurrentType();
    }

    @Override
    public boolean          next() {
        return delegate.next();
    }

    @Override
    public boolean          isAtEnd() {
        return delegate.isAtEnd();
    }

    @Override
    public void             setAvailabilityListener(Runnable maybeAvailable) {
        delegate.setAvailabilityListener(maybeAvailable);
    }

    @Override
    public void             close() {
        delegate.close();
    }

    @Override
    public TickCursor       getNestedInstance() {
        return delegate;
    }

    @Override
    public String           toString() {
        return "TickCursorWrapper (" + super.toString() + ")";
    }
}
