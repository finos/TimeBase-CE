package com.epam.deltix.qsrv.hf.tickdb.test;

import com.epam.deltix.qsrv.hf.tickdb.pub.DXTickDB;

public interface EmbeddedServer {

    /**
     * Starts Server and returns local server port
     * @return port associated with server
     */
    int             start() throws Exception;

    /**
     * Stops server
     */
    void            stop() throws Exception;

    /**
     * Returns Timebase running with server
     * @return Timebase instance
     */
    DXTickDB        getDB();

    /**
     * Server port
     * @return port number
     */
    int             getPort();
}
