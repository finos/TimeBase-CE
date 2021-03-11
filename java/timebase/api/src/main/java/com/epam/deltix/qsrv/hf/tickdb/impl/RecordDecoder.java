package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.data.stream.MessageDecoder;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;

/**
 *
 */
public interface RecordDecoder<T extends InstrumentMessage> extends MessageDecoder<T> {

    public RecordClassDescriptor    getCurrentType();

    public int                      getCurrentTypeIndex();

    // TODO: add for performance
    //public int                      getCurrentEntityIndex();
}
