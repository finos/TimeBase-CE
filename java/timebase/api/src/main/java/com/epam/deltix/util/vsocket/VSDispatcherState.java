package com.epam.deltix.util.vsocket;

enum VSDispatcherState {
    INITIAL, // No connection attempt made yet
    CONNECTED, // At least one connection established, no transports in "recovery" state
    RECONNECTING, // At least one transport in "recovery" state, trying to reconnect
    DISCONNECTING, // Disconnect process initiated, waiting for all shutdown-related actions to complete
    DISCONNECTED // Can be set only at the end of VSDispatcher.close() method
}