package com.epam.deltix.util.io;

import com.epam.deltix.util.net.SSLContextProvider;

import javax.net.ssl.*;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 *
 */
public class SSLClientContextProvider {

    public static final String CLIENT_KEYSTORE_PROPNAME             = "deltix.ssl.clientKeyStore";
    public static final String CLIENT_KEYSTORE_PASS_PROPNAME        = "deltix.ssl.clientKeyStorePass";
    public static final String CLIENT_SSL_TRUST_ALL                 = "deltix.ssl.clientSSLTrustAll";

    private static SSLContext   sslContext = null;
    private static Exception    error = null;

    static {

        try {
            String keystoreFile = System.getProperty(CLIENT_KEYSTORE_PROPNAME);
            String keystorePass = System.getProperty(CLIENT_KEYSTORE_PASS_PROPNAME);
            boolean trustAll = (System.getProperty(CLIENT_SSL_TRUST_ALL) != null);

            if (keystoreFile != null && keystorePass != null) {
                sslContext = SSLContextProvider.createSSLContext(keystoreFile, keystorePass, trustAll);
            } else {
               sslContext = SSLContext.getDefault();
            }
        } catch (GeneralSecurityException | IOException e) {
            error = e;
        }
    }

    public static SSLContext getSSLContext() {
        if (sslContext == null && error != null)
            throw new RuntimeException(error);

        return sslContext;
    }

}
