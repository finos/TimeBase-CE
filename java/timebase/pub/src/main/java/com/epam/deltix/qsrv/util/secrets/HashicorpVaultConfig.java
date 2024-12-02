/*
 * Copyright 2024 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.util.secrets;

public class HashicorpVaultConfig extends VaultConfig {

    public static final String ID = "HASHICORP";

    static final String VAULT_TOKEN_ENV_PROP = "VAULT_TOKEN";
    private static final String VAULT_TOKEN_PROP = "vault.token";
    static final String VAULT_TOKEN = System.getProperty(VAULT_TOKEN_PROP);

    private static final String VAULT_OPEN_TIMEOUT_PROP = "vault.open.timeout";
    static final Integer VAULT_OPEN_TIMEOUT = Integer.getInteger(VAULT_OPEN_TIMEOUT_PROP);

    private static final String VAULT_READ_TIMEOUT_PROP = "vault.read.timeout";
    static final Integer VAULT_READ_TIMEOUT = Integer.getInteger(VAULT_READ_TIMEOUT_PROP);

    private static final String VAULT_SSL_VERIFY_PROP = "vault.ssl.verify";
    private static final String VAULT_SSL_VERIFY_STR = System.getProperty(VAULT_SSL_VERIFY_PROP);
    static final Boolean VAULT_SSL_VERIFY = VAULT_SSL_VERIFY_STR != null ? Boolean.valueOf(VAULT_SSL_VERIFY_STR) : null;

    private static final String VAULT_SSL_CERT_PROP = "vault.ssl.cert";
    static final String VAULT_SSL_CERT = System.getProperty(VAULT_SSL_CERT_PROP);

    private static final String VAULT_ENGINE_VERSION_PROP = "vault.engine.version";
    static final Integer VAULT_ENGINE_VERSION = Integer.getInteger(VAULT_ENGINE_VERSION_PROP);


    private String token;
    private Integer openTimeout;
    private Integer readTimeout;
    private Boolean sslVerify;
    private String sslCert;
    private Integer engineVersion;

    public static Builder builder() {
        return new HashicorpVaultConfig().new Builder();
    }

    static Builder defaultBuilder() {
        return new HashicorpVaultConfig().new Builder().defaults();
    }

    private HashicorpVaultConfig() {
    }

    public String getToken() {
        return token;
    }

    public Integer getOpenTimeout() {
        return openTimeout;
    }

    public Integer getReadTimeout() {
        return readTimeout;
    }

    public Boolean isSslVerify() {
        return sslVerify;
    }

    public String getSslCert() {
        return sslCert;
    }

    public boolean isSslConfigured() {
        return sslVerify != null || (sslCert != null && !sslCert.isEmpty());
    }

    public Integer getEngineVersion() {
        return engineVersion;
    }

    public class Builder {
        private Builder() {
        }

        public Builder addr(String addr) {
            HashicorpVaultConfig.this.addr = addr;
            return this;
        }

        public Builder token(String token) {
            HashicorpVaultConfig.this.token = token;
            return this;
        }

        public Builder openTimeout(Integer openTimeout) {
            HashicorpVaultConfig.this.openTimeout = openTimeout;
            return this;
        }

        public Builder readTimeout(Integer readTimeout) {
            HashicorpVaultConfig.this.readTimeout = readTimeout;
            return this;
        }

        public Builder sslVerify(Boolean sslVerify) {
            HashicorpVaultConfig.this.sslVerify = sslVerify;
            return this;
        }

        public Builder sslCert(String sslCert) {
            HashicorpVaultConfig.this.sslCert = sslCert;
            return this;
        }

        public Builder engineVersion(Integer engineVersion) {
            HashicorpVaultConfig.this.engineVersion = engineVersion;
            return this;
        }

        public Builder defaults() {
            if (VAULT_ADDR != null) {
                addr(VAULT_ADDR);
            } else if (VAULT_ADDR_ENV != null) {
                addr(VAULT_ADDR_ENV);
            }
            token(VAULT_TOKEN);
            openTimeout(VAULT_OPEN_TIMEOUT);
            readTimeout(VAULT_READ_TIMEOUT);
            sslVerify(VAULT_SSL_VERIFY);
            sslCert(VAULT_SSL_CERT);
            engineVersion(VAULT_ENGINE_VERSION);
            return this;
        }

        public HashicorpVaultConfig build() {
            return HashicorpVaultConfig.this;
        }

    }

}