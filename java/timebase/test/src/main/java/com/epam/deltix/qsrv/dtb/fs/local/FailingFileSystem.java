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

import com.epam.deltix.qsrv.dtb.fs.pub.AbstractFileSystem;
import com.epam.deltix.qsrv.dtb.fs.pub.AbstractPath;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Test-only file system that can be used to emulate some file system errors (like IOExceptions).
 *
 * @author Alexei Osipov
 */
public class FailingFileSystem implements AbstractFileSystem {
    private final LocalFS delegate;

    private final Map<String, Supplier<Throwable>> failOnFileName = new HashMap<>();

    public FailingFileSystem(LocalFS delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean isAbsolutePath(String path) {
        return delegate.isAbsolutePath(path);
    }

    @Override
    public AbstractPath createPath(String path) {
        return wrap(delegate.createPath(path));
    }

    @Override
    public AbstractPath createPath(AbstractPath parent, String child) {
        return wrap(delegate.createPath(parent, child));
    }

    @Override
    public String getSeparator() {
        return delegate.getSeparator();
    }

    FailingPathImpl wrap(AbstractPath path) {
        return new FailingPathImpl(path, this);
    }

    public void failOnFileName(String fileName, Supplier<Throwable> throwableSupplier) {
        failOnFileName.put(normalize(fileName), throwableSupplier);
    }

    public void checkError(String path) {
        String normalized = normalize(path);
        for (Map.Entry<String, Supplier<Throwable>> entry : failOnFileName.entrySet()) {
            if (normalized.endsWith(entry.getKey())) {
                throwUnchecked(entry.getValue().get());
            }
        }
    }

    private static void throwUnchecked(Throwable e) {
        FailingFileSystem.throwAny(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwAny(Throwable e) throws E {
        throw (E)e;
    }

    private String normalize(String fileName) {
        return fileName.replaceAll("\\\\", "/");
    }


}