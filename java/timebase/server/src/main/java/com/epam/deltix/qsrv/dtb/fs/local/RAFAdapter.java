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

import com.epam.deltix.util.io.RandomAccessFileToOutputStreamAdapter;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;

/**
 * Created by Alex Karpovich on 17/08/2020.
 */
class RAFAdapter extends RandomAccessFileToOutputStreamAdapter {
    private final boolean isExists;
    private final Path parent;

    public RAFAdapter(RandomAccessFile raf, File file) {
        super(raf);
        this.parent = file.getParentFile().toPath();
        this.isExists = file.exists();
    }

    @Override
    public void close () throws IOException {
        raf.getChannel().force(true);
        raf.close ();

        if (!isExists)
            PathImpl.fsync(parent, true);
    }
}