package com.epam.deltix.qsrv.dtb.fs.chunkcache;

/**
 * @author Alexei Osipov
 */
enum ChunkLoadStrategy {
    ALWAYS_NO_LIMIT, // Always request stream without limit
    ALWAYS_WHAT_REQUESTED, // Always request what is needed to fulfill current request
    ALWAYS_ONE_CHUNK, // Always request only one chunk at a time
    MODE_1 // Requests data for current request or decided to use no limit depending on previous requests
}
