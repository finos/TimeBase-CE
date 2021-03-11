package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *
 */
public abstract class MessageFork <T> implements MessageChannel <T> {
    private final MessageChannel<T> mAcceptedMessageConsumer;
    private final MessageChannel <T>       mRejectedMessageConsumer;
    
    public MessageFork (
        MessageChannel <T>         acceptedMessageConsumer, 
        MessageChannel <T>         rejectedMessageConsumer
    )
    {
        mAcceptedMessageConsumer = acceptedMessageConsumer;
        mRejectedMessageConsumer = rejectedMessageConsumer;
    }

    protected abstract boolean  accept (T message);
    
    public final void           send (T message) {
        MessageChannel <T>         consumer = 
            accept (message) ?
                mAcceptedMessageConsumer :
                mRejectedMessageConsumer;
        
        if (consumer != null)
            consumer.send (message);
    }
    
    public void                 close () {
        if (mAcceptedMessageConsumer != null)
            mAcceptedMessageConsumer.close ();
        
        if (mRejectedMessageConsumer != null)
            mRejectedMessageConsumer.close ();
    }
}
