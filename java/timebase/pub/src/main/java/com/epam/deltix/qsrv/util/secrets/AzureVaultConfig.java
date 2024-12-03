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

public class AzureVaultConfig extends VaultConfig {

    public static final String ID = "AZURE";

    private static final String VAULT_AZURE_CLIENT_ID_PROP = "vault.azure.clientId";
    public static final String VAULT_AZURE_CLIENT_ID = System.getProperty(VAULT_AZURE_CLIENT_ID_PROP);

    private static final String VAULT_AZURE_CLIENT_SECRET_PROP = "vault.azure.clientSecret";
    public static final String VAULT_AZURE_CLIENT_SECRET = System.getProperty(VAULT_AZURE_CLIENT_SECRET_PROP);

    private static final String VAULT_AZURE_TENANT_ID_PROP = "vault.azure.tenantId";
    public static final String VAULT_AZURE_TENANT_ID = System.getProperty(VAULT_AZURE_TENANT_ID_PROP);

    private String clientId;
    private String clientSecret;
    private String tenantId;

    public static Builder builder() {
        return new AzureVaultConfig().new Builder();
    }

    static Builder defaultBuilder() {
        return new AzureVaultConfig().new Builder().defaults();
    }

    private AzureVaultConfig() {
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isClientIdAuthorization() {
        return clientId != null && !clientId.isEmpty() &&
            clientSecret != null && !clientSecret.isEmpty() &&
            tenantId != null && !tenantId.isEmpty();
    }

    public class Builder {
        private Builder() {
        }

        public Builder addr(String addr) {
            AzureVaultConfig.this.addr = addr;
            return this;
        }

        public Builder clientId(String clientId) {
            AzureVaultConfig.this.clientId = clientId;
            return this;
        }

        public Builder clientSecret(String clientSecret) {
            AzureVaultConfig.this.clientSecret = clientSecret;
            return this;
        }

        public Builder tenantId(String tenantId) {
            AzureVaultConfig.this.tenantId = tenantId;
            return this;
        }

        public Builder defaults() {
            if (VAULT_ADDR != null) {
                addr(VAULT_ADDR);
            } else if (VAULT_ADDR_ENV != null) {
                addr(VAULT_ADDR_ENV);
            }
            clientId(VAULT_AZURE_CLIENT_ID);
            clientSecret(VAULT_AZURE_CLIENT_SECRET);
            tenantId(VAULT_AZURE_TENANT_ID);
            return this;
        }

        public AzureVaultConfig build() {
            return AzureVaultConfig.this;
        }

    }

}