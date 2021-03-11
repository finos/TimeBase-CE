package com.epam.deltix.util.vsocket;

/**
 *
 */
public enum TransportType {
    SOCKET_TCP,
    @Deprecated
    AERON_IPC,
    OFFHEAP_IPC;
}
