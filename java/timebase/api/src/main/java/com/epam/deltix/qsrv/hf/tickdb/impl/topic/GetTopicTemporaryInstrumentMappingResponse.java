package com.epam.deltix.qsrv.hf.tickdb.impl.topic;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
public class GetTopicTemporaryInstrumentMappingResponse {
    private final IntegerToObjectHashMap<ConstantIdentityKey> mapping;

    public GetTopicTemporaryInstrumentMappingResponse(IntegerToObjectHashMap<ConstantIdentityKey> mapping) {
        this.mapping = mapping;
    }

    public IntegerToObjectHashMap<ConstantIdentityKey> getMapping() {
        return mapping;
    }
}
