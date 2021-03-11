package com.epam.deltix.qsrv.hf.topic.consumer;

import com.epam.deltix.timebase.messages.ConstantIdentityKey;
import com.epam.deltix.util.collections.generated.IntegerToObjectHashMap;

/**
 * @author Alexei Osipov
 */
public interface MappingProvider {
    /**
     * Returns snapshot of current index to InstrumentKey mapping for the topic.
     *
     * Position in the array represents entity index. Position 0 means entity index 0.
     */
    ConstantIdentityKey[] getMappingSnapshot();

    /**
     * Returns snapshot of current index to InstrumentKey mapping for the topic.
     * Result contains a mapping between indexes and entities.
     * May include zero, one, multiple or all temporary mapping entries depending on server heuristics.
     * If the result does not contain needed entry then consumer should stop.
     *
     * @param neededTempEntityIndex entity index that need to be decoded
     * @return mapping
     */
    IntegerToObjectHashMap<ConstantIdentityKey> getTempMappingSnapshot(int neededTempEntityIndex);
}
