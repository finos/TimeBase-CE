package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.timebase.messages.IdentityKey;

public class TickCursorFactory {
    
    public static TickCursor    create(DXTickStream stream, long time) {
        return stream.select(time, null);
    }

    public static TickCursor    create(DXTickStream stream, long time, IdentityKey ... entities) {
        return stream.select(time, null, null, entities);
    }

    public static TickCursor    create(DXTickStream stream, long time, String ... symbols) {
        return create(stream, time, null, symbols);
    }

    public static TickCursor    create(DXTickStream stream, long time, SelectionOptions options) {
        return stream.select(time, options);
    }

    public static TickCursor    create(
             DXTickStream           stream,
             long                   time,
             SelectionOptions       options,
             String ...             symbols)
    {
        IdentityKey[] ids = new IdentityKey[symbols.length];
        for (int i = 0; i < symbols.length; i++)
            ids[i] = new ConstantIdentityKey(symbols[i]);

        return stream.select(time, options, null, ids);
    }
}
