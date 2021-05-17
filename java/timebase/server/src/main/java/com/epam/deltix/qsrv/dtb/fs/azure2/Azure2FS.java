/*
 * Copyright 2021 EPAM Systems, Inc
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
package com.epam.deltix.qsrv.dtb.fs.azure2;

import com.microsoft.azure.datalake.store.ADLStoreClient;
import com.microsoft.azure.datalake.store.oauth2.ClientCredsTokenProvider;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.util.lang.Util;

import javax.annotation.Nullable;
import java.util.Properties;


/**
 * Azure Data Lake integration that uses "azure-data-lake-store-sdk" client API.
 */
public class Azure2FS extends AzureFsBase {

    static final Log LOG = LogFactory.getLog(Azure2PathImpl.class);

    // Connection timeout (in millis)
    protected static final int CONNECT_AND_READ_TIMEOUT = Integer.getInteger(AZURE_TIMEOUT_PROP, 60) * 1000;

    ADLStoreClient                      adlsFSClient;

    private Azure2FS(ADLStoreClient adlsFSClient, @Nullable Long reopenOnSeekThreshold, int prefetchSize) {
        super(reopenOnSeekThreshold, prefetchSize);
        this.adlsFSClient = adlsFSClient;
    }

    @Override
    public AbstractPath createPath(String path) {
        return new Azure2PathImpl(path, this);
    }

    @Override
    public AbstractPath createPath(AbstractPath parent, String child) {
        Azure2PathImpl p = (Azure2PathImpl) Util.unwrap(parent);

        return new Azure2PathImpl(p.getPathString() + AZURE_PATH_SEPARATOR + child, this);
    }

    /**
     * Configures FS from System Properties.
     */
    public static Azure2FS create() {
        Properties props = System.getProperties();
        return create(props);
    }

    /**
     * Configures FS using provided properties.
     */
    public static Azure2FS create(Properties props) {
        String fullAccount = getRequiredProperty(props, AZURE_FULL_ACCOUTN_PROP);
        String clientId = getRequiredProperty(props, AZURE_CLIENT_ID_PROP);
        String authEndpoint = getRequiredProperty(props, AZURE_AUTH_TOKEN_ENDPOINT_PROP);
        String secretDef = getRequiredProperty(props, AZURE_SECRET_PROP);

        String sValue1 = props.getProperty(AZURE_REOPEN_ON_SEEK_THRESHOLD_PROP);
        Long reopenOnSeekThreshold = sValue1 != null ? Long.valueOf(sValue1) : null; // Optional

        String sValue2 = props.getProperty(AZURE_PREFETCH_SIZE_PROP);
        int prefetchSize = sValue2 != null ? Integer.valueOf(sValue2) : 0; // Optional

        return create(clientId, secretDef, fullAccount, authEndpoint, reopenOnSeekThreshold, prefetchSize);
    }

    public static Azure2FS create(String clientId, String clientSecret, String fullAccount, String authEndpoint, @Nullable Long reopenOnSeekThreshold, int prefetchSize) {
        ClientCredsTokenProvider tokenProvider = new ClientCredsTokenProvider(authEndpoint, clientId, clientSecret);
        ADLStoreClient client = ADLStoreClient.createClient(fullAccount, tokenProvider);

        return new Azure2FS(
                client,
                reopenOnSeekThreshold,
                prefetchSize
        );
    }

    public static boolean isFullAccountNameSet() {
        return System.getProperty(AZURE_FULL_ACCOUTN_PROP) != null;
    }

    public static boolean isAllRequiredPropertiesSet() {
        return isFullAccountNameSet()
                && System.getProperty(AZURE_CLIENT_ID_PROP) != null
                && System.getProperty(AZURE_AUTH_TOKEN_ENDPOINT_PROP) != null
                && System.getProperty(AZURE_SECRET_PROP) != null;
    }


    private static String getRequiredProperty(Properties props, String propertyName) {
        String value = props.getProperty(propertyName);
        if (value == null) {
            throw new RuntimeException("Property is not set: " + propertyName);
        }
        return value;
    }

    public ADLStoreClient getAzureClient() {
        return adlsFSClient;
    }

    @Override
    public boolean isReadsWithLimitPreferable() {
        return true;
    }


    @Override
    public String getSeparator() {
        return Azure2FS.AZURE_PATH_SEPARATOR;
    }
}
