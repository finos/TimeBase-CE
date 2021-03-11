package com.epam.deltix.qsrv.dtb.fs.hdfs;

import com.epam.deltix.qsrv.dtb.fs.pub.*;
import com.epam.deltix.util.lang.Util;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.util.Arrays;

class FilePathImpl implements AbstractPath {

    final String            pathString;
    final Path              path;
    final DistributedFS     dfs;

    FilePathImpl(Path path, DistributedFS dfs) {
        this.path = path;
        this.pathString = path.toString(); // avoid further allocs (using pathString for object identity)
        this.dfs = dfs;
    }

    FilePathImpl(String path, DistributedFS dfs) {
        this (new Path(dfs.delegate.getUri() + path), dfs);
    }

    @Override
    public AbstractFileSystem getFileSystem () {
        return (dfs);
    }

    @Override
    public String getPathString () {
        return pathString;
    }
    
    @Override
    public String getName() {
        return path.getName();
    }

    @Override
    public String[] listFolder() throws IOException {
        FileStatus[] files = dfs.delegate.listStatus(path);

        String[] names = new String[files.length];

        for (int i = 0; i < files.length; i++)
            names[i] = files[i].getPath().getName();

        Arrays.sort(names);
        return names;
    }

    @Override
    public AbstractPath append(String name) {
        return new FilePathImpl(new Path(path, name), dfs);
    }

    @Override
    public AbstractPath getParentPath() {
        return new FilePathImpl(path.getParent(), dfs);
    }

    @Override
    public InputStream openInput(long offset) throws IOException {
        FSDataInputStream in = dfs.delegate.open(path);
        in.seek(offset);

        return in;
    }

    @Override
    public OutputStream openOutput(long size) throws IOException {
        if (!dfs.delegate.exists(path))
            return dfs.delegate.create(path);

        if (size > 0)
            throw new UnsupportedOperationException("Append is not supported.");

        // recreate file
        Path tmp = new Path(path.getParent(), path.getName() + "." + System.currentTimeMillis());
        if (!dfs.delegate.rename(path, tmp))
            throw new IllegalStateException("Cannot recreate file: " + path);

        dfs.delegate.delete(tmp, true);

        return dfs.delegate.create(path);
    }

    @Override
    public long length() {
        try {
            return dfs.delegate.getContentSummary(path).getLength();
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean isFile() {
        try {
            return dfs.delegate.isFile(path);
        } catch (FileNotFoundException x) {
            return false;
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean isFolder() {
        try {
            return exists() && dfs.delegate.getFileStatus(path).isDirectory();
        } catch (FileNotFoundException x) {
            return false;
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public boolean exists() {
        try {
            return dfs.delegate.exists(path);
        } catch (IOException e) {
            throw new com.epam.deltix.util.io.UncheckedIOException(e);
        }
    }

    @Override
    public void makeFolder() throws IOException {
        dfs.delegate.mkdirs(path);
    }

    @Override
    public void makeFolderRecursive() throws IOException {
        dfs.delegate.mkdirs(path);
    }

    @Override
    public void moveTo(AbstractPath newPath) throws IOException {
        if (!dfs.delegate.rename(path, ((FilePathImpl) Util.unwrap(newPath)).path))
            throw new IOException ("Failed to rename " + this + " --> " + newPath);
    }

    @Override
    public AbstractPath renameTo(String newName) throws IOException {
        Path out = new Path(path.getParent(), newName);
        if (!dfs.delegate.rename(path, out))
            throw new IOException ("Failed to rename " + this + " --> " + newName);

        return new FilePathImpl(out, dfs);
    }

    @Override
    public void deleteExisting() throws IOException {
        if (!dfs.delegate.delete(path, true))
            throw new IOException ("Failed to delete " + this);
    }

    @Override
    public String toString() {
        return dfs.delegate + ":" + path;
    }

    @Override
    public void deleteIfExists() throws IOException {
        if (exists() && !dfs.delegate.delete(path, true))
            throw new IOException ("Failed to delete " + this);
    }
}
