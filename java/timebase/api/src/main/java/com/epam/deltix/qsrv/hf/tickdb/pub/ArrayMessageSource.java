package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.streaming.MessageSource;

import java.util.Arrays;

/**
 * MessageSource implementation backed by an array of messages
 */
public class ArrayMessageSource<T> implements MessageSource<T> {
    private final T[] messages;
    private final int len;
    private T currentMessage;
    private int idx = -1;

    public ArrayMessageSource(T[] messages) {
        this.messages = messages;
        len = messages != null ? messages.length : 0;
    }

    @Override
    public T getMessage() {
        return currentMessage;
    }

    @Override
    public boolean next() {
        if (++idx < len) {
            currentMessage = messages[idx];
            return true;
        } else
            return false;
    }

    @Override
    public boolean isAtEnd() {
        return idx >= len;
    }

    @Override
    public void close() {
        if (messages != null)
            Arrays.fill(messages, null);
    }
}
