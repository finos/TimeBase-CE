package com.epam.deltix.qsrv.dtb.fs;

import com.epam.deltix.gflog.*;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.FSFactory;

import java.io.IOException;

public class FSLocator {
    private static final Log LOGGER = LogFactory.getLog(FSLocator.class);

    private AbstractPath root;
    private final String rootPath;

    public FSLocator(String rootPath) {
        this.rootPath = rootPath;

        try {
            createRootPathIfEmpty();
        } catch (Exception e) {
            LOGGER.log(LogLevel.WARN).append("Error initializing FileSystem locator").append(e).commit();
        }
    }

    private void createRootPathIfEmpty() throws IOException {
        if (root == null) {
            if (rootPath == null || rootPath.isEmpty())
                throw new IllegalStateException("FileSystem location root is empty");

            root = FSFactory.createPath(rootPath);
        }
    }

    public synchronized String getPath(String name) {
        try {
            createRootPathIfEmpty();
            return root.getFileSystem().createPath(root, name).getPathString(); // add absolute path function
        } catch (IOException ioe) {
            throw new com.epam.deltix.util.io.UncheckedIOException(ioe);
        }
    }

    public synchronized String getPath(String name, String subPath) {
        try {
            createRootPathIfEmpty();
            AbstractFileSystem fs = root.getFileSystem();
            return fs.createPath(fs.createPath(root, name), subPath).getPathString(); // add absolute path function
        } catch (IOException ioe) {
            throw new com.epam.deltix.util.io.UncheckedIOException(ioe);
        }
    }
}
