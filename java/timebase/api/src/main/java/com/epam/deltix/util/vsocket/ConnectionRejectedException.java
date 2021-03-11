package com.epam.deltix.util.vsocket;

import java.io.IOException;

/**
 *
 */
public class ConnectionRejectedException extends IOException {
    public final String                 serverId;
    public final int                    errorCode;

    public ConnectionRejectedException (String serverId, int errorCode) {
        super ("Connection rejected by " + serverId + "; error code: " + errorCode);

        this.serverId = serverId;
        this.errorCode = errorCode;
    }        
}
