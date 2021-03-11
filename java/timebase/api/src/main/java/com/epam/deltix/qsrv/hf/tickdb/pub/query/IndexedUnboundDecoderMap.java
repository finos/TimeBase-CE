package com.epam.deltix.qsrv.hf.tickdb.pub.query;

import com.epam.deltix.qsrv.hf.pub.codec.*;
import com.epam.deltix.util.collections.generated.ObjectArrayList;

/**
 *  Used with message sources to quickly product {@link UnboundDecoder} instances
 *  corresponding to raw messages, by using the type index, commonly provided
 *  by {@link TypedMessageSource} and its subsclasses. This class is not
 *  thread-safe, as it is designed to work in the cursor read thread.
 */
public class IndexedUnboundDecoderMap {
    private final CodecFactory                          factory;
    private final ObjectArrayList <UnboundDecoder>      decoders =
        new ObjectArrayList <UnboundDecoder> ();

    public IndexedUnboundDecoderMap (CodecFactory factory) {
        this.factory = factory;
    }

    public UnboundDecoder       getDecoder (StreamMessageSource info) {
        int                 idx = info.getCurrentTypeIndex ();
        int                 n = decoders.size ();

        if (idx < n)
            return (decoders.getObjectNoRangeCheck (idx));

        assert idx == n : "Index jump " + n + " --> " + idx;

        UnboundDecoder      decoder =
            factory.createFixedUnboundDecoder (info.getCurrentType ());

        decoders.add (decoder);

        return (decoder);
    }
}
