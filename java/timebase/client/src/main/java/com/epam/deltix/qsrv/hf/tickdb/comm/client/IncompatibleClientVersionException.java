package com.epam.deltix.qsrv.hf.tickdb.comm.client;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;
import com.epam.deltix.qsrv.hf.tickdb.client.Version;

/**
 *
 */
public class IncompatibleClientVersionException extends RuntimeException {
    public final int            serverProtocolVersion;
    public final String         serverBuildVersion;

    public IncompatibleClientVersionException (int serverProtocolVersion, String serverBuildVersion) {
        super (
            "Server version " + serverBuildVersion + " (PV#" +
            serverProtocolVersion + ") is incompatible with this client version " +
            Version.getVersion() + " (PV#" +
            TDBProtocol.VERSION + "). Connection was refused by server."
        );
        
        this.serverProtocolVersion = serverProtocolVersion;
        this.serverBuildVersion = serverBuildVersion;
    }
}
