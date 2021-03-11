package com.epam.deltix.qsrv.hf.pub.codec;

import com.epam.deltix.qsrv.hf.pub.md.RecordClassDescriptor;
import com.epam.deltix.qsrv.hf.pub.TypeLoader;

import java.util.Map;
import java.util.HashMap;

/**
 * Not thread-safe wrapper for CodecFactory that caches results
 */
public class CachedCodecFactory {

    private final CodecFactory codecFactory;

    private Map<RecordClassDescriptor, UnboundDecoder> fixedUnboundDecoders;
    private Map<RecordClassDescriptor, BoundDecoder> fixedBoundDecoders;
    private Map<RecordClassDescriptor, FixedUnboundEncoder> fixedUnboundEncoders;

    public CachedCodecFactory(CodecFactory codecFactory) {
        this.codecFactory = codecFactory;
    }

    public UnboundDecoder createFixedUnboundDecoder(RecordClassDescriptor type) {
        UnboundDecoder result = null;
        if (fixedUnboundDecoders == null) {
            fixedUnboundDecoders = new HashMap<RecordClassDescriptor,UnboundDecoder>();
        } else {
            result = fixedUnboundDecoders.get(type);
        }

        if (result == null) {
            result = codecFactory.createFixedUnboundDecoder(type);
            fixedUnboundDecoders.put (type, result);
        }
        return result;
    }

    public BoundDecoder createFixedBoundDecoder(TypeLoader loader, RecordClassDescriptor type) {
        BoundDecoder result = null;
        if (fixedBoundDecoders == null) {
            fixedBoundDecoders = new HashMap<RecordClassDescriptor,BoundDecoder>();
        } else {
            result = fixedBoundDecoders.get(type);
        }

        if (result == null) {
            result = codecFactory.createFixedBoundDecoder(loader, type);
            fixedBoundDecoders.put (type, result);
        }
        return result;
    }

    public UnboundEncoder createPolyUnboundEncoder (RecordClassDescriptor[] types) {
        return codecFactory.createPolyUnboundEncoder(types);
    }

    public FixedUnboundEncoder createFixedUnboundEncoder(RecordClassDescriptor type) {
        FixedUnboundEncoder result = null;
        if (fixedUnboundEncoders == null) {
            fixedUnboundEncoders = new HashMap<RecordClassDescriptor,FixedUnboundEncoder>();
        } else {
            result = fixedUnboundEncoders.get(type);
        }

        if (result == null) {
            result = codecFactory.createFixedUnboundEncoder(type);
            fixedUnboundEncoders.put (type, result);
        }
        return result;
    }
}
