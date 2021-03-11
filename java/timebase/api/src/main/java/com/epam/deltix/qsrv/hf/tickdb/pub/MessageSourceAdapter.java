package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.streaming.MessageSource;

public class MessageSourceAdapter<T> implements MessageSource <T>  {
    private final MessageSource<T> delegate;

    public MessageSourceAdapter(MessageSource<T> delegate) {
        this.delegate = delegate;
    }

    protected MessageSource<T> getDelegate() {
        return delegate;
    }
    
    @Override
    public T getMessage() {
        return delegate.getMessage();
    }

    @Override
    public boolean next() {
        return delegate.next();
    }

    @Override
    public boolean isAtEnd() {
        return delegate.isAtEnd();
    }

    @Override
    public void close() {
        delegate.close();
    }


}
