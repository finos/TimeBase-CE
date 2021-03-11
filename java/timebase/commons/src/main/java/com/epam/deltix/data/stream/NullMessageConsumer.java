package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *  Ignores messages
 */
public class NullMessageConsumer <T> implements MessageChannel<T> {
    public void         send (T message) {        
    }

    public void         close () {
    }    
}
