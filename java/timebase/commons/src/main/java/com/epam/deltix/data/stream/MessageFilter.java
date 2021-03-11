package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *  Base class for message filters.
 */
public abstract class MessageFilter <T> implements MessageChannel <T> {
    protected final MessageChannel<T> delegate;
    
    public MessageFilter (
        MessageChannel <T>         delegate
    )
    {
        this.delegate = delegate;
    }

    public void                 send (T msg) {
        if (delegate != null)
            delegate.send (msg);
    }


    public void                 close () {
        if (delegate != null)
            delegate.close ();
    }
}
