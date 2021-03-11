package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.pub.*;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.SubscriptionChangeListener;
import com.epam.deltix.timebase.messages.MessageInfo;
import com.epam.deltix.util.lang.Wrapper;

import java.io.IOException;

/**
 *
 */
public class TickLoaderWrapper<T extends MessageInfo> implements Wrapper<TickLoader<T>>, TickLoader<T> {

    private final TickLoader<T>             delegate;
    private final AuthorizationContext      context;

    public TickLoaderWrapper(TickLoader<T> delegate, AuthorizationContext context) {
        this.delegate = delegate;
        this.context = context;
    }

    @Override
    public                  WritableTickStream getTargetStream() {
        return delegate.getTargetStream();
    }

    @Override
    public void             addEventListener(LoadingErrorListener listener) {
        delegate.addEventListener(listener);
    }

    @Override
    public void             removeEventListener(LoadingErrorListener listener) {
        delegate.removeEventListener(listener);
    }

    @Override
    public void             addSubscriptionListener(SubscriptionChangeListener listener) {
        delegate.addSubscriptionListener(listener);
    }

    @Override
    public void             removeSubscriptionListener(SubscriptionChangeListener listener) {
        delegate.removeSubscriptionListener(listener);
    }

    @Override
    public void             removeUnique(T msg) {
        delegate.removeUnique(msg);
    }

    @Override
    public void             send(T msg) {
//        WritableTickStream stream = getTargetStream();
//        if (stream instanceof DXTickStream)
//            authorizationContext.checkWritable((DXTickStream) stream);

        delegate.send(msg);
    }

    @Override
    public void             close() {
        delegate.close();
    }

    @Override
    public TickLoader       getNestedInstance() {
        return delegate;
    }

    @Override
    public void flush() throws IOException {
        delegate.flush();
    }
}
