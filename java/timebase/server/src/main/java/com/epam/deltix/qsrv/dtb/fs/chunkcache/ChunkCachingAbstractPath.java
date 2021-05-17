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
package com.epam.deltix.qsrv.dtb.fs.chunkcache;

import com.epam.deltix.qsrv.dtb.fs.common.DelegatingAbstractPath;
import com.epam.deltix.qsrv.dtb.fs.common.DelegatingOutputStream;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@NotThreadSafe
@ParametersAreNonnullByDefault
final class ChunkCachingAbstractPath extends DelegatingAbstractPath<ChunkCachingAbstractPath> {

    private final ChunkCachingFileSystem cfs;

    ChunkCachingAbstractPath(AbstractPath delegate, ChunkCachingFileSystem cfs) {
        super(delegate);
        this.cfs = cfs;
    }

    @Override
    protected ChunkCachingAbstractPath wrap(AbstractPath path) {
        return cfs.wrap(path);
    }

    @Override
    public AbstractFileSystem getFileSystem() {
        return cfs;
    }

    @Override
    public ChunkCachingAbstractPath append(String name) {
        return wrap(delegate.append(name));
    }

    @Override
    public ChunkCachingAbstractPath getParentPath() {
        return wrap(delegate.getParentPath());
    }

    @Override
    public InputStream openInput(long offset) throws IOException {
        return new ChunkCacheInputStream(this, cfs.getCache(), offset);
    }

    @Override
    public OutputStream openOutput(long size) throws IOException {
        //TODO: Make sure we return BufferedOutputStream here
        final OutputStream os = delegate.openOutput(size);
        cfs.getCache().invalidateIfExists(this);

        if (size != 0 && size <= cfs.getMaxFileSizeInBytesForWrite()) {
            // Cache write
            return new ChunkCachingOutputStream(os, this, cfs.getCache(), size);
        } else {
            return new DelegatingOutputStream(os) {
                @Override
                public void close() throws IOException {
                    super.close();
                    cfs.getCache().invalidateIfExists(ChunkCachingAbstractPath.this);
                }
            };
        }
    }

    @Override
    public void moveTo(AbstractPath newPath) throws IOException {
        // Perform move and invalidate data
        cfs.getCache().invalidateIfExists(newPath);

        try {
            delegate.moveTo(newPath);
        } finally {
            cfs.getCache().invalidateIfExists(this);
        }
    }

    @Override
    public ChunkCachingAbstractPath renameTo(String newName) throws IOException {
        final AbstractPath newPath = getParentPath().append(newName);

        ChunkCachingAbstractPath result = null;
        cfs.getCache().invalidateIfExists(newPath);
        try {
            result = cfs.wrap(delegate.renameTo(newName));
            return result;
        } finally {
            cfs.getCache().invalidateIfExists(this);
            if (result == null) {
                // Operation failed. We don't have resulting path so we have to build it.
                cfs.getCache().invalidateIfExists(newPath);
            } else {
                cfs.getCache().invalidateIfExists(result);
            }
        }
    }

    @Override
    public void deleteExisting() throws IOException {
        delegate.deleteExisting();
        cfs.getCache().invalidateIfExists(this);
    }

    @Override
    public void deleteIfExists() throws IOException {
        delegate.deleteIfExists();
        cfs.getCache().invalidateIfExists(this);
    }

    @Override
    public String toString() {
        return delegate.toString() + " (CC)";
    }
}
