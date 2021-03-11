package com.epam.deltix.util.vsocket;

import javax.net.ssl.SSLContext;

public class TLSContext {

    public SSLContext       context;
    public final String     protocol = "TLS";

    // disables SSL communication for loopback connections
    public boolean          preserveLoopback = true;

    // port for SSL connections
    public int              port;

    public TLSContext(boolean preserveLoopback, int port) {
        this.preserveLoopback = preserveLoopback;
        this.port = port;
    }
}
