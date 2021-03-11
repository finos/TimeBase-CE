package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.data.stream.RealTimeMessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.query.TypedMessageSource;
import com.epam.deltix.util.concurrent.IntermittentlyAvailableResource;

interface TickStreamReader extends
        MessageSource<InstrumentMessage>,
        TickStreamRelated,
        TypedMessageSource,
        IntermittentlyAvailableResource,
        RealTimeMessageSource<InstrumentMessage>
{
    void reset(long time);
}
