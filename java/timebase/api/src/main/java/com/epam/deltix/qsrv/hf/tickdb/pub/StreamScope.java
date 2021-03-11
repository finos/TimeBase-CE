package com.epam.deltix.qsrv.hf.tickdb.pub;

/**
 *  Determines the scope of a stream's durability, if any.
 */
public enum StreamScope {
    /**
     *  All messages are stored in TimeBase
     */
    DURABLE,

    /**
     * All messages are stored in external data file. 
     */
    EXTERNAL_FILE,

    /**
     *  The stream does not store data on disk, but its key and
     *  structure are durable.
     */
    TRANSIENT,

    /**
     *  The stream does not leave any permanent trace
     */
    RUNTIME
}