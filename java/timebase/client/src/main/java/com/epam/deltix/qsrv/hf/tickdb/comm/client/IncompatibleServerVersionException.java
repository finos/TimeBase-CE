package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.client.Version;

/**
 *
 */
public class IncompatibleServerVersionException extends RuntimeException {
    public final int            serverProtocolVersion;
    public final String         serverBuildVersion;

    public IncompatibleServerVersionException (int serverProtocolVersion, String serverBuildVersion) {
        super (
            "Client version " + Version.getVersion() + " (PV#" +
            TDBProtocol.VERSION + ") is incompatible with server version " +
            serverBuildVersion + " (PV#" + serverProtocolVersion +
            "). Minimum compatible server PV# is " + TDBProtocol.MIN_SERVER_VERSION
        );
        
        this.serverProtocolVersion = serverProtocolVersion;
        this.serverBuildVersion = serverBuildVersion;
    }
}
