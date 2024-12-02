package com.epam.deltix.util.oauth.impl;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.IOUtils;
import com.nimbusds.jose.util.X509CertUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.epam.deltix.util.oauth.KeystoreConfig;
import com.epam.deltix.util.oauth.KeystoreType;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CertificateTokenQuery implements TokenQuery {

    private static final long EXPIRATION_TIME_MS = 10 * 60 * 1000;

    private final String url;
    private final String clientId;
    private final Map<String, String> parameters = new HashMap<>();

    private final KeystoreConfig keystoreConfig;

    private final JWK certJwk;
    private final JWSSigner jwsSigner;

    public CertificateTokenQuery(String url, String clientId, KeystoreConfig keystoreConfig, Map<String, String> parameters) {
        this.url = url;
        this.clientId = clientId;
        this.keystoreConfig = keystoreConfig;
        this.parameters.putAll(parameters);
        this.parameters.putIfAbsent("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

        KeyProvider keyProvider = createKeyProvider();
        try {
            certJwk = JWK.parse(keyProvider.certificate());
            jwsSigner = createSigner(keyProvider.privateKey());
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize key store.", t);
        }
    }

    @Override
    public Map<String, String> getParameters() {
        this.parameters.put("client_assertion", buildAssertion());
        return parameters;
    }

    private String buildAssertion() {
        JWSHeader.Builder headerBuilder = new JWSHeader.Builder(JWSAlgorithm.PS256)
            .type(new JOSEObjectType("JWT"))
            .x509CertSHA256Thumbprint(certJwk.getX509CertSHA256Thumbprint());

        Date curDate = new Date();
        Date expirationDate = new Date(curDate.getTime() + EXPIRATION_TIME_MS);
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
            .audience(url)
            .expirationTime(expirationDate)
            .issuer(clientId)
            .jwtID(UUID.randomUUID().toString())
            .notBeforeTime(curDate)
            .subject(clientId)
            .issueTime(curDate);

        SignedJWT signedJWT = new SignedJWT(
            headerBuilder.build(),
            claimsBuilder.build()
        );

        try {
            signedJWT.sign(jwsSigner);
            return signedJWT.serialize();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create assertion.", t);
        }
    }

    private KeyProvider createKeyProvider() {
        if (keystoreConfig.getType() == KeystoreType.PKCS12) {
            return new KeystoreKeyProvider(
                "PKCS12", keystoreConfig.getLocation(),
                keystoreConfig.getAlias(), keystoreConfig.getPassword()
            );
        } else if (keystoreConfig.getType() == KeystoreType.JKS) {
            return new KeystoreKeyProvider(
                "JKS", keystoreConfig.getLocation(),
                keystoreConfig.getAlias(), keystoreConfig.getPassword()
            );
        } else if (keystoreConfig.getType() == KeystoreType.PEM) {
            return new PemKeyProvider(
                keystoreConfig.getPemCertLocation(), keystoreConfig.getPemCert(),
                keystoreConfig.getPemKeyLocation(), keystoreConfig.getPemKey()
            );
        } else {
            throw new RuntimeException("Keystore type is empty");
        }
    }

    private JWSSigner createSigner(PrivateKey privateKey) {
        try {
            if (privateKey instanceof RSAPrivateKey) {
                return new RSASSASigner(privateKey);
            } else if (privateKey instanceof ECPrivateKey) {
                return new ECDSASigner((ECPrivateKey) privateKey);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create JWTSigner.", t);
        }

        throw new RuntimeException("Failed to create JWTSigner. Unknown private key format. RSA or EC required.");
    }

    private interface KeyProvider {

        PrivateKey privateKey();

        X509Certificate certificate();

    }

    private static class KeystoreKeyProvider implements KeyProvider {

        private final String alias;
        private final KeyStore keyStore;
        private final char[] keystorePassword;

        public KeystoreKeyProvider(String keystoreType, String keystorePath, String alias, String password) {
            this.alias = alias;
            this.keystorePassword = password.toCharArray();

            try {
                keyStore = KeyStore.getInstance(keystoreType);
                try (InputStream is = Files.newInputStream(Paths.get(keystorePath))) {
                    keyStore.load(is, keystorePassword);
                }
            } catch (Throwable t) {
                throw new RuntimeException("Failed to load KeyStore: " + keystorePath, t);
            }
        }

        @Override
        public PrivateKey privateKey() {
            try {
                return (PrivateKey) keyStore.getKey(alias, keystorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get private key from keystore.", e);
            }
        }

        @Override
        public X509Certificate certificate() {
            Certificate certificate;
            try {
                certificate = keyStore.getCertificate(alias);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get certificate from keystore.", e);
            }

            if (certificate instanceof X509Certificate) {
                return (X509Certificate) certificate;
            }
            throw new RuntimeException("Unknown certificate format, X.509 is required.");
        }
    }

    private static class PemKeyProvider implements KeyProvider {

        private final String pemCertLocation;
        private final String pemCert;
        private final String pemKeyLocation;
        private final String pemKey;

        public PemKeyProvider(String pemCertLocation, String pemCert, String pemKeyLocation, String pemKey) {
            this.pemCertLocation = pemCertLocation;
            this.pemCert = pemCert;
            this.pemKeyLocation = pemKeyLocation;
            this.pemKey = pemKey;
        }

        @Override
        public PrivateKey privateKey() {
            try {
                String pemEncodedKey = pemKey;
                if (pemEncodedKey == null) {
                    if (pemKeyLocation == null) {
                        throw new RuntimeException("pemKeyLocation is not specified.");
                    }
                    pemEncodedKey = IOUtils.readFileToString(new File(pemKeyLocation), StandardCharsets.UTF_8);
                }

                JWK jwk = JWK.parseFromPEMEncodedObjects(pemEncodedKey);
                if (jwk instanceof RSAKey) {
                    return jwk.toRSAKey().toKeyPair().getPrivate();
                } else if (jwk instanceof ECKey) {
                    return jwk.toECKey().toKeyPair().getPrivate();
                } else {
                    throw new RuntimeException("Invalid Key type. RSA or EC required.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get private key from keystore.", e);
            }
        }

        @Override
        public X509Certificate certificate() {
            try {
                String pemEncodedCert = pemCert;
                if (pemEncodedCert == null) {
                    if (pemCertLocation == null) {
                        throw new RuntimeException("pemCertLocation is not specified.");
                    }
                    pemEncodedCert = IOUtils.readFileToString(new File(pemCertLocation), StandardCharsets.UTF_8);
                }

                return X509CertUtils.parse(pemEncodedCert);
            } catch (Exception e) {
                throw new RuntimeException("Failed to get certificate from keystore.", e);
            }
        }
    }

}
