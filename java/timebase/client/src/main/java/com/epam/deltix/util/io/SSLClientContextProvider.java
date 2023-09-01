/*
 * Copyright 2023 EPAM Systems, Inc
 *
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. Licensed under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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

    // use this flag only when you need dynamically change key store, for example in UI (QSArchitect)
    private static boolean useDynamicKeystore;

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
        if (useDynamicKeystore) {
            return getDynamicSSLContext();
        }

        if (sslContext == null && error != null)
            throw new RuntimeException(error);

        return sslContext;
    }

    public static void useDynamicKeystore(boolean useDynamicKeystore) {
        SSLClientContextProvider.useDynamicKeystore = useDynamicKeystore;
}

    private static SSLContext getDynamicSSLContext() {
        String keystoreFile = System.getProperty(CLIENT_KEYSTORE_PROPNAME);
        String keystorePass = System.getProperty(CLIENT_KEYSTORE_PASS_PROPNAME);
        boolean trustAll = (System.getProperty(CLIENT_SSL_TRUST_ALL) != null);

        try {
            return SSLContextProvider.createSSLContext(keystoreFile, keystorePass, trustAll);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

}