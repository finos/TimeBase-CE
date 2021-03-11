package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageSource;

/**
 *  Decorates a message source with a customizable filter.
 */
public abstract class FilterableMessageSource <T> implements MessageSource <T> {
    protected final MessageSource<T>        source;
    private boolean                         atEnd = false;
    
    protected FilterableMessageSource (MessageSource <T> source) {
        this.source = source;
    }

    public T                            getMessage () {
        return (source.getMessage ());
    }

    public boolean                      isAtEnd () {
        return (atEnd);
    }

    protected abstract boolean          acceptCurrent ();

    public boolean                      next () {
        for (;;) {
            if (!source.next ()) {
                atEnd = true;
                return (false);
            }

            if (acceptCurrent ())
                return (true);
        }
    }

    public void                         close () {
        source.close ();
    }
}
