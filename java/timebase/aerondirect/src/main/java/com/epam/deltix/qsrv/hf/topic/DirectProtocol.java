package com.epam.deltix.qsrv.hf.topic;

import org.agrona.BitUtil;

/**
 * Communication types:
 * <ul>
 *     <li>A) From Loader to Consumer - Metadata and Message Data - high traffic. Low latency required. Aeron.</li>
 *     <li>B) From Loader to Server - Metadata (temporary entity ids), latency can be high. VSChannel.</li>
 *     <li>C) From Server to Loader - Metadata (permanent entity ids), latency can be high. Aeron (we could use sockets but we need fast non-blocking implementation).</li>
 *     <li>D) From Loader to Server and from Server to Loader - new loader registration process. Executed once per loader.</li>
 *     <li>E) From Consumer to Server and from Server to Consumer - new consumer registration process. Executed once per consumer.</li>
 * </ul>
 *
 * Conventions:
 * <ul>
 *     <li>Consumer reads data only from E-type channel.</li>
 *     <li>Loader must send permanent entity index to client as soon as it known to loader. That's not server's responsibility.</li>
 *     <li>Server always responds to loader with all requested symbols</li>
 *
 *     <li>Types are defined upon topic creation and can't be changed</li>
 *     <li>Entities can be initialized upon topic creation. So all loaders will know such entities upon start.</li>
 *     <li>Entities can be added/updated upon loader creation. Each loader get full list of known entities upon start.</li>
 *     <li>Entities can be added by any loader on the fly. Such entities get temporary ID. Loader is responsible to send that temporary mapping to topic server. Topic server will assign permanent id</li>
 * </ul>
 *
 *
 *
 * @author Alexei Osipov
 */
public class DirectProtocol {
    public static final byte CODE_MSG = 1; // Data message
    public static final byte CODE_METADATA = 2; // Metadata (mapping of entity to index)
    public static final byte CODE_TEMP_INDEX_REMOVED = 3; // Indication that specific temporary index is not used anymore
    public static final byte CODE_END_OF_STREAM = 4; // Indicated that specific loader is closed and will not publish more data

    public static final int CODE_OFFSET = BitUtil.align(0, Byte.BYTES); // 0
    public static final int AFTER_CODE_OFFSET = BitUtil.align(CODE_OFFSET + Byte.BYTES, Byte.BYTES);

    // Data message structure

    public static final int TYPE_OFFSET = BitUtil.align(AFTER_CODE_OFFSET, Byte.BYTES); // 1
    // 2 bytes wasted here
    public static final int ENTITY_OFFSET = BitUtil.align(TYPE_OFFSET + Byte.BYTES, Integer.BYTES); // 4
    public static final int TIME_OFFSET = BitUtil.align(ENTITY_OFFSET + Integer.BYTES, Long.BYTES); // 8
    public static final int DATA_OFFSET = BitUtil.align(TIME_OFFSET + Long.BYTES, Byte.BYTES); // 16
    public static final int REQUIRED_HEADER_SIZE = DATA_OFFSET;

    // Metadata message structure
    // MDI-style access so we don't have to fix alignment
    public static final int METADATA_OFFSET = BitUtil.align(AFTER_CODE_OFFSET, Byte.BYTES); // 1

    // MDI-style access so we don't have to fix alignment
    public static final int TEMP_INDEX_REMOVED_DATA_OFFSET = BitUtil.align(AFTER_CODE_OFFSET, Byte.BYTES); // 1
    public static final int MAX_PUBLISHER_NUMBER = -Byte.MIN_VALUE - 1;

    // MDI-style access so we don't have to fix alignment
    public static final int END_OF_STREAM_DATA_OFFSET = BitUtil.align(AFTER_CODE_OFFSET, Byte.BYTES); // 1

    public static int getFirstTempEntryIndex(int publisherNumber) {
        return getMaxTempEntryIndex(publisherNumber);
    }

    public static int getMinTempEntryIndex(int publisherNumber) {
        int maxPublisherNumber = -Byte.MIN_VALUE - 1;
        if (publisherNumber > maxPublisherNumber) {
            throw new IllegalArgumentException("No more than " + maxPublisherNumber + " publishers permitted");
        }
        if (publisherNumber < 1) {
            throw new IllegalArgumentException("Publisher number must start from 1");
        }
        int result = -(publisherNumber << 24 | 0x00_FF_FF_FF);
        assert isValidTempIndex(result);
        return result;
    }

    public static int getMaxTempEntryIndex(int publisherNumber) {
        int maxPublisherNumber = MAX_PUBLISHER_NUMBER;
        if (publisherNumber > maxPublisherNumber) {
            throw new IllegalArgumentException("No more than " + maxPublisherNumber + " publishers permitted");
        }
        if (publisherNumber < 1) {
            throw new IllegalArgumentException("Publisher number must start from 1");
        }
        int result = -((publisherNumber << 24) | 0x00_00_00_00);
        assert isValidTempIndex(result);
        return result;
    }

    public static int getPublisherNumberFromTempIndex(int entityIndex) {
        assert isValidTempIndex(entityIndex);
        return (-entityIndex) >>> 24;
    }

    /**
     * More strict check than in {@link #isTempIndex}.
     * The difference is that we treat value -1 as invalid temp index because it's not permitted as value.
     * This is done because we want to use value "-1" as "NOT_FOUND" value in lookups.
     */
    public static boolean isValidTempIndex(int entityIndex) {
        return entityIndex < -1;
    }

    public static boolean isTempIndex(int entityIndex) {
        return entityIndex < 0;
    }

    public static boolean isValidPermanentIndex(int entityIndex) {
        return entityIndex >= 0;
    }
}
