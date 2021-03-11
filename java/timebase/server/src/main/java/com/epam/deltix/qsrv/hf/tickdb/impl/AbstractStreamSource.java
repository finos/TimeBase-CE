package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.hf.tickdb.impl.multiplexer.PrioritizedMessageSourceMultiplexer;
import com.epam.deltix.streaming.MessageSource;
import com.epam.deltix.timebase.messages.InstrumentMessage;
import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickStream;
import com.epam.deltix.qsrv.hf.tickdb.pub.SelectionOptions;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

abstract class AbstractStreamSource implements StreamSource {

    static int UNIQUE_READER = 0;
    static int VERSIONS_READER = 1;

    private final IntegerToObjectHashMap<MessageSource<InstrumentMessage>> specialReaders =
            new IntegerToObjectHashMap<MessageSource<InstrumentMessage>>();

    protected final PrioritizedMessageSourceMultiplexer<InstrumentMessage> mx;
    protected final SelectionOptions options;

    AbstractStreamSource(PrioritizedMessageSourceMultiplexer<InstrumentMessage> mx, SelectionOptions options) {
        assert mx != null;

        this.mx = mx;
        this.options = options;
    }

    protected void                addSpecialReaders (DXTickStream stream, long time) {
        addSpecialReader(stream, time, UNIQUE_READER);
        addSpecialReader(stream, time, VERSIONS_READER);
    }

    protected void                addSpecialReader (DXTickStream stream, long time, int type) {

        if ((stream instanceof TickStreamImpl)) {

            TickStreamImpl ss = (TickStreamImpl) stream;

            if (ss.isUnique () && options.rebroadcast && type == UNIQUE_READER) {
                if (!specialReaders.containsKey(UNIQUE_READER)) {
                    UniqueMessageReader umr = new UniqueMessageReader(ss.accumulator, options, ss);

                    specialReaders.put(0, umr);
                    mx.add(umr);
                }
            }

            if (options.versionTracking && ss.versioning && type == VERSIONS_READER) {
                if (!specialReaders.containsKey(VERSIONS_READER)) {
                    StreamVersionsReader reader = ss.createVersionsReader(time, options);

                    if (reader != null) {
                        specialReaders.put(1, reader);
                        mx.add(reader);
                    }
                }
            }
        }
    }

    protected void          removeSpecialReaders() {
        for (MessageSource<InstrumentMessage> spr : specialReaders)
            mx.closeAndRemove (spr);

        specialReaders.clear();
    }
}
