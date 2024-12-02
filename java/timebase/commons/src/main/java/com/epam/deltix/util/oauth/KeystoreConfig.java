package com.epam.deltix.util.oauth;

/**
 * Configuration for Keystore which supports different types of keystores.
 * If the Keystore type is PKCS12 or JKS, the {@code location}, {@code alias}, and {@code password} are required.
 * If the Keystore type is PEM, you can either specify the locations of the private key and certificate files
 * or provide the key and certificate directly as strings.
 */
public class KeystoreConfig {

    private KeystoreType type = KeystoreType.PKCS12;

    private String location;

    private String alias;

    private String password;

    private String pemKeyLocation;

    private String pemCertLocation;

    private String pemKey;

    private String pemCert;

    private KeystoreConfig() {
    }

    public KeystoreType getType() {
        return type;
    }

    public String getLocation() {
        return location;
    }

    public String getAlias() {
        return alias;
    }

    public String getPassword() {
        return password;
    }

    public String getPemKeyLocation() {
        return pemKeyLocation;
    }

    public String getPemCertLocation() {
        return pemCertLocation;
    }

    public String getPemKey() {
        return pemKey;
    }

    public String getPemCert() {
        return pemCert;
    }

    public static Builder builder() {
        return new KeystoreConfig().new Builder();
    }

    public class Builder {

        private Builder() {
        }

        /**
         * Configures the Keystore as PKCS12 type.
         *
         * @param location the location of the keystore file
         * @param alias the alias used in the keystore
         * @param password the password for the keystore
         * @return the builder
         */
        public Builder withPkcs12(String location, String alias, String password) {
            KeystoreConfig.this.type = KeystoreType.PKCS12;
            KeystoreConfig.this.location = location;
            KeystoreConfig.this.alias = alias;
            KeystoreConfig.this.password = password;
            return this;
        }

        /**
         * Configures the Keystore as JKS type.
         *
         * @param location the location of the keystore file
         * @param alias the alias used in the keystore
         * @param password the password for the keystore
         * @return the builder
         */
        public Builder withJks(String location, String alias, String password) {
            KeystoreConfig.this.type = KeystoreType.JKS;
            KeystoreConfig.this.location = location;
            KeystoreConfig.this.alias = alias;
            KeystoreConfig.this.password = password;
            return this;
        }

        /**
         * Configures the PEM Keystore with file locations.
         *
         * @param pemKeyLocation the location of the PEM private key file
         * @param pemCertLocation the location of the PEM certificate file
         * @return the builder
         */
        public Builder withPemFiles(String pemKeyLocation, String pemCertLocation) {
            KeystoreConfig.this.type = KeystoreType.PEM;
            KeystoreConfig.this.pemKeyLocation = pemKeyLocation;
            KeystoreConfig.this.pemCertLocation = pemCertLocation;
            return this;
        }

        /**
         * Configures the PEM Keystore with PEM strings.
         *
         * @param pemKey the PEM private key as a string
         * @param pemCert the PEM certificate as a string
         * @return the builder
         */
        public Builder withPem(String pemKey, String pemCert) {
            KeystoreConfig.this.type = KeystoreType.PEM;
            KeystoreConfig.this.pemKey = pemKey;
            KeystoreConfig.this.pemCert = pemCert;
            return this;
        }

        public KeystoreConfig build() {
            return KeystoreConfig.this;
        }

    }
}
