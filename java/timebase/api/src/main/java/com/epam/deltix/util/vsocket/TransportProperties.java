package com.epam.deltix.util.vsocket;


/**
 *
 */
public class TransportProperties {
    //public static final String      TRANSPORT_DIR_DEF = Home.getPath("temp/dxipc");

    public final TransportType      transportType;
    public final String             transportDir;

//    public TransportProperties() {
//        this(TransportType.SOCKET_TCP);
//    }

//    public TransportProperties(TransportType transportType) {
//        this(transportType, TRANSPORT_DIR_DEF);
//    }

    public TransportProperties(TransportType transportType, String transportDir) {
        this.transportType = transportType;
        this.transportDir = transportDir;
    }
}
