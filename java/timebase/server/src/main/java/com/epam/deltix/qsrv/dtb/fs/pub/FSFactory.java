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
package com.epam.deltix.qsrv.dtb.fs.pub;

import com.google.common.annotations.VisibleForTesting;
import com.epam.deltix.qsrv.dtb.fs.azure.AzureFS;
import com.epam.deltix.qsrv.dtb.fs.azure2.Azure2FS;
import com.epam.deltix.qsrv.dtb.fs.cache.CachingFileSystem;
import com.epam.deltix.qsrv.dtb.fs.chunkcache.ChunkCachingFileSystem;
import com.epam.deltix.qsrv.dtb.fs.hdfs.DistributedFS;
import com.epam.deltix.qsrv.dtb.fs.local.LocalFS;

import java.io.IOException;
import java.util.HashMap;

public final class FSFactory {
    private static final HashMap<String, AbstractFileSystem> cache = new HashMap<>();
    private static AbstractFileSystem LOCAL_FS;

    public static final String LOCAL_PROTOCOL_ID = "file";
    public static final String HDFS_PROTOCOL_ID = "hdfs";
    public static final String S3_PROTOCOL_ID = "s3";
    public static final String AZURE_PROTOCOL_ID = "azure";
    public static final String SCHEME_SEPARATOR = "://";

    private static int  fileSize;
    private static long localFsCacheSizeInBytes = 0;
    private static long dfsCacheSizeInBytes = 0;
    private static double preallocateRatio = 0;

//    //TODO: Dependency injection
//    public static void init(QuantServiceConfig config) {
//        long ramCacheSize = config.getLong("ramCacheSize", 0);
//        if (ramCacheSize > 0) {
//            isCacheEnabled = config.getBoolean("cacheEnabled", Boolean.valueOf(System.getProperty("TimeBase.cacheEnabled", "true")));
//
//            if (isCacheEnabled)
//                cacheSize = (int) (ramCacheSize / TSRoot.MAX_FILE_SIZE_DEF);
//        }
//    }

    public static void init(int maxFileSize, long localFsCacheSizeInBytes, long dfsCacheSizeInBytes, double preallocateRatio) {
        fileSize = maxFileSize;
        FSFactory.localFsCacheSizeInBytes = localFsCacheSizeInBytes;
        FSFactory.dfsCacheSizeInBytes = dfsCacheSizeInBytes;
        FSFactory.preallocateRatio = preallocateRatio;
    }

    public static AbstractFileSystem create(String uri) throws IOException {
        if (needLocalFS(uri))
            return getLocalFS();
        else
            return getDistributedFS(uri);
    }

    public static AbstractFileSystem createNonCached(String uri) throws IOException {
        AbstractFileSystem fs = create(uri);
        if (fs instanceof CachingFileSystem) {
            return ((CachingFileSystem) fs).getNestedInstance();
        } else if (fs instanceof ChunkCachingFileSystem) {
            return ((ChunkCachingFileSystem) fs).getNestedInstance();
        } else {
            return fs;
        }
    }

    public static AbstractFileSystem getLocalFS() {
        synchronized (cache) {
            if (LOCAL_FS == null) {
                LOCAL_FS = wrapChunkedCache(new LocalFS(), localFsCacheSizeInBytes);
            }
            return LOCAL_FS;
        }
    }

    /**
     * This method allows to set local file system to a provided value.
     *
     * WARNING: This should be used only for tests.
     *
     * WARNING: This method modifies global state so it will affect all interaction.
     * If you want to reset behavior to a default then you should use {@code FSFactory.forceSetLocalFS(null)}.
     */
    @VisibleForTesting
    public static void forceSetLocalFS(AbstractFileSystem forcedLocalFS) {
        synchronized (cache) {
            LOCAL_FS = forcedLocalFS;
        }
    }

    /**
     * This method allows to set local file system to a provided value.
     * This method will wrap provided FS as it usually done with {@link LocalFS}.
     *
     * WARNING: This should be used only for tests.
     *
     * WARNING: This method modifies global state so it will affect all interaction.
     * If you want to reset behavior to a default then you should use {@code FSFactory.forceSetLocalFS(null)}.
     */
    @VisibleForTesting
    public static void forceSetLocalWrappedFS(AbstractFileSystem forcedLocalFS) {
        synchronized (cache) {
            LOCAL_FS = wrapChunkedCache(forcedLocalFS, localFsCacheSizeInBytes);
        }
    }

    public static AbstractFileSystem getDistributedFS(String uri) throws IOException {
        if (uri == null)
            throw new NullPointerException("FileSystem URI can't be null.");

        String scheme = extractScheme(uri);
        String address = extractAddress(uri);

        synchronized (cache) {
            AbstractFileSystem fs = cache.get(address);
            if (fs == null) {
                fs = createDistributedFS(scheme, address);
                if (dfsCacheSizeInBytes > 0) {
                    fs = wrapChunkedCache(fs, dfsCacheSizeInBytes);
                }
                cache.put(address, fs);
            }
            return fs;
        }
    }

    private static AbstractFileSystem   createDistributedFS(String scheme, String address) throws IOException {
        if (isAzureFSScheme(scheme)) {
            // Choose AzureFs implementation
            if (AzureFS.isAccountNameSet() && !Azure2FS.isFullAccountNameSet()) {
                // Use old Azure client
                return AzureFS.create();
            } else {
                // Use new Azure client
                return Azure2FS.create();
            }
        }

        if (isDistributedFSScheme(scheme))
            return DistributedFS.createFromUrl(scheme + SCHEME_SEPARATOR + address);

        throw new IllegalArgumentException("No supported FileSystem for scheme '" + scheme + "' (Address: '" + address + "').");
    }

    private static AbstractFileSystem wrapChunkedCache(AbstractFileSystem fs, long cacheSizeInBytes) {
        if (cacheSizeInBytes > (1 << 20)) { // 1 MB
            long cacheSizeToUse = cacheSizeInBytes * 3 / 4; // size x0.75 to leave some space for GC work in
            return new ChunkCachingFileSystem(fs, cacheSizeToUse, fileSize, preallocateRatio);
        }
        return fs;
    }

    public static AbstractPath      createPath(String uri) throws IOException {
        return create(uri).createPath(uri);
    }

    private static boolean          needLocalFS(String uri) {
        return uri == null || isLocalFSScheme(extractScheme(uri));
    }

    private static boolean          isLocalFSScheme(String scheme) {
        return scheme == null || scheme.isEmpty() || scheme.equalsIgnoreCase(LOCAL_PROTOCOL_ID);
    }

    private static boolean          isDistributedFSScheme(String scheme) {
        if (HDFS_PROTOCOL_ID.equalsIgnoreCase(scheme))
            return true;
        else if (S3_PROTOCOL_ID.equalsIgnoreCase(scheme))
            return true;

        return false;
    }

    private static boolean          isAzureFSScheme(String scheme) {
        return AZURE_PROTOCOL_ID.equalsIgnoreCase(scheme);
    }

    private static String           extractScheme(String uri) {
        int index = uri.indexOf(SCHEME_SEPARATOR);
        return (index == -1) ? null : uri.substring(0, index);
    }

    private static String           extractAddress(String uri) {
        int index = uri.indexOf(SCHEME_SEPARATOR);
        String address = uri.substring(index == -1 ? 0 : index + SCHEME_SEPARATOR.length());
        int slashIndex = address.indexOf("/");
        if (slashIndex != -1)
            address = address.substring(0, slashIndex);

        return address;
    }
}
