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

public enum SecretsStorage {
    INSTANCE;

    public static boolean isSecretsStorageValue(String value) {
        return VaultStorage.isVaultValue(value);
    }

    private final VaultStorageFactory vaultFactory = new VaultStorageFactory();
    private VaultStorage vault;

    public void init() {
        initVault();
    }

    public void init(SecretsStorageConfig config) {
        if (config.getVaultConfig() != null) {
            initVault(config.getVaultConfig());
        }
    }

    public String getSecret(String secretUri) {
        if (VaultStorage.isVaultValue(secretUri)) {
            return getOrInitVault().getSecret(secretUri);
        }

        return secretUri;
    }

    private synchronized VaultStorage getOrInitVault() {
        initVault();
        return vault;
    }

    private synchronized void initVault() {
        if (vault == null) {
            vault = vaultFactory.create();
        }
    }

    private synchronized void initVault(VaultConfig config) {
        if (vault == null) {
            vault = vaultFactory.create(config);
        }
    }

    public synchronized void close() {
        vault = null;
    }

}