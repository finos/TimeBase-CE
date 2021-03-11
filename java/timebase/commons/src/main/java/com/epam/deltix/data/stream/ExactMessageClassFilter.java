package com.epam.deltix.data.stream;

import com.epam.deltix.streaming.MessageChannel;

/**
 *
 */
public class ExactMessageClassFilter <T> extends MessageFork <T> {
    private final Class <? extends T>   mClass;
    private final boolean               mPass;
    
    public ExactMessageClassFilter (
        Class <? extends T>         cls,
        MessageChannel<T> acceptedMessageConsumer,
        MessageChannel <T>         rejectedMessageConsumer,
        boolean                     pass
    )
    {
        super (acceptedMessageConsumer, rejectedMessageConsumer);
        
        mClass = cls;
        mPass = pass;
    }
    
    protected boolean               accept (T message) {
        return ((message.getClass () == mClass) == mPass);
    }    
}
