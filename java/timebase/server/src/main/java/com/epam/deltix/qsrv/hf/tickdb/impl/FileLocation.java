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
package com.epam.deltix.qsrv.hf.tickdb.impl;

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;

/**
 * Represents file on abstract file system with bound {@link File} object for local files.
 *
 * This is temporary data object for migrating APIs that work with {@link File} to {@link AbstractPath}
 * possibly backed by remote storage systems.
 *
 * @author Alexei Osipov
 */
@ParametersAreNonnullByDefault
class FileLocation {
    private final File file;
    private final AbstractPath path;
    private final boolean isLocal;

    FileLocation(@Nullable File file) {
        this.file = file;
        this.path = file != null ? FSFactory.getLocalFS().createPath(file.getPath()) : null;
        if (path != null) {
            path.setCacheMetadata(false);
        }
        this.isLocal = true;
    }

    FileLocation(AbstractPath path) {
        this.file = null;
        path.setCacheMetadata(false);
        this.path = path;
        this.isLocal = false;
    }

    @Nullable
    public File getFile() {
        return file;
    }

    @Nonnull
    public AbstractPath getPath() {
        return path;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public boolean isRemote() {
        return !isLocal;
    }
}
