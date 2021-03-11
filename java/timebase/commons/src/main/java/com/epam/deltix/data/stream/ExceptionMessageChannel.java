package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *  Throws an exception whenever {@link #send} is called. This class is
 *  useful when connected to the reject channel of a {@link MessageFork},
 *  for instance.
 */
public class ExceptionMessageChannel <T> implements MessageChannel<T> {
    /**
     *  Throws <code>new IllegalArgumentException (msg.toString ())</code>
     *
     *  @param msg       The message.
     *
     *  @throws java.lang.IllegalArgumentException   Always.
     */
    public void         send (T msg) throws IllegalArgumentException {
        throw new IllegalArgumentException (msg.toString ());
    }

    public void         close () {
    }
}
