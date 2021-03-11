package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.timebase.messages.IdentityKey;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionManager;

/**
 * @author Andy
 *         Date: Feb 16, 2010 2:27:17 PM
 */
public class TickCursorAdapter extends MessageSourceAdapter<InstrumentMessage> implements TickCursor, SubscriptionManager, RealTimeMessageSource<InstrumentMessage> {

    public TickCursorAdapter(TickCursor delegate) {
        super(delegate);
    }

    private TickCursor getCursorDelegate() {
        return (TickCursor) getDelegate();
    }

    @Override
    public void add(IdentityKey[] ids, String[] types) {
        getCursorDelegate().add(ids, types);
    }

    @Override
    public void remove(IdentityKey[] ids, String[] types) {
        getCursorDelegate().add(ids, types);
    }

    public void addStream(TickStream... tickStreams) {
        getCursorDelegate().addStream(tickStreams);
    }

    public void addTypes(String... names) {
        getCursorDelegate().addTypes(names);
    }

    @Override
    public void setTypes(String... names) {
        getCursorDelegate().setTypes(names);
    }

    public void clearAllEntities() {
        getCursorDelegate().clearAllEntities();
    }

    public int getCurrentEntityIndex() {
        return getCursorDelegate().getCurrentEntityIndex();
    }

    public int getCurrentStreamIndex() {
        return getCursorDelegate().getCurrentStreamIndex();
    }

    public String getCurrentStreamKey() {
        return getCursorDelegate().getCurrentStreamKey();
    }

    public TickStream getCurrentStream() {
        return getCursorDelegate().getCurrentStream();
    }

    public RecordClassDescriptor getCurrentType() {
        return getCursorDelegate().getCurrentType();
    }

    public int getCurrentTypeIndex() {
        return getCursorDelegate().getCurrentTypeIndex();
    }

    public void removeAllStreams() {
        getCursorDelegate().removeAllStreams();
    }

    public void removeEntities(IdentityKey[] ids, int offset, int length) {
        getCursorDelegate().removeEntities(ids, offset, length);
    }

    public void removeEntity(IdentityKey id) {
        getCursorDelegate().removeEntity(id);
    }

    public void removeStream(TickStream... tickStreams) {
        getCursorDelegate().removeStream(tickStreams);
    }

    public void removeTypes(String... names) {
        getCursorDelegate().removeTypes(names);
    }

    public void reset(long time) {
        getCursorDelegate().reset(time);
    }

    public void setAvailabilityListener(Runnable lnr) {
        getCursorDelegate().setAvailabilityListener(lnr);
    }

    public void setTimeForNewSubscriptions(long time) {
        getCursorDelegate().setTimeForNewSubscriptions(time);
    }

    public void subscribeToAllEntities() {
        getCursorDelegate().subscribeToAllEntities();
    }

    public void subscribeToAllTypes() {
        getCursorDelegate().subscribeToAllTypes();
    }

    public void addEntities(IdentityKey[] ids, int offset, int length) {
        getCursorDelegate().addEntities(ids, offset, length);
    }

    public void addEntity(IdentityKey id) {
        getCursorDelegate().addEntity(id);
    }

    @Override
    public void add(CharSequence[] symbols, String[] types) {
        getCursorDelegate().add(symbols, types);
    }

    @Override
    public void remove(CharSequence[] symbols, String[] types) {
        getCursorDelegate().remove(symbols, types);
    }

    @Override
    public void subscribeToAllSymbols() {
        getCursorDelegate().subscribeToAllSymbols();
    }

    @Override
    public void clearAllSymbols() {
        getCursorDelegate().clearAllSymbols();
    }

    @Override
    public void addSymbol(CharSequence symbol) {
        getCursorDelegate().addSymbol(symbol);
    }

    @Override
    public void addSymbols(CharSequence[] symbols, int offset, int length) {
        getCursorDelegate().addSymbols(symbols, offset, length);
    }

    @Override
    public void removeSymbol(CharSequence symbol) {
        getCursorDelegate().removeSymbol(symbol);
    }

    @Override
    public void removeSymbols(CharSequence[] symbols, int offset, int length) {
        getCursorDelegate().removeSymbols(symbols, offset, length);
    }

    public boolean isClosed() {
        return getCursorDelegate().isClosed();
    }

    /// Optional SubscriptionManager interface

    private static final String SUBSCRIPTION_MANAGER_UNSUPPORTED = "Subscription manager interface is not supported by ";

    @Override
    public IdentityKey[] getSubscribedEntities() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof SubscriptionManager)
            return ((SubscriptionManager)delegate).getSubscribedEntities();
        throw new UnsupportedOperationException(SUBSCRIPTION_MANAGER_UNSUPPORTED + delegate.getClass().getSimpleName());
    }

    @Override
    public boolean isAllEntitiesSubscribed() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof SubscriptionManager)
            return ((SubscriptionManager)delegate).isAllEntitiesSubscribed();
        throw new UnsupportedOperationException(SUBSCRIPTION_MANAGER_UNSUPPORTED + delegate.getClass().getSimpleName());
    }

    @Override
    public boolean hasSubscribedTypes() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof SubscriptionManager)
            return ((SubscriptionManager)delegate).hasSubscribedTypes();
        
        throw new UnsupportedOperationException(SUBSCRIPTION_MANAGER_UNSUPPORTED + delegate.getClass().getSimpleName());
    }

    @Override
    public String[] getSubscribedTypes() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof SubscriptionManager)
            return ((SubscriptionManager)delegate).getSubscribedTypes();
        throw new UnsupportedOperationException(SUBSCRIPTION_MANAGER_UNSUPPORTED + delegate.getClass().getSimpleName());
    }

    @Override
    public boolean isAllTypesSubscribed() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof SubscriptionManager)
            return ((SubscriptionManager)delegate).isAllTypesSubscribed();
        throw new UnsupportedOperationException(SUBSCRIPTION_MANAGER_UNSUPPORTED + delegate.getClass().getSimpleName());
    }

    // Optional LiveMessageSource interface

    private static final String LIVE_SOURCE_UNSUPPORTED = "Realtime Message Source interface is not supported by ";


    @Override
    public boolean isRealTime() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof RealTimeMessageSource)
            return ((RealTimeMessageSource)delegate).isRealTime();
        throw new UnsupportedOperationException(LIVE_SOURCE_UNSUPPORTED + delegate.getClass().getSimpleName());
    }

    @Override
    public boolean realTimeAvailable() {
        TickCursor delegate = getCursorDelegate();
        if (delegate instanceof RealTimeMessageSource)
            return ((RealTimeMessageSource)delegate).realTimeAvailable();
        throw new UnsupportedOperationException(LIVE_SOURCE_UNSUPPORTED + delegate.getClass().getSimpleName());
    }
}

