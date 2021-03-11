package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.pub.*;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.tickdb.pub.TickStream;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.util.lang.Util;

import java.util.HashMap;

/**
 *  Efficiently buffers all information contained in a {@link StreamMessageSource}
 *  instance.
 */
public final class MessageInfoBuffer implements StreamMessageSource<InstrumentMessage> {
    private static class ClassToMessageMap 
        extends HashMap<Class <? extends InstrumentMessage>, InstrumentMessage>
    {
    };

    private RawMessage                          stockRawMessage = null;
    private ClassToMessageMap                   stockMessageMap = null;
    public InstrumentMessage                    currentMessage = null;
    public int                                  currentTypeIdx = -1;
    public RecordClassDescriptor                currentType = null;
    public int                                  currentStreamIdx = -1;
    public TickStream                           currentStream = null;
    public int                                  currentEntityIdx = -1;
    
    public MessageInfoBuffer () {
    }

    public void                 setMessageNoCopy (InstrumentMessage msg) {
        currentMessage = msg;
    }
    
    public void                 setUpNoCopy (StreamMessageSource<InstrumentMessage> delegate, InstrumentMessage msg) {
        currentMessage = msg;
        currentType = delegate.getCurrentType ();
        currentTypeIdx = delegate.getCurrentTypeIndex ();
        currentStream = delegate.getCurrentStream ();
        currentStreamIdx = delegate.getCurrentStreamIndex ();
        currentEntityIdx = delegate.getCurrentEntityIndex ();
    }
    
    public void                 copyFrom (StreamMessageSource<InstrumentMessage> delegate) {
        InstrumentMessage                   msg = delegate.getMessage ();
        InstrumentMessage                   copy;
        Class <? extends InstrumentMessage> msgClass = msg.getClass ();

        if (msgClass == RawMessage.class) {
            if (stockRawMessage == null) {
                stockRawMessage = new RawMessage ();
                stockRawMessage.data = new byte [256];
            }
            
            copy = stockRawMessage;
        }
        else {
            if (stockMessageMap == null) {
                stockMessageMap = new ClassToMessageMap ();
                copy = null;
            }
            else
                copy = stockMessageMap.get (msgClass);

            if (copy == null) {
                copy = Util.newInstanceNoX (msgClass);
                stockMessageMap.put (msgClass, copy);
            }
        }

        copy.copyFrom(msg);
        //
        //  Buffer everything else
        //
        setUpNoCopy (delegate, copy);        
    }

    @Override
    public InstrumentMessage            getMessage () {
        return (currentMessage);
    }

    @Override
    public int                          getCurrentTypeIndex () {
        return (currentTypeIdx);
    }

    @Override
    public RecordClassDescriptor        getCurrentType () {
        return (currentType);
    }

    @Override
    public String                       getCurrentStreamKey () {
        return (currentStream == null ? null : currentStream.getKey ());
    }

    @Override
    public int                          getCurrentStreamIndex () {
        return (currentStreamIdx);
    }

    @Override
    public TickStream                   getCurrentStream () {
        return (currentStream);
    }

    @Override
    public int                          getCurrentEntityIndex () {
        return (currentEntityIdx);
    }            
}
