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

import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.util.text.Mangle;

class AzureVaultStorage implements VaultStorage {

    private static final Log LOG = LogFactory.getLog(AzureVaultStorage.class);

    private final SecretClient secretClient;

    /**
     * Default constructor will try to create a vault connection with a default configuration if vault URI was specified.
     * In case of vault URI wasn't configured, it will not throw an exception.
     */
    AzureVaultStorage() {
        AzureVaultConfig config = AzureVaultConfig.defaultBuilder().build();
        if (config.isConfigured()) {
            secretClient = createVault(config);
        } else {
            secretClient = null; // wasn't configured
        }
    }

    /**
     * Will throw an exception in case of configuration error.
     */
    AzureVaultStorage(AzureVaultConfig config) {
        secretClient = createVault(config);
    }

    private SecretClient createVault(AzureVaultConfig config) {
        try {
            String addr = config.getAddr();
            if (addr == null) {
                throw new IllegalArgumentException("Vault address wasn't specified. Specify " +
                    AzureVaultConfig.VAULT_ADDR_ENV_PROP + " environment variable or " +
                    AzureVaultConfig.VAULT_ADDR_PROP + " system property."
                );
            }

            SecretClient client;
            if (config.isClientIdAuthorization()) {
                client = new SecretClientBuilder()
                    .vaultUrl(addr)
                    .credential(
                        new ClientSecretCredentialBuilder()
                            .clientId(config.getClientId())
                            .clientSecret(Mangle.split(config.getClientSecret()))
                            .tenantId(config.getTenantId())
                            .build()
                    )
                    .buildClient();
            } else {
                client = new SecretClientBuilder()
                    .vaultUrl(addr)
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();
            }

            LOG.info().append("Azure Key Vault client created at URI ").append(addr).commit();
            return client;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public String getSecret(String secretUri) {
        if (!VaultStorage.isVaultValue(secretUri)) {
            return secretUri;
        }

        if (secretClient == null) {
            throw new RuntimeException("Can't resolve secret value started from '" + VAULT_URI_SCHEME + "'. " +
                "Vault storage isn't configured. " +
                "Specify " + AzureVaultConfig.VAULT_ADDR_ENV_PROP +
                " environment variable or " + AzureVaultConfig.VAULT_ADDR_PROP + " system property.");
        }

        int startIndex = VAULT_URI_SCHEME.length();
        if (secretUri.charAt(startIndex) == '/') {
            startIndex++;
        }

        String name = secretUri.substring(startIndex);
        String secret = getSecrets(name);
        if (secret == null) {
            throw new RuntimeException("Cannot find secret " + name);
        }
        return secret;
    }

    private String getSecrets(String name) {
        KeyVaultSecret secret = secretClient.getSecret(name);
        return secret.getValue();
    }

}