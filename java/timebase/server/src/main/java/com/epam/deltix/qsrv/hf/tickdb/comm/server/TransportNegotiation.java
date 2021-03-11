package com.epam.deltix.qsrv.hf.tickdb.comm.server;

import com.epam.deltix.qsrv.hf.tickdb.comm.TDBProtocol;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Alexei Osipov
 */
public class TransportNegotiation {
    private TransportNegotiation() {
    }

    public static void writeSelectedTransport(int version, DataOutputStream dout, int transportType) throws IOException {
        boolean aeronSupported = version >= TDBProtocol.AERON_SUPPORT_VERSION;
        if (aeronSupported) {
            dout.write(transportType);
        }
    }
}
