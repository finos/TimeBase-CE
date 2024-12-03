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

import com.bettercloud.vault.SslConfig;
import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.HealthResponse;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.util.text.Mangle;

import java.io.File;
import java.util.Map;

class HashicorpVaultStorage implements VaultStorage {

    private static final Log LOG = LogFactory.getLog(HashicorpVaultStorage.class);

    private final Vault vault;

    /**
     * Default constructor will try to create a vault connection with a default configuration if vault URI was specified.
     * In case of vault URI wasn't configured, it will not throw an exception.
     */
    HashicorpVaultStorage() {
        HashicorpVaultConfig config = HashicorpVaultConfig.defaultBuilder().build();
        if (config.isConfigured()) {
            vault = createVault(buildVaultConfig(config));
        } else {
            vault = null; // wasn't configured
        }
    }

    /**
     * Will throw an exception in case of configuration error.
     */
    HashicorpVaultStorage(HashicorpVaultConfig config) {
        vault = createVault(buildVaultConfig(config));
    }

    private VaultConfig buildVaultConfig(HashicorpVaultConfig config) {
        try {
            VaultConfig vaultConfig = new VaultConfig();
            if (config.getAddr() != null) {
                vaultConfig.address(config.getAddr());
            }

            if (config.getToken() != null) {
                vaultConfig.token(Mangle.split(config.getToken()));
            } else {
                String token = System.getenv(HashicorpVaultConfig.VAULT_TOKEN_ENV_PROP);
                if (token != null) {
                    vaultConfig.token(Mangle.split(token));
                }
            }

            if (config.getOpenTimeout() != null) {
                vaultConfig.openTimeout(config.getOpenTimeout());
            }

            if (config.getReadTimeout() != null) {
                vaultConfig.readTimeout(config.getReadTimeout());
            }

            if (config.isSslConfigured()) {
                SslConfig sslConfig = new SslConfig();
                if (config.isSslVerify() != null) {
                    sslConfig.verify(config.isSslVerify());
                }
                if (config.getSslCert() != null && !config.getSslCert().isEmpty()) {
                    sslConfig.pemFile(new File(config.getSslCert()));
                }
                vaultConfig.sslConfig(sslConfig.build());
            }

            if (config.getEngineVersion() != null) {
                vaultConfig.engineVersion(config.getEngineVersion());
            }

            return vaultConfig.build();
        } catch (VaultException ex) {
            throw new RuntimeException("Failed to configure Vault client: " + ex.getMessage());
        }
    }

    private Vault createVault(VaultConfig config) {
        try {
            Vault vault = new Vault(config);
            healthCheck(vault, config.getAddress());
            return vault;
        } catch (VaultException ex) {
            throw new RuntimeException("Failed to initialize Vault at URI " + config.getAddress() + ": " + ex.getMessage());
        }
    }

    private void healthCheck(Vault vault, String vaultUri) throws VaultException {
        try {
            vault.auth().lookupSelf();
            LOG.info().append("Successfully connected to Vault at URI ").append(vaultUri).commit();
        } catch (Exception ex) {
            LOG.warn().append("Vault token lookup failed: ").append(ex.getMessage()).commit();
            // try to figure out why
            String vaultStatus = getVaultStatus(vault);
            LOG.warn().append("Vault at URI ").append(vaultUri).append(" is ").append(vaultStatus).commit();
        }
    }

    private String getVaultStatus(Vault vault) throws VaultException {
        HealthResponse response = vault.debug().health(true, 200, 429, 500);
        if (!response.getInitialized()) {
            return "not initialized";
        } else if (response.getSealed()) {
            return "sealed";
        } else if (response.getStandby()) {
            return "on a stand by node";
        }

        return "active";
    }

    @Override
    public String getSecret(String secretUri) {
        if (!VaultStorage.isVaultValue(secretUri)) {
            return secretUri;
        }

        if (vault == null) {
            throw new RuntimeException("Can't resolve secret value started from '" + VAULT_URI_SCHEME + "'. " +
                "Vault storage isn't configured. " +
                "Specify " + HashicorpVaultConfig.VAULT_ADDR_ENV_PROP +
                " environment variable or " + HashicorpVaultConfig.VAULT_ADDR_PROP + " system property.");
        }

        int startIndex = VAULT_URI_SCHEME.length();
        if (secretUri.charAt(startIndex) == '/') {
            startIndex++;
        }

        int index = secretUri.lastIndexOf("/");
        if (index <= startIndex) {
            throw new RuntimeException("Missing vault path in the secret URI");
        }

        String path = secretUri.substring(startIndex, index);
        String name = secretUri.substring(index + 1);
        String secret = getSecrets(path).get(name);
        if (secret == null) {
            throw new RuntimeException("Cannot find secret " + name + " at specified path " + path);
        }
        return secret;
    }

    private Map<String,String> getSecrets(String path) {
        try {
            return vault.logical().read(path).getData();
        }
        catch (VaultException ex) {
            throw new RuntimeException("Failed to read secret from vault", ex);
        }
    }

}