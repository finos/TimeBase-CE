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
package com.epam.deltix.qsrv.dtb.fs.local;

import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import com.epam.deltix.qsrv.dtb.fs.common.DelegatingAbstractPath;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author Alexei Osipov
 */
public class FailingPathImpl extends DelegatingAbstractPath {
    private static final Log LOG = LogFactory.getLog(FailingPathImpl.class);

    private final FailingFileSystem ffs;

    FailingPathImpl(AbstractPath delegate, FailingFileSystem ffs) {
        super(delegate);
        this.ffs = ffs;
    }

    @Override
    protected AbstractPath wrap(AbstractPath path) {
        return ffs.wrap(path);
    }

    @Override
    public AbstractFileSystem getFileSystem() {
        return ffs;
    }

    @Override
    public InputStream openInput(long offset) throws IOException {
        LOG.info("openInput %s").with(this);
        ffs.checkError(getPathString());
        return super.openInput(offset);
    }

    @Override
    public OutputStream openOutput(long size) throws IOException {
        LOG.info("openOutput %s").with(this);
        ffs.checkError(getPathString());
        return super.openOutput(size);
    }
}