package com.epam.deltix.qsrv.hf.topic.loader;

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.qsrv.hf.topic.DirectProtocol;
import com.epam.deltix.util.memory.MemoryDataOutput;

/**
 * Defines some message serialization operations that may be used for tests.
 *
 * @author Alexei Osipov
 */
@VisibleForTesting
public class DirectTopicLoaderCodec {

    @VisibleForTesting
    public static void writeSingleEntryInstrumentMetadata(MemoryDataOutput mdo, CharSequence symbol, int entityIndex) {
        mdo.writeByte(DirectProtocol.CODE_METADATA);
        mdo.writeInt(1); // Record count
        mdo.writeInt(entityIndex);
        mdo.writeStringNonNull(symbol);
    }
}
