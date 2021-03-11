package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *
 */
public class Tee <T> implements MessageChannel<T> {
    private final MessageChannel <T>       mOut1;
    private final MessageChannel <T>       mOut2;
    
    public Tee (MessageChannel <T> out1, MessageChannel <T> out2) {
        mOut1 = out1;
        mOut2 = out2;
    }

    public void         send (T message) {
        if (mOut1 != null)
            mOut1.send (message);
        
        if (mOut2 != null)
            mOut2.send (message);
    }

    public void         close () {
        if (mOut1 != null)
            mOut1.close ();
        
        if (mOut2 != null)
            mOut2.close ();
    }
    
}
