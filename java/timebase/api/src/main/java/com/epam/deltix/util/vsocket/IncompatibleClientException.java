package com.epam.deltix.util.vsocket;

import java.io.IOException;

/**
 *
 */
public class IncompatibleClientException extends IOException {
    public final String             serverId;
    public final int                serverVersion;

    public IncompatibleClientException (String serverId, int serverVersion) {
        super ("VS protocol version " + serverVersion +
            " of server " + serverId + 
            " is incompatible; expected [" + VSClient.MIN_COMP_SERVER_VERSION +
            " .. " + VSClient.MAX_COMP_SERVER_VERSION + "]"
        );
        
        this.serverId = serverId;
        this.serverVersion = serverVersion;
    }
}
