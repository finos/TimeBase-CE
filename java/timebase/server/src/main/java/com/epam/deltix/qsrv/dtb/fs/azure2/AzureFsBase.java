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

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;

/**
 * Base class for AzureFS implementations.
 *
 * @author Alexei Osipov
 */
public abstract class AzureFsBase implements AbstractFileSystem {


    public static final String AZURE_CLIENT_ID_PROP = "TimeBase.fileSystem.azure.clientId";
    @Deprecated
    public static final String AZURE_TENANT_ID_PROP = "TimeBase.fileSystem.azure.tenantId";
    public static final String AZURE_SECRET_PROP = "TimeBase.fileSystem.azure.secret";
    @Deprecated
    public static final String AZURE_ACCOUTN_PROP = "TimeBase.fileSystem.azure.account";
    public static final String AZURE_FULL_ACCOUTN_PROP = "TimeBase.fileSystem.azure.fullAccount";
    public static final String AZURE_TIMEOUT_PROP = "TimeBase.fileSystem.azure.timeout";
    public static final String AZURE_AUTH_TOKEN_ENDPOINT_PROP = "TimeBase.fileSystem.azure.authTokenEndpoint";
    public static final String AZURE_REOPEN_ON_SEEK_THRESHOLD_PROP = "TimeBase.fileSystem.azure.reopenOnSeekThreshold";
    public static final String AZURE_PREFETCH_SIZE_PROP = "TimeBase.fileSystem.azure.prefetchSize";
    public static final String AZURE_MAX_READ_RETRY_PROP = "TimeBase.fileSystem.azure.maxReadRetires";
    public static final String AZURE_TIME_TO_WAIT_AFTER_IOE_PROP = "TimeBase.fileSystem.azure.timeToWaitAfterIOException";

    public static final String AZURE_PATH_SEPARATOR = "/";

    private static final long DEFAULT_REOPEN_ON_SEEK_THRESHOLD = 1024 * 1024; // 1 MB


    private final long reopenOnSeekThreshold;
    private final int prefetchSize;

    protected AzureFsBase(Long reopenOnSeekThreshold, int prefetchSize) {
        this.reopenOnSeekThreshold = reopenOnSeekThreshold != null ? reopenOnSeekThreshold : DEFAULT_REOPEN_ON_SEEK_THRESHOLD;
        this.prefetchSize = prefetchSize;
    }

    @Override
    public boolean isAbsolutePath(String path) {
        return true;
    }


    @Override
    public long getReopenOnSeekThreshold() {
        return reopenOnSeekThreshold;
    }

    @Override
    public int getPrefetchSize() {
        return prefetchSize;
    }

    @Override
    public boolean isLocal() {
        return false;
    }
}