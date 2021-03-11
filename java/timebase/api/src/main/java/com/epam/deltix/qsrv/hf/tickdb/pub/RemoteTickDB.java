package com.epam.deltix.qsrv.hf.tickdb.pub;

import com.epam.deltix.qsrv.hf.spi.conn.Disconnectable;

public interface RemoteTickDB extends DXTickDB, Disconnectable {

    int                  getTimeout();

    /**
     *  Sets timeout for socket connections, in milliseconds.
     *  A timeout of zero is interpreted as an infinite timeout.
     *
     *  By default - 5 sec for remote connections, 1 sec for local connections.
     *  @param timeout the specified timeout, in milliseconds.
     *  @see #getTimeout()
     */

    void                    setTimeout(int timeout);

    /*
     * Returns server start time. If not connected - returns -1;
     */
    long                    getServerStartTime();

    /*
     *  Return true if current connection uses authentication.
     *  Valid for "open" state only.
     */
    boolean                 isSecured();
}
