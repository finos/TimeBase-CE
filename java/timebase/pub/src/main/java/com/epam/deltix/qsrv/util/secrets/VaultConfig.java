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

public class VaultConfig {

    public static final VaultProviderType DEFAULT_PROVIDER = VaultProviderType.HASHICORP;

    static final String VAULT_PROVIDER_ENV_PROP = "VAULT_PROVIDER";
    static final String VAULT_PROVIDER_ENV = System.getenv(VAULT_PROVIDER_ENV_PROP);
    static final String VAULT_PROVIDER_PROP = "vault.provider";
    static final String VAULT_PROVIDER = System.getProperty(VAULT_PROVIDER_PROP);

    static final String VAULT_ADDR_ENV_PROP = "VAULT_ADDR";
    static final String VAULT_ADDR_ENV = System.getenv(VAULT_ADDR_ENV_PROP);

    static final String VAULT_ADDR_PROP = "vault.addr";
    static final String VAULT_ADDR = System.getProperty(VAULT_ADDR_PROP);

    protected String addr;

    public String getAddr() {
        return addr;
    }

    public boolean isConfigured() {
        return addr != null;
    }

    public static VaultProviderType getProvider() {
        VaultProviderType provider = null;
        if (VAULT_PROVIDER != null && !VAULT_PROVIDER.isEmpty()) {
            provider = getProvider(VAULT_PROVIDER);
        } else if (VAULT_PROVIDER_ENV != null && !VAULT_PROVIDER_ENV.isEmpty()) {
            provider = getProvider(VAULT_PROVIDER_ENV);
        }

        return provider != null ? provider : VaultProviderType.HASHICORP;
    }

    public static VaultProviderType getProvider(String value) {
        if (HashicorpVaultConfig.ID.equalsIgnoreCase(value)) {
            return VaultProviderType.HASHICORP;
        } else if (AzureVaultConfig.ID.equalsIgnoreCase(value)) {
            return VaultProviderType.AZURE;
        }

        return DEFAULT_PROVIDER;
    }


}