package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class GetTopicInstrumentMappingResponse {
    private final List<ConstantIdentityKey> mapping;

    public GetTopicInstrumentMappingResponse(List<ConstantIdentityKey> mapping) {
        this.mapping = mapping;
    }

    public List<ConstantIdentityKey> getMapping() {
        return mapping;
    }

    public ConstantIdentityKey[] getMappingArray() {
        return getMapping().toArray(new ConstantIdentityKey[0]);
    }
}
