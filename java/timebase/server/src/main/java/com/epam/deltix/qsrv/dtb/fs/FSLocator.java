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
package com.epam.deltix.qsrv.dtb.fs;

import com.epam.deltix.gflog.api.*;
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