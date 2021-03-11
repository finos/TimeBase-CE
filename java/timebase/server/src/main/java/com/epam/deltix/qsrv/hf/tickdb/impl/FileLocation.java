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
