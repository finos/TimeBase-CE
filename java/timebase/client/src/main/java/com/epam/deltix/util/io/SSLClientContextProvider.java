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

    private static final String CLIENT_KEYSTORE_PASS_DEFAULT        = "deltix";
    private static final String CLIENT_KEYSTORE_LOCATION            = "cert/localhost.jks";

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
                //if test keystore exists than use it (for tests only)
                File keyStore = Home.isSet() ? Home.getFile(CLIENT_KEYSTORE_LOCATION) : null;

                if (keyStore != null && keyStore.exists()) {
                    sslContext = SSLContextProvider.createSSLContext(
                        keyStore.getAbsolutePath(),
                        CLIENT_KEYSTORE_PASS_DEFAULT,
                        trustAll);
                } else //create default keystore with java cacert
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
