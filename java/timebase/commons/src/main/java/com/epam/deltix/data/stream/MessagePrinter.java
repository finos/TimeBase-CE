package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

import java.io.*;

/**
 *  Prints messages, one per line,
 *  optionally prefixed by the specified string.
 */
public final class MessagePrinter <T> extends MessageFork <T> {
    private final StringBuilder     mStringBuilder = new StringBuilder ();
    private final PrintStream       mOut;
    public volatile String          prefix = "";
    
    public MessagePrinter () {
        this (null);
    }        
    
    public MessagePrinter (MessageChannel<T> next) {
        this (System.out, next);
    }
    
    public MessagePrinter (PrintStream out, MessageChannel <T> next) {
        super (next, null);
        mOut = out;
    }
    
    public boolean                  accept (T message) {
        mStringBuilder.setLength (0);
        mStringBuilder.append (prefix);
        
        if (message instanceof PrintableMessage)
            ((PrintableMessage) message).print (mStringBuilder);
        else
            mStringBuilder.append (message);
        
        mOut.println (mStringBuilder);
        return (true);
    }

    public void                     close () {
        mOut.flush ();
    }    
}
