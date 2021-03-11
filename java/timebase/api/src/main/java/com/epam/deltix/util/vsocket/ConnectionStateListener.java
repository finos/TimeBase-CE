package com.epam.deltix.util.vsocket;

abstract class ConnectionStateListener {
    
    abstract void onDisconnected();

    abstract void onReconnected();

    /**
     * @return true if transport is already known to be unrecoverable
     */
    abstract boolean onTransportStopped(VSocketRecoveryInfo recoveryInfo);

    /**
     * @return true if transport was permanently lost (can't be recovered anymore)
     */
    abstract boolean onTransportBroken(VSocketRecoveryInfo recoveryInfo);
}
