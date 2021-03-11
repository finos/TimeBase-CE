package com.epam.deltix.qsrv;

/**
 *
 */

public class SSLProperties extends SSLConfig {
    public boolean                  enableSSL       = false;
    public boolean                  sslForLoopback  = false;

    public SSLProperties() {
        this(false);
    }

    public SSLProperties(boolean enableSSL) {
        this(enableSSL, false);
    }

    public SSLProperties(boolean enableSSL, boolean sslForLoopback) {
        this.enableSSL = enableSSL;
        this.sslForLoopback = sslForLoopback;
    }
}
