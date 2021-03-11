package com.epam.deltix.qsrv.hf.pub;

public enum ConnectionStatus {

    Connected,
    
    Disconnected,
    
    Unknown;

    public boolean isConnected() {
        return this == Connected;
    }

    public static ConnectionStatus combine (ConnectionStatus status1, ConnectionStatus status2) {
        if (status1 == Disconnected || status2 == Disconnected)
            return Disconnected;

        if (status1 == Unknown || status2 == Unknown)
            return Unknown;

        return Connected;
    }
}
